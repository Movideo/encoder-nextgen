package com.movideo.nextgen.encoder.concurrency;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.log4j.Logger;

import com.movideo.nextgen.encoder.common.Util;
import com.movideo.nextgen.encoder.config.Constants;
import com.movideo.nextgen.encoder.models.EncodingJob;
import com.movideo.nextgen.encoder.tasks.Task;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class RetryThreadPoolManager extends Thread {

    private static final Logger log = Logger.getLogger(RetryThreadPoolManager.class);

    JedisPool redisPool;
    String listToWatch, workerInputList, taskClassName;
    ThreadPoolExecutor executor;
    private static final int MAX_RETRIES = 3;
    private static final long MAX_WAIT_INTERVAL = 600000;

    public RetryThreadPoolManager(JedisPool redisPool, String listToWatch, ThreadPoolExecutor executor,
	    String taskClassName) {
	this.redisPool = redisPool;
	this.listToWatch = listToWatch;
	this.workerInputList = listToWatch + "_WORKING";
	this.executor = executor;
	this.taskClassName = taskClassName;
    }

    private Runnable getTaskInstance(String jobString) {
	Runnable task;
	try {

	    @SuppressWarnings("unchecked")
	    Class<Task> taskClass = (Class<Task>) Class.forName(taskClassName);
	    Constructor<Task> constructor = taskClass.getConstructor(JedisPool.class, String.class);
	    task = constructor.newInstance(redisPool, jobString);
	    return task;

	} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
		| NoSuchMethodException | SecurityException | ClassNotFoundException e) {

	    log.error("Unable to create worker threads. Exception is: \n", e);
	    System.exit(1);
	}
	return null;
    }

    public void run() {
	Jedis jedis = redisPool.getResource();

	while (true) {

	    /* New jobs */
	    while (jedis.llen(listToWatch) > 0) {

		// Assumes that the task class already knows that the
		// job source
		// is workerInputList
		final String jobString = jedis.brpoplpush(listToWatch, workerInputList, 1);

		/* Thread safety */
		if (jobString == null) {
		    continue;
		}
		// each retry job treated as independent tasks
		executor.getThreadFactory().newThread(new Thread(new Runnable() {

		    @Override
		    public void run() {
			try {
			    Runnable task;
			    EncodingJob job = Util.getBitcodinJobFromJSON(jobString);
			    job.setRetry(true);
			    //
			    if (job.getRetryCount() == MAX_RETRIES || !job.isRetry()) {

				log.fatal("The job with id : " + job.getEncodingJobId()
					+ " failed after retries. Moving it into "
					+ Constants.REDIS_JOB_IRRECOVERABLE_ERROR_LIST);
				Util.moveJobToNextList(redisPool, workerInputList,
					Constants.REDIS_JOB_IRRECOVERABLE_ERROR_LIST, jobString, jobString);
			    }
			    if (job.isRetry() && job.getRetryCount() < MAX_RETRIES) {
				long waitTime = Math.min(getWaitTimeExp(job.getRetryCount()), MAX_WAIT_INTERVAL);
				Thread.sleep(waitTime);
				job.setRetryCount(job.getRetryCount() + 1);
				log.debug("Retrying the job : " + job.getEncodingJobId() + " : Retrying count"
					+ job.getRetryCount());
				String updatedJobString = job.toString();
				task = getTaskInstance(updatedJobString);
				if (task == null) {
				    log.fatal("ThreadPoolManager : run() -> Cannot instantiate worker");
				    return;
				}

				executor.submit(task);

			    }
			} catch (InterruptedException e) {
			    log.error("Interrupted exception occured at RetryThreadPoolManager");
			}
		    }
		})).start();
	    }
	}
    }

    public static long getWaitTimeExp(int retryCount) {

	long waitTime = ((long) Math.pow(2, retryCount) * 10000L);

	return waitTime;
    }
}
