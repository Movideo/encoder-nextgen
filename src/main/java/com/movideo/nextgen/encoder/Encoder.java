package com.movideo.nextgen.encoder;

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
import com.movideo.nextgen.encoder.models.EncodingJob;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Test class with main method that does all the initialization. Should be
 * converted to an orchestrator class, called by Dropbox processor
 *
 * @author yramasundaram
 */
public class Encoder
{

	private static EncodingJob job;
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

		JedisPool redisPool = new JedisPool(new JedisPoolConfig(), appConfig.getRedisConnectionString());
		RedisQueueConnectionConfig config = new RedisQueueConnectionConfig();
		config.setPool(redisPool);
		QueueManager queueManager = new RedisQueueManager(config);

		EncodeDAO encodeDAO = new EncodeDAO(appConfig.getDatabaseConnectionString(), appConfig.getDatabaseName());

		TaskFactory taskFactory = new BitcodingTaskFactory(queueManager, encodeDAO);

		log.debug("About to start threadpool manager for Bitcodin job creation");
		initMessageListener(appConfig.getCorePoolSize(), appConfig.getMaxPoolSize(), appConfig.getKeepAliveTime(), TimeUnit.MINUTES,
				TaskType.CREATE_ENCONDING_JOB.name(), Constants.REDIS_INPUT_LIST, taskFactory, queueManager);

		log.debug("About to start threadpool manager for Bitcodin job poller");
		initMessageListener(appConfig.getCorePoolSize(), appConfig.getMaxPoolSize(), appConfig.getKeepAliveTime(), TimeUnit.MINUTES,
				TaskType.POLL_ENCODING_JOB_STATUS.name(), Constants.REDIS_PENDING_LIST, taskFactory, queueManager);

//		addSampleJobs(redisPool, appConfig);

	}

	private static String getMediaUrlFromSegments(int clientId, int mediaId, String fileName)
	{
		return Constants.AZURE_INPUT_URL_PREFIX + Constants.AZURE_INPUT_BLOB_CONTAINER_PREFIX + clientId + "/" + Constants.AZURE_INPUT_BLOB_MEDIA_PATH_PREFIX + "/" + mediaId + "/" + fileName;
	}

	private static void initMessageListener(int corePoolSize, int maxPoolSize, long keepAliveTime, TimeUnit unit,
			String workerClassName, String listToWatch, TaskFactory taskFactory, QueueManager queueManager)
	{

		ThreadPoolExecutor executor = new ThreadPoolExecutor(corePoolSize, maxPoolSize, keepAliveTime, unit, new LinkedBlockingDeque<Runnable>());


		ThreadPoolManager manager = new ThreadPoolManager(queueManager, taskFactory, listToWatch, executor, workerClassName);
		manager.start();
	}


	public static void addSampleJobs(JedisPool redisPool, AppConfig appConfig) {
		Jedis jedis = redisPool.getResource();
		EncodingJob job = createSampleJobFromConfig(appConfig);
		for (int i = 0; i < appConfig.getParalleljobCountforTest(); i++)
		{
			jedis.lpush(Constants.REDIS_INPUT_LIST, job.toString());
		}

		jedis.close();
	}

	public static EncodingJob createSampleJobFromConfig(AppConfig appConfig)
	{

		String[] manifestTypes = { "mpd" };
		EncodingJob job = new EncodingJob();

		//	boolean createNewOutput = true;

		// TODO: This needs to be constructed from Dropbox processor
		job.setStatus(appConfig.getSampleJobStatus());
		job.setMediaId(appConfig.getSampleJobMediaId());
		job.setEncodingProfileId(appConfig.getSampleJobencProfileId());
		job.setClientId(appConfig.getClientId());
		job.setManifestTypes(manifestTypes);
		job.setSpeed(appConfig.getSampleJobSpeed());
		job.setInputFileName(appConfig.getSampleJobInputFile());
		// job.setDrmType(Constants.CENC_ENCRYPTION_TYPE);
		job.setProductId("1235-5678-9055");
		job.setVariant("HD");
		job.setInputFileUrl(getMediaUrlFromSegments(job.getClientId(), job.getMediaId(), job.getInputFileName()));

	/*
	 * if need to create new output, create it, else use the default one
	 * given
	 */
		//	if (createNewOutput) {
		//
		//	    int outputId = BitcodinProxy.preCreateOutputfromConfig(appConfig.getEncodedOutputStorageType(),
		//		    Constants.BITCODIN_OUTPUT_DEFAULT_NAME, Constants.AZURE_OUPUT_ACCOUNT_NAME,
		//		    Constants.AZURE_OUPUT_ACCOUNT_KEY, Constants.AZURE_OUTPUT_BLOB_CONTAINER,
		//		    appConfig.getEncodedOutputPrefix());
		//
		//	    /* fallback to default id if error */
		//	    if ((outputId == -1)) {
		//		outputId = appConfig.getSampleJobDefOutputId();
		//	    }
		//	    job.setOutputId(outputId);
		//	} else
		//	    job.setOutputId(appConfig.getSampleJobDefOutputId());

		log.debug("Sample Job: \n" + job);

		return job;
	}
}
