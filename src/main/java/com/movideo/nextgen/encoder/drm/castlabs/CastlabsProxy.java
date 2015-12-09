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

				if(job.isReprocess())
				{
					//TODO: Handle FPS
					String url = Util.getConfigProperty("castlabs.cenc.key.delete.url");

					url = url.replace("[assetId]", job.getProductId()).replace("[variantId]", job.getVariant());
					url = url + "?ticket="
							+ getCasToken(url);
					log.debug("URL for deleting previously ingested CENC Key: " + url);
					HttpResponse response = CastlabsHttpHelper.getRawHttpResponse(url, null, "delete",
							getFrontEndHeaders());
					log.info("Attempted to delete key for asset: " + job.getProductId() + ", variant: " + job.getVariant() + " and the response code is: " + response.getStatusLine().getStatusCode());
					// Not checking specific errors here to allow just-in-case re-process requests

				}

				if(manifestType.equalsIgnoreCase(Util.getConfigProperty("stream.mpd.manifest.type")))
				{
					ingestKeysObject.put("keyId", keysMap.get("kidBase64"));
					asset.put("type", "CENC");
					drmInfo.setLicenseUrl(Util.getConfigProperty("castlabs.cenc.laUrl"));
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
				ingestKeysObject.put("streamType", Util.getConfigProperty("castlabs.stream.default.type"));
				ingestKeysObject.put("algorithm", Util.getConfigProperty("castlabs.default.encryption.algorithm"));

				ingestKeys.put(ingestKeysObject);

				log.debug("Asset ID: " + job.getProductId());
				log.debug("Variant " + job.getVariant());
				asset.put("assetId", job.getProductId());
				asset.put("variantId", job.getVariant());
				asset.put("ingestKeys", ingestKeys);

				assets.put(asset);
				payload.put("assets", assets);

				log.debug("PAYLOAD: \n" + payload);

				String url = Util.getConfigProperty("castlabs.key.ingest.url") + "?ticket="
						+ getCasToken(Util.getConfigProperty("castlabs.key.ingest.url"));

				JSONObject responseJson = CastlabsHttpHelper.makeHttpCall(url, payload.toString(), "post",
						getFrontEndHeaders());

				JSONArray keys = responseJson.getJSONArray("assets").getJSONObject(0).getJSONArray("keys");

				checkErrors(keys);

				if(manifestType.equalsIgnoreCase(Util.getConfigProperty("stream.mpd.manifest.type")))
				{
					String pssh = getPsshBoxFromResponse(keys);
					log.debug("PSSH from Castlabs is: " + pssh);
					drmInfo.setPssh(pssh);
					drmInfoMap.put(manifestType, drmInfo);
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
