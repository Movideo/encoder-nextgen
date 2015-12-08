package com.movideo.nextgen.encoder.models;

import java.util.List;

public class AzureBlobInfo
{
	private String accountName, accountKey, container;
	List<String> blobReferences;

	public String getAccountName()
	{
		return accountName;
	}

	public void setAccountName(String accountName)
	{
		this.accountName = accountName;
	}

	public String getAccountKey()
	{
		return accountKey;
	}

	public void setAccountKey(String accountKey)
	{
		this.accountKey = accountKey;
	}

	public List<String> getBlobReferences()
	{
		return blobReferences;
	}

	public void setBlobReferences(List<String> blobReferences)
	{
		this.blobReferences = blobReferences;
	}

	public String getContainer()
	{
		return container;
	}

	public void setContainer(String container)
	{
		this.container = container;
	}

}
