package com.movideo.nextgen.encoder.models;

/**
 * Encoding profile info used in creating Bitcodin encoding profiles
 * @author yramasundaram
 *
 */
public class EncodingProfileInfo {
	
	private String name;
	private StreamConfig[] audioConfigs, videoConfigs;
	
	public StreamConfig[] getAudioConfigs() {
		return audioConfigs;
	}
	public void setAudioConfigs(StreamConfig[] audioConfig) {
		this.audioConfigs = audioConfig;
	}
	public StreamConfig[] getVideoConfigs() {
		return videoConfigs;
	}
	public void setVideoConfigs(StreamConfig[] videoConfig) {
		this.videoConfigs = videoConfig;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

}