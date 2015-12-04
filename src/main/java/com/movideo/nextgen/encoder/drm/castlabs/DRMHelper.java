package com.movideo.nextgen.encoder.drm.castlabs;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.DatatypeConverter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import com.amazonaws.util.json.JSONException;
import com.castlabs.drmtoday.util.UUIDHelper;
import com.movideo.nextgen.encoder.models.DRMInfo;
import com.movideo.nextgen.encoder.models.EncodingJob;

public class DRMHelper
{

	private static final Logger log = LogManager.getLogger();

	private static String generateKey(BinaryFormat outputFormat, BinaryFormat defaultOutputFormat, byte[] array)
	{
		if(array == null)
		{
			return null;
		}

		if(outputFormat == null)
		{
			outputFormat = defaultOutputFormat;
		}

		switch(outputFormat)
		{

			case HEX:
				return KeyUtils.byteArrayToHexString(array);

			case UUID:
				if(array.length != 16)
				{
					return "Could not display as UUID because length != 16 bytes: " + KeyUtils.byteArrayToHexString(array);
				}
				return UUIDHelper.bytesToUuid(array).toString();

			case B64:
				return DatatypeConverter.printBase64Binary(array);

			case B64URL:
				return DatatypeConverter.printBase64Binary(array);

			case UTF8:
				return new String(array, StandardCharsets.UTF_8);

		}

		throw new IllegalArgumentException("Output format " + outputFormat + " is not supported");
	}

	public static Map<String, String> generateKeyKidPair()
	{
		Map<String, String> keyMap = new HashMap<>();

		byte[] keyArray = KeyUtils.createRandomByteArray(16);
		String keyHex = generateKey(BinaryFormat.HEX, BinaryFormat.HEX, keyArray);
		String keyBase64 = generateKey(BinaryFormat.B64, BinaryFormat.B64, keyArray);
		keyArray = KeyUtils.createRandomByteArray(16);
		String kidHex = generateKey(BinaryFormat.HEX, BinaryFormat.HEX, keyArray);
		String kidBase64 = generateKey(BinaryFormat.B64, BinaryFormat.B64, keyArray);

		keyMap.put("keyHex", keyHex);
		keyMap.put("kidHex", kidHex);
		keyMap.put("keyBase64", keyBase64);
		keyMap.put("kidBase64", kidBase64);

		return keyMap;
	}

	public static Map<String, DRMInfo> getDRMInfo(EncodingJob job) throws CastlabsException
	{
		Map<String, DRMInfo> drmInfoMap = CastlabsProxy.ingestKeys(job);
		//		drmInfo.setKeys(new String[] { encryptionKeys.get("kidHex"), encryptionKeys.get("keyHex") });
		return drmInfoMap;
	}

	public static JSONObject ingestKeys(EncodingJob job) throws JSONException
	{
		return null;
	}

	public static void main(String[] args)
	{
		Map<String, String> keys = generateKeyKidPair();
		log.debug("KeyId Hex: " + keys.get("keyHex") + ", Kid Hex: " + keys.get("kidHex") + ", KeyId Base64: " + keys.get("keyBase64") + ", Kid Base64 : " + keys.get("kidBase64"));
	}

}
