package com.movideo.nextgen.encoder.tasks;

import org.json.JSONException;
import org.json.JSONObject;

import com.movideo.nextgen.encoder.bitcodin.BitcodinException;
import com.movideo.nextgen.encoder.bitcodin.BitcodinProxy;
import com.movideo.nextgen.encoder.bitcodin.models.BitcodinJob;
import com.movideo.nextgen.encoder.common.Util;
import com.movideo.nextgen.encoder.config.Constants;

import redis.clients.jedis.JedisPool;

/**
 * Polls Bitcodin for the status of the id specified in the input
 * If the status is completed or errored, moves it to the appropriate list
 * If not, replaces the message back in the list for round-robin polling
 * @author yramasundaram
 *
 */
public class PollBitcodinJobStatus extends Task {

	private String pendingListName = Constants.REDIS_PENDING_LIST,
			workingListName = Constants.REDIS_PENDING_WORKING_LIST, errorListName = Constants.REDIS_POLL_ERROR_LIST,
			successListName = Constants.REDIS_FINISHED_LIST;

	public PollBitcodinJobStatus(JedisPool redisPool, String jobString) {
		super(redisPool, jobString);
	}

	@Override
	public void run() {

		String status;
		JSONObject input, response;
		BitcodinJob job;

		System.out.println("Inside poller");
		System.out.println("Input string is: " + jobString);
		try {
			input = new JSONObject(jobString);
			System.out.println("JSON Object version: \n" + input);
		} catch (JSONException e) {
			Util.moveJobToNextList(redisPool, workingListName, errorListName, jobString, jobString);
			return;
		}
		try {
			job = Util.getBitcodinJobFromJSON(input);
		} catch (JSONException e) {
			Util.moveJobToNextList(redisPool, workingListName, errorListName, jobString, jobString);
			return;
		}

		System.out.println("Now polling Job id: " + job.getBitcodinJobId());

		try {
			response = BitcodinProxy.getJobStatus(job.getBitcodinJobId());
			status = response.getString("status");
			System.out.println("STATUS IS: " + status);

		} catch (BitcodinException | JSONException e) {
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
			System.out.println("I'm in the ERROR status block");
			job.setStatus(Constants.STATUS_JOB_FAILED);
			Util.moveJobToNextList(redisPool, workingListName, errorListName, jobString, job.toString());
			return;
		}

	}

}
