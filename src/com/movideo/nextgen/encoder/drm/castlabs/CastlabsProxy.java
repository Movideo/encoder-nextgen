package com.movideo.nextgen.encoder.drm.castlabs;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.movideo.nextgen.encoder.concurrency.ThreadPoolManager;
import com.movideo.nextgen.encoder.config.Constants;
import com.movideo.nextgen.encoder.models.DRMInfo;
import com.movideo.nextgen.encoder.models.EncodingJob;

public class CastlabsProxy {
	
	private static final Logger log = Logger.getLogger(ThreadPoolManager.class);

	public static String getCasToken(String targetService) throws CastlabsException {

		HttpResponse response = CastlabsHttpHelper.getRawHttpResponse(Constants.CASTLABS_CAS_AUTH_URL,
				Constants.CASTLABS_AUTH_PAYLOAD, "post", getAuthHeaders());

		// Response code has to be 201
		if (response.getStatusLine().getStatusCode() != 201) {
			try {
				throw new CastlabsException(response.getStatusLine().getStatusCode(),
						"Unable to authenticate with Castlabs server. Response is: \n"
								+ EntityUtils.toString(response.getEntity(), "UTF-8"),
						null);
			} catch (ParseException | IOException e) {
				throw new CastlabsException(response.getStatusLine().getStatusCode(),
						"Unable to authenticate with Castlabs server. Corrupt response received.", null);
			}
		}

		// Get location from response headers
		String casUrl = response.getFirstHeader("location").getValue();
		response = CastlabsHttpHelper.getRawHttpResponse(casUrl, "service=" + targetService, "post", getAuthHeaders());

		// Response code has to be 200
		if (response.getStatusLine().getStatusCode() != 200) {
			try {
				throw new CastlabsException(response.getStatusLine().getStatusCode(),
						"Unable to get Castlabs CAS token for service. Response is: \n"
								+ EntityUtils.toString(response.getEntity(), "UTF-8"),
						null);
			} catch (ParseException | IOException e) {
				throw new CastlabsException(response.getStatusLine().getStatusCode(),
						"Unable to get Castlabs CAS token for service. Corrupt response received.", null);
			}
		}

		try {
			String casToken = EntityUtils.toString(response.getEntity(), "UTF-8");
			log.debug("CAS Token: " + casToken);
			return casToken;
		} catch (ParseException | IOException e) {
			throw new CastlabsException(response.getStatusLine().getStatusCode(),
					"Unable to get Castlabs CAS token for service. Corrupt response received.", null);
		}

	}

	public static DRMInfo ingestKeys(String[] keys, EncodingJob job) throws CastlabsException {
		/*
		 * { "assets": [ { "type": "CENC", "assetId": "asset12345", "variantId"
		 * : "variant12345", "ingestKeys": [ { "keyId":
		 * "h3r2uBLxCHZo8qwd1Slmrg==", "streamType": "VIDEO_AUDIO", "algorithm":
		 * "AES", "key": "YgGUf51q9Vfn2heGaTS9nw==" } ] } ] }
		 */
		DRMInfo drmInfo = new DRMInfo();
		JSONObject payload = new JSONObject();

		JSONArray assets = new JSONArray();
		JSONObject asset = new JSONObject();
		JSONArray ingestKeys = new JSONArray();
		JSONObject ingestKeysObject = new JSONObject();

		try {

			if (job.getDrmType().equals(Constants.CENC_ENCRYPTION_TYPE)) {
				ingestKeysObject.put("keyId", keys[0]);
				asset.put("type", "CENC");
				drmInfo.setLicenseUrl(Constants.CENC_LA_URL);
			} else {
				ingestKeysObject.put("iv", keys[0]);
				asset.put("type", "FAIRPLAY");
				//TODO: Understand how to set license URL for AES/FPS
			}
			ingestKeysObject.put("key", keys[1]);
			ingestKeysObject.put("streamType", "VIDEO_AUDIO");
			ingestKeysObject.put("algorithm", "AES");

			ingestKeys.put(ingestKeysObject);

			log.debug("Asset ID: " + job.getProductId());
			log.debug("Variant " + job.getVariant());
			asset.put("assetId", job.getProductId());
			asset.put("variant", job.getVariant());
			asset.put("ingestKeys", ingestKeys);
			
			assets.put(asset);
			payload.put("assets", assets);

			log.debug("PAYLOAD: \n" + payload);
			
			String url = Constants.CASTLABS_KEY_INGEST_URL + "?ticket=" + getCasToken(Constants.CASTLABS_KEY_INGEST_URL);

			JSONObject responseJson = CastlabsHttpHelper.makeHttpCall(url,
					payload.toString(), "post", getFrontEndHeaders());
			
			//CENC Jobs require a PSSH box response for further processing
			if(job.getDrmType().equals(Constants.CENC_ENCRYPTION_TYPE)){
				
				// TODO: Is there a better way to parse this response?
				JSONObject systemId = responseJson.getJSONArray("assets").getJSONObject(0).getJSONArray("keys")
						.getJSONObject(0).getJSONObject("cencResponse").getJSONObject("systemId");
				
				@SuppressWarnings("unchecked")
				Iterator<String> iterator = systemId.keys();
				
				while(iterator.hasNext()){
					JSONObject currentObject = systemId.getJSONObject(iterator.next());
					if(currentObject.getString("name").equals("Widevine")){
						drmInfo.setPssh(currentObject.getString("psshBoxContent"));
						return drmInfo;
					}
				}
				
			}
			// Type is AES. We don't need any specific response for AES requests
			else{
				return drmInfo;
			}

		} catch (JSONException e) {
			throw new CastlabsException(500, "Invalid Castlabs request/response", e);
		}
			
		//Type is CENC, but no PSSH found in response
		throw new CastlabsException(500, "Unable to find PSSH info in response", null);

	}

	private static Map<String, String> getAuthHeaders() {
		Map<String, String> headers = new HashMap<String, String>();
		headers.put("Accept", "*/*");
		headers.put("Content-Type", "application/x-www-form-urlencoded");
		return headers;
	}

	private static Map<String, String> getFrontEndHeaders() {
		Map<String, String> headers = new HashMap<String, String>();
		headers.put("Accept", "application/json");
		headers.put("Content-Type", "application/json");
		headers.put("Host", Constants.CASTLABS_KEY_INGEST_HOST_HEADER);
		return headers;

	}
	
//	public static void main(String[] args) throws CastlabsException {
//		//String[] keys = DRMHelper.generateKeyKidPair();
//		EncodingJob job = new EncodingJob();
//		job.setProductId("Abracadabra12345");
//		job.setDrmType(Constants.CENC_ENCRYPTION_TYPE);
//		job.setVariant("HD");
//		//System.out.println(ingestKeys(keys, job));
//	}

}
