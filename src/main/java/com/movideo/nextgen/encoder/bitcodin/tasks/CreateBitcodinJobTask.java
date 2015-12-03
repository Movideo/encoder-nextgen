package com.movideo.nextgen.encoder.bitcodin.tasks;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonSyntaxException;
import com.movideo.nextgen.common.multithreading.Task;
import com.movideo.nextgen.common.queue.QueueException;
import com.movideo.nextgen.common.queue.QueueManager;
import com.movideo.nextgen.encoder.bitcodin.BitcodinDRMConfigBuilder;
import com.movideo.nextgen.encoder.bitcodin.BitcodinException;
import com.movideo.nextgen.encoder.bitcodin.BitcodinProxy;
import com.movideo.nextgen.encoder.common.Util;
import com.movideo.nextgen.encoder.config.Constants;
import com.movideo.nextgen.encoder.models.EncodeSummary;
import com.movideo.nextgen.encoder.models.EncodingJob;
import com.movideo.nextgen.encoder.models.InputConfig;
import com.movideo.nextgen.encoder.models.OutputConfig;

import net.minidev.json.JSONArray;

/**
 * Runnable class that submits a new job to Bitcodin and queues it into the pending list for subsequent polling
 *
 * @author yramasundaram
 */
public class CreateBitcodinJobTask extends Task
{

	private static final Logger log = LogManager.getLogger();

	private String workingListName = Constants.REDIS_INPUT_WORKING_LIST, errorListName = Constants.REDIS_JOB_ERROR_LIST,
		successListName = Constants.REDIS_PENDING_LIST;

	public CreateBitcodinJobTask(QueueManager queueManager, String jobString)
	{
	super(queueManager, jobString);
	}

	private JSONObject constructManifestObject(JSONObject manifestUrls, String type, JSONObject createJobResponse)
		throws JSONException
	{

	JSONObject manifest = new JSONObject();
	manifest.put("type", type);
	// TODO: Hacky logic - understand why Bitcodin cannot send our urls back
	manifest.put("url", createJobResponse.getString("outputPath").replace(".bitblobstorage", "") + "/"
		+ createJobResponse.getInt("jobId") + "." + type);
	return manifest;
	}

	private EncodeSummary getEncodeSummary(EncodingJob job, JSONObject createJobResponse)
		throws JSONException, IOException
	{

	JSONObject encodeSummary = new JSONObject();

	encodeSummary.put("object", "encoding");
	encodeSummary.put("product_id", job.getProductId());
	encodeSummary.put("media_id", job.getMediaId());
	encodeSummary.put("variant", job.getVariant());
	encodeSummary.put("mediaConfigurations",
		createJobResponse.getJSONObject("input").getJSONArray("mediaConfigurations"));
	JSONArray manifests = new JSONArray();
	JSONObject manifestUrls = createJobResponse.getJSONObject("manifestUrls");

	if(manifestUrls.has("mpdUrl"))
	{
		manifests.add(constructManifestObject(manifestUrls, "mpd", createJobResponse));
	}

	if(manifestUrls.has("m3u8Url"))
	{
		manifests.add(constructManifestObject(manifestUrls, "m3u8", createJobResponse));
	}
	encodeSummary.put("manifests", manifests);

	String json = encodeSummary.toString();
	ObjectMapper objectMapper = new ObjectMapper();
	return objectMapper.reader().withType(EncodeSummary.class).readValue(json);
	}

