package com.movideo.nextgen.encoder.bitcodin.tasks;

import com.movideo.nextgen.common.multithreading.Task;
import com.movideo.nextgen.common.multithreading.TaskFactory;
import com.movideo.nextgen.common.queue.QueueManager;
import com.movideo.nextgen.encoder.dao.EncodeDAO;

/**
 * Created by rranawaka on 26/11/2015.
 */
public class BitcodinTaskFactory implements TaskFactory
{
	private QueueManager queueManager;
	private EncodeDAO encodeDao;

	public BitcodinTaskFactory(QueueManager queueManager, EncodeDAO encodeDao)
	{
		this.queueManager = queueManager;
		this.encodeDao = encodeDao;
	}

	@Override
	public Task createTaskInstance(String task, String jobString) throws InstantiationException
	{
		TaskType taskType = TaskType.valueOf(task);

		switch(taskType)
		{
			case CREATE_ENCONDING_JOB:
				return new CreateBitcodinJobTask(queueManager, encodeDao, jobString);
			case POLL_ENCODING_JOB_STATUS:
				return new PollBitcodinJobStatusTask(queueManager, encodeDao, jobString);
			case PROCESS_ENCODING_REQUEST:
				return new ProcessEncodeRequestTask(queueManager, jobString);
			default:
				throw new InstantiationException("invalid taskName " + task);
		}

	}
}
