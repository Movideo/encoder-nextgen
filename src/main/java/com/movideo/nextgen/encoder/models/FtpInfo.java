package com.movideo.nextgen.encoder.models;

import org.json.JSONObject;

public class FtpInfo
{
	private String username;
	private String password;
	private String host;
	private String ip;
	private String name;
	private String path;
	private int mediaId;
	private String prefix;
	private long outputId;

	public String getUsername()
	{
		return username;
	}

	public void setUsername(String username)
	{
		this.username = username;
	}

	public String getPassword()
	{
		return password;
	}

	public void setPassword(String password)
	{
		this.password = password;
	}

	public String getHost()
	{
		return host;
	}

	public void setHost(String host)
	{
		this.host = host;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getPath()
	{
		return path;
	}

	public void setPath(String path)
	{
		this.path = path;
	}

	public String getIp()
	{
		return ip;
	}

	public void setIp(String ip)
	{
		this.ip = ip;
	}

	public int getMediaId()
	{
		return mediaId;
	}

	public void setMediaId(int mediaId)
	{
		this.mediaId = mediaId;
	}

	public String getPrefix()
	{
		return prefix;
	}

	public void setPrefix(String prefix)
	{
		this.prefix = prefix;
	}

	public long getOutputId()
	{
		return outputId;
	}

	public void setOutputId(long outputId)
	{
		this.outputId = outputId;
	}

	@Override
	public String toString()
	{
		return new JSONObject(this).toString();
	}

}
