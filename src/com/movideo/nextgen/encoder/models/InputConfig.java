package com.movideo.nextgen.encoder.models;

/**
 * Holds parameters related to Bitcodin input (S3/Azure/etc.)
 * @author yramasundaram
 *
 */
public class InputConfig {
	
	private String type, accountName, accountKey, container;
	
	public InputConfig (String type, String accountName, String accountKey, String container){
		this.type = type;
		this.accountName = accountName;
		this.accountKey = accountKey;
		this.container = container;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getAccountName() {
		return accountName;
	}

	public void setAccountName(String accountName) {
		this.accountName = accountName;
	}

	public String getAccountKey() {
		return accountKey;
	}

	public void setAccountKey(String accountKey) {
		this.accountKey = accountKey;
	}

	public String getContainer() {
		return container;
	}

	public void setContainer(String container) {
		this.container = container;
	}
	
	
}
