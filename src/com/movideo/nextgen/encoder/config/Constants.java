package com.movideo.nextgen.encoder.config;

/**
 * Contains all the constants. Right now, this class is a dump
 * containing every configuration ever needed. Needs to be cleaned up
 * and portion of this information needs to come from dropbox and some
 * others from the application init
 * @author yramasundaram
 *
 */
public interface Constants {
	
	/* Hardcoding */
	int clientId = 524;
	
	/* Redis */
	String REDIS_CONNECTION_STRING = "localhost";
	String REDIS_INPUT_LIST = "INPUT_LIST";
	String REDIS_INPUT_WORKING_LIST = "INPUT_LIST_WORKING";
	String REDIS_PENDING_LIST = "PENDING_LIST";
	String REDIS_PENDING_WORKING_LIST = "PENDING_LIST_WORKING";
	String REDIS_JOB_ERROR_LIST = "JOB_ERROR_LIST";
	String REDIS_POLL_ERROR_LIST = "POLL_ERROR_LIST";
	String REDIS_FINISHED_LIST = "FINISHED_LIST";
	
	/* Bitcodin */
	String BITCODIN_API_KEY = "78e7e7a41713c7c5d3b0aaa2279ec9c07a84d60e84df1bcb5a97ddd6e6ecb711";
	String BITCODIN_API_URL_PREFIX = "https://portal.bitcodin.com/api/";
	
	/* Azure BLOB */
	String AZURE_INPUT_TYPE = "abs";
	String AZURE_INPUT_ACCOUNT_NAME = "movideoqaoriginal1";
	String AZURE_INPUT_URL_PREFIX = "http://" + AZURE_INPUT_ACCOUNT_NAME + ".blob.core.windows.net/";
	String AZURE_INPUT_ACCOUNT_KEY = "WC04CZf/RGvozV3852blzAVU10Zvngz4t4ftYGwobh0wlsFoc8XGf3ShW0DyDnV1L2gVy6mwGmUavB2JHnGxDQ==";
	//Blob containers are configured as original-<client_id>
	String AZURE_INPUT_BLOB_CONTAINER_PREFIX = "original-";
	String AZURE_INPUT_BLOB_MEDIA_PATH_PREFIX = "media";
	
	/* Error defs */
	String NO_MEDIA_ID_IN_INPUT = "NO_MEDIA_ID_IN_INPUT";
	String INVALID_INPUT_MESSAGE = "INVALID_INPUT_MESSAGE";
	String BITCODIN_REQUEST_FAILED = "BITCODIN_REQUEST_FAILED";
	String BITCODIN_RESPONSE_INVALID = "BITCODIN_RESPONSE_INVALID";
	String BITCODIN_JOB_FAILED = "BITCODIN_JOB_FAILED";
	
	/* Status Defs */
	String STATUS_NEW = "NEW";
	String STATUS_RECEIVED = "RECEIVED";
	String STATUS_JOB_SUBMITTED = "SUBMITTED";
	String STATUS_JOB_SUCCESSFUL = "SUCCESSFUL";
	String STATUS_JOB_FAILED = "ERROR";

}
