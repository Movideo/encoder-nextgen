package com.movideo.nextgen.encoder;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.movideo.nextgen.common.multithreading.TaskFactory;
import com.movideo.nextgen.common.multithreading.ThreadPoolManager;
import com.movideo.nextgen.common.queue.QueueManager;
import com.movideo.nextgen.common.queue.redis.RedisQueueConnectionConfig;
import com.movideo.nextgen.common.queue.redis.RedisQueueManager;
import com.movideo.nextgen.encoder.bitcodin.tasks.BitcodingTaskFactory;
import com.movideo.nextgen.encoder.bitcodin.tasks.TaskType;
import com.movideo.nextgen.encoder.config.AppConfig;
import com.movideo.nextgen.encoder.config.Constants;
import com.movideo.nextgen.encoder.dao.EncodeDAO;
import com.movideo.nextgen.encoder.test.SampleGenerator;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Protocol;

/**
 * Test class with main method that does all the initialization. Should be converted to an orchestrator class, called by Dropbox processor
 *
 * @author yramasundaram
 */
public class Encoder
{

	private static final Logger log = LogManager.getLogger();

	public static void main(String[] args) throws InterruptedException, IOException
	{
		AppConfig appConfig;
		ClassLoader loader = Thread.currentThread().getContextClassLoader();
		try (InputStream in = loader.getResourceAsStream("config.properties"))
		{
			Properties prop = new Properties();
			prop.load(in);
			appConfig = new AppConfig(prop);
		}

		//		JedisPool redisPool = new JedisPool(new JedisPoolConfig(), appConfig.getRedisConnectionString(),
		//				appConfig.getRedisPort(), Protocol.DEFAULT_TIMEOUT, appConfig.getRedisPassword());
		JedisPool redisPool = new JedisPool(new JedisPoolConfig(), appConfig.getRedisConnectionString(),
				appConfig.getRedisPort(), Protocol.DEFAULT_TIMEOUT);
		RedisQueueConnectionConfig config = new RedisQueueConnectionConfig();
		config.setPool(redisPool);
		QueueManager queueManager = new RedisQueueManager(config);

		EncodeDAO encodeDAO = new EncodeDAO(appConfig.getDatabaseConnectionString(), appConfig.getDatabaseName());

		TaskFactory taskFactory = new BitcodingTaskFactory(queueManager, encodeDAO);

		initMessageListener(appConfig.getCorePoolSize(), appConfig.getMaxPoolSize(), appConfig.getKeepAliveTime(),
				TimeUnit.MINUTES, TaskType.PROCESS_ENCODING_REQUEST.name(), Constants.REDIS_ENCODE_REQUEST_LIST, taskFactory, queueManager);

		log.debug("About to start threadpool manager for Bitcodin job creation");
		initMessageListener(appConfig.getCorePoolSize(), appConfig.getMaxPoolSize(), appConfig.getKeepAliveTime(), TimeUnit.MINUTES,
				TaskType.CREATE_ENCONDING_JOB.name(), Constants.REDIS_INPUT_LIST, taskFactory, queueManager);

		log.debug("About to start threadpool manager for Bitcodin job poller");
		initMessageListener(appConfig.getCorePoolSize(), appConfig.getMaxPoolSize(), appConfig.getKeepAliveTime(), TimeUnit.MINUTES,
				TaskType.POLL_ENCODING_JOB_STATUS.name(), Constants.REDIS_PENDING_LIST, taskFactory, queueManager);

		//		addSampleJobs(redisPool, appConfig);

		SampleGenerator.addSampleRequest(redisPool, appConfig);

	}

	private static void initMessageListener(int corePoolSize, int maxPoolSize, long keepAliveTime, TimeUnit unit,
			String workerClassName, String listToWatch, TaskFactory taskFactory, QueueManager queueManager)
	{

		ThreadPoolExecutor executor = new ThreadPoolExecutor(corePoolSize, maxPoolSize, keepAliveTime, unit, new LinkedBlockingDeque<Runnable>());

		ThreadPoolManager manager = new ThreadPoolManager(queueManager, taskFactory, listToWatch, executor, workerClassName);
		manager.start();
	}

}
