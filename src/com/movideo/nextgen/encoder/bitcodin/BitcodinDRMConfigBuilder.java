package com.movideo.nextgen.encoder.bitcodin;

import javax.annotation.Nonnull;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import com.movideo.nextgen.encoder.config.Constants;
import com.movideo.nextgen.encoder.drm.castlabs.CastlabsException;
import com.movideo.nextgen.encoder.drm.castlabs.DRMHelper;
import com.movideo.nextgen.encoder.models.DRMInfo;
import com.movideo.nextgen.encoder.models.EncodingJob;

public class BitcodinDRMConfigBuilder {

    private static final Logger log = Logger.getLogger(BitcodinDRMConfigBuilder.class);

    @Nonnull
    public static JSONObject getDRMConfigJSON(EncodingJob job) throws BitcodinException {

	DRMInfo drmInfo;

	try {
	    drmInfo = DRMHelper.getDRMInfo(job);
	} catch (CastlabsException e) {
	    throw new BitcodinException(e.getStatus(), e.getMessage(), e.getOriginalException());
	}

	switch (job.getDrmType()) {

	case Constants.CENC_ENCRYPTION_TYPE: {
	    try {
		return getCENCConfig(drmInfo, job);
	    } catch (JSONException e) {
		throw new BitcodinException(Constants.STATUS_CODE_SERVER_ERROR, e.getMessage(), e);
	    }
	}

	case Constants.AES_ENCRYPTION_TYPE:
	case Constants.FPS_ENCRYPTION_TYPE:
	    try {
		return getHlsConfig(drmInfo, job);
	    } catch (JSONException e) {
		throw new BitcodinException(Constants.STATUS_CODE_SERVER_ERROR, e.getMessage(), e);
	    }

	default:
	    return null;
	}

    }

    private static JSONObject getCENCConfig(DRMInfo info, EncodingJob job) throws JSONException {
	JSONObject drmConfig = new JSONObject();
	/*
	 * "system": "widevine_playready", "kid":
	 * "eb676abbcb345e96bbcf616630f1a3da", "key":
	 * "100b6c20940f779a4589152b57d2dacb" "laUrl":
	 * "http://playready.directtaps.net/pr/svc/rightsmanager.asmx?PlayRight=1&ContentKey=EAtsIJQPd5pFiRUrV9Layw==",
	 * "method": "mpeg_cenc", "pssh":
	 * "#CAESEOtnarvLNF6Wu89hZjDxo9oaDXdpZGV2aW5lX3Rlc3QiEGZrajNsamFTZGZhbGtyM2oqAkhEMgA=",
	 */
	String[] keys = info.getKeys();
	drmConfig.put("system", job.getDrmType());
	drmConfig.put("kid", keys[0]);
	drmConfig.put("key", keys[1]);
	drmConfig.put("laUrl", info.getLicenseUrl());
	drmConfig.put("method", "mpeg_cenc");
	drmConfig.put("pssh", info.getPssh());

	log.debug("BitcodinDRMConfigBuilder :getCENCConfig() ->  DRM type is CENC and drmConfig object is: \n"
		+ drmConfig.toString());

	return drmConfig;
    }

    private static JSONObject getHlsConfig(DRMInfo info, EncodingJob job) throws JSONException {

	/*
	 * "hlsEncryptionConfig": { "method": "SAMPLE-AES", "key":
	 * "cab5b529ae28d5cc5e3e7bc3fd4a544d", "iv":
	 * "08eecef4b026deec395234d94218273d", "uri":
	 * "https://your.license.server/getlicense" }
	 */
	String[] keys = info.getKeys();
	JSONObject hlsEncryptionConfig = new JSONObject();
	hlsEncryptionConfig.put("method", job.getDrmType());
	hlsEncryptionConfig.put("iv", keys[0]);
	hlsEncryptionConfig.put("key", keys[1]);
	hlsEncryptionConfig.put("uri", info.getLicenseUrl());

	log.debug("BitcodinDRMConfigBuilder :getHlsConfig() ->DRM type is " + job.getDrmType()
		+ " and drmConfig object is: \n" + hlsEncryptionConfig.toString());

	return hlsEncryptionConfig;
    }

    // public static void main(String[] args) throws BitcodinException {
    // EncodingJob job = new EncodingJob();
    // job.setProductId("Abracadabra123456");
    // job.setDrmType(Constants.CENC_ENCRYPTION_TYPE);
    // job.setVariant("HD");
    // System.out.println(getDRMConfigJSON(job));
    // }

}
