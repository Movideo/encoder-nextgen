package com.movideo.nextgen.encoder.drm.castlabs;

import java.nio.charset.StandardCharsets;

import javax.xml.bind.DatatypeConverter;

import org.json.JSONObject;

import com.amazonaws.util.json.JSONException;
import com.castlabs.drmtoday.util.UUIDHelper;
import com.movideo.nextgen.encoder.models.DRMInfo;
import com.movideo.nextgen.encoder.models.EncodingJob;

public class DRMHelper {

	private static String generateKey(BinaryFormat outputFormat, BinaryFormat defaultOutputFormat, byte[] array) {
		if (array == null) {
			return null;
		}

		if (outputFormat == null) {
			outputFormat = defaultOutputFormat;
		}

		switch (outputFormat) {

		case HEX:
			return KeyUtils.byteArrayToHexString(array);

		case UUID:
			if (array.length != 16) {
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

	private static String[] generateKeyKidPair() {
		String keyHex = generateKey(BinaryFormat.B64, BinaryFormat.B64, KeyUtils.createRandomByteArray(16));
		String kidHex = generateKey(BinaryFormat.B64, BinaryFormat.B64, KeyUtils.createRandomByteArray(16));
		return new String[] { keyHex, kidHex };
	}
	

	public static DRMInfo getDRMInfo(EncodingJob job) throws CastlabsException {
		String[] encryptionKeys = generateKeyKidPair();
		DRMInfo drmInfo = CastlabsProxy.ingestKeys(encryptionKeys, job);
		drmInfo.setKeys(encryptionKeys);
		return drmInfo;
	}

	public static JSONObject ingestKeys(EncodingJob job) throws JSONException {
		return null;
	}

}
