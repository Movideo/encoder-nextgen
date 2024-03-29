package com.movideo.nextgen.encoder.bitcodin;

import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.movideo.nextgen.common.encoder.models.SubtitleInfo;
import com.movideo.nextgen.encoder.common.Util;
import com.movideo.nextgen.encoder.models.EncodingJob;
import com.movideo.nextgen.encoder.models.EncodingProfileInfo;
import com.movideo.nextgen.encoder.models.FtpInfo;
import com.movideo.nextgen.encoder.models.InputConfig;
import com.movideo.nextgen.encoder.models.OutputConfig;
import com.movideo.nextgen.encoder.models.StreamConfig;

/**
 * Handles all Bitcodin communication. Should ideally expose one endpoint for every Bitcodin API, but right now, the primary ones used are job status,
 * create input and create jobs. Encoding profile and output are technically pre-created, so that we don't have to re-create them per request; but it
 * would be handy to have a complete script.
 * 
 * @author yramasundaram
 */
public class BitcodinProxy
{

	private static final Logger log = LogManager.getLogger();

	public static JSONObject listJobs() throws BitcodinException
	{
		return BitcodinHttpHelper.makeHttpCall("jobs?page=1", null, "get");
	}

	public static JSONObject getJobStatus(long jobId) throws BitcodinException
	{
		return BitcodinHttpHelper.makeHttpCall("job/" + jobId + "/status", null, "get");
	}

	public static JSONObject getTransferStatus(long jobId) throws BitcodinException
	{
		return BitcodinHttpHelper.makeHttpCall("job/" + jobId + "/transfers", null, "get");
	}

	public static JSONObject createFTPOutput(FtpInfo ftpInfo) throws BitcodinException, NumberFormatException
	{
		JSONObject payload = new JSONObject();
		try
		{
			payload.put("name", ftpInfo.getName());
			payload.put("username", ftpInfo.getUsername());
			payload.put("password", ftpInfo.getPassword());
			payload.put("type", "ftp");
			payload.put("host", ftpInfo.getPath());

			log.info("BitcodinProxy: createFTPOutput() -> Payload to output create: \n" + payload);
		}
		catch(JSONException e)
		{
			throw new BitcodinException(Integer.parseInt(Util.getConfigProperty("error.codes.bad.request")), e.getMessage(), e);
		}
		return BitcodinHttpHelper.makeHttpCall("output/create", payload.toString(), "post");
	}

	public static JSONObject transferJobOutput(long jobId, long outputId) throws BitcodinException
	{
		JSONObject payload = new JSONObject();
		try
		{
			payload.put("jobId", jobId);
			payload.put("outputId", outputId);
		}
		catch(JSONException e)
		{
			throw new BitcodinException(Integer.parseInt(Util.getConfigProperty("error.codes.bad.request")), e.getMessage(), e);
		}
		return BitcodinHttpHelper.makeHttpCall("job/transfer", payload.toString(), "post");
	}

	public static JSONObject createManifestWithSubs(long jobId, List<SubtitleInfo> subtitles, String outputFileName, String manifestType, String subtitleType) throws BitcodinException
	{
		JSONObject payload = new JSONObject();
		try
		{
			payload.put("jobId", jobId);
			payload.put("subtitles", new JSONArray(subtitles));
			payload.put("outputFileName", outputFileName);
		}
		catch(JSONException e)
		{
			throw new BitcodinException(Integer.parseInt(Util.getConfigProperty("error.codes.bad.request")), e.getMessage(), e);
		}

		String apiPath = null;
		if(manifestType.equals("mpd"))
		{
			apiPath = "manifest/mpd/" + subtitleType;
		}
		else if(manifestType.equals("m3u8"))
		{
			apiPath = "manifest/hls/" + subtitleType;
		}
		//		String apiPath = "manifest/" + manifestType + "/" + subtitleType;

		log.info("Payload in create subtitle call: " + payload);
		return BitcodinHttpHelper.makeHttpCall(apiPath, payload.toString(), "post");
	}

	public static JSONObject createJob(InputConfig inputConfig, OutputConfig outputConfig, EncodingJob job, Map<String, JSONObject> drmConfigMap)
			throws BitcodinException
	{

		JSONObject payload = new JSONObject();

		try
		{
			boolean hasSubs = (job.getSubtitleList() != null && job.getSubtitleList().size() > 0) ? true : false;
			log.debug("Job hasSubs? " + hasSubs);
			payload.put("encodingProfileId", job.getEncodingProfileId());
			payload.put("manifestTypes", job.getManifestTypes());
			payload.put("speed", job.getSpeed());
			// TODO: Add support for audioMetadata
			log.debug("About to create input");

			JSONObject response = createAzureInput(inputConfig, job);

			if(response == null)
			{
				throw new BitcodinException(Integer.parseInt(Util.getConfigProperty("error.codes.internal.server.error")), "Response is null", null);
			}

			log.debug("Created input id: " + response.get("inputId"));
			payload.put("inputId", response.getInt("inputId"));

			response = createAzureOutput(outputConfig);

			if(response == null)
			{
				throw new BitcodinException(Integer.parseInt(Util.getConfigProperty("error.codes.internal.server.error")), "Response is null", null);
			}

			long outputId = response.getLong("outputId");

			log.debug("Created output id: " + outputId);

			//If the job has subtitles to be processed, output should not be specified
			if(!hasSubs)
			{
				payload.put("outputId", outputId);
			}

			if(drmConfigMap != null)
			{
				payload.put(Util.getConfigProperty("bitcodin.drm.cenc.drmConfig.key"), drmConfigMap.get(Util.getConfigProperty("bitcodin.drm.cenc.encryptionType")));
				payload.put(Util.getConfigProperty("bitcodin.drm.hls.drmConfig.key"), drmConfigMap.get(Util.getConfigProperty("bitcodin.drm.fps.encryptionType")));
			}

			log.info("BitcodinProxy: createJob() -> Payload sent to Bitcodin create Job API :" + payload.toString());
			response = BitcodinHttpHelper.makeHttpCall("job/create", payload.toString(), "post");
			if(hasSubs)
			{
				log.info("Job has subs. Adding output id " + outputId + " to response!");
				response.put("outputId", outputId);
			}
			log.debug("BitcodinProxy: createJob() -> Returning response from Bitcodin: \n" + response.toString());

			return response;

		}
		catch(JSONException e)
		{
			throw new BitcodinException(Integer.parseInt(Util.getConfigProperty("error.codes.internal.server.error")), "Job creation failed", e);
		}

	}

