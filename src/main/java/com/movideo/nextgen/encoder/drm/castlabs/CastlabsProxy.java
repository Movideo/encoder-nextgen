package com.movideo.nextgen.encoder.drm.castlabs;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.movideo.nextgen.encoder.config.Constants;
import com.movideo.nextgen.encoder.models.DRMInfo;
import com.movideo.nextgen.encoder.models.EncodingJob;

public class CastlabsProxy
{

	private static final Logger log = LogManager.getLogger();

	public static String getCasToken(String targetService) throws CastlabsException
	{

		HttpResponse response = CastlabsHttpHelper.getRawHttpResponse(Constants.CASTLABS_CAS_AUTH_URL,
				Constants.CASTLABS_AUTH_PAYLOAD, "post", getAuthHeaders());

		// Response code has to be 201
		if(response.getStatusLine().getStatusCode() != 201)
		{
			try
			{
				throw new CastlabsException(response.getStatusLine().getStatusCode(),
						"Unable to authenticate with Castlabs server. Response is: \n"
								+ EntityUtils.toString(response.getEntity(), "UTF-8"),
						null);
			}
			catch(ParseException | IOException e)
			{
				throw new CastlabsException(response.getStatusLine().getStatusCode(),
						"Unable to authenticate with Castlabs server. Corrupt response received.", null);
			}
		}

		// Get location from response headers
		String casUrl = response.getFirstHeader("location").getValue();
		response = CastlabsHttpHelper.getRawHttpResponse(casUrl, "service=" + targetService, "post", getAuthHeaders());

		// Response code has to be 200
		if(response.getStatusLine().getStatusCode() != 200)
		{
			try
			{
				throw new CastlabsException(response.getStatusLine().getStatusCode(),
						"Unable to get Castlabs CAS token for service. Response is: \n"
								+ EntityUtils.toString(response.getEntity(), "UTF-8"),
						null);
			}
			catch(ParseException | IOException e)
			{
				throw new CastlabsException(response.getStatusLine().getStatusCode(),
						"Unable to get Castlabs CAS token for service. Corrupt response received.", null);
			}
		}

		try
		{
			String casToken = EntityUtils.toString(response.getEntity(), "UTF-8");
			log.debug("CastlabsProxy : getCasToken() -> CAS Token: " + casToken);
			return casToken;
		}
		catch(ParseException | IOException e)
		{
			throw new CastlabsException(response.getStatusLine().getStatusCode(),
					"Unable to get Castlabs CAS token for service. Corrupt response received.", null);
		}

	}

