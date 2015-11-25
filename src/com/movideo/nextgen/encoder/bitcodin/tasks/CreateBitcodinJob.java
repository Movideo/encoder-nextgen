package com.movideo.nextgen.encoder.bitcodin.tasks;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.gson.JsonSyntaxException;
import com.movideo.nextgen.common.multithreading.Task;
import com.movideo.nextgen.common.queue.QueueException;
import com.movideo.nextgen.common.queue.QueueManager;
import com.movideo.nextgen.encoder.bitcodin.BitcodinDRMConfigBuilder;
import com.movideo.nextgen.encoder.bitcodin.BitcodinException;
import com.movideo.nextgen.encoder.bitcodin.BitcodinProxy;
import com.movideo.nextgen.encoder.common.Util;
import com.movideo.nextgen.encoder.config.Constants;
import com.movideo.nextgen.encoder.models.EncodingJob;
import com.movideo.nextgen.encoder.models.InputConfig;

/**
 * Runnable class that submits a new job to Bitcodin and queues it into the
 * pending list for subsequent polling
 * 
 * @author yramasundaram
 *
 */
public class CreateBitcodinJob extends Task {

    private static final Logger log = LogManager.getLogger();

    private String workingListName = Constants.REDIS_INPUT_WORKING_LIST, errorListName = Constants.REDIS_JOB_ERROR_LIST,
	    successListName = Constants.REDIS_PENDING_LIST;

    // TODO: Config
    private InputConfig inputConfig = new InputConfig(Constants.AZURE_INPUT_TYPE, Constants.AZURE_INPUT_ACCOUNT_NAME,
	    Constants.AZURE_INPUT_ACCOUNT_KEY, Constants.AZURE_INPUT_BLOB_CONTAINER_PREFIX + Constants.clientId);

    public CreateBitcodinJob(QueueManager queueManager, String jobString) {
	super(queueManager, jobString);
    }

    @Override
    public void run() {

	log.debug("CreateBitcodinJob : run() -> Executing job creator");

	JSONObject response, drmConfig = null;
	EncodingJob job;

	// int mediaId;
	// TODO: Replace all Sysouts with proper log statements. Retain key
	// information for debug purposes

	log.debug("CreateBitcodinJob : run() -> Job string is: " + jobString);

	try {

	    try {
		job = Util.getBitcodinJobFromJSON(jobString);
	    } catch (JsonSyntaxException e) {
		log.error("Could not extract bitcodin job from job string", e);
		queueManager.moveQueues(workingListName, errorListName, jobString, null);
		// Util.moveJobToNextList(redisPool, workingListName,
		// errorListName, jobString, jobString);
		return;
	    }

	    // TODO: Track these statuses by Media Id. Dropbox processor creates
	    // the
	    // first entry
	    // which needs to be subsquently updated at each point.
	    job.setStatus(Constants.STATUS_RECEIVED);

	    if (job.getDrmType() != null) {
		try {
		    drmConfig = BitcodinDRMConfigBuilder.getDRMConfigJSON(job);
		} catch (BitcodinException e) {
		    // TODO: Define an error handler to avoid repetition
		    log.error("An error occured while fetching DRM configuration", e);
		    job.setStatus(Constants.STATUS_JOB_FAILED);
		    queueManager.moveQueues(workingListName, errorListName, jobString, job.toString());
		    // Util.moveJobToNextList(redisPool, workingListName,
		    // errorListName, jobString, job.toString());

		    return;
		}
	    }

	    try {
		log.debug("About to call createJob");
		response = BitcodinProxy.createJob(inputConfig, job, drmConfig);
		log.debug("CreateBitcodinJob : run() -> Got back the response from Bitcodin");
		job.setStatus(Constants.STATUS_JOB_SUBMITTED);
	    } catch (BitcodinException e) {
		log.error("Job creation failed", e);
		job.setStatus(Constants.STATUS_JOB_FAILED);
		queueManager.moveQueues(workingListName, errorListName, jobString, job.toString());
		// Util.moveJobToNextList(redisPool, workingListName,
		// errorListName, jobString, job.toString());

		return;
	    }

	    log.debug("Response string is: " + response.toString());

	    try {
		job.setEncodingJobId(response.getInt("jobId"));
	    } catch (JSONException e) {
		// This shouldn't happen either. Implies we got a 200 from
		// Bitcodin but no jobId
		log.error("An error occured while fetching jobId from the response", e);
		job.setStatus(Constants.STATUS_JOB_FAILED);
		queueManager.moveQueues(workingListName, errorListName, jobString, job.toString());
		// Util.moveJobToNextList(redisPool, workingListName,
		// errorListName, jobString, job.toString());
		return;
	    }

	    // Util.moveJobToNextList(redisPool, workingListName,
	    // successListName, jobString, job.toString());
	    queueManager.moveQueues(workingListName, successListName, jobString, job.toString());
	} catch (QueueException e) {
	    log.error("CreateBitcodinJob :: Queue Exception when trying to process job " + e.getMessage());
	    return;
	}
    }

}
