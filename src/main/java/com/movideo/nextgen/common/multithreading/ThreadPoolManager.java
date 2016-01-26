package com.movideo.nextgen.common.multithreading;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.movideo.nextgen.common.queue.QueueException;
import com.movideo.nextgen.common.queue.QueueManager;

/**
 * Creates the manager thread that listens to a specific list and pushes messages into the next working queue for processing. This is the thread that
 * needs to be monitored to make sure jobs are being pushed fine
 *
 * @author yramasundaram
 */
public class ThreadPoolManager extends Thread
{

	private static final Logger log = LogManager.getLogger();
	private final TaskFactory taskFactory;
	private QueueManager queueManager;
	private Throttler throttler;
	private boolean throttled;

	String listToWatch, workerInputList, taskName;
	ThreadPoolExecutor executor;

	/**
	 * Construct the manager thread
	 *
	 * @param listToWatch
	 *            - Input list
	 * @param executor
	 *            - ThreadPoolExecutor to be used for submitting the tasks
	 */
	public ThreadPoolManager(QueueManager queueManager, TaskFactory taskFactory, String listToWatch, ThreadPoolExecutor executor, String taskName, Throttler throttler)
	{
		this.taskFactory = taskFactory;
		this.listToWatch = listToWatch;
		this.workerInputList = listToWatch + "_WORKING";
		this.executor = executor;
		this.taskName = taskName;
		this.queueManager = queueManager;
		this.throttler = throttler;
		this.throttled = throttler == null ? false : true;
	}

	private Runnable getTaskInstance(String jobString)
	{

		try
		{

			return taskFactory.createTaskInstance(taskName, jobString);

		}
		catch(InstantiationException e)
		{

			log.error("Unable to create worker threads. Exception is: \n", e);
			System.exit(1);
		}
		return null;
	}

	private boolean isThrottleNeeded()
	{
		log.debug("About to call throttle from " + taskName);
		return(throttled ? throttler.isThrottleNeeded() : false);
	}

	private void powerNap(long time)
	{
		try
		{
			Thread.sleep(time);
		}
		catch(InterruptedException e)
		{
			log.error("Unable to sleep in this restless world!", e);
		}
	}

	private long getQueueLength()
	{
		long queueLength;
		while(true)
		{
			try
			{
				queueLength = queueManager.getQueueLength(listToWatch);
				break;
			}
			catch(Exception exception)
			{
				// This happens when the thread times out after inactivity for a long time. So sleep and resume
				log.error("Unable to get queue length for " + listToWatch + "! Attempting to retry after 5 minutes");
				log.debug(exception.getMessage());
				powerNap(5 * 60 * 1000);
				log.info("Trying to connect to " + listToWatch + " after 5 minutes sleep");
			}
		}
		return queueLength;
	}

	@Override
	public void run()
	{

		Runnable task;

		// Not required in a clustered environment

		// /* Push any left over jobs to the threadpool queue for processing */
		// long stuckJobsListLength = jedis.llen(workerInputList);
		// if(stuckJobsListLength > 0){
		// List<String> stuckJobsList = jedis.lrange(workerInputList, 0, -1);
		//
		// for (int counter = 0; counter < stuckJobsListLength; counter++){
		// task = getTaskInstance(stuckJobsList.get(counter));
		// if(task == null){
		// log.fatal("FATAL: Cannot instantiate worker");
		// return;
		// }
		//
		// executor.submit(task);
		// }
		//
		// }

		while(true)
		{

			/* New jobs */
			try
			{

				while(getQueueLength() > 0 && !isThrottleNeeded())
				{

					// Assumes that the task class already knows that the job source
					// is workerInputList
					byte[] jobBytes = (byte[]) queueManager.moveAndReturnTopElement(listToWatch, workerInputList);

					/* Thread safety */
					if(jobBytes == null || jobBytes.length == 0)
					{
						continue;
					}

					String jobString = new String(jobBytes, StandardCharsets.UTF_8);
					log.info("Job string : " + jobString);

					task = getTaskInstance(jobString);
					if(task == null)
					{
						log.fatal("Cannot instantiate worker");
						continue;
					}

					executor.submit(task);

					//If this job is throttled, introduce an artificial delay for the actual requests to get created.
					//Ex: Create Bitcodin job takes about a couple of minutes to get created because of input & output creation calls involved
					if(throttled)
					{
						log.debug("Throttled job. Taking a 5 minute power nap before pushing next job through!");
						powerNap(5 * 60 * 1000);
					}
				}

				// Throttling needed per the previous condition. Sleep for a significant amount of time and wait for jobs to free up
				while(getQueueLength() > 0 && isThrottleNeeded())
				{
					log.info("Job workers busy. Taking a 15 minute power nap");
					powerNap(15 * 60 * 1000);

				}

				while(getQueueLength() == 0)
				{
					log.debug("No jobs to process. Retrying after 5 mins");
					powerNap(5 * 60 * 1000);
				}

			}
			catch(QueueException e)
			{
				// TODO Auto-generated catch block
				log.fatal("Queue Exception: " + e.getMessage());
				System.exit(1);
			}
		}
	}

}
