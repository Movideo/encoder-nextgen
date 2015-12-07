package com.movideo.nextgen.encoder.bitcodin.tasks;

import com.movideo.nextgen.common.queue.QueueManager;
import com.movideo.nextgen.encoder.config.Constants;

public class RetryPollBitcodinJobStatusTask extends RetryJobTask
{

	public RetryPollBitcodinJobStatusTask(QueueManager queueManager, String jobString)
	{
	super(queueManager, jobString, Constants.REDIS_PENDING_LIST,
		Constants.REDIS_POLL_ERROR_WORKING_LIST, Constants.REDIS_POLL_ERROR_LIST, Constants.REDIS_FINISHED_LIST, Constants.REDIS_JOB_IRRECOVERABLE_ERROR_LIST);
	}

}
