package com.movideo.nextgen.common.http;

import java.io.IOException;
import java.util.Map;

import org.apache.commons.codec.Charsets;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import com.movideo.nextgen.encoder.common.EncoderException;
import com.movideo.nextgen.encoder.common.Util;

public class HttpHelper
{

	private static final Logger log = LogManager.getLogger();

	private static final HttpClient httpClient = HttpClientBuilder.create().build();

	protected HttpHelper()
	{
	}

	protected static JSONObject httpService(String url, String method, Map<String, String> headers, String payload)
			throws EncoderException
	{

		return httpService(getUriRequestFromParams(url, method, headers, payload));

	}

	protected static JSONObject httpService(HttpUriRequest uriRequest) throws EncoderException
	{
		HttpResponse httpResponse;
		try
		{
			log.info("HttpHelper : httpService() -> REQUEST IN HTTPSERVICE: " + uriRequest);
			httpResponse = httpClient.execute(uriRequest);
			int responseCode = httpResponse.getStatusLine().getStatusCode();
			HttpEntity entity = httpResponse.getEntity();
			String responseString;

			try
			{
				responseString = EntityUtils.toString(entity, "UTF-8");
				log.info("HttpHelper : httpService() -> RESPONSE IN HTTPSERVICE: " + responseString);
				if(responseCode >= Integer.parseInt(Util.getConfigProperty("error.codes.bad.request")))
				{
					throw new EncoderException(responseCode, responseString, null);
				}
				return new JSONObject(responseString);
			}
			catch(ParseException e)
			{
				throw new EncoderException(Integer.parseInt(Util.getConfigProperty("error.codes.bad.request")),
						"ParseException occured while making response string", e);
			}
			catch(IOException e)
			{
				throw new EncoderException(Integer.parseInt(Util.getConfigProperty("error.codes.internal.server.error")),
						"IOException occured while making response string", e);
			}
			catch(JSONException e)
			{
				throw new EncoderException(Integer.parseInt(Util.getConfigProperty("error.codes.bad.request")), "Error occured while making json object",
						e);
			}
		}
		catch(ClientProtocolException e)
		{
			throw new EncoderException(Integer.parseInt(Util.getConfigProperty("error.codes.bad.request")), "HttpRequest excecution failed", e);
		}
		catch(IOException e)
		{
			throw new EncoderException(Integer.parseInt(Util.getConfigProperty("error.codes.internal.server.error")), "HttpRequest excecution failed", e);
		}

	}

	protected static HttpResponse rawHttpService(HttpUriRequest uriRequest) throws EncoderException
	{
		try
		{
			return httpClient.execute(uriRequest);
		}
		catch(IOException e)
		{
			throw new EncoderException(Integer.parseInt(Util.getConfigProperty("error.codes.internal.server.error")), e.getMessage(), e);
		}
	}

	protected static HttpResponse rawHttpService(String url, String method, Map<String, String> headers, String payload)
			throws EncoderException
	{

		return rawHttpService(getUriRequestFromParams(url, method, headers, payload));

	}

	private static HttpUriRequest getUriRequestFromParams(String url, String method, Map<String, String> headers,
			String payload) throws EncoderException
	{

		HttpUriRequest uriRequest;
		if(method.equals("post"))
		{
			HttpPost post = new HttpPost(url);
			post.setEntity(new StringEntity(payload, Charsets.UTF_8));
			uriRequest = post;
		}
		else if(method.equals("delete"))
		{
			HttpDelete delete = new HttpDelete(url);
			uriRequest = delete;
		}
		else
		{
			HttpGet get = new HttpGet(url);
			uriRequest = get;
		}

		for(Map.Entry<String, String> header : headers.entrySet())
		{
			uriRequest.setHeader(header.getKey(), header.getValue());
		}

		return uriRequest;
	}

}
