package com.movideo.nextgen.common.multithreading;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.movideo.nextgen.common.queue.QueueException;
import com.movideo.nextgen.common.queue.QueueManager;

/**
 * Creates the manager thread that listens to a specific list and pushes
 * messages into the next working queue for processing. This is the thread that
 * needs to be monitored to make sure jobs are being pushed fine
 * 
 * @author yramasundaram
 *
 */
public class ThreadPoolManager extends Thread {

    private static final Logger log = LogManager.getLogger();
    private QueueManager queueManager;

    String listToWatch, workerInputList, taskClassName;
    ThreadPoolExecutor executor;

    /**
     * Construct the manager thread
     * 
     * @param manager
     *            - QueueManager that abstracts communication with the queue
     * @param listToWatch
     *            - Input list
     * @param executor
     *            - ThreadPoolExecutor to be used for submitting the tasks
     * @param taskClassName
     *            - The task type that need to be created
     */
    public ThreadPoolManager(QueueManager manager, String listToWatch, ThreadPoolExecutor executor,
	    String taskClassName) {
	this.queueManager = manager;
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
	    Constructor<Task> constructor = taskClass.getConstructor(QueueManager.class, String.class);
	    task = constructor.newInstance(queueManager, jobString);
	    return task;

	} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
		| NoSuchMethodException | SecurityException | ClassNotFoundException e) {

	    log.error("Unable to create worker threads. Exception is: \n", e);
	    System.exit(1);
	}
	return null;
    }

    public void run() {
	
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

	while (true) {

	    /* New jobs */
	    try {
		while (queueManager.getQueueLength(listToWatch) > 0) {

		// Assumes that the task class already knows that the job source
		// is workerInputList
		String jobString = (String) queueManager.moveAndReturnTopElement(listToWatch, workerInputList);

		/* Thread safety */
		if (jobString == null) {
		    continue;
		}

		task = getTaskInstance(jobString);
		if (task == null) {
		    log.fatal("Cannot instantiate worker");
		    return;
		}

		executor.submit(task);
		}
	    } catch (QueueException e) {
		// TODO Auto-generated catch block
		log.fatal("Queue Exception: " + e.getMessage());
		return;
	    }
	}
    }

}
