package com.movideo.nextgen.encoder;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.FileBasedConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.movideo.nextgen.common.multithreading.TaskFactory;
import com.movideo.nextgen.common.multithreading.ThreadPoolManager;
import com.movideo.nextgen.common.queue.QueueManager;
import com.movideo.nextgen.common.queue.redis.RedisQueueConnectionConfig;
import com.movideo.nextgen.common.queue.redis.RedisQueueManager;
import com.movideo.nextgen.encoder.bitcodin.concurrency.BitcodinThrottler;
import com.movideo.nextgen.encoder.bitcodin.tasks.BitcodinTaskFactory;
import com.movideo.nextgen.encoder.bitcodin.tasks.TaskType;
import com.movideo.nextgen.encoder.common.Util;
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
	private static Configuration applicationConfig;

	public static void main(String[] args) throws InterruptedException, IOException
	{
		initProperties();

		if(applicationConfig.size() == 0)
		{
			printErrorAndExit("Could not load properties. Aborting now!");
		}
		Util.setApplicationConfig(applicationConfig);
		JedisPool redisPool;
		final String environment = Util.getConfigProperty("environment.type");

		int corePoolSize = Integer.parseInt(Util.getConfigProperty("threadpool.corePoolSize")),
				maxPoolSize = Integer.parseInt(Util.getConfigProperty("threadpool.maxPoolSize")),
				keepAliveTime = Integer.parseInt(Util.getConfigProperty("threadpool.keepAliveTime"));

		if(environment.equalsIgnoreCase("development"))
		{
			redisPool = new JedisPool(new JedisPoolConfig(), Util.getConfigProperty("redis.host"),
					Integer.parseInt(Util.getConfigProperty("redis.port")), Protocol.DEFAULT_TIMEOUT);
		}
		else
		{
			redisPool = new JedisPool(new JedisPoolConfig(), Util.getConfigProperty("redis.host"),
					Integer.parseInt(Util.getConfigProperty("redis.port")), Protocol.DEFAULT_TIMEOUT, Util.getConfigProperty("redis.password"));
		}

		RedisQueueConnectionConfig config = new RedisQueueConnectionConfig();
		config.setPool(redisPool);
		QueueManager queueManager = new RedisQueueManager(config);

		EncodeDAO encodeDAO = new EncodeDAO(Util.getConfigProperty("couch.url"), Util.getConfigProperty("couch.dbName"));

		TaskFactory taskFactory = new BitcodinTaskFactory(queueManager, encodeDAO);

		log.debug("About to start threadpool manager for Bitcodin job creation");
		initMessageListener(corePoolSize, maxPoolSize, keepAliveTime,
				TimeUnit.MINUTES, TaskType.PROCESS_ENCODING_REQUEST.name(), Util.getConfigProperty("redis.encodeOrchestrator.input.list"), taskFactory, queueManager);

		initMessageListener(corePoolSize, maxPoolSize, keepAliveTime, TimeUnit.MINUTES,
				TaskType.CREATE_ENCONDING_JOB.name(), Util.getConfigProperty("redis.encoder.input.list"), taskFactory, queueManager);

		log.debug("About to start threadpool manager for Bitcodin job poller");
		initMessageListener(corePoolSize, maxPoolSize, keepAliveTime, TimeUnit.MINUTES,
				TaskType.POLL_ENCODING_JOB_STATUS.name(), Util.getConfigProperty("redis.poller.input.list"), taskFactory, queueManager);

		SampleGenerator.addSampleRequest(redisPool);

	}

	private static void initMessageListener(int corePoolSize, int maxPoolSize, long keepAliveTime, TimeUnit unit,
			String workerClassName, String listToWatch, TaskFactory taskFactory, QueueManager queueManager)
	{

		BitcodinThrottler throttler = workerClassName.equals(TaskType.CREATE_ENCONDING_JOB.name()) ? new BitcodinThrottler() : null;
		ThreadPoolExecutor executor = new ThreadPoolExecutor(corePoolSize, maxPoolSize, keepAliveTime, unit, new LinkedBlockingDeque<Runnable>());

		ThreadPoolManager manager = new ThreadPoolManager(queueManager, taskFactory, listToWatch, executor, workerClassName, throttler);
		manager.start();
	}

	private static void initProperties() throws IOException
	{
		String configPath = System.getenv("ENCODER_CONFIG_FILE_PATH");
		//String configPath = "/Users/yramasundaram/";
		if(configPath == null)
		{
			printErrorAndExit("Could load application properties. Aborting now!");
		}
		Parameters params = new Parameters();
		FileBasedConfigurationBuilder<FileBasedConfiguration> builder = new FileBasedConfigurationBuilder<FileBasedConfiguration>(PropertiesConfiguration.class)
				.configure(params.properties()
						.setFileName(configPath + "config.properties"));
		try
		{
			applicationConfig = builder.getConfiguration();
			log.info("Successfully loaded properties. Environment: " + applicationConfig.getString("environment.type"));

		}
		catch(ConfigurationException cex)
		{
			printErrorAndExit("Could load application properties. Aborting now!");
		}
	}

	private static void printErrorAndExit(String errMessage)
	{
		log.fatal(errMessage);
		System.out.println(errMessage);
		System.exit(1);
	}
}
