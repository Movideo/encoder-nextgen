package com.movideo.nextgen.encoder.bitcodin;

/**
 * Custom exception class for Bitcodin processing errors
 * @author yramasundaram
 *
 */
@SuppressWarnings("serial")
public class BitcodinException extends Exception {
	private int status;
	private String message;
	private Throwable originalException;
	
	public BitcodinException(int status, String message, Throwable t){
		this.status = status;
		this.message = message;
		this.originalException = t;
	}
	
	@Override
	public String getMessage(){
		return status + ": " + message;
	}
	
	public Throwable getOriginalException(){
		return originalException;
	}
}
