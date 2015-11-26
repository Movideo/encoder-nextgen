package com.movideo.nextgen.common.exception;

@SuppressWarnings("serial")
public class MovideoException extends Exception {
    private int status;
    private String message;
    private Throwable originalException;

    public MovideoException(int status, String message, Throwable t) {
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
