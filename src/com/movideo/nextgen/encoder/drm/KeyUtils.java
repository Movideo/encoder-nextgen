package com.movideo.nextgen.encoder.drm;

import java.security.SecureRandom;
import java.util.Objects;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.xml.bind.DatatypeConverter;

import com.castlabs.drmtoday.util.UUIDHelper;

public class KeyUtils {
	private static final SecureRandom SECURE_RANDOM = new SecureRandom();
	//private static final char[] HEXCHARS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
	
	@Nonnull
	public static String byteArrayToHexString(@Nonnull byte[] b) {
		Objects.requireNonNull(b);
		return DatatypeConverter.printHexBinary(b).toLowerCase();
	}

	@Nonnull
	public static byte[] hexStringToByteArray(@Nonnull String s) {
		Objects.requireNonNull(s);
		return DatatypeConverter.parseHexBinary(s);
	}

	public static byte[] createRandomByteArray(int length) {
		byte[] ret = new byte[length];
		SECURE_RANDOM.nextBytes(ret);

		return ret;
	}

	public static byte[] createRandomKeyId() {
		return UUIDHelper.asByteArray(UUID.randomUUID());
	}

	public static byte[] createUninitializedByteArray(int length) {
		return new byte[length];
	}

	public static byte[] createSimpleCounterFilledByteArray(int length) {
		byte[] ret = new byte[length];
		byte currByte = 0;
		for (int i = 0; i < length; i++) {
			currByte = (byte) (currByte + 1);
			ret[i] = currByte;
		}
		return ret;
	}
	
}
