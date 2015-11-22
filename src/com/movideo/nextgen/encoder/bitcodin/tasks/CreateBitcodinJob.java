package com.movideo.nextgen.encoder.bitcodin.tasks;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import redis.clients.jedis.JedisPool;

import com.movideo.nextgen.encoder.bitcodin.BitcodinDRMConfigBuilder;
import com.movideo.nextgen.encoder.bitcodin.BitcodinException;
import com.movideo.nextgen.encoder.bitcodin.BitcodinProxy;
import com.movideo.nextgen.encoder.common.Util;
import com.movideo.nextgen.encoder.config.Constants;
import com.movideo.nextgen.encoder.models.EncodingJob;
import com.movideo.nextgen.encoder.models.InputConfig;
import com.movideo.nextgen.encoder.tasks.Task;

/**
 * Runnable class that submits a new job to Bitcodin and queues it into the 
 * pending list for subsequent polling
 * @author yramasundaram
 *
 */
public class CreateBitcodinJob extends Task {

	private static final Logger log = Logger.getLogger(CreateBitcodinJob.class);

	private String workingListName = Constants.REDIS_INPUT_WORKING_LIST, errorListName = Constants.REDIS_JOB_ERROR_LIST, successListName = Constants.REDIS_PENDING_LIST;
	
	// TODO: Config
	private InputConfig inputConfig = new InputConfig(Constants.AZURE_INPUT_TYPE, Constants.AZURE_INPUT_ACCOUNT_NAME,
			Constants.AZURE_INPUT_ACCOUNT_KEY, Constants.AZURE_INPUT_BLOB_CONTAINER_PREFIX + Constants.clientId);

	public CreateBitcodinJob(JedisPool pool, String jobString) {
		super(pool, jobString);
	}

	@Override
	public void run() {
		
		log.debug("Executing job creator");

		JSONObject jobJson, response, drmConfig = null;
		EncodingJob job;
		String outputid;
		
		// int mediaId;

		log.debug("Job string is: " + jobString);

		try {
			jobJson = new JSONObject(jobString);
			log.debug("Processing Media: " + jobJson.get("mediaId"));
		} catch (JSONException e) {
			// Ideally, this should NEVER happen
			log.warn("Could not convert job to JSON Object", e);
			Util.moveJobToNextList(redisPool, workingListName, errorListName, jobString, jobString);
			return;
		}

		log.debug("JobJSON string is: " + jobJson);

		try {
			job = Util.getBitcodinJobFromJSON(jobJson);
		} catch (JSONException e) {
			// This should never happen either, because the input string is controlled by us
			log.warn("Could not extract bitcodin job from JSON Object", e);
			Util.moveJobToNextList(redisPool, workingListName, errorListName, jobString, jobString);
			return;
		}

		//TODO: Track these statuses by Media Id. Dropbox processor creates the first entry
		// which needs to be subsquently updated at each point.
		job.setStatus(Constants.STATUS_RECEIVED);
		
		if(job.getDrmType() != null){
			try {
				drmConfig = BitcodinDRMConfigBuilder.getDRMConfigJSON(job);
			} catch (BitcodinException e) {
				//TODO: Define an error handler to avoid repetition
				log.error("An error occured while fetching DRM configuration",e);
				job.setStatus(Constants.STATUS_JOB_FAILED);
				Util.moveJobToNextList(redisPool, workingListName, errorListName, jobString, job.toString());

				return;
			}
		}
		 

		try {
			response = BitcodinProxy.createJob(inputConfig, job, drmConfig);
			log.debug("Got back the response from Bitcodin");
			job.setStatus(Constants.STATUS_JOB_SUBMITTED);
		} catch (BitcodinException e) {
			e.printStackTrace();
			job.setStatus(Constants.STATUS_JOB_FAILED);
			Util.moveJobToNextList(redisPool, workingListName, errorListName, jobString, job.toString());

			return;
		}
		
		log.debug("Response string for create job is: " + response.toString());

		try {
			job.setEncodingJobId(response.getInt("jobId"));
		} catch (JSONException e) {
			e.printStackTrace();
			// This shouldn't happen either. Implies we got a 200 from
			// Bitcodin but no jobId

			job.setStatus(Constants.STATUS_JOB_FAILED);
			Util.moveJobToNextList(redisPool, workingListName, errorListName, jobString, job.toString());
			return;
		}

		Util.moveJobToNextList(redisPool, workingListName, successListName, jobString, job.toString());

	}

}
