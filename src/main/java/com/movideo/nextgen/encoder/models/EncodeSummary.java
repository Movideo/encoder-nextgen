package com.movideo.nextgen.encoder.models;

/*
 * { "_id": "5caa400c82e6748e95b3ed1bf8002a8e", "_rev": "3-db8c46ab781c7a3b0afd57d8625f6592", "object": "encoding", "product_id": "93023", "media_id":
 * 12345, "variant": "HD", "manifests": [ { "type": "hls", "url": "http://movideo.com/encoded-524/media/848044/IOS_IPAD/stream_ipad.m3u8" }, { "type":
 * "mpd", "url": "http://movideo.com/encoded-524/media/848044/IOS_IPAD/stream_ipad.mpd" } ], "mediaConfigurations": [ { "streamId": 0, "duration": 0,
 * "rate": 29.97002997003, "codec": "h264", "type": "video", "bitrate": 0, "width": 512, "height": 288, "pixelFormat": "yuv420p",
 * "sampleAspectRatioNum": 1, "sampleAspectRatioDen": 1, "displayAspectRatioNum": 16, "displayAspectRatioDen": 9, "closedCaptions": false }, {
 * "streamId": 1, "duration": 0, "rate": 44100, "codec": "aac", "type": "audio", "bitrate": 0, "sampleFormat": 2, "channelFormat": "stereo" } ] }
 */

import org.ektorp.support.CouchDbDocument;

public class EncodeSummary extends CouchDbDocument
{
	private Manifest[] manifests;

	private String product_id;

	private String media_id;

	private String object;

	private MediaConfiguration[] mediaConfigurations;

	private String variant;

	public Manifest[] getManifests()
	{
	return manifests;
	}

	public void setManifests(Manifest[] manifests)
	{
	this.manifests = manifests;
	}

	public String getProduct_id()
	{
	return product_id;
	}

	public void setProduct_id(String product_id)
	{
	this.product_id = product_id;
	}

	public String getMedia_id()
	{
	return media_id;
	}

	public void setMedia_id(String media_id)
	{
	this.media_id = media_id;
	}

	public String getObject()
	{
	return object;
	}

	public void setObject(String object)
	{
	this.object = object;
	}

	public MediaConfiguration[] getMediaConfigurations()
	{
	return mediaConfigurations;
	}

	public void setMediaConfigurations(MediaConfiguration[] mediaConfigurations)
	{
	this.mediaConfigurations = mediaConfigurations;
	}

	public String getVariant()
	{
	return variant;
	}

	public void setVariant(String variant)
	{
	this.variant = variant;
	}

}
