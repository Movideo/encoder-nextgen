package com.movideo.nextgen.encoder.bitcodin;

import com.movideo.nextgen.encoder.common.EncoderException;

/**
 * Custom exception class for Bitcodin processing errors
 * 
 * @author yramasundaram
 *
 */
@SuppressWarnings("serial")
public class BitcodinException extends EncoderException {

    public BitcodinException(int status, String message, Throwable t) {
	super(status, message, t);
    }
}
