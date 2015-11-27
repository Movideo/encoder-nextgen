package com.movideo.nextgen.encoder.common;

import com.movideo.nextgen.common.exception.MovideoException;

@SuppressWarnings("serial")
public class EncoderException extends MovideoException {
    
    public EncoderException(int status, String message, Throwable t) {
	super(status, message, t);
    }

}
