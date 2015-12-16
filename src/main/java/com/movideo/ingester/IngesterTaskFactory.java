package com.movideo.ingester;

import com.movideo.nextgen.common.multithreading.Task;
import com.movideo.nextgen.common.multithreading.TaskFactory;
import com.movideo.nextgen.common.queue.QueueManager;
import com.movideo.nextgen.encoder.bitcodin.tasks.CreateBitcodinJobTask;
import com.movideo.nextgen.encoder.bitcodin.tasks.PollBitcodinJobStatusTask;
import com.movideo.nextgen.encoder.bitcodin.tasks.ProcessEncodeRequestTask;
import com.movideo.nextgen.encoder.bitcodin.tasks.TaskType;

public class IngesterTaskFactory implements TaskFactory
{

	
	private QueueManager queueManager;
	

	public IngesterTaskFactory(QueueManager queueManager)
	{
		this.queueManager = queueManager;
	}
	
	@Override
	public Task createTaskInstance(String task, String jobString) throws InstantiationException
	{
		TaskType taskType = TaskType.valueOf(task);

		switch(taskType)
		{
			case PARSE_XML:
				return new ParseXMLTask(queueManager, jobString);
			
			default:
				throw new InstantiationException("invalid taskName " + task);
		}

	}

}
