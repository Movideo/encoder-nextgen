package com.movideo.nextgen.encoder.drm.castlabs;

import com.movideo.nextgen.encoder.common.EncoderException;

@SuppressWarnings("serial")
public class CastlabsException extends EncoderException {

    public CastlabsException(int status, String message, Throwable t) {
	super(status, message, t);
    }

}
