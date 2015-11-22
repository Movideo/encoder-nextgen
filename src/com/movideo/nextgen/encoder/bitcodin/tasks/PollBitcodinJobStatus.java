package com.movideo.nextgen.encoder.bitcodin.tasks;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import com.movideo.nextgen.encoder.bitcodin.BitcodinException;
import com.movideo.nextgen.encoder.bitcodin.BitcodinProxy;
import com.movideo.nextgen.encoder.common.Util;
import com.movideo.nextgen.encoder.config.Constants;
import com.movideo.nextgen.encoder.models.EncodingJob;
import com.movideo.nextgen.encoder.tasks.Task;

import redis.clients.jedis.JedisPool;

/**
 * Polls Bitcodin for the status of the id specified in the input
 * If the status is completed or errored, moves it to the appropriate list
 * If not, replaces the message back in the list for round-robin polling
 * @author yramasundaram
 *
 */
public class PollBitcodinJobStatus extends Task {
	
	private static final Logger log = Logger.getLogger(PollBitcodinJobStatus.class);


	private String pendingListName = Constants.REDIS_PENDING_LIST,
			workingListName = Constants.REDIS_PENDING_WORKING_LIST, errorListName = Constants.REDIS_POLL_ERROR_LIST,
			successListName = Constants.REDIS_FINISHED_LIST;

	public PollBitcodinJobStatus(JedisPool redisPool, String jobString) {
		super(redisPool, jobString);
	}

	@Override
	public void run() {
		log.debug("Executing poller");

		String status;
		JSONObject input, response;
		EncodingJob job;

		log.debug("Input string is: " + jobString);
		
		try {
			input = new JSONObject(jobString);
			log.debug("JSON Object version: \n" + input);
		} catch (JSONException e) {
			log.warn("Could not convert job to JSON Object", e);
			Util.moveJobToNextList(redisPool, workingListName, errorListName, jobString, jobString);
			return;
		}
		try {
			job = Util.getBitcodinJobFromJSON(input);
		} catch (JSONException e) {
			log.warn("Could not extract bitcodin job from JSON Object", e);
			Util.moveJobToNextList(redisPool, workingListName, errorListName, jobString, jobString);
			return;
		}

		log.debug("Now polling Job id: " + job.getEncodingJobId());

		try {
			response = BitcodinProxy.getJobStatus(job.getEncodingJobId());
			status = response.getString("status");
			log.debug("STATUS IS: " + status);

		} catch (BitcodinException | JSONException e) {
			log.error("Encoding failed for the job id "+job.getEncodingJobId(), e);
			job.setErrorType(Constants.STATUS_JOB_FAILED);
			Util.moveJobToNextList(redisPool, workingListName, errorListName, jobString, job.toString());
			return;
		}

		if (status != null && (status.equalsIgnoreCase("Created") || status.equalsIgnoreCase("Enqueued")
				|| status.equalsIgnoreCase("In Progress"))) {
			// Put back in the pending list to check back later
			Util.moveJobToNextList(redisPool, workingListName, pendingListName, jobString, job.toString());

		} else if (status != null && status.equalsIgnoreCase("Finished")) {
			Util.moveJobToNextList(redisPool, workingListName, successListName, jobString, job.toString());

		} else {
			log.debug("job failed");
			job.setStatus(Constants.STATUS_JOB_FAILED);
			Util.moveJobToNextList(redisPool, workingListName, errorListName, jobString, job.toString());
			return;
		}

	}

}
