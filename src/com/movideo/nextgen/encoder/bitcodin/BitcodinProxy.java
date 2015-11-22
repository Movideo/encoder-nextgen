package com.movideo.nextgen.encoder.bitcodin;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.movideo.nextgen.encoder.config.Constants;
import com.movideo.nextgen.encoder.models.EncodingJob;
import com.movideo.nextgen.encoder.models.EncodingProfileInfo;
import com.movideo.nextgen.encoder.models.InputConfig;
import com.movideo.nextgen.encoder.models.StreamConfig;

/**
 * Handles all Bitcodin communication. Should ideally expose one endpoint for
 * every Bitcodin API, but right now, the primary ones used are job status,
 * create input and create jobs. Encoding profile and output are technically
 * pre-created, so that we don't have to re-create them per request; but it
 * would be handy to have a complete script.
 * 
 * @author yramasundaram
 *
 */
public class BitcodinProxy {

	private static final Logger log = Logger.getLogger(BitcodinProxy.class);
	public static JSONObject listJobs() throws BitcodinException {
		return BitcodinHttpHelper.makeHttpCall("jobs?page=1", null, "get");
	}

	public static JSONObject getJobStatus(long jobId) throws BitcodinException {
		return BitcodinHttpHelper.makeHttpCall("job/" + jobId + "/status", null, "get");
	}

	public static JSONObject createJob(InputConfig inputConfig, EncodingJob job, JSONObject drmConfig)
			throws BitcodinException {

		JSONObject payload = new JSONObject();

		try {

			payload.put("encodingProfileId", job.getEncodingProfileId());
			payload.put("manifestTypes", job.getManifestTypes());
			payload.put("speed", job.getSpeed());
			// TODO: Add support for audioMetadata
			payload.put("outputId", job.getOutputId());
			log.info("Payload ready");
			
			JSONObject response = createAzureInput(inputConfig, job);

			if (response == null) {
				throw new BitcodinException(500, "Response is null", null);
			}

			payload.put("inputId", Integer.parseInt(response.get("inputId").toString()));
			String drmType = job.getDrmType();
			switch (drmType) {
				case Constants.CENC_ENCRYPTION_TYPE:
					payload.put("drmConfig", drmConfig);
					break;
				case Constants.AES_ENCRYPTION_TYPE:
				case Constants.FPS_ENCRYPTION_TYPE:
					payload.put("hlsEncryptionConfig", drmConfig);
					break;
			}

			log.info("Payload is: \n" + payload.toString());
			response = BitcodinHttpHelper.makeHttpCall("job/create", payload.toString(), "post");
			log.debug("Returning response from Bitcodin: \n" + response.toString());

			return response;

		} catch (JSONException e) {
			throw new BitcodinException(500, "Job creation failed", e);
		}	
		
	}

	public static JSONObject createJobWithIds(long encodingProfileId, String[] manifestTypes, String speed,
			long inputId, long outputId) throws BitcodinException {

		JSONObject payload = new JSONObject();

		try {

			payload.put("encodingProfileId", encodingProfileId);
			payload.put("manifestTypes", manifestTypes);
			payload.put("speed", speed);
			// TODO: Add support for audioMetadata
			payload.put("inputId", inputId);
			payload.put(" ", outputId);
			
			return BitcodinHttpHelper.makeHttpCall("job/create", payload.toString(), "post");
			
		} catch (JSONException e) {
			throw new BitcodinException(500, "Job creation failed", e);		}

	}

	public static JSONObject createAzureInput(InputConfig config, EncodingJob job) throws BitcodinException {

		JSONObject payload = new JSONObject();

		try {

			payload.put("type", config.getType());
			payload.put("accountName", config.getAccountName());
			payload.put("accountKey", config.getAccountKey());
			payload.put("url", job.getInputFileUrl());
			payload.put("container", config.getContainer());

			log.info("Payload to input create: \n" + payload);

			return BitcodinHttpHelper.makeHttpCall("input/create", payload.toString(), "post");

		} catch (JSONException e) {
			throw new BitcodinException(500, "An error occured while creating input", e);
		}

	}

