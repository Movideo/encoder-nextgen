package com.movideo.ingester;

import java.net.URI;

import com.movideo.nextgen.encoder.models.AzureBlobInfo;

public class IngesterTransferJson
{
	
	private URI uri;
	private AzureBlobInfo azureBlobInfo;
	private String blobName;
	public AzureBlobInfo getAzureBlobInfo()
	{
	return azureBlobInfo;
	}
	public void setAzureBlobInfo(AzureBlobInfo azureBlobInfo)
	{
	this.azureBlobInfo = azureBlobInfo;
	}
	public URI getUri()
	{
	return uri;
	}
	public void setUri(URI uri)
	{
	this.uri = uri;
	}
	public String getBlobName()
	{
	return blobName;
	}
	public void setBlobName(String blobName)
	{
	this.blobName = blobName;
	}
	



}
