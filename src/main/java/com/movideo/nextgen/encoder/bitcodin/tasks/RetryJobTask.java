package com.movideo.nextgen.encoder.bitcodin.tasks;

import java.util.Calendar;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonSyntaxException;
import com.movideo.nextgen.common.multithreading.Task;
import com.movideo.nextgen.common.queue.QueueException;
import com.movideo.nextgen.common.queue.QueueManager;
import com.movideo.nextgen.encoder.common.Util;
import com.movideo.nextgen.encoder.config.Constants;
import com.movideo.nextgen.encoder.models.EncodingJob;

public class RetryJobTask extends Task
{
	private static final Logger log = LogManager.getLogger();
	private static final int MAX_RETRIES = 3;
	private static final int MAX_WAIT_INTERVAL = 60 * 10 * 1000;
	private Calendar calendar = Calendar.getInstance();
	private EncodingJob job;

	private String inputList, workingList, errorList, successList, irrecoverableList;

	public RetryJobTask(QueueManager queueManager, String jobString)
	{
	super(queueManager, jobString);
	}

	public RetryJobTask(QueueManager queueManager, String jobString, String inputList, String workingList,
		String errorList, String successList, String irrecoverableList)
	{
	super(queueManager, jobString);
	this.inputList = inputList;
	this.workingList = workingList;
	this.errorList = errorList;
	this.successList = successList;
	this.irrecoverableList = irrecoverableList;
	}

	@Override
	public void run()
	{
	log.debug("ErrorRetry : run() -> Executing job creator");
	try
	{
		try
		{
		job = Util.getBitcodinJobFromJSON(jobString);

		}
		catch(JsonSyntaxException e)
		{
		log.error("Could not extract bitcodin job from job string moving to irrecoverable list", e);
		queueManager.moveQueues(workingList, irrecoverableList, jobString, jobString);
		return;
		}

		job.setStatus(Constants.STATUS_JOB_RETRY);
		//TODO , the isRetry Set Logic needs to be firmed up before this can be used effectively
		if(job.isRetry() && job.getRetryCount() < MAX_RETRIES)
		{
		if(job.getRetryTime() < calendar.getTimeInMillis())
		{
			long waitTime = Util.getWaitTimeExp(job.getRetryCount());
			job.setRetryTime(waitTime + calendar.getTimeInMillis());
			job.setRetryCount(job.getRetryCount() + 1);
			queueManager.moveQueues(workingList, inputList, jobString, job.toString());
		}
		else
		{
			//Push it back to the list not yet time for this to be processed
			queueManager.moveQueues(workingList, errorList, jobString, job.toString());
		}
		}
		else
		{
		log.info("Maximum count of " + job.getRetryCount() + " reached moving to irrecoverable list");
		queueManager.moveQueues(workingList, irrecoverableList, jobString, job.toString());
		}
	}
	catch(QueueException e)
	{
		log.error("Unable to move message to the next list! Exception is: " + e.getMessage());
	}
	}

}
