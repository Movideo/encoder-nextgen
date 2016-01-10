package com.movideo.nextgen.common.encoder.models;

import java.util.List;

public class EncodeRequest
{

	private int clientId;
	private int mediaId;
	private String productId;
	private String variant;
	private String inputFilename;
	private List<EncodeInfo> encodeInfo;
	private List<SubtitleInfo> subtitleInfo;
	private String speed;
	private boolean cdnSyncRequired;

	public EncodeRequest()
	{
		//TODO: Turning on cdn sync by default. Needs to come from CouchDB client settings
		cdnSyncRequired = true;
	}

	public int getClientId()
	{
		return clientId;
	}

	public void setClientId(int clientId)
	{
		this.clientId = clientId;
	}

	public int getMediaId()
	{
		return mediaId;
	}

	public void setMediaId(int mediaId)
	{
		this.mediaId = mediaId;
	}

	public String getProductId()
	{
		return productId;
	}

	public void setProductId(String productId)
	{
		this.productId = productId;
	}

	public String getVariant()
	{
		return variant;
	}

	public void setVariant(String variant)
	{
		this.variant = variant;
	}

	public String getInputFilename()
	{
		return inputFilename;
	}

	public void setInputFilename(String inputFilename)
	{
		this.inputFilename = inputFilename;
	}

	public List<EncodeInfo> getEncodeInfo()
	{
		return encodeInfo;
	}

	public void setEncodeInfo(List<EncodeInfo> encodeInfo)
	{
		this.encodeInfo = encodeInfo;
	}

	public List<SubtitleInfo> getSubtitleInfo()
	{
		return subtitleInfo;
	}

	public void setSubtitleInfo(List<SubtitleInfo> subtitleInfo)
	{
		this.subtitleInfo = subtitleInfo;
	}

	public String getSpeed()
	{
		return speed;
	}

	public void setSpeed(String speed)
	{
		this.speed = speed;
	}

	public boolean isCdnSyncRequired()
	{
		return cdnSyncRequired;
	}

	public void setCdnSyncRequired(boolean cdnSyncRequired)
	{
		this.cdnSyncRequired = cdnSyncRequired;
	}

}
