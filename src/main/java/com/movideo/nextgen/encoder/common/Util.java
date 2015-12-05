package com.movideo.nextgen.encoder.common;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.amazonaws.util.json.JSONException;
import com.google.gson.Gson;
import com.movideo.nextgen.common.encoder.models.SubtitleInfo;
import com.movideo.nextgen.encoder.config.Constants;
import com.movideo.nextgen.encoder.models.EncodingJob;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * Utility class with static methods across the board
 * 
 * @author yramasundaram
 */
public class Util
{

	private static final Logger log = LogManager.getLogger();

	/**
	 * Constructs a Bitcodin Job object from input JSON string Primarily avoids serialization and de-serialization
	 * 
	 * @param json
	 *            - JSON string representing the job
	 * @return
	 * @throws JSONException
	 */
	public static EncodingJob getBitcodinJobFromJSON(String jsonString)
	{

		Gson gson = new Gson();
		EncodingJob job = gson.fromJson(jsonString, EncodingJob.class);

		return job;
	}

	/**
	 * Used to move a specific item from one Redis list to another by the value This is different from the pop operations, because pop always gets the
	 * first or last element out and since the list is constantly changing, the only way to identify a specific element uniquely, is its value
	 * 
	 * @param pool
	 *            - Redis connection pool
	 * @param fromListName
	 *            - Source list
	 * @param toListName
	 *            - Target list
	 * @param originalValue
	 *            - This is used to locate the message to delete
	 * @param newValue
	 *            - This is the value to be posted to the new queue (may be having extra parameters)
	 */
	public static void moveJobToNextList(JedisPool pool, String fromListName, String toListName, String originalValue,
			String newValue)
	{

		/*
		 * Jedis escapes JSON string when storing, but doesn't escape it when
		 * matching values for LREM. For now, escaping double quotes seems to be
		 * sufficient. Will have to revisit if there are other problematic
		 * characters
		 */
		String valueToBeRemoved = originalValue.replaceAll("\"", "\\\"");
		Jedis jedis = pool.getResource();
		jedis.lpush(toListName, newValue);
		Long response = jedis.lrem(fromListName, 1, valueToBeRemoved);
		log.debug("Util : moveJobToNextList() -> IN MOVE TO NEXT LIST: Number of entries deleted is: " + response);
		jedis.close();
		// TODO: Messages pushed into the error list are not processed at the
		// moment. Need to implement error handling and
		// selective re-tries based on error types.
	}

	// public static void main(String[] args) throws JSONException {
	// String json =
	// "{\"mediaId\":837935,\"encodingProfileId\":35364,\"inputFileName\":\"ForYourIceOnly.mp4\",\"status\":\"NEW\",\"speed\":\"premium\",\"bitcodinJobId\":0,\"serialversionuid\":-2746341744995209121,\"outputId\":19496,\"clientId\":524,\"inputId\":0,\"inputFileUrl\":\"http://movideoqaoriginal1.blob.core.windows.net/original-524/media/837935/ForYourIceOnly.mp4\",\"manifestTypes\":[\"mpd\"],\"retryCount\":0}";
	// getBitcodinJobFromJSON(json);
	// }

	public static String getMediaUrlFromSegments(int clientId, int mediaId, String fileName)
	{
		return Constants.AZURE_INPUT_URL_PREFIX + Constants.AZURE_INPUT_BLOB_CONTAINER_PREFIX + clientId + "/"
				+ Constants.AZURE_INPUT_BLOB_MEDIA_PATH_PREFIX + "/" + mediaId + "/" + fileName;
	}

	public static List<SubtitleInfo> formatSubUrls(List<SubtitleInfo> inputSubsList, int clientId, int mediaId)
	{
		List<SubtitleInfo> outputSubsList = new ArrayList<>();
		for(SubtitleInfo inputSub : inputSubsList)
		{
			SubtitleInfo outputSub = inputSub;
//			outputSub.setUrl(getMediaUrlFromSegments(clientId, mediaId, inputSub.getUrl()));
			outputSub.setUrl(inputSub.getUrl());

			outputSubsList.add(outputSub);
		}
		return outputSubsList;
	}

}