	public static JSONObject createJobWithIds(long encodingProfileId, String[] manifestTypes, String speed,
			long inputId, long outputId) throws BitcodinException
	{

		JSONObject payload = new JSONObject();

		try
		{

			payload.put("encodingProfileId", encodingProfileId);
			payload.put("manifestTypes", manifestTypes);
			payload.put("speed", speed);
			// TODO: Add support for audioMetadata
			payload.put("inputId", inputId);
			payload.put("outputId", outputId);

			return BitcodinHttpHelper.makeHttpCall("job/create", payload.toString(), "post");

		}
		catch(JSONException e)
		{
			throw new BitcodinException(Integer.parseInt(Util.getConfigProperty("error.codes.internal.server.error")), "Job creation failed", e);
		}

	}

	public static JSONObject createAzureInput(InputConfig config, EncodingJob job) throws BitcodinException
	{

		JSONObject payload = new JSONObject();

		try
		{

			payload.put("type", config.getType());
			payload.put("accountName", config.getAccountName());
			payload.put("accountKey", config.getAccountKey());
			payload.put("url", job.getInputFileUrl());
			payload.put("container", config.getContainer());

			log.info("BitcodinProxy: createAzureInput() -> Payload to input create: \n" + payload);

			return BitcodinHttpHelper.makeHttpCall("input/create", payload.toString(), "post");

		}
		catch(JSONException e)
		{
			throw new BitcodinException(Integer.parseInt(Util.getConfigProperty("error.codes.internal.server.error")), "An error occured while creating input", e);
		}

	}

	public static JSONObject createAzureOutput(OutputConfig config) throws BitcodinException
	{

		JSONObject payload = new JSONObject();

		try
		{

			/*
			 * payload.put("type", "azure"); payload.put("name", "testname1");
			 * payload.put("accountName", "movideoqaencoded1");
			 * payload.put("accountKey",
			 * "vbSDcGSy2mbW55B2xMpkJ5Ns93CxNYJUIOz0kEdtQzhzv1+Wh87o5Daf9cf9zt6v1h2nLdiR/bzQqGvEPWFAGA=="
			 * ); payload.put("container", "encoded-524"); payload.put("prefix",
			 * "testrunbitcodin2");
			 */

			payload.put("type", config.getType());
			payload.put("name", config.getName());
			payload.put("accountName", config.getAccountName());
			payload.put("accountKey", config.getAccountKey());
			payload.put("container", config.getContainer());
			payload.put("prefix", config.getPrefix());
			log.info("BitcodinProxy: createAzureOutput() ->Payload to output create: \n" + payload);
			return BitcodinHttpHelper.makeHttpCall("output/create", payload.toString(), "post");

		}
		catch(JSONException e)
		{

			throw new BitcodinException(Integer.parseInt(Util.getConfigProperty("error.codes.internal.server.error")), "Output creation failed", e);
		}

	}

	public static JSONObject getJobCount(String status) throws BitcodinException
	{
		return BitcodinHttpHelper.makeHttpCall("jobs/1/" + status, null, "get");
	}

	public static JSONObject createEncodingProfile(String profileName, EncodingProfileInfo encodingProfileInfo)
			throws BitcodinException
	{

		JSONObject payload = new JSONObject();

		try
		{

			payload.put("type", "azure");
			payload.put("name", profileName);

			StreamConfig[] audioConfigs = encodingProfileInfo.getAudioConfigs();
			JSONArray audioStreamConfigs = new JSONArray();

			for(StreamConfig config : audioConfigs)
			{

				JSONObject audioStreamConfig = new JSONObject();
				audioStreamConfig.put("defaultStreamId", config.getDefaultStreamId());
				audioStreamConfig.put("bitrate", config.getBitrate());
				audioStreamConfigs.put(audioStreamConfig);

			}
			payload.put("audioStreamConfigs", audioStreamConfigs);

			StreamConfig[] videoConfigs = encodingProfileInfo.getVideoConfigs();
			JSONArray videoStreamConfigs = new JSONArray();

			for(StreamConfig config : videoConfigs)
			{

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

		}
		catch(JSONException e)
		{

			throw new BitcodinException(Integer.parseInt(Util.getConfigProperty("error.codes.internal.server.error")), "Encoding profile creation failed", e);
		}

	}

}
