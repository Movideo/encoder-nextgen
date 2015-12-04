package com.movideo.nextgen.common.encoder.models;

public class EncodeInfo
{

	private int encodingProfileId;
	private StreamInfo streamInfo;
	private boolean reprocessing;

	public int getEncodingProfileId()
	{
		return encodingProfileId;
	}

	public void setEncodingProfileId(int encodingProfileId)
	{
		this.encodingProfileId = encodingProfileId;
	}

	public StreamInfo getStreamInfo()
	{
		return streamInfo;
	}

	public void setStreamInfo(StreamInfo streamInfo)
	{
		this.streamInfo = streamInfo;
	}

	public boolean isReprocessing()
	{
		return reprocessing;
	}

	public void setReprocessing(boolean reprocessing)
	{
		this.reprocessing = reprocessing;
	}

}
