package com.movideo.nextgen.encoder.test;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.movideo.nextgen.common.encoder.models.AudioConfig;
import com.movideo.nextgen.common.encoder.models.EncodeInfo;
import com.movideo.nextgen.common.encoder.models.EncodeRequest;
import com.movideo.nextgen.common.encoder.models.StreamInfo;
import com.movideo.nextgen.common.encoder.models.SubtitleInfo;
import com.movideo.nextgen.common.encoder.models.VideoConfig;
import com.movideo.nextgen.encoder.config.AppConfig;
import com.movideo.nextgen.encoder.config.Constants;
import com.movideo.nextgen.encoder.models.EncodingJob;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * Created by rranawaka on 26/11/2015.
 */
public class SampleGenerator
{
	private static final Logger log = LogManager.getLogger();

	public static void addSampleRequest(JedisPool redisPool, AppConfig appConfig)
	{
		Jedis jedis = redisPool.getResource();
		String job = createSampleEncodingRequest();
		for(int i = 0; i < appConfig.getParalleljobCountforTest(); i++)
		{
			jedis.lpush(Constants.REDIS_ENCODE_REQUEST_LIST, job);
		}

		jedis.close();
	}

	public static String createSampleEncodingRequest()
	{
		EncodeRequest request = new EncodeRequest();
		request.setClientId(524);
		request.setMediaId(848095);
		request.setProductId("999999999");
		request.setVariant("HD");
		request.setInputFilename("movie.mp4");
		request.setSpeed("premium");

		List<SubtitleInfo> subList = new ArrayList<>();

		SubtitleInfo subtitleEn = new SubtitleInfo();
		subtitleEn.setLangLong("English");
		subtitleEn.setLangShort("en");
		subtitleEn.setUrl("track_en.vtt");
		subList.add(subtitleEn);

		SubtitleInfo subtitleVi = new SubtitleInfo();
		subtitleVi.setLangLong("Vietnamese");
		subtitleVi.setLangShort("vi");
		subtitleVi.setUrl("track_vi.vtt");
		subList.add(subtitleVi);

		request.setSubtitleInfo(subList);

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
		//		streamInfo.setProtectionRequired(true);
		streamInfo.setManifestType(manifestTypes);
		streamInfo.setAudioConfig(audioConfList);
		streamInfo.setVideoConfig(videoConfList);

		EncodeInfo encodeInfo = new EncodeInfo();
		encodeInfo.setReprocessing(true);
		encodeInfo.setEncodingProfileId(37944);
		encodeInfo.setStreamInfo(streamInfo);

		List<EncodeInfo> encodeInfoList = new ArrayList<>();
		encodeInfoList.add(encodeInfo);

		request.setEncodeInfo(encodeInfoList);
		String jobJson = new Gson().toJson(request);

		return jobJson;
	}

	public static void addSampleJobs(JedisPool redisPool, AppConfig appConfig)
	{
		Jedis jedis = redisPool.getResource();
		EncodingJob job = createSampleJobFromConfig(appConfig);
		for(int i = 0; i < appConfig.getParalleljobCountforTest(); i++)
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

	private static String getMediaUrlFromSegments(int clientId, int mediaId, String fileName)
	{
		return Constants.AZURE_INPUT_URL_PREFIX + Constants.AZURE_INPUT_BLOB_CONTAINER_PREFIX + clientId + "/" + Constants.AZURE_INPUT_BLOB_MEDIA_PATH_PREFIX + "/" + mediaId + "/" + fileName;
	}
}
