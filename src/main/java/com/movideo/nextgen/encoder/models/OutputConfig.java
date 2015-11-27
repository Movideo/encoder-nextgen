package com.movideo.nextgen.encoder.models;

/**
 * Holds parameters related to Bitcodin input (S3/Azure/etc.)
 * 
 * @author yramasundaram
 * 
 */
public class OutputConfig {

    private String type, name, accountName, accountKey, container, prefix;

    public OutputConfig(String type, String name, String accountName, String accountKey, String container,
	    String prefix) {
	this.type = type;
	this.name = name;
	this.accountName = accountName;
	this.accountKey = accountKey;
	this.container = container;
	this.prefix = prefix;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

}
