package com.movideo.ingester;

import java.util.concurrent.ThreadPoolExecutor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.movideo.nextgen.common.queue.QueueManager;

public class IngestThreadPoolManager extends Thread
{

	private static final Logger log = LogManager.getLogger();

	private QueueManager queueManager;
	ThreadPoolExecutor executor;

	public IngestThreadPoolManager(QueueManager queueManager, ThreadPoolExecutor executor)
	{
	    this.queueManager = queueManager;
	    this.executor = executor;
	}

	@Override
	public void run()
	{
	
		while(true)
		{

			
			try
			{
				

					// Assumes that the task class already knows that the job source
					// is workerInputList
				//	String jobString = (String) queueManager.moveAndReturnTopElement(listToWatch, workerInputList);

			

					IngestPoller poller = new IngestPoller(queueManager);
					

					executor.submit(poller);
				}
			
			catch(Exception e)
			{
				// TODO Auto-generated catch block
				log.fatal("Exception: " + e.getMessage());
				return;
			}
			
			try
		{
		Thread.sleep(3000);
		}
		catch(InterruptedException e)
		{
		// TODO Auto-generated catch block
		e.printStackTrace();
		}
		}
	}
	

}
