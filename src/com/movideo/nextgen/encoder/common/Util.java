package com.movideo.nextgen.encoder.common;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.movideo.nextgen.encoder.models.EncodingJob;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * Utility class with static methods across the board
 * @author yramasundaram
 *
 */
public class Util {
	
	/**
	 * Constructs a Bitcodin Job object from input JSON string
	 * Primarily avoids serialization and de-serialization
	 * @param json - JSON string representing the job
	 * @return
	 * @throws JSONException
	 */
	public static EncodingJob getBitcodinJobFromJSON(JSONObject json) throws JSONException {
		
		EncodingJob job = new EncodingJob();
		if(json.has("encodingJobId")){
			job.setEncodingJobId(json.getInt("encodingJobId"));
		}
		job.setInputId(json.getInt("inputId"));
		job.setOutputId(json.getInt("outputId"));
		job.setClientId(json.getInt("clientId"));
		job.setMediaId(json.getInt("mediaId"));
		job.setStatus(json.getString("status"));
		job.setInputFileName(json.getString("inputFileName"));
		job.setInputFileUrl(json.getString("inputFileUrl"));
		job.setEncodingProfileId(json.getInt("encodingProfileId"));
		job.setSpeed(json.getString("speed"));
		job.setProductId(json.getString("productId"));
		job.setVariant(json.getString("variant"));
		if(json.has("errorType")){
			job.setErrorType(json.getString("errorType"));
		}
		if(json.has("drmType")){
			job.setDrmType(json.getString("drmType"));
		}

		ArrayList<String> manifestTypes = new ArrayList<String>();
		
		JSONArray manifestArray = json.getJSONArray("manifestTypes");
		int len = manifestArray.length();
		String[] result = new String[len];
		
		for (int num = 0; num < len; num++){ 
		    manifestTypes.add(manifestArray.get(num).toString());
		 } 
		
		job.setManifestTypes(manifestTypes.toArray(result));
		
		return job;
	}
	
	/**
	 * Used to move a specific item from one Redis list to another by the value
	 * This is different from the pop operations, because pop always gets the
	 * first or last element out and since the list is constantly changing,
	 * the only way to identify a specific element uniquely, is its value
	 * @param pool - Redis connection pool
	 * @param fromListName - Source list
	 * @param toListName - Target list
	 * @param originalValue - This is used to locate the message to delete
	 * @param newValue - This is the value to be posted to the new queue (may be having extra parameters)
	 */
	public static void moveJobToNextList(JedisPool pool, String fromListName, String toListName, String originalValue, String newValue){
		
		/* Jedis escapes JSON string when storing, but doesn't escape it when matching values for LREM.
		 * For now, escaping double quotes seems to be sufficient. Will have to revisit if there are other
		 * problematic characters 
		 */
		String valueToBeRemoved = originalValue.replaceAll("\"", "\\\"");
		Jedis jedis = pool.getResource();
		jedis.lpush(toListName, newValue);
		Long response = jedis.lrem(fromListName, 1, valueToBeRemoved);
		System.out.println("IN MOVE TO NEXT LIST: Number of entries deleted is: " + response);		
		jedis.close();
		//TODO: Messages pushed into the error list are not processed at the moment. Need to implement error handling and
		// selective re-tries based on error types.
	}
}
