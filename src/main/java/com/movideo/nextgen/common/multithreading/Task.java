package com.movideo.nextgen.common.multithreading;

import java.nio.charset.StandardCharsets;

import com.movideo.nextgen.common.queue.QueueManager;

/**
 * Super class for all Tasks. Can implement Callable instead of Runnable, but we don't have the need to return anything or throw checked exceptions.
 * Might be handy to implement Callable when converting this to a library.
 * 
 * @author yramasundaram
 */
public class Task implements Runnable
{

	protected QueueManager queueManager;
	protected byte[] jobStringAsBytes;

	public Task(QueueManager manager, String jobString)
	{
		this.queueManager = manager;
		this.jobStringAsBytes = jobString.getBytes(StandardCharsets.UTF_8);
	}

	@Override
	public void run()
	{
	}

}
