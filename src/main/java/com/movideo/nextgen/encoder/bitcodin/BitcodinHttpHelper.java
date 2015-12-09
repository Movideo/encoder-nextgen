package com.movideo.nextgen.encoder.bitcodin;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import com.movideo.nextgen.common.http.HttpHelper;
import com.movideo.nextgen.encoder.common.EncoderException;
import com.movideo.nextgen.encoder.common.Util;

/**
 * Helper class for HTTP calls
 * 
 * @author yramasundaram
 */
public class BitcodinHttpHelper extends HttpHelper
{
	private static final Logger log = LogManager.getLogger();
	private static Map<String, String> headers = new HashMap<>();

	private BitcodinHttpHelper()
	{
	}

	private static Map<String, String> getHeaders()
	{
		if(headers.size() == 0)
		{
			headers = Util.getHeadersMap(Util.getConfigProperty("bitcodin.request.headers"));
		}

		return headers;
	}

	public static JSONObject makeHttpCall(String route, String payload, String method) throws BitcodinException
	{

		String endpoint = Util.getConfigProperty("bitcodin.api.url.prefix") + route;

		try
		{
			log.debug("Endpoint is: " + endpoint);
			return httpService(endpoint, method, getHeaders(), payload != null ? payload : null);
		}
		catch(EncoderException e)
		{
			throw new BitcodinException(e.getStatus(), e.getMessage(), e.getOriginalException());
		}

	}

	//	public static void main(String[] args) throws EncoderException
	//	{
	//		String payload = "{\"accountName\":\"movideoqaoriginal1\",\"accountKey\":\"WC04CZf/RGvozV3852blzAVU10Zvngz4t4ftYGwobh0wlsFoc8XGf3ShW0DyDnV1L2gVy6mwGmUavB2JHnGxDQ==\",\"container\":\"original-524\",\"type\":\"abs\",\"url\":\"http://movideoqaoriginal1.blob.core.windows.net/original-524/media/848095/movie.mp4\"}";
	//		log.debug(httpService("https://portal.bitcodin.com/api/input/create", "post", getHeaders(), payload != null ? payload : null));
	//	}
}