	public static Map<String, DRMInfo> ingestKeys(EncodingJob job) throws CastlabsException
	{

		DRMInfo drmInfo;
		JSONObject payload;

		JSONArray assets;
		JSONObject asset;
		JSONArray ingestKeys;
		JSONObject ingestKeysObject;

		String[] manifestTypes = job.getManifestTypes();
		Map<String, DRMInfo> drmInfoMap = new HashMap<>();

		log.debug("Encoding job is: " + job.toString());

		try
		{

			for(String manifestType : manifestTypes)
			{
				Map<String, String> keysMap = DRMHelper.generateKeyKidPair();
				drmInfo = new DRMInfo();
				payload = new JSONObject();

				assets = new JSONArray();
				asset = new JSONObject();
				ingestKeys = new JSONArray();
				ingestKeysObject = new JSONObject();

				log.debug("Current manifest type: " + manifestType);
				if(manifestType.equalsIgnoreCase(Constants.MPEG_DASH_MANIFEST_TYPE))
				{
					if(job.isReprocess())
					{
						//TODO: Key Deletion logic
						String url = Constants.CASTLABS_CENC_KEY_DELETE_URL;

						url = url.replace("[assetId]", job.getProductId()).replace("[variantId]", job.getVariant());
						url = url + "?ticket="
								+ getCasToken(url);
						log.debug("URL for deleting previously ingested CENC Key: " + url);
						CastlabsHttpHelper.makeHttpCall(url, null, "delete",
								getFrontEndHeaders());
						// Not checking specific errors here to allow just-in-case re-process requests

					}
					ingestKeysObject.put("keyId", keysMap.get("kidBase64"));
					asset.put("type", "CENC");
					drmInfo.setLicenseUrl(Constants.CENC_LA_URL);
				}
				else
				{
					ingestKeysObject.put("iv", keysMap.get("kidBase64"));
					asset.put("type", "FAIRPLAY");
					// TODO: Understand how to set license URL for AES/FPS				
				}

				drmInfo.setKeys(new String[] { keysMap.get("kidHex"), keysMap.get("keyHex") });
				for(String key : drmInfo.getKeys())
				{
					log.debug(key);
				}

				ingestKeysObject.put("key", keysMap.get("keyBase64"));
				ingestKeysObject.put("streamType", Constants.CASTLABS_DEFAULT_STREAM_TYPE);
				ingestKeysObject.put("algorithm", Constants.CASTLABS_DEFAULT_ALGORITHM);

				ingestKeys.put(ingestKeysObject);

				log.debug("Asset ID: " + job.getProductId());
				log.debug("Variant " + job.getVariant());
				asset.put("assetId", job.getProductId());
				asset.put("variantId", job.getVariant());
				asset.put("ingestKeys", ingestKeys);

				assets.put(asset);
				payload.put("assets", assets);

				log.debug("PAYLOAD: \n" + payload);

				String url = Constants.CASTLABS_KEY_INGEST_URL + "?ticket="
						+ getCasToken(Constants.CASTLABS_KEY_INGEST_URL);

				JSONObject responseJson = CastlabsHttpHelper.makeHttpCall(url, payload.toString(), "post",
						getFrontEndHeaders());

				if(manifestType.equalsIgnoreCase(Constants.MPEG_DASH_MANIFEST_TYPE))
				{

					JSONArray keys = responseJson.getJSONArray("assets").getJSONObject(0).getJSONArray("keys");
					if(keys.getJSONObject(0).has("errors"))
					{
						JSONArray errors = keys.getJSONObject(0).getJSONArray("errors");
						StringBuffer errorBuffer = new StringBuffer();
						int numErrors = errors.length();
						for(int counter = 0; counter < numErrors; counter++)
						{
							errorBuffer.append(errors.getString(counter));
							if(counter != numErrors - 1)
							{
								errorBuffer.append(", ");
							}
						}
						throw new CastlabsException(Constants.STATUS_CODE_BAD_REQUEST, errorBuffer.toString(), null);

					}
					// TODO: Is there a better way to parse this response?
					JSONObject systemId = keys
							.getJSONObject(0).getJSONObject("cencResponse").getJSONObject("systemId");

					@SuppressWarnings("unchecked")
					Iterator<String> iterator = systemId.keys();

					while(iterator.hasNext())
					{
						JSONObject currentObject = systemId.getJSONObject(iterator.next());
						if(currentObject.getString("name").equals("Widevine"))
						{
							drmInfo.setPssh(currentObject.getString("psshBoxContent"));
							drmInfoMap.put(manifestType, drmInfo);
							break;
						}
						if(drmInfo.getPssh() == null)
						{
							// Type is CENC, but no PSSH found in response
							throw new CastlabsException(Constants.STATUS_CODE_SERVER_ERROR, "Unable to find PSSH info in response", null);

						}
					}

				}
				else
				{
					drmInfoMap.put(manifestType, drmInfo);
				}

			}

			return drmInfoMap;

		}
		catch(JSONException e)
		{
			throw new CastlabsException(Constants.STATUS_CODE_SERVER_ERROR, "Invalid Castlabs request/response", e);
		}

	}

	private static Map<String, String> getAuthHeaders()
	{
		Map<String, String> headers = new HashMap<String, String>();
		headers.put("Accept", "*/*");
		headers.put("Content-Type", "application/x-www-form-urlencoded");
		return headers;
	}

	private static Map<String, String> getFrontEndHeaders()
	{
		Map<String, String> headers = new HashMap<String, String>();
		headers.put("Accept", "application/json");
		headers.put("Content-Type", "application/json");
		headers.put("Host", Constants.CASTLABS_KEY_INGEST_HOST_HEADER);
		return headers;

	}

	//	public static void main(String[] args) throws CastlabsException
	//	{
	//		Map<String, String> keyMap = new HashMap<>();
	//		keyMap.put("keyHex", "f497a34c2bf83a2074d2571f68b15bff");
	//		keyMap.put("kidHex", "8d6b13727a19df7af6901606426f96fc");
	//		keyMap.put("keyBase64", "9JejTCv4OiB00lcfaLFb/w==");
	//		keyMap.put("kidBase64", "jWsTcnoZ33r2kBYGQm+W/A==");
	//
	//		EncodingJob job = new EncodingJob();
	//		job.setClientId(524);
	//		job.setManifestTypes(new String[] { "mpd", "m3u8" });
	//		job.setProductId("12345");
	//		job.setProtectionRequired(true);
	//		job.setVariant("HD");
	//
	//		ingestKeys(keyMap, job);
	//	}

}
