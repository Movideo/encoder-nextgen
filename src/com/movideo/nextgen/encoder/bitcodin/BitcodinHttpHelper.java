package com.movideo.nextgen.encoder.bitcodin;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.json.JSONObject;

import com.movideo.nextgen.encoder.common.EncoderException;
import com.movideo.nextgen.encoder.common.HttpHelper;
import com.movideo.nextgen.encoder.config.Constants;

/**
 * Helper class for HTTP calls
 * 
 * @author yramasundaram
 *
 */
public class BitcodinHttpHelper extends HttpHelper {
    private static final Logger log = Logger.getLogger(BitcodinHttpHelper.class);

    private BitcodinHttpHelper() {
    }

    private static Map<String, String> getHeaders() {
	Map<String, String> headers = new HashMap<String, String>();
	headers.put("Content-Type", "application/json");
	headers.put("bitcodin-api-version", "application/json");
	headers.put("bitcodin-api-key", "78e7e7a41713c7c5d3b0aaa2279ec9c07a84d60e84df1bcb5a97ddd6e6ecb711");

	return headers;
    }

    public static JSONObject makeHttpCall(String route, String payload, String method) throws BitcodinException {

	String endpoint = Constants.BITCODIN_API_URL_PREFIX + route;

	try {
	    return httpService(endpoint, method, getHeaders(), payload != null ? payload : null);
	} catch (EncoderException e) {
	    // TODO Auto-generated catch block
	    throw new BitcodinException(e.getStatus(), e.getMessage(), e.getOriginalException());
	}

    }
}
