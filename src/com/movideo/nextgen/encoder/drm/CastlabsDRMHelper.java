package com.movideo.nextgen.encoder.drm;

import java.nio.charset.StandardCharsets;

import javax.xml.bind.DatatypeConverter;

import com.castlabs.drmtoday.util.UUIDHelper;

public class CastlabsDRMHelper {
	
	public static String writeByteArray(BinaryFormat outputFormat, BinaryFormat defaultOutputFormat, byte[] array) {
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
	
	public static void main(String[] args) {
		KeyUtils.createRandomByteArray(16);

		System.out.println(writeByteArray(BinaryFormat.HEX, BinaryFormat.HEX,
				KeyUtils.createRandomByteArray(16)));
	}


}
