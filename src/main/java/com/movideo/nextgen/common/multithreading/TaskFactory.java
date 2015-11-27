package com.movideo.nextgen.common.multithreading;

/**
 * Created by rranawaka on 26/11/2015.
 */
public interface TaskFactory
{
	Task createTaskInstance(String taskName, String jobString) throws InstantiationException;
}
