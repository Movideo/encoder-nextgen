package com.movideo.nextgen.common.queue;

public abstract class QueueManager {

    private QueueConnectionConfig config;

    public QueueManager(QueueConnectionConfig config) {
	this.config = config;
    }

    public QueueConnectionConfig getConfig() {
	return config;
    }

    public abstract void push(String queueName, Object message) throws QueueException;

    public abstract void moveQueues(String fromQueue, String toQueue, Object message, Object newMessage)
	    throws QueueException;

    public abstract Object moveAndReturnTopElement(String fromQueue, String toQueue) throws QueueException;

    public abstract Object pop(String queueName) throws QueueException;

    public abstract void removeFromQueue(String fromQueue, Object message) throws QueueException;

    public abstract long getQueueLength(String queueName) throws QueueException;

}
