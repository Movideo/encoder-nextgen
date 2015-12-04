package com.movideo.nextgen.common.encoder.models;

public class SubtitleInfo
{

	private String url;
	private String langLong;
	private String langShort;
	private String type = "vtt";

	public String getUrl()
	{
		return url;
	}

	public void setUrl(String url)
	{
		this.url = url;
	}

	public String getLangLong()
	{
		return langLong;
	}

	public void setLangLong(String langLong)
	{
		this.langLong = langLong;
	}

	public String getLangShort()
	{
		return langShort;
	}

	public void setLangShort(String langShort)
	{
		this.langShort = langShort;
	}

	public String getType()
	{
		return type;
	}

	public void setType(String type)
	{
		this.type = type;
	}

}
