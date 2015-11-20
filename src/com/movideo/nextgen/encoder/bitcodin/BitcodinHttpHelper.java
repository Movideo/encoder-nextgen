package com.movideo.nextgen.encoder.bitcodin;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import com.movideo.nextgen.encoder.config.Constants;

/**
 * Helper class for HTTP calls
 * @author yramasundaram
 *
 */
public class BitcodinHttpHelper {
	private static HttpClient httpClient;

	private BitcodinHttpHelper() {
	}

	private static HttpClient getHttpClient() {

		if (httpClient == null) {
			httpClient = HttpClientBuilder.create().build();
		}
		return httpClient;
	}

	public static JSONObject httpGet(String route) throws BitcodinException {

		String endpoint = Constants.BITCODIN_API_URL_PREFIX + route;

		httpClient = getHttpClient();
		HttpGet httpGet = new HttpGet(endpoint);
		httpGet.setHeader("Content-Type", "application/json");
		httpGet.setHeader("bitcodin-api-version", "v1");
		httpGet.setHeader("bitcodin-api-key", Constants.BITCODIN_API_KEY);

		return httpService(httpGet);

	}

	// Could be optimized further - possibly combine get and post into the service method using super classes?
	public static JSONObject httpPost(String route, String payload) throws BitcodinException {

		String endpoint = Constants.BITCODIN_API_URL_PREFIX + route;

		httpClient = getHttpClient();

		HttpPost httpPost = new HttpPost(endpoint);
		httpPost.setHeader("Content-Type", "application/json");
		httpPost.setHeader("bitcodin-api-version", "v1");
		httpPost.setHeader("bitcodin-api-key", Constants.BITCODIN_API_KEY);

		try {
			httpPost.setEntity(new StringEntity(payload));
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}

		return httpService(httpPost);

	}
	
	private static JSONObject httpService(HttpUriRequest uriRequest) throws BitcodinException {
		HttpResponse httpResponse;
		try {

			httpResponse = httpClient.execute(uriRequest);
			int responseCode = httpResponse.getStatusLine().getStatusCode();
			HttpEntity entity = httpResponse.getEntity();
			String responseString;

			try {
				responseString = EntityUtils.toString(entity, "UTF-8");
				System.out.println("RESPONSE IN HTTPSERVICE: " + responseString);
				if(responseCode >= 400){
					throw new BitcodinException(responseCode, responseString, null);
				}
				return new JSONObject(responseString);
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (ClientProtocolException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		return null;
	}
	

}