	@Override
	public void run()
	{
	JSONObject response, drmConfig = null;
	EncodingJob job;

	log.debug("CreateBitcodinJob : run() -> Job string is: " + jobString);
	try
	{
		try
		{
		job = Util.getBitcodinJobFromJSON(jobString);
		}
		catch(JsonSyntaxException e)
		{
		job = new EncodingJob();
		job.setOriginalJobstring(jobString);
		log.error("Could not extract bitcodin job from job string", e);
		job.setErrorType(Constants.ERROR_JSON_PARSING);
		queueManager.moveQueues(workingListName, errorListName, jobString, job.getOriginalJobstring());
		return;
		}

		InputConfig inputConfig = new InputConfig(Constants.AZURE_INPUT_TYPE, Constants.AZURE_INPUT_ACCOUNT_NAME,
			Constants.AZURE_INPUT_ACCOUNT_KEY, Constants.AZURE_INPUT_BLOB_CONTAINER_PREFIX + job.getClientId());

		OutputConfig outputConfig = new OutputConfig(Constants.OUTPUT_STORAGE_TYPE,
			Constants.BITCODIN_OUTPUT_NAME_PREFIX + job.getClientId() + "-" + job.getMediaId(),
			Constants.AZURE_OUTPUT_ACCOUNT_NAME, Constants.AZURE_OUTPUT_ACCOUNT_KEY,
			Constants.AZURE_OUTPUT_BLOB_CONTAINER_PREFIX + job.getClientId(),
			Constants.AZURE_OUTPUT_BLOB_MEDIA_PATH_PREFIX + "/" + job.getMediaId() + "/");

		// TODO: Track these status by Media Id. Dropbox processor creates
		// the  first entry which needs to be subsequently updated at each point.
		job.setStatus(Constants.STATUS_RECEIVED);

		if(job.getDrmType() != null)
		{
		try
		{
			drmConfig = BitcodinDRMConfigBuilder.getDRMConfigJSON(job);
		}
		catch(BitcodinException e)
		{
			// TODO: Define an error handler to avoid repetition
			if(e.getStatus() >= Constants.STATUS_CODE_SERVER_ERROR)
			{
			job.setRetry(true);
			}
			log.error("An error occured while fetching DRM configuration", e);
			job.setStatus(Constants.STATUS_JOB_FAILED);
			job.setErrorType(Constants.ERROR_DRM_CONFIG);
			queueManager.moveQueues(workingListName, errorListName, jobString, job.toString());
			return;
		}
		}

		try
		{
		log.debug("About to call createJob");
		response = BitcodinProxy.createJob(inputConfig, outputConfig, job, drmConfig);
		log.debug("CreateBitcodinJob : run() -> Got back the response from Bitcodin");
		job.setStatus(Constants.STATUS_JOB_SUBMITTED);
		}
		catch(BitcodinException e)
		{
		if(e.getStatus() >= Constants.STATUS_CODE_SERVER_ERROR)
		{
			job.setRetry(true);
		}
		log.error("Job creation failed", e);
		job.setStatus(Constants.STATUS_JOB_FAILED);
		job.setErrorType(Constants.ERROR_JOB_CREATION);
		queueManager.moveQueues(workingListName, errorListName, jobString, job.toString());
		return;
		}

		log.debug("Response string is: " + response.toString());

		// TODO: Error handling. Assumes Bitcodin will always return success
		// response if response code is a non-error code

		try
		{
		job.setEncodingJobId(response.getInt("jobId"));
		}
		catch(JSONException e)
		{
		// This shouldn't happen either. Implies we got a 200 from
		// Bitcodin but no jobId
		log.error("An error occured while fetching jobId from the response", e);
		job.setStatus(Constants.STATUS_JOB_FAILED);
		job.setErrorType(Constants.ERROR_GETTING_JOB_ID);
		queueManager.moveQueues(workingListName, errorListName, jobString, job.toString());
		return;
		}

		try
		{
		job.setEncodeSummary(getEncodeSummary(job, response));
		}
		catch(JSONException | IOException e)
		{
		log.error("An error occured while creating the job summary", e);
		job.setStatus(Constants.STATUS_JOB_FAILED);
		job.setErrorType(Constants.ERROR_JOB_SUMMARY);
		queueManager.moveQueues(workingListName, errorListName, jobString, job.toString());
		return;
		}

		log.info("Successfully created Job with jobId  " + job.getEncodingJobId() + "after " + job.getRetryCount()
			+ " retry\n" + "Resetting retry count to 0");
		job.setRetryCount(0);
		queueManager.moveQueues(workingListName, successListName, jobString, job.toString());
	}
	catch(QueueException e)
	{
		log.error("CreateBitcodinJob :: Queue Exception when trying to process job", e);
		return;
	}
	}

}
