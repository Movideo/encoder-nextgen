package com.movideo.nextgen.encoder.bitcodin.tasks;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.gson.JsonSyntaxException;
import com.movideo.nextgen.common.multithreading.Task;
import com.movideo.nextgen.common.queue.QueueException;
import com.movideo.nextgen.common.queue.QueueManager;
import com.movideo.nextgen.encoder.bitcodin.BitcodinException;
import com.movideo.nextgen.encoder.bitcodin.BitcodinProxy;
import com.movideo.nextgen.encoder.common.Util;
import com.movideo.nextgen.encoder.config.Constants;
import com.movideo.nextgen.encoder.models.EncodingJob;

/**
 * Polls Bitcodin for the status of the id specified in the input If the status
 * is completed or errored, moves it to the appropriate list If not, replaces
 * the message back in the list for round-robin polling
 * 
 * @author yramasundaram
 *
 */
public class PollBitcodinJobStatus extends Task {

    private static final Logger log = LogManager.getLogger();

    private String pendingListName = Constants.REDIS_PENDING_LIST,
	    workingListName = Constants.REDIS_PENDING_WORKING_LIST, errorListName = Constants.REDIS_POLL_ERROR_LIST,
	    successListName = Constants.REDIS_FINISHED_LIST;

    public PollBitcodinJobStatus(QueueManager manager, String jobString) {
	super(manager, jobString);
    }

    @Override
    public void run() {

	log.debug("PollBitcodinJobStatus : run() -> Executing poller");

	String status;
	JSONObject response;
	EncodingJob job;

	log.debug("PollBitcodinJobStatus : run() -> Job string is: " + jobString);
	try {

	    try {
		job = Util.getBitcodinJobFromJSON(jobString);
	    } catch (JsonSyntaxException e) {
		log.error("Could not extract bitcodin job from JSON Object", e);
		queueManager.moveQueues(workingListName, errorListName, jobString, null);
		// Util.moveJobToNextList(redisPool, workingListName,
		// errorListName, jobString, jobString);
		return;
	    }

	    log.debug("PollBitcodinJobStatus : run() -> Now polling Job id: " + job.getEncodingJobId());

	    try {
		response = BitcodinProxy.getJobStatus(job.getEncodingJobId());
		status = response.getString("status");
		log.debug("PollBitcodinJobStatus : run() -> Response Status is: " + status);

	    } catch (BitcodinException | JSONException e) {
		log.error("Encoding failed for the job id " + job.getEncodingJobId(), e);
		job.setErrorType(Constants.STATUS_JOB_FAILED);
		queueManager.moveQueues(workingListName, errorListName, jobString, job.toString());
		// Util.moveJobToNextList(redisPool, workingListName,
		// errorListName, jobString, job.toString());
		return;
	    }

	    if (status != null && (status.equalsIgnoreCase("Created") || status.equalsIgnoreCase("Enqueued")
		    || status.equalsIgnoreCase("In Progress"))) {
		// Put back in the pending list to check back later
		queueManager.moveQueues(workingListName, pendingListName, jobString, job.toString());
		// Util.moveJobToNextList(redisPool, workingListName,
		// pendingListName, jobString, job.toString());

	    } else if (status != null && status.equalsIgnoreCase("Finished")) {
		queueManager.moveQueues(workingListName, successListName, jobString, job.toString());
		// Util.moveJobToNextList(redisPool, workingListName,
		// successListName, jobString, job.toString());

	    } else {
		log.error("Job failed");
		job.setStatus(Constants.STATUS_JOB_FAILED);
		queueManager.moveQueues(workingListName, errorListName, jobString, job.toString());
		// Util.moveJobToNextList(redisPool, workingListName,
		// errorListName, jobString, job.toString());
		return;
	    }
	} catch (QueueException e) {
	    log.error("PollBitcodinJob :: Queue Exception when trying to process job " + e.getMessage());
	    return;
	}

    }

}
