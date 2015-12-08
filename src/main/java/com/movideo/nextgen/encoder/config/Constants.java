package com.movideo.nextgen.encoder.config;

/**
 * Contains all the constants. Right now, this class is a dump containing every configuration ever needed. Needs to be cleaned up and portion of this
 * information needs to come from dropbox and some others from the application init
 * 
 * @author yramasundaram
 */
public interface Constants
{

	/* Hardcoding */
	int clientId = 524;

	/* Redis */
	String REDIS_CONNECTION_STRING = "localhost";
	String REDIS_ENCODE_REQUEST_LIST = "ENCODE_REQUEST_LIST";
	String REDIS_ENCODE_REQUEST_WORKING_LIST = "ENCODE_REQUEST_LIST_WORKING";
	String REDIS_ENCODE_REQUEST_ERROR_LIST = "ENCODE_REQUEST_ERROR_LIST";
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
	// Blob containers are configured as original-<client_id>
	String AZURE_INPUT_BLOB_CONTAINER_PREFIX = "original-";
	String AZURE_INPUT_BLOB_MEDIA_PATH_PREFIX = "media";

	/* Bitcodin defaults used for Output creation if required */
	String OUTPUT_STORAGE_TYPE = "azure";

	/* Output BLOB details */
	String AZURE_OUTPUT_ACCOUNT_NAME = "movideoqaencoded1";
	String AZURE_OUTPUT_URL_PREFIX = "https://" + AZURE_OUTPUT_ACCOUNT_NAME + ".blob.core.windows.net/";
	String AZURE_OUPUT_ACCOUNT_KEY = "vbSDcGSy2mbW55B2xMpkJ5Ns93CxNYJUIOz0kEdtQzhzv1+Wh87o5Daf9cf9zt6v1h2nLdiR/bzQqGvEPWFAGA==";
	String AZURE_OUTPUT_BLOB_CONTAINER_PREFIX = "encoded-";
	String BITCODIN_OUTPUT_NAME_PREFIX = "Output-";
	String AZURE_OUTPUT_BLOB_MEDIA_PATH_PREFIX = "media";

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
	Integer STATUS_CODE_SERVER_ERROR = 500;
	Integer STATUS_CODE_BAD_REQUEST = 400;

	/* DRM */
	String CENC_ENCRYPTION_TYPE = "widevine_playready";
	String AES_ENCRYPTION_TYPE = "AES-128";
	String FPS_ENCRYPTION_TYPE = "SAMPLE-AES";
	String BITCODIN_CENC_METHOD = "mpeg_cenc";
	String HLS_MANIFEST_TYPE = "m3u8";
	String MPEG_DASH_MANIFEST_TYPE = "mpd";
	String CENC_LA_URL = "http://lic.drmtoday.com/license-proxy-headerauth/drmtoday/RightsManager.asmx";
	String CASTLABS_MERCHANT_USER_ID = "packaging";
	String CASTLABS_MERCHANT_PASSWORD = "msl38mdh3894ldhs";
	String CASTLABS_MERCHANT_NAME = "movideo";
	String CASTLABS_DEFAULT_STREAM_TYPE = "VIDEO_AUDIO";
	String CASTLABS_DEFAULT_ALGORITHM = "AES";
	String CASTLABS_KEY_INGEST_HOST_HEADER = "fe.staging.drmtoday.com";
	String CASTLABS_CAS_AUTH_URL = "https://auth.staging.drmtoday.com/cas/v1/tickets";
	String CASTLABS_KEY_INGEST_URL = "https://fe.staging.drmtoday.com/frontend/api/keys/v2/ingest/"
			+ CASTLABS_MERCHANT_NAME;
	String CASTLABS_CENC_KEY_DELETE_URL = "https://fe.staging.drmtoday.com/frontend/rest/keys/v1/cenc/merchant/" + CASTLABS_MERCHANT_NAME + "/key/assetId/[assetId]/streamType/" + CASTLABS_DEFAULT_STREAM_TYPE + "/variantId/[variantId]";
	String CASTLABS_AUTH_PAYLOAD = "username=" + CASTLABS_MERCHANT_NAME + "::" + CASTLABS_MERCHANT_USER_ID
			+ "&password=" + CASTLABS_MERCHANT_PASSWORD;
	String BITCODIN_CENC_DRM_CONFIG_KEY = "drmConfig";
	String BITCODIN_HLS_DRM_CONFIG_KEY = "drmConfig";
	String HLS_STREAM_TYPE = "hls";
	String MPEG_DASH_STREAM_TYPE = "mpeg-dash";

}
