package com.movideo.nextgen.encoder.test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.movideo.nextgen.common.encoder.models.AudioConfig;
import com.movideo.nextgen.common.encoder.models.EncodeInfo;
import com.movideo.nextgen.common.encoder.models.EncodeRequest;
import com.movideo.nextgen.common.encoder.models.StreamInfo;
import com.movideo.nextgen.common.encoder.models.VideoConfig;
import com.movideo.nextgen.common.multithreading.ThreadPoolManager;
import com.movideo.nextgen.common.queue.QueueConnectionConfig;
import com.movideo.nextgen.common.queue.QueueManager;
import com.movideo.nextgen.common.queue.redis.RedisQueueConnectionConfig;
import com.movideo.nextgen.common.queue.redis.RedisQueueManager;
import com.movideo.nextgen.encoder.bitcodin.tasks.CreateBitcodinJobTask;
import com.movideo.nextgen.encoder.bitcodin.tasks.PollBitcodinJobStatusTask;
import com.movideo.nextgen.encoder.bitcodin.tasks.ProcessEncodeRequestTask;
import com.movideo.nextgen.encoder.config.AppConfig;
import com.movideo.nextgen.encoder.config.Constants;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class LogEncodeRequestTest {
    
    private static final Logger log = LogManager.getLogger();
    private static JedisPool redisPool;
    
    public static void main(String[] args) {
	
	AppConfig appConfig = new AppConfig("config.properties");
	
	redisPool = new JedisPool(new JedisPoolConfig(), appConfig.getRedisConnectionString());
	Jedis jedis = redisPool.getResource();
	
	RedisQueueConnectionConfig config = new RedisQueueConnectionConfig();
	config.setPool(redisPool);


	/* HD Profile ID: 37944 */
	// {
	// "name": "Demo High Definition",
	// "videoStreamConfigs": [
	// {
	// "defaultStreamId": 0,
	// "bitrate": 5000000,
	// "profile": "high",
	// "preset": "premium",
	// "codec": "h264",
	// "height": 360,
	// "width": 640
	// }
	// ],
	// "audioStreamConfigs": [
	// {
	// "defaultStreamId": 0,
	// "bitrate": 256000
	// }
	// ]
	// }

	/* SD profile - 37945 */
	// {
	// "name": "Demo Standard Definition",
	// "videoStreamConfigs": [
	// {
	// "defaultStreamId": 0,
	// "bitrate": 1200000,
	// "profile": "main",
	// "preset": "standard",
	// "codec": "h264",
	// "height": 360,
	// "width": 640
	// }
	// ],
	// "audioStreamConfigs": [
	// {
	// "defaultStreamId": 0,
	// "bitrate": 256000
	// }
	// ]
	// }

	/* Construct test encode request objects */
	EncodeRequest request = new EncodeRequest();
	request.setClientId(524);
	request.setMediaId(848044);
	request.setProductId("1234567890");
	request.setVariant("HD");
	request.setInputFilename("vid.mp4");
	request.setSpeed("premium");

	VideoConfig videoConfig = new VideoConfig();
	videoConfig.setBitRate(5000000);
	videoConfig.setCodec("h264");
	videoConfig.setHeight(360);
	videoConfig.setWidth(640);

	List<VideoConfig> videoConfList = new ArrayList<VideoConfig>();
	videoConfList.add(videoConfig);

	AudioConfig audioConfig = new AudioConfig();
	audioConfig.setBitRate(256000);
	audioConfig.setCodec("aac");

	List<AudioConfig> audioConfList = new ArrayList<AudioConfig>();
	audioConfList.add(audioConfig);

	List<String> manifestTypes = new ArrayList<String>();
	manifestTypes.add("m3u8");
	manifestTypes.add("mpd");

	StreamInfo streamInfo = new StreamInfo();
	streamInfo.setProtectionRequired(false);
	streamInfo.setManifestType(manifestTypes);
	streamInfo.setAudioConfig(audioConfList);
	streamInfo.setVideoConfig(videoConfList);

	EncodeInfo encodeInfo = new EncodeInfo();
	encodeInfo.setEncodingProfileId(37944);
	encodeInfo.setStreamsList(streamInfo);

	List<EncodeInfo> encodeInfoList = new ArrayList<>();
	encodeInfoList.add(encodeInfo);

	request.setEncodeInfo(encodeInfoList);
	String jobJson = new Gson().toJson(request);

	log.debug(jobJson);
	
	initMessageListener(appConfig.getCorePoolSize(), appConfig.getMaxPoolSize(), appConfig.getKeepAliveTime(),
		TimeUnit.MINUTES, ProcessEncodeRequestTask.class.getName(), Constants.REDIS_ENCODE_REQUEST_LIST, config);
	
	initMessageListener(appConfig.getCorePoolSize(), appConfig.getMaxPoolSize(), appConfig.getKeepAliveTime(),
		TimeUnit.MINUTES, CreateBitcodinJobTask.class.getName(), Constants.REDIS_INPUT_LIST, config);

	initMessageListener(appConfig.getCorePoolSize(), appConfig.getMaxPoolSize(), appConfig.getKeepAliveTime(),
		TimeUnit.MINUTES, PollBitcodinJobStatusTask.class.getName(), Constants.REDIS_PENDING_LIST, config);
	
	jedis.lpush(Constants.REDIS_ENCODE_REQUEST_LIST, jobJson);

    }
    
    private static void initMessageListener(int corePoolSize, int maxPoolSize, long keepAliveTime, TimeUnit unit,
	    String workerClassName, String listToWatch, QueueConnectionConfig config) {

	ThreadPoolExecutor executor = new ThreadPoolExecutor(corePoolSize, maxPoolSize, keepAliveTime, unit,
		new LinkedBlockingDeque<Runnable>());
	
	
	QueueManager queueManager = new RedisQueueManager(config);
	ThreadPoolManager manager = new ThreadPoolManager(queueManager, listToWatch, executor, workerClassName);
	manager.start();
    }

}
