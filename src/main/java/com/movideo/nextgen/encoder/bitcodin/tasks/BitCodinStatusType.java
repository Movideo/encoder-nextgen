package com.movideo.nextgen.encoder.bitcodin.tasks;

/**
 * Created by msharma on 03/12/2015.
 */
public enum BitCodinStatusType
{
	CREATED("Created"), ENQUEUED("Enqueued"), INPROGRESS("In Progress"), FINISHED("Finished"), ERROR("Error");

	private String statusName;

	private BitCodinStatusType(String status)
	{
	this.statusName = status;
	}

	public String getName()
	{
	return statusName;
	}

}
