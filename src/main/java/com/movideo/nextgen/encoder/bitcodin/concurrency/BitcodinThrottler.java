package com.movideo.nextgen.encoder.bitcodin.concurrency;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;

import com.movideo.nextgen.common.multithreading.Throttler;
import com.movideo.nextgen.encoder.bitcodin.BitcodinException;
import com.movideo.nextgen.encoder.bitcodin.BitcodinProxy;
import com.movideo.nextgen.encoder.common.Util;

public class BitcodinThrottler extends Throttler
{
	private static final Logger log = LogManager.getLogger();
	private Map<String, Long> limits = new HashMap<>();
	private String jobType;
	private long limit;

	public String getJobType()
	{
		return jobType;
	}

	public BitcodinThrottler()
	{
		//By default we assume job type is standard
		this(Util.getConfigProperty("bitcodin.job.speed.standard"));
	}

	public BitcodinThrottler(String jobType)
	{
		initLimits();
		this.jobType = jobType;
		this.limit = limits.get(jobType);
	}

	private void initLimits()
	{
		limits.put(Util.getConfigProperty("bitcodin.job.speed.standard"), Long.parseLong(Util.getConfigProperty("bitcodin.job.speed.standard.job.limit")));
		limits.put(Util.getConfigProperty("bitcodin.job.speed.premium"), Long.parseLong(Util.getConfigProperty("bitcodin.job.speed.premium.job.limit")));
	}

	//Method not synchronized because it's only called from a single Threadpoolmanager thread
	@Override
	public boolean isThrottleNeeded()
	{
		long remainingSlots;

		try
		{
			remainingSlots = limit - getActiveJobsCount();
		}
		catch(BitcodinException | JSONException e)
		{
			log.error("Unable to get active jobs count from Bitcodin", e);
			//Unable to reach Bitcodin. Better to ask the threadpool manager to stop pushing more jobs
			return true;
		}
		if(remainingSlots <= 0)
		{
			return true;
		}
		return false;
	}

	private long getActiveJobsCount() throws BitcodinException, JSONException
	{
		String counterField = Util.getConfigProperty("bitcodin.job.status.response.counter");
		return BitcodinProxy.getJobCount(Util.getConfigProperty("bitcodin.job.status.inprogress")).getLong(counterField) +
				BitcodinProxy.getJobCount(Util.getConfigProperty("bitcodin.job.status.enqueued")).getLong(counterField);
	}

}
