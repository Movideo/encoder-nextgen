package com.movideo.nextgen.encoder.bitcodin;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import com.movideo.nextgen.encoder.common.EncoderException;
import com.movideo.nextgen.encoder.common.HttpHelper;
import com.movideo.nextgen.encoder.config.Constants;

/**
 * Helper class for HTTP calls
 * @author yramasundaram
 *
 */
public class BitcodinHttpHelper extends HttpHelper{

	private BitcodinHttpHelper() {
	}
	
	private static Map<String, String> getHeaders(){
		Map<String, String> headers = new HashMap<String, String>();
		headers.put("Content-Type", "application/json");
		headers.put("bitcodin-api-version", "application/json");
		headers.put("Content-Type", "application/json");
		
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
