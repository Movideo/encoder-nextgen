package com.movideo.nextgen.encoder.test;

import java.io.UnsupportedEncodingException;

import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Protocol;

public class RedisPusher
{
	public static void main(String[] args) throws JsonSyntaxException, JsonIOException, UnsupportedEncodingException
	{
		String[] jobs = new String[] {
				"{\"mediaId\":1425281,\"encodingProfileId\":44386,\"subtitleList\":[{\"langLong\":\"Tiếng Việt\",\"langShort\":\"vi\",\"type\":\"vtt\",\"url\":\"mission_impossible_2_vi.vtt\"}],\"inputFileName\":\"mission_impossible_2.mp4\",\"speed\":\"standard\",\"status\":\"RETRY\",\"cdnFtpInfo\":{\"mediaId\":0,\"username\":\"wmhyqyg\",\"host\":\"ftphcm.cdnviet.com\",\"password\":\"bhd@123#\",\"ip\":\"113.164.15.170\"},\"variant\":\"HD\",\"reprocess\":false,\"clientId\":457,\"inputId\":0,\"productId\":\"84971\",\"inputFileUrl\":\"http://movideooriginal1.blob.core.windows.net/original-457/media/1425281/mission_impossible_2.mp4\",\"manifestTypes\":[\"mpd\",\"m3u8\"],\"retryCount\":0,\"protectionRequired\":true,\"cdnSyncRequired\":true}",
				"{\"mediaId\":1425281,\"encodingProfileId\":44387,\"subtitleList\":[{\"langLong\":\"Tiếng Việt\",\"langShort\":\"vi\",\"type\":\"vtt\",\"url\":\"mission_impossible_2_vi.vtt\"}],\"inputFileName\":\"mission_impossible_2.mp4\",\"speed\":\"standard\",\"status\":\"RETRY\",\"cdnFtpInfo\":{\"mediaId\":0,\"username\":\"wmhyqyg\",\"host\":\"ftphcm.cdnviet.com\",\"password\":\"bhd@123#\",\"ip\":\"113.164.15.170\"},\"variant\":\"SD\",\"reprocess\":false,\"clientId\":457,\"inputId\":0,\"productId\":\"84971\",\"inputFileUrl\":\"http://movideooriginal1.blob.core.windows.net/original-457/media/1425281/mission_impossible_2.mp4\",\"manifestTypes\":[\"mpd\",\"m3u8\"],\"retryCount\":0,\"protectionRequired\":true,\"cdnSyncRequired\":true}",
				"{\"mediaId\":1424766,\"encodingProfileId\":44386,\"subtitleList\":[{\"langLong\":\"Tiếng Việt\",\"langShort\":\"vi\",\"type\":\"vtt\",\"url\":\"mission_impossible_vi.vtt\"}],\"inputFileName\":\"mission_impossible.mp4\",\"speed\":\"standard\",\"status\":\"RETRY\",\"cdnFtpInfo\":{\"mediaId\":0,\"username\":\"wmhyqyg\",\"host\":\"ftphcm.cdnviet.com\",\"password\":\"bhd@123#\",\"ip\":\"113.164.15.170\"},\"variant\":\"HD\",\"reprocess\":false,\"clientId\":457,\"productId\":\"84962\",\"inputFileUrl\":\"http://movideooriginal1.blob.core.windows.net/original-457/media/1424766/mission_impossible.mp4\",\"manifestTypes\":[\"mpd\",\"m3u8\"],\"retryCount\":0,\"protectionRequired\":true,\"cdnSyncRequired\":true}"
		};

		JedisPool pool = new JedisPool(new JedisPoolConfig(), "encoder.redis.cache.windows.net",
				6379, Protocol.DEFAULT_TIMEOUT, "WUGEpMI6T//CIbADYANles0j8lOlN4/RC8kiGei3Z94=");
		try (Jedis jedis = pool.getResource())
		{
			for(String job : jobs)
			{
				jedis.lpush("ENCODE_INPUT_LIST", job);
				System.out.println("Added : " + job);
			}
			System.out.println("DONE!");
		}

		pool.close();
	}

}