	/*
	 * Request for an output creation to Bitcodin. To be used if no output id is
	 * pre-created.
	 */

	public static int preCreateOutputfromConfig(String type, String name,
			String accountName, String accountKey, String container,
			String prefix) {

		JSONObject response;
		int outputId = -1;

		try {
			response = BitcodinProxy.createAzureOutput(type, name, accountName,
					accountKey, container, prefix);
			log.debug("createAzureOutput response from Bitcodin");

		} catch (BitcodinException e) {
			e.printStackTrace();
			return outputId;
		}

		try {
			outputId = response.getInt("outputId");
			log.debug("output Id is" + response.get("outputId"));

		} catch (JSONException e) {
			log.debug("Bitcodin Error:Could not create new output");
			e.printStackTrace();
			return outputId;

		}

		return outputId;
	}

	public static JSONObject createAzureOutput(String type, String name, String accountName, String accountKey, String container,String prefix) throws BitcodinException{
	
		JSONObject payload = new JSONObject();
		
		try {
	
			/*payload.put("type", "azure");
			payload.put("name", "testname1");
			payload.put("accountName", "movideoqaencoded1");
			payload.put("accountKey", "vbSDcGSy2mbW55B2xMpkJ5Ns93CxNYJUIOz0kEdtQzhzv1+Wh87o5Daf9cf9zt6v1h2nLdiR/bzQqGvEPWFAGA==");
			payload.put("container", "encoded-524");
			payload.put("prefix", "testrunbitcodin2"); */
			
			payload.put("type", type);
			payload.put("name", name);
			payload.put("accountName",accountName);
			payload.put("accountKey", accountKey);
			payload.put("container", container);
			payload.put("prefix", prefix); 
			log.debug("Payload to output create: \n" + payload);
			return BitcodinHttpHelper.makeHttpCall("output/create", payload.toString(), "post");

		} catch (JSONException e) {

			throw new BitcodinException(500, "Output creation failed", e);
		}

	}

	public static JSONObject createEncodingProfile(String profileName, EncodingProfileInfo encodingProfileInfo)
			throws BitcodinException {

		JSONObject payload = new JSONObject();

		try {

			payload.put("type", "azure");
			payload.put("name", profileName);

			StreamConfig[] audioConfigs = encodingProfileInfo.getAudioConfigs();
			JSONArray audioStreamConfigs = new JSONArray();

			for (StreamConfig config : audioConfigs) {

				JSONObject audioStreamConfig = new JSONObject();
				audioStreamConfig.put("defaultStreamId", config.getDefaultStreamId());
				audioStreamConfig.put("bitrate", config.getBitrate());
				audioStreamConfigs.put(audioStreamConfig);

			}
			payload.put("audioStreamConfigs", audioStreamConfigs);

			StreamConfig[] videoConfigs = encodingProfileInfo.getVideoConfigs();
			JSONArray videoStreamConfigs = new JSONArray();

			for (StreamConfig config : videoConfigs) {

				JSONObject videoStreamConfig = new JSONObject();
				videoStreamConfig.put("defaultStreamId", config.getDefaultStreamId());
				videoStreamConfig.put("bitrate", config.getBitrate());
				videoStreamConfig.put("profile", config.getProfile());
				videoStreamConfig.put("preset", config.getPreset());
				videoStreamConfig.put("height", config.getHeight());
				videoStreamConfig.put("width", config.getWidth());
				videoStreamConfigs.put(videoStreamConfig);

			}

			return BitcodinHttpHelper.makeHttpCall("output/create", payload.toString(), "post");

		} catch (JSONException e) {

			throw new BitcodinException(500,"Encoding profile creation failed", e);
		}

	}


}
