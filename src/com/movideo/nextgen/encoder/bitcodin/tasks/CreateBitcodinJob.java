package com.movideo.nextgen.encoder.bitcodin.tasks;

import org.json.JSONException;
import org.json.JSONObject;

import com.movideo.nextgen.encoder.bitcodin.BitcodinDRMConfigBuilder;
import com.movideo.nextgen.encoder.bitcodin.BitcodinException;
import com.movideo.nextgen.encoder.bitcodin.BitcodinProxy;
import com.movideo.nextgen.encoder.common.Util;
import com.movideo.nextgen.encoder.config.Constants;
import com.movideo.nextgen.encoder.models.DRMInfo;
import com.movideo.nextgen.encoder.models.EncodingJob;
import com.movideo.nextgen.encoder.models.InputConfig;
import com.movideo.nextgen.encoder.tasks.Task;

import redis.clients.jedis.JedisPool;

/**
 * Runnable class that submits a new job to Bitcodin and queues it into the 
 * pending list for subsequent polling
 * @author yramasundaram
 *
 */
public class CreateBitcodinJob extends Task {

	private String workingListName = Constants.REDIS_INPUT_WORKING_LIST, errorListName = Constants.REDIS_JOB_ERROR_LIST, successListName = Constants.REDIS_PENDING_LIST;
	
	// TODO: Config
	private InputConfig inputConfig = new InputConfig(Constants.AZURE_INPUT_TYPE, Constants.AZURE_INPUT_ACCOUNT_NAME,
			Constants.AZURE_INPUT_ACCOUNT_KEY, Constants.AZURE_INPUT_BLOB_CONTAINER_PREFIX + Constants.clientId);

	public CreateBitcodinJob(JedisPool pool, String jobString) {
		super(pool, jobString);
	}

	@Override
	public void run() {
		
		JSONObject jobJson, response, drmConfig = null;
		EncodingJob job;
		
		// int mediaId;
		//TODO: Replace all Sysouts with proper log statements. Retain key information for debug purposes
		System.out.println("In job creator");

		System.out.println("Job string is: " + jobString);

		try {
			jobJson = new JSONObject(jobString);
			System.out.println("Processing Media: " + jobJson.get("mediaId"));
		} catch (JSONException e) {
			// Ideally, this should NEVER happen
			Util.moveJobToNextList(redisPool, workingListName, errorListName, jobString, jobString);
			return;
		}

		System.out.println("JobJSON string is: " + jobJson);

		try {
			job = Util.getBitcodinJobFromJSON(jobJson);
		} catch (JSONException e) {
			// This should never happen either, because the input string is controlled by us
			e.printStackTrace();
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
				e.printStackTrace();
				job.setStatus(Constants.STATUS_JOB_FAILED);
				Util.moveJobToNextList(redisPool, workingListName, errorListName, jobString, job.toString());

				return;
			}
		}
		 

		try {
			response = BitcodinProxy.createJob(inputConfig, job, drmConfig);
			System.out.println("Got back the response from Bitcodin");
			job.setStatus(Constants.STATUS_JOB_SUBMITTED);
		} catch (BitcodinException e) {
			e.printStackTrace();
			job.setStatus(Constants.STATUS_JOB_FAILED);
			Util.moveJobToNextList(redisPool, workingListName, errorListName, jobString, job.toString());

			return;
		}
		
		System.out.println("Response string is: " + response.toString());

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
