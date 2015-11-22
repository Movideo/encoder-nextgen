package com.movideo.nextgen.encoder.models;

	
/**
 * Holds audio and video config information required to create Bitcodin encoding profiles
 * @author yramasundaram
 *
 */
public class StreamConfig{
		
		private int defaultStreamId;
		private int bitrate;
		private String profile;
		private String preset;
		private int height;
		private int width;
		
		public int getDefaultStreamId() {
			return defaultStreamId;
		}
		public void setDefaultStreamId(int defaultStreamId) {
			this.defaultStreamId = defaultStreamId;
		}
		public int getBitrate() {
			return bitrate;
		}
		public void setBitrate(int bitrate) {
			this.bitrate = bitrate;
		}
		public String getProfile() {
			return profile;
		}
		public void setProfile(String profile) {
			this.profile = profile;
		}
		public String getPreset() {
			return preset;
		}
		public void setPreset(String preset) {
			this.preset = preset;
		}
		public int getHeight() {
			return height;
		}
		public void setHeight(int height) {
			this.height = height;
		}
		public int getWidth() {
			return width;
		}
		public void setWidth(int width) {
			this.width = width;
		}

}
