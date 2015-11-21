package com.movideo.nextgen.encoder.models;

import org.json.JSONObject;

public class DRMInfo {
	
	private String pssh;
	private String licenseUrl;
	private String[] keys;
	
	public String getPssh() {
		return pssh;
	}
	public void setPssh(String pssh) {
		this.pssh = pssh;
	}
	public String getLicenseUrl() {
		return licenseUrl;
	}
	public void setLicenseUrl(String licenseUrl) {
		this.licenseUrl = licenseUrl;
	}
	public String[] getKeys() {
		return keys;
	}
	public void setKeys(String[] keys) {
		this.keys = keys;
	}
	
	@Override
	public String toString(){
		return new JSONObject(this).toString();
	}

}
