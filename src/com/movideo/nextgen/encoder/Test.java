package com.movideo.nextgen.encoder;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import com.movideo.nextgen.encoder.bitcodin.BitcodinProxy;
import com.movideo.nextgen.encoder.bitcodin.tasks.CreateBitcodinJob;
import com.movideo.nextgen.encoder.bitcodin.tasks.PollBitcodinJobStatus;
import com.movideo.nextgen.encoder.concurrency.ThreadPoolManager;
import com.movideo.nextgen.encoder.config.AppConfig;
import com.movideo.nextgen.encoder.config.Constants;
import com.movideo.nextgen.encoder.models.EncodingJob;

/**
 * Test class with main method that does all the initialization. Should be
 * converted to an orchestrator class, called by Dropbox processor
 * 
 * @author yramasundaram
 *
 */
public class Test {

	private static EncodingJob job;
	private static JedisPool redisPool;

	public static void main(String[] args) throws InterruptedException {

		AppConfig appConfig = new AppConfig("config.properties");

		redisPool = new JedisPool(new JedisPoolConfig(),
				appConfig.getRedisConnectionString());
		Jedis jedis = redisPool.getResource();

		job = createSampleJobFromConfig(appConfig);

		initMessageListener(appConfig.getCorePoolSize(),
				appConfig.getMaxPoolSize(), appConfig.getKeepAliveTime(),
				TimeUnit.MINUTES, CreateBitcodinJob.class.getName(),
				Constants.REDIS_INPUT_LIST);
		initMessageListener(appConfig.getCorePoolSize(),
				appConfig.getMaxPoolSize(), appConfig.getKeepAliveTime(),
				TimeUnit.MINUTES, PollBitcodinJobStatus.class.getName(),
				Constants.REDIS_PENDING_LIST);

		for (int i = 0; i < appConfig.getParalleljobCountforTest(); i++) {
			jedis.lpush(Constants.REDIS_INPUT_LIST, job.toString());
			// Thread.sleep(500);
		}

	}

	private static String getMediaUrlFromSegments(int clientId, int mediaId,
			String fileName) {
		return Constants.AZURE_INPUT_URL_PREFIX
				+ Constants.AZURE_INPUT_BLOB_CONTAINER_PREFIX + clientId + "/"
				+ Constants.AZURE_INPUT_BLOB_MEDIA_PATH_PREFIX + "/" + mediaId
				+ "/" + fileName;
	}

	private static void initMessageListener(int corePoolSize, int maxPoolSize,
			long keepAliveTime, TimeUnit unit, String workerClassName,
			String listToWatch) {

		ThreadPoolExecutor executor = new ThreadPoolExecutor(corePoolSize,
				maxPoolSize, keepAliveTime, unit,
				new LinkedBlockingDeque<Runnable>());
		ThreadPoolManager manager = new ThreadPoolManager(redisPool,
				listToWatch, executor, workerClassName);
		manager.start();
	}

	public static EncodingJob createSampleJobFromConfig(AppConfig appConfig) {

		String[] manifestTypes = { "mpd" };
		EncodingJob job = new EncodingJob();

		boolean bCreateNewOutput = true;

		// TODO: This needs to be constructed from Dropbox processor
		job.setStatus(appConfig.getSampleJobStatus());
		job.setMediaId(appConfig.getSampleJobMediaId());
		job.setEncodingProfileId(appConfig.getSampleJobencProfileId());
		job.setClientId(appConfig.getClientId());
		job.setManifestTypes(manifestTypes);
		job.setSpeed(appConfig.getSampleJobSpeed());
		job.setInputFileName(appConfig.getSampleJobInputFile());
		job.setDrmType(appConfig.getDrmType());
		job.setVariant(appConfig.getVariant());
		job.setProductId(appConfig.getProductId());
		job.setInputFileUrl(getMediaUrlFromSegments(job.getClientId(),
				job.getMediaId(), job.getInputFileName()));

		/*
		 * if need to create new output, create it, else use the default one
		 * given
		 */
		if (bCreateNewOutput) {

			int outputId = BitcodinProxy.preCreateOutputfromConfig(
					appConfig.getEncodedOutputStorageType(),
					Constants.BITCODIN_OUTPUT_DEFAULT_NAME,
					Constants.AZURE_OUPUT_ACCOUNT_NAME,
					Constants.AZURE_OUPUT_ACCOUNT_KEY,
					Constants.AZURE_OUTPUT_BLOB_CONTAINER,
					appConfig.getEncodedOutputPrefix());

			/* fallback to default id if error */
			if ((outputId == -1)) {
				outputId = appConfig.getSampleJobDefOutputId();
			}
			job.setOutputId(outputId);
		} else
			job.setOutputId(appConfig.getSampleJobDefOutputId());

		return job;
	}
}
