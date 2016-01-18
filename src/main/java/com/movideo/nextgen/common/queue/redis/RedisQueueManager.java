package com.movideo.nextgen.common.queue.redis;

import java.nio.charset.StandardCharsets;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.movideo.nextgen.common.queue.QueueConnectionConfig;
import com.movideo.nextgen.common.queue.QueueException;
import com.movideo.nextgen.common.queue.QueueManager;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class RedisQueueManager extends QueueManager
{
	private static JedisPool pool;
	private static final Logger log = LogManager.getLogger();

	public RedisQueueManager(QueueConnectionConfig config)
	{
		super(config);
		if(pool == null)
		{
			pool = ((RedisQueueConnectionConfig) config).getPool();
		}
		log.info("RedisQueueManager initialized. Active count in pool: " + pool.getNumActive());
	}

	@Override
	public void push(String queueName, Object message) throws QueueException
	{
		try (Jedis jedis = pool.getResource())
		{
			jedis.lpush(queueName, message.toString());
		}
	}

	@Override
	public void moveQueues(String fromQueue, String toQueue, Object message, Object newMessage) throws QueueException
	{

		try (Jedis jedis = pool.getResource())
		{
			log.debug("About to move message between lists");
			String valueToBeRemoved = ((String) message).replaceAll("\"", "\\\"");
			log.debug("Value to be removed: " + valueToBeRemoved);
			jedis.lpush(toQueue, newMessage == null ? (String) message : (String) newMessage);
			Long deleteCount = jedis.lrem(fromQueue, 1, valueToBeRemoved);
			log.debug("Count of values removed: " + deleteCount);
		}
	}

	@Override
	public Object moveAndReturnTopElement(String fromQueue, String toQueue) throws QueueException
	{
		try (Jedis jedis = pool.getResource())
		{
			return new String(jedis.brpoplpush(fromQueue.getBytes(), toQueue.getBytes(), 1), StandardCharsets.UTF_8);
		}
	}

	@Override
	public Object pop(String queueName) throws QueueException
	{
		try (Jedis jedis = pool.getResource())
		{
			return jedis.rpop(queueName);
		}
	}

	@Override
	public void removeFromQueue(String fromQueue, Object message) throws QueueException
	{
		try (Jedis jedis = pool.getResource())
		{
			String valueToBeRemoved = ((String) message).replaceAll("\"", "\\\"");
			jedis.lrem(fromQueue, 1, valueToBeRemoved);
		}

	}

	@Override
	public long getQueueLength(String queueName) throws QueueException
	{
		try (Jedis jedis = pool.getResource())
		{
			return jedis.llen(queueName);
		}
	}

}
