package com.movideo.nextgen.encoder.bitcodin.tasks;

import java.util.Calendar;

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
import com.movideo.nextgen.encoder.dao.EncodeDAO;
import com.movideo.nextgen.encoder.models.EncodingJob;

/**
 * In case of error,Polls Bitcodin for the status of the id specified in the
 * input If the status is completed or errored, moves it to the appropriate list
 * If not, replaces the message back in the list for round-robin polling
 *
 * @author yramasundaram
 */
public class RetryPollBitcodinJobStatusTask extends Task {

    private static final Logger log = LogManager.getLogger();
    private static final int MAX_RETRIES = 3;
    private static final long MAX_WAIT_INTERVAL = 600000;
    private Calendar calendar = Calendar.getInstance();

    private String workingListName = Constants.REDIS_POLL_ERROR_WORKING_LIST,
	    errorListName = Constants.REDIS_POLL_ERROR_LIST, successListName = Constants.REDIS_FINISHED_LIST,
	    irrecoverableListName = Constants.REDIS_JOB_IRRECOVERABLE_ERROR_LIST;

    private EncodeDAO encodeDAO;

    public RetryPollBitcodinJobStatusTask(QueueManager manager, EncodeDAO encodeDAO, String jobString) {
	super(manager, jobString);
	this.encodeDAO = encodeDAO;
    }

    @Override
    public void run() {

	log.debug("RetryPollBitcodinJobStatusTask : run() -> Executing poller");

	String status;
	JSONObject response;
	EncodingJob job;

	log.debug("RetryPollBitcodinJobStatusTask : run() -> Job string is: " + jobString);
	try {

	    try {
		job = Util.getBitcodinJobFromJSON(jobString);
	    } catch (JsonSyntaxException e) {
		job = new EncodingJob();
		job.setOriginalJobstring(jobString);
		log.error("Could not extract bitcodin job from JSON Object", e);
		queueManager.moveQueues(workingListName, errorListName, jobString, job.getOriginalJobstring());
		return;
	    }

	    log.debug("RetryPollBitcodinJobStatusTask : run() -> Now polling Job id: " + job.getEncodingJobId());
	    job.setStatus(Constants.STATUS_POLL_RETRY);
	    if (job.isRetry() && job.getRetryCount() < MAX_RETRIES) {
		if (job.getRetryTime() < calendar.getTimeInMillis()) {
		    long waitTime = Util.getWaitTimeExp(job.getRetryCount());
		    job.setRetryTime(waitTime + calendar.getTimeInMillis());
		    job.setRetryCount(job.getRetryCount() + 1);

		    try {
			response = BitcodinProxy.getJobStatus(job.getEncodingJobId());
			status = response.getString("status");
			log.debug("RetryPollBitcodinJobStatusTask : run() -> Response Status is: " + status);

		    } catch (BitcodinException | JSONException e) {
			log.error("Encoding failed for the job id " + job.getEncodingJobId(), e);
			job.setErrorType(Constants.STATUS_POLL_RETRY_FAILED);
			queueManager.moveQueues(workingListName, errorListName, jobString, job.toString());
			return;
		    }

		    if (status != null && (status.equalsIgnoreCase("Created") || status.equalsIgnoreCase("Enqueued")
			    || status.equalsIgnoreCase("In Progress"))) {
			// Put back in the pending error list to check back
			// later
			queueManager.moveQueues(workingListName, errorListName, jobString, job.toString());

		    } else if (status != null && status.equalsIgnoreCase("Finished")) {
			queueManager.moveQueues(workingListName, successListName, jobString, job.toString());
			log.debug("Encode summary for this job is: " + job.getEncodeSummary());
			encodeDAO.storeEncodeSummary(job.getEncodeSummary());

		    } else {
			log.error("Job failed");
			job.setStatus(Constants.STATUS_POLL_RETRY_FAILED);
			queueManager.moveQueues(workingListName, errorListName, jobString, job.toString());
			return;
		    }
		} else
		    queueManager.moveQueues(workingListName, errorListName, jobString, job.toString());
	    } else {
		if (!job.isRetry())
		    log.debug("Failed due to invalid JSON -> " + job.getOriginalJobstring()
			    + " moving to irrecoverable list");
		else
		    log.debug("Maximum count of " + job.getRetryCount() + " reached moving to irrecoverable list");
		queueManager.moveQueues(workingListName, irrecoverableListName, jobString, job.toString());
	    }
	} catch (QueueException e) {
	    log.error("RetryPollBitcodinJobStatusTask :: Queue Exception when trying to process job " + e.getMessage());
	    return;
	}

    }

}
