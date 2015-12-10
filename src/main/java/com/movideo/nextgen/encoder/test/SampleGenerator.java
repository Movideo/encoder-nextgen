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
import com.movideo.nextgen.encoder.common.Util;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * Created by rranawaka on 26/11/2015.
 */
public class SampleGenerator
{
	private static final Logger log = LogManager.getLogger();

	public static void addSampleRequest(JedisPool redisPool)
	{
		Jedis jedis = redisPool.getResource();
		String job = createSampleEncodingRequest();
		int numJobs = Integer.parseInt(Util.getConfigProperty("sample.test.job.numParalleljobs"));

		for(int i = 0; i < numJobs; i++)
		{
			jedis.lpush(Util.getConfigProperty("redis.encodeOrchestrator.input.list"), job);
		}

		jedis.close();
	}

	public static String createSampleEncodingRequest()
	{
		EncodeRequest request = new EncodeRequest();

		request.setClientId(Integer.parseInt(Util.getConfigProperty("sample.test.job.clientid")));
		request.setMediaId(Integer.parseInt(Util.getConfigProperty("sample.test.job.mediaId")));

		request.setProductId(Util.getConfigProperty("sample.test.job.productId"));
		request.setVariant(Util.getConfigProperty("sample.test.job.variant"));
		request.setInputFilename(Util.getConfigProperty("sample.test.job.inputFileName"));
		request.setSpeed(Util.getConfigProperty("sample.test.job.speed"));

		List<SubtitleInfo> subList = new ArrayList<>();

		SubtitleInfo subtitleEn = new SubtitleInfo();
		subtitleEn.setLangLong(Util.getConfigProperty("sample.test.job.subtitle.en.langLong"));
		subtitleEn.setLangShort(Util.getConfigProperty("sample.test.job.subtitle.en.langShort"));
		subtitleEn.setUrl(Util.getConfigProperty("sample.test.job.subtitle.en.url"));
		subList.add(subtitleEn);

		SubtitleInfo subtitleVi = new SubtitleInfo();
		subtitleVi.setLangLong(Util.getConfigProperty("sample.test.job.subtitle.vi.langLong"));
		subtitleVi.setLangShort(Util.getConfigProperty("sample.test.job.subtitle.vi.langShort"));
		subtitleVi.setUrl(Util.getConfigProperty("sample.test.job.subtitle.vi.url"));
		subList.add(subtitleVi);

		request.setSubtitleInfo(subList);

		VideoConfig videoConfig = new VideoConfig();
		videoConfig.setBitRate(Integer.parseInt(Util.getConfigProperty("sample.test.job.videoconfig.bitRate")));
		videoConfig.setCodec(Util.getConfigProperty("sample.test.job.videoconfig.codec"));
		videoConfig.setHeight(Integer.parseInt(Util.getConfigProperty("sample.test.job.videoconfig.height")));
		videoConfig.setWidth(Integer.parseInt(Util.getConfigProperty("sample.test.job.videoconfig.width")));

		List<VideoConfig> videoConfList = new ArrayList<VideoConfig>();
		videoConfList.add(videoConfig);

		AudioConfig audioConfig = new AudioConfig();
		audioConfig.setBitRate(Integer.parseInt(Util.getConfigProperty("sample.test.job.audioconfig.bitRate")));
		audioConfig.setCodec(Util.getConfigProperty("sample.test.job.audioconfig.codec"));

		List<AudioConfig> audioConfList = new ArrayList<AudioConfig>();
		audioConfList.add(audioConfig);

		List<String> manifestTypes = new ArrayList<String>();
		String[] manifestConfig = Util.getConfigProperty("sample.test.job.manifestTypes").split(",");
		for(String manifestType : manifestConfig)
		{
			manifestTypes.add(manifestType);
		}

		StreamInfo streamInfo = new StreamInfo();
		//		streamInfo.setProtectionRequired(true);
		streamInfo.setManifestType(manifestTypes);
		streamInfo.setAudioConfig(audioConfList);
		streamInfo.setVideoConfig(videoConfList);

		EncodeInfo encodeInfo = new EncodeInfo();
		encodeInfo.setReprocessing(Boolean.parseBoolean(Util.getConfigProperty("sample.test.job.drm")));
		encodeInfo.setEncodingProfileId(Integer.parseInt(Util.getConfigProperty("sample.test.job.encodingProfileId")));
		encodeInfo.setStreamInfo(streamInfo);

		List<EncodeInfo> encodeInfoList = new ArrayList<>();
		encodeInfoList.add(encodeInfo);

		request.setEncodeInfo(encodeInfoList);
		String jobJson = new Gson().toJson(request);
		log.debug("About to log the following encode request: \n" + jobJson);

		return jobJson;
	}
}
