package com.movideo.nextgen.encoder;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.movideo.nextgen.encoder.bitcodin.tasks.CreateBitcodinJob;
import com.movideo.nextgen.encoder.bitcodin.tasks.PollBitcodinJobStatus;
import com.movideo.nextgen.encoder.concurrency.ThreadPoolManager;
import com.movideo.nextgen.encoder.config.Constants;
import com.movideo.nextgen.encoder.models.EncodingJob;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * Test class with main method that does all the initialization.
 * Should be converted to an orchestrator class, called by Dropbox processor
 * @author yramasundaram
 *
 */
public class Test {
	
	private static EncodingJob job;
	private static JedisPool redisPool;

	public static void main(String[] args) throws InterruptedException {
	
		//TODO: Config
		int corePoolSize = 5;
		int maxPoolSize = 10;
		long keepAliveTime = 1;
		TimeUnit unit = TimeUnit.MINUTES;

		redisPool = new JedisPool(new JedisPoolConfig(), Constants.REDIS_CONNECTION_STRING);
		Jedis jedis = redisPool.getResource();		
		String[] manifestTypes = {"mpd"};
		job = new EncodingJob();
	
		//TODO: This needs to be constructed from Dropbox processor
		job.setStatus(Constants.STATUS_NEW);
		job.setMediaId(837935);
		job.setEncodingProfileId(35364);
		job.setOutputId(19496);
		job.setClientId(524);
		//Except for input ID, there is no need to create any of the other ids (encoding, output) per request.
		//job.setInputId(45879);
		job.setManifestTypes(manifestTypes);
		job.setSpeed("premium");
		job.setInputFileName("ForYourIceOnly.mp4");
		job.setInputFileUrl(getMediaUrlFromSegments(job.getClientId(), job.getMediaId(), job.getInputFileName()));
		job.setDrmType(Constants.CENC_ENCRYPTION_TYPE);
		job.setProductId("1234-5678-9012");
		job.setVariant("HD");
		
		initMessageListener(corePoolSize, maxPoolSize, keepAliveTime, unit, CreateBitcodinJob.class.getName(), Constants.REDIS_INPUT_LIST);
		initMessageListener(corePoolSize, maxPoolSize, keepAliveTime, unit, PollBitcodinJobStatus.class.getName(), Constants.REDIS_PENDING_LIST);
		
		System.out.println("About to push job to input list \n" + job.toString());
	
		//for(int i = 0; i < 50; i++){
			jedis.lpush(Constants.REDIS_INPUT_LIST, job.toString());
			//Thread.sleep(500);
		//}		
		
	}
	
	private static String getMediaUrlFromSegments(int clientId, int mediaId, String fileName){
		return Constants.AZURE_INPUT_URL_PREFIX + Constants.AZURE_INPUT_BLOB_CONTAINER_PREFIX + clientId + "/" + Constants.AZURE_INPUT_BLOB_MEDIA_PATH_PREFIX + "/" +  mediaId + "/" + fileName;
	}
	
	private static void initMessageListener(int corePoolSize, int maxPoolSize, long keepAliveTime,
			TimeUnit unit, String workerClassName, String listToWatch) {

		ThreadPoolExecutor executor = new ThreadPoolExecutor(corePoolSize, maxPoolSize, keepAliveTime, unit, new LinkedBlockingDeque<Runnable>());				
		ThreadPoolManager manager = new ThreadPoolManager(redisPool, listToWatch, executor, workerClassName);
		manager.start();
	}
	
}
