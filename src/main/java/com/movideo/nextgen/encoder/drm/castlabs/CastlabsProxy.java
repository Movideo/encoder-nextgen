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

import com.movideo.nextgen.encoder.common.Util;
import com.movideo.nextgen.encoder.models.DRMInfo;
import com.movideo.nextgen.encoder.models.EncodingJob;

public class CastlabsProxy
{

	private static final Logger log = LogManager.getLogger();
	private static Map<String, String> authHeaders = new HashMap<>();
	private static Map<String, String> frontendHeaders = new HashMap<>();

	public static String getCasToken(String targetService) throws CastlabsException
	{

		HttpResponse response = CastlabsHttpHelper.getRawHttpResponse(Util.getConfigProperty("castlabs.cas.auth.url"),
				Util.getConfigProperty("castlabs.auth.payload"), "post", getAuthHeaders());

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

		DRMInfo cencDrmInfo = null, hlsDrmInfo = null;
		JSONObject payload;

		JSONArray assets;
		JSONObject asset;
		JSONArray ingestKeys;
		JSONObject ingestKey;

		String[] manifestTypes = job.getManifestTypes();
		Map<String, DRMInfo> drmInfoMap = new HashMap<>();
		String keyIngestType;
		Map<String, String> keysMap = DRMHelper.generateKeyKidIvSet();

		log.debug("Encoding job is: " + job.toString());

		if(manifestTypes.length > 1)
		{
			keyIngestType = "COMBINED";
		}
		else
		{
			String manifestType = manifestTypes[0];
			if(manifestType.equalsIgnoreCase(Util.getConfigProperty("stream.mpd.manifest.type")))
			{
				keyIngestType = "CENC";
			}
			else
			{
				keyIngestType = "FPS";
			}
		}

		try
		{
			if(job.isReprocess())
			{
				// This deletes all keys for this asset.
				String url = Util.getConfigProperty("castlabs.cenc.key.delete.url");

				url = url.replace("[assetId]", job.getProductId()).replace("[variantId]", job.getVariant());
				url = url + "?ticket="
						+ getCasToken(url);
				log.info("URL for deleting previously ingested CENC Key: " + url);

				// Example: {"assets":[{"assetId":"1234567890-0123456789","variantId":"HD","keys":[{"streamType":"VIDEO_AUDIO"}]}]}

				payload = new JSONObject();
				assets = new JSONArray();
				asset = new JSONObject();
				asset.put("assetId", job.getProductId());
				asset.put("variantId", job.getVariant());

				JSONArray keys = new JSONArray();
				JSONObject key = new JSONObject();
				key.put("streamType", Util.getConfigProperty("castlabs.stream.default.type"));
				keys.put(key);
				asset.put("keys", keys);

				assets.put(asset);
				payload.put("assets", assets);

				log.info("Payload to delete key call: \n" + payload);

				HttpResponse response = CastlabsHttpHelper.getRawHttpResponse(url, payload.toString(), "post",
						getFrontEndHeaders());
				log.info("Attempted to delete key for asset: " + job.getProductId() + ", variant: " + job.getVariant() + " and the response code is: " + response.getStatusLine().getStatusCode());
				// Not checking specific errors here to allow just-in-case re-process requests

			}

			/* Example payload
			 * {
				"assets": [
					{
						"type": "FAIRPLAY",
						"assetId": "1234-5678-9995",
						"variantId" : "HD",
						"ingestKeys": [
							{
								"streamType": "VIDEO_AUDIO",
								"algorithm": "AES",
								"key": "p+qMRXmM5vBxj9gcpg0pBw==",
								"iv": "4vC/SerRgWLwsM/i2KpYoA==", --> Only for FPS and Combined FPS+CENC requests
								"keyId" : "44bI7ScgtGfVZepG8YYVVg==" --> Only for CENC and Combined FPS+CENC requests
							}
						]
					}
				 ]
			   }
			 */

			payload = new JSONObject();
			assets = new JSONArray();

			log.info("Ingest Keys: Manifest types: \n");
			for(String manifestType : job.getManifestTypes())
			{
				log.info(manifestType);
			}

			asset = new JSONObject();
			asset.put("assetId", job.getProductId());
			asset.put("variantId", job.getVariant());

			asset.put("type", keyIngestType.equals("CENC") ? "CENC" : "FAIRPLAY");
			ingestKeys = new JSONArray();
			ingestKey = new JSONObject();
			ingestKey.put("streamType", Util.getConfigProperty("castlabs.stream.default.type"));
			ingestKey.put("algorithm", Util.getConfigProperty("castlabs.default.encryption.algorithm"));

			// Common for all combinations
			ingestKey.put("key", keysMap.get("keyBase64"));

			if(keyIngestType.equals("CENC") || keyIngestType.equals("COMBINED"))
			{
				ingestKey.put("keyId", keysMap.get("kidBase64"));
				cencDrmInfo = new DRMInfo();
				cencDrmInfo.setKeys(new String[] { keysMap.get("kidHex"), keysMap.get("keyHex") });
				cencDrmInfo.setLicenseUrl(Util.getConfigProperty("castlabs.cenc.laUrl"));
			}

			if(keyIngestType.equals("FAIRPLAY") || keyIngestType.equals("COMBINED"))
			{
				ingestKey.put("iv", keysMap.get("ivBase64"));
				hlsDrmInfo = new DRMInfo();
				hlsDrmInfo.setKeys(new String[] { keysMap.get("ivHex"), keysMap.get("keyHex") });
				hlsDrmInfo.setLicenseUrl(Util.getConfigProperty("castlabs.fps.licenseUrl"));
			}

			ingestKeys.put(ingestKey);
			asset.put("ingestKeys", ingestKeys);
			assets.put(asset);
			payload.put("assets", assets);

			log.info("Payload to ingestKeys request: \n" + payload);

			String url = Util.getConfigProperty("castlabs.key.ingest.url") + "?ticket="
					+ getCasToken(Util.getConfigProperty("castlabs.key.ingest.url"));

			JSONObject responseJson = CastlabsHttpHelper.makeHttpCall(url, payload.toString(), "post",
					getFrontEndHeaders());

			JSONArray keys = responseJson.getJSONArray("assets").getJSONObject(0).getJSONArray("keys");

			checkErrors(keys);

			if(keyIngestType.equals("CENC") || keyIngestType.equals("COMBINED"))
			{
				String pssh = getPsshBoxFromResponse(keys);
				log.debug("PSSH from Castlabs is: " + pssh);
				cencDrmInfo.setPssh(pssh);
				drmInfoMap.put(Util.getConfigProperty("stream.mpd.manifest.type"), cencDrmInfo);
			}
			if(keyIngestType.equals("FPS") || keyIngestType.equals("COMBINED"))
			{
				drmInfoMap.put(Util.getConfigProperty("stream.hls.manifest.type"), hlsDrmInfo);
			}

			log.info("Entries in DRMInfoMap:");
			for(Map.Entry<String, DRMInfo> entry : drmInfoMap.entrySet())
			{
				log.info(entry.getKey() + ": " + entry.getValue());
			}

			return drmInfoMap;

		}
		catch(JSONException e)
		{
			throw new CastlabsException(Integer.parseInt(Util.getConfigProperty("error.codes.bad.request")), "Invalid Castlabs request/response", e);
		}

	}

