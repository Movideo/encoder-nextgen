package com.movideo.nextgen.encoder.bitcodin;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import com.movideo.nextgen.common.http.HttpHelper;
import com.movideo.nextgen.encoder.common.EncoderException;
import com.movideo.nextgen.encoder.common.Util;
import com.movideo.nextgen.encoder.config.AppConfig;
import com.movideo.nextgen.encoder.config.Constants;

/**
 * Helper class for HTTP calls
 * 
 * @author yramasundaram
 */
public class BitcodinHttpHelper extends HttpHelper
{
	private static final Logger log = LogManager.getLogger();

	private BitcodinHttpHelper()
	{
	}

	private static Map<String, String> getHeaders()
	{
	AppConfig appConfig = Util.readPropertyFile();
	Map<String, String> headers = new HashMap<String, String>();
	headers.put("Content-Type", appConfig.getBitCodinContentype());
	headers.put("bitcodin-api-version", appConfig.getBitCodinApiVersion());
	headers.put("bitcodin-api-key", appConfig.getBitCodinApiKey());

	return headers;
	}

	public static JSONObject makeHttpCall(String route, String payload, String method) throws BitcodinException
	{

	String endpoint = Constants.BITCODIN_API_URL_PREFIX + route;

	try
	{
		log.debug("Endpoint is: " + endpoint);
		return httpService(endpoint, method, getHeaders(), payload != null ? payload : null);
	}
	catch(EncoderException e)
	{
		// TODO Auto-generated catch block
		throw new BitcodinException(e.getStatus(), e.getMessage(), e.getOriginalException());
	}

	}
}
