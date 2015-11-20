package com.movideo.nextgen.encoder.bitcodin.models;

import java.io.Serializable;

import org.json.JSONObject;

/**
 * Holds all the information related to a Bitcodin job.
 * This gets converted to a JSON string and back,
 * when storing and processing respectively
 * @author yramasundaram
 *
 */
public class BitcodinJob implements Serializable {

	/**
	 * Auto-generated id
	 */
	private static final long serialVersionUID = -2746341744995209121L;

	private int bitcodinJobId;
	private int retryCount;
	private int encodingProfileId;
	private int inputId;
	private int outputId;
	private int clientId;
	private int mediaId;
	private String status;
	private String inputFileName;
	private String inputFileUrl;
	private String speed;
	private String[] manifestTypes;
	private String errorType;
	private String drmType;

	public BitcodinJob(int encodingProfileId, int inputId, int outputId, int clientId, int mediaId,
			String inputFilename, String inputFileUrl, String speed, String[] manifestTypes) {
		
		this.encodingProfileId = encodingProfileId;
		this.inputId = inputId;
		this.outputId = outputId;
		this.clientId = clientId;
		this.mediaId = mediaId;
		this.inputFileName = inputFilename;
		this.inputFileUrl = inputFileUrl;
		this.speed = speed;
		this.manifestTypes = manifestTypes;
	}
	
	public BitcodinJob(){}

	public int getEncodingProfileId() {
		return encodingProfileId;
	}

	public void setEncodingProfileId(int encodingProfileId) {
		this.encodingProfileId = encodingProfileId;
	}

	public int getInputId() {
		return inputId;
	}

	public void setInputId(int inputId) {
		this.inputId = inputId;
	}

	public int getOutputId() {
		return outputId;
	}

	public void setOutputId(int outputId) {
		this.outputId = outputId;
	}

	public String[] getManifestTypes() {
		return manifestTypes;
	}

	public void setManifestTypes(String[] manifestTypes) {
		this.manifestTypes = manifestTypes;
	}

	public String getSpeed() {
		return speed;
	}

	public void setSpeed(String speed) {
		this.speed = speed;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	public int getClientId() {
		return clientId;
	}

	public void setClientId(int clientId) {
		this.clientId = clientId;
	}

	public int getMediaId() {
		return mediaId;
	}

	public void setMediaId(int mediaId) {
		this.mediaId = mediaId;
	}

	public String getInputFileName() {
		return inputFileName;
	}

	public void setInputFileName(String inputFileName) {
		this.inputFileName = inputFileName;
	}

	public String getInputFileUrl() {
		return inputFileUrl;
	}

	public void setInputFileUrl(String url) {
		this.inputFileUrl = url;
	}

	public int getBitcodinJobId() {
		return bitcodinJobId;
	}

	public void setBitcodinJobId(int bitcodinJobId) {
		this.bitcodinJobId = bitcodinJobId;
	}

	public int getRetryCount() {
		return retryCount;
	}

	public void setRetryCount(int retryCount) {
		this.retryCount = retryCount;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getErrorType() {
		return errorType;
	}

	public void setErrorType(String errorType) {
		this.errorType = errorType;
	}

	public String getDrmType() {
		return drmType;
	}

	public void setDrmType(String drmType) {
		this.drmType = drmType;
	}

	@Override
	public String toString() {
		return (new JSONObject(this)).toString();
	}

}
