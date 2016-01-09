package com.movideo.nextgen.common.multithreading;

public abstract class Throttler
{
	protected long maxJobCount;

	public Throttler()
	{
	}

	public Throttler(long maxJobCount)
	{
		this.maxJobCount = maxJobCount;
	}

	public abstract boolean isThrottleNeeded();

	public long getMaxJobCount()
	{
		return maxJobCount;
	}

	public void setMaxJobCount(int maxJobCount)
	{
		this.maxJobCount = maxJobCount;
	}

}
