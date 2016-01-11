package com.movideo.nextgen.encoder.models;

import java.io.Serializable;
import java.util.List;

import org.json.JSONObject;

import com.movideo.nextgen.common.encoder.models.SubtitleInfo;

/**
 * Holds all the information related to a Bitcodin job. This gets converted to a JSON string and back, when storing and processing respectively
 * 
 * @author yramasundaram
 */
public class EncodingJob implements Serializable
{

	/**
	 * Auto-generated id
	 */
	private static final long serialVersionUID = -2746341744995209121L;

	private int encodingJobId;
	private int retryCount;
	private int encodingProfileId;
	private long inputId;
	private long outputId;
	private int clientId;
	private int mediaId;
	private String productId;
	private String variant;
	private String status;
	private String inputFileName;
	private String inputFileUrl;
	private String speed;
	private String[] manifestTypes;
	private String errorType;
	private boolean protectionRequired;
	private EncodeSummary encodeSummary;
	private boolean reprocess;
	private List<SubtitleInfo> subtitleList;
	private FtpInfo cdnFtpInfo;
	private boolean cdnSyncRequired;

	public EncodingJob()
	{
		//TODO: Hardcoding VTN prod FTP Info as default. Needs to come from CouchDB client config
		cdnFtpInfo = new FtpInfo();
		cdnFtpInfo.setHost("ftphcm.cdnviet.com");
		cdnFtpInfo.setPassword("bhd@123#");
		cdnFtpInfo.setUsername("wmhyqyg");
		cdnFtpInfo.setIp("113.164.15.170");
	}

	public int getEncodingProfileId()
	{
		return encodingProfileId;
	}

	public void setEncodingProfileId(int encodingProfileId)
	{
		this.encodingProfileId = encodingProfileId;
	}

	public long getInputId()
	{
		return inputId;
	}

	public void setInputId(long inputId)
	{
		this.inputId = inputId;
	}

	public long getOutputId()
	{
		return outputId;
	}

	public void setOutputId(long outputId)
	{
		this.outputId = outputId;
	}

	public String[] getManifestTypes()
	{
		return manifestTypes;
	}

	public void setManifestTypes(String[] manifestTypes)
	{
		this.manifestTypes = manifestTypes;
	}

	public String getSpeed()
	{
		return speed;
	}

	public void setSpeed(String speed)
	{
		this.speed = speed;
	}

	public static long getSerialversionuid()
	{
		return serialVersionUID;
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

	public String getInputFileName()
	{
		return inputFileName;
	}

	public void setInputFileName(String inputFileName)
	{
		this.inputFileName = inputFileName;
	}

	public String getInputFileUrl()
	{
		return inputFileUrl;
	}

	public void setInputFileUrl(String url)
	{
		this.inputFileUrl = url;
	}

	public int getEncodingJobId()
	{
		return encodingJobId;
	}

	public void setEncodingJobId(int encodingJobId)
	{
		this.encodingJobId = encodingJobId;
	}

	public int getRetryCount()
	{
		return retryCount;
	}

	public void setRetryCount(int retryCount)
	{
		this.retryCount = retryCount;
	}

	public String getStatus()
	{
		return status;
	}

	public void setStatus(String status)
	{
		this.status = status;
	}

	public String getErrorType()
	{
		return errorType;
	}

	public void setErrorType(String errorType)
	{
		this.errorType = errorType;
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

	public EncodeSummary getEncodeSummary()
	{
		return encodeSummary;
	}

	public void setEncodeSummary(EncodeSummary encodeSummary)
	{
		this.encodeSummary = encodeSummary;
	}

	public boolean isProtectionRequired()
	{
		return protectionRequired;
	}

	public void setProtectionRequired(boolean protectionRequired)
	{
		this.protectionRequired = protectionRequired;
	}

	public boolean isReprocess()
	{
		return reprocess;
	}

	public void setReprocess(boolean reprocess)
	{
		this.reprocess = reprocess;
	}

	public List<SubtitleInfo> getSubtitleList()
	{
		return subtitleList;
	}

	public void setSubtitleList(List<SubtitleInfo> subtitleList)
	{
		this.subtitleList = subtitleList;
	}

	public FtpInfo getCdnFtpInfo()
	{
		return cdnFtpInfo;
	}

	public void setCdnFtpInfo(FtpInfo cdnFtpInfo)
	{
		this.cdnFtpInfo = cdnFtpInfo;
	}

	public boolean isCdnSyncRequired()
	{
		return cdnSyncRequired;
	}

	public void setCdnSyncRequired(boolean cdnSyncRequired)
	{
		this.cdnSyncRequired = cdnSyncRequired;
	}

	@Override
	public String toString()
	{
		return (new JSONObject(this)).toString();
	}

}
