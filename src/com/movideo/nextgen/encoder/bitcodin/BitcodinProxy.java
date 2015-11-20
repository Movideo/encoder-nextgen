package com.movideo.nextgen.encoder.bitcodin;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.movideo.nextgen.encoder.bitcodin.models.BitcodinJob;
import com.movideo.nextgen.encoder.bitcodin.models.EncodingProfileInfo;
import com.movideo.nextgen.encoder.bitcodin.models.InputConfig;
import com.movideo.nextgen.encoder.bitcodin.models.StreamConfig;

/**
 * Handles all Bitcodin communication. Should ideally expose one endpoint for every Bitcodin API, but right now,
 * the primary ones used are job status, create input and create jobs. Encoding profile and output are technically
 * pre-created, so that we don't have to re-create them per request; but it would be handy to have a complete script.
 * @author yramasundaram
 *
 */
public class BitcodinProxy {
	
	public static JSONObject listJobs() throws BitcodinException {
		return BitcodinHttpHelper.httpGet("jobs?page=1");
	}
	
	public static JSONObject getJobStatus(long jobId) throws BitcodinException{
		return BitcodinHttpHelper.httpGet("job/" + jobId + "/status");
	}
	
	public static JSONObject createJob(InputConfig inputConfig, BitcodinJob job) throws BitcodinException{
		
		JSONObject payload = new JSONObject();
	
		try {
			
			payload.put("encodingProfileId", job.getEncodingProfileId());
			payload.put("manifestTypes", job.getManifestTypes());
			payload.put("speed", job.getSpeed());
			//TODO: Add support for audioMetadata
			payload.put("outputId", job.getOutputId());
			
			JSONObject response = createAzureInput(inputConfig, job);
			
			if(response == null){
				throw new BitcodinException(500, "Response is null", null);
			}
			
			payload.put("inputId", Integer.parseInt(response.get("inputId").toString()));
			System.out.println("Payload is: \n" + payload.toString());
			response = BitcodinHttpHelper.httpPost("job/create", payload.toString());
			System.out.println("Returning response from Bitcodin: \n" + response.toString());
			
			return response;
			
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
		
		return null;		
	}
	
	public static JSONObject createJobWithIds(long encodingProfileId, String[] manifestTypes, String speed, long inputId, long outputId) throws BitcodinException{
		
		JSONObject payload = new JSONObject();
	
		try {
			
			payload.put("encodingProfileId", encodingProfileId);
			payload.put("manifestTypes", manifestTypes);
			payload.put("speed", speed);
			//TODO: Add support for audioMetadata
			payload.put("inputId", inputId);
			payload.put("outputId", outputId);
			
			return BitcodinHttpHelper.httpPost("job/create", payload.toString());
			
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
		
		return null;		
	}

	
	public static JSONObject createAzureInput(InputConfig config, BitcodinJob job) throws BitcodinException{
		
		JSONObject payload = new JSONObject();
		
		try {
			
			payload.put("type", config.getType());
			payload.put("accountName", config.getAccountName());
			payload.put("accountKey", config.getAccountKey());
			payload.put("url", job.getInputFileUrl());
			payload.put("container", config.getContainer());
			
			System.out.println("Payload to input create: \n" + payload);
			
			return BitcodinHttpHelper.httpPost("input/create", payload.toString());
			
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}
	
	public static JSONObject createAzureOutput(String type, String profileName, String accountName, String accountKey, String container) throws BitcodinException{
		
		JSONObject payload = new JSONObject();
		
		try {
			
			payload.put("type", "azure");
			payload.put("name", profileName);
			payload.put("accountName", accountName);
			payload.put("accountKey", accountKey);
			payload.put("container", container);
			
			return BitcodinHttpHelper.httpPost("output/create", payload.toString());
			
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}

	public static JSONObject createEncodingProfile(String profileName, EncodingProfileInfo encodingProfileInfo) throws BitcodinException{
		
		JSONObject payload = new JSONObject();
		
		try {
			
			payload.put("type", "azure");
			payload.put("name", profileName);
			
			StreamConfig[] audioConfigs = encodingProfileInfo.getAudioConfigs();
			JSONArray audioStreamConfigs = new JSONArray();
			
			for(StreamConfig config : audioConfigs){
				
				JSONObject audioStreamConfig = new JSONObject();
				audioStreamConfig.put("defaultStreamId", config.getDefaultStreamId());
				audioStreamConfig.put("bitrate", config.getBitrate());
				audioStreamConfigs.put(audioStreamConfig);
				
			}
			payload.put("audioStreamConfigs", audioStreamConfigs);

			StreamConfig[] videoConfigs = encodingProfileInfo.getVideoConfigs();
			JSONArray videoStreamConfigs = new JSONArray();

			for(StreamConfig config : videoConfigs){
				
				JSONObject videoStreamConfig = new JSONObject();
				videoStreamConfig.put("defaultStreamId", config.getDefaultStreamId());
				videoStreamConfig.put("bitrate", config.getBitrate());
				videoStreamConfig.put("profile", config.getProfile());
				videoStreamConfig.put("preset", config.getPreset());
				videoStreamConfig.put("height", config.getHeight());
				videoStreamConfig.put("width", config.getWidth());
				videoStreamConfigs.put(videoStreamConfig);
				
			}
			
			return BitcodinHttpHelper.httpPost("output/create", payload.toString());
			
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}


	public static void main(String[] args) {
		//System.out.println(BitcodinProxy.listJobs().toString());

	}

}