	private static Map<String, String> getAuthHeaders()
	{
		if(authHeaders.size() == 0)
		{
			authHeaders = Util.getHeadersMap(Util.getConfigProperty("castlabs.headers.auth"));
		}

		return authHeaders;
	}

	private static Map<String, String> getFrontEndHeaders()
	{
		if(frontendHeaders.size() == 0)
		{
			frontendHeaders = Util.getHeadersMap(Util.getConfigProperty("castlabs.headers.frontend"));
		}

		return frontendHeaders;

	}

	private static String getPsshBoxFromResponse(JSONArray keys) throws JSONException, CastlabsException
	{
		log.debug("Keys: " + keys);
		// TODO: Is there a better way to parse this response?
		JSONObject systemId = keys
				.getJSONObject(0).getJSONObject("cencResponse").getJSONObject("systemId");

		log.debug("systemId: " + systemId);

		@SuppressWarnings("unchecked")
		Iterator<String> iterator = systemId.keys();

		while(iterator.hasNext())
		{
			JSONObject currentObject = systemId.getJSONObject(iterator.next());
			log.debug("Current Object: " + currentObject);
			if(currentObject.getString("name").equals("Widevine"))
			{
				log.debug("Found PSSH: " + currentObject.getString("psshBoxContent"));
				return currentObject.getString("psshBoxContent");
			}
		}
		throw new CastlabsException(Integer.parseInt(Util.getConfigProperty("error.codes.bad.request")), "Unable to find PSSH info in response", null);

	}

	private static void checkErrors(JSONArray keys) throws CastlabsException, JSONException
	{
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
			throw new CastlabsException(Integer.parseInt(Util.getConfigProperty("error.codes.bad.request")), errorBuffer.toString(), null);

		}

	}

}
