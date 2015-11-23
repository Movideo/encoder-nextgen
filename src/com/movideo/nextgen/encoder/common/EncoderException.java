package com.movideo.nextgen.encoder.common;

@SuppressWarnings("serial")
public class EncoderException extends Exception {
    private int status;
    private String message;
    private Throwable originalException;

    public EncoderException(int status, String message, Throwable t) {
	this.status = status;
	this.message = message;
	this.originalException = t;
    }

    @Override
    public String getMessage() {
	return status + ": " + message;
    }

    public Throwable getOriginalException() {
	return originalException;
    }

    public int getStatus() {
	return status;
    }

    public void setStatus(int status) {
	this.status = status;
    }

}
