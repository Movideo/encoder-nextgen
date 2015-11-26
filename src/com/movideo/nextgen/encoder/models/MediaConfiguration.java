package com.movideo.nextgen.encoder.models;

public class MediaConfiguration {
    private int streamId, duration, rate, bitrate, width, height, sampleAspectRatioNum, sampleAspectRationDen, displayAspectRatioNum, displayAspectRatioDen, sampleFormat;
    private String codec, type, pixelFormat, channelFormat;
    private boolean closedCaptions;
    public int getStreamId() {
        return streamId;
    }
    public void setStreamId(int streamId) {
        this.streamId = streamId;
    }
    public int getDuration() {
        return duration;
    }
    public void setDuration(int duration) {
        this.duration = duration;
    }
    public int getRate() {
        return rate;
    }
    public void setRate(int rate) {
        this.rate = rate;
    }
    public int getBitrate() {
        return bitrate;
    }
    public void setBitrate(int bitrate) {
        this.bitrate = bitrate;
    }
    public int getWidth() {
        return width;
    }
    public void setWidth(int width) {
        this.width = width;
    }
    public int getHeight() {
        return height;
    }
    public void setHeight(int height) {
        this.height = height;
    }
    public int getSampleAspectRatioNum() {
        return sampleAspectRatioNum;
    }
    public void setSampleAspectRatioNum(int sampleAspectRatioNum) {
        this.sampleAspectRatioNum = sampleAspectRatioNum;
    }
    public int getSampleAspectRationDen() {
        return sampleAspectRationDen;
    }
    public void setSampleAspectRationDen(int sampleAspectRationDen) {
        this.sampleAspectRationDen = sampleAspectRationDen;
    }
    public int getDisplayAspectRatioNum() {
        return displayAspectRatioNum;
    }
    public void setDisplayAspectRatioNum(int displayAspectRatioNum) {
        this.displayAspectRatioNum = displayAspectRatioNum;
    }
    public int getDisplayAspectRatioDen() {
        return displayAspectRatioDen;
    }
    public void setDisplayAspectRatioDen(int displayAspectRatioDen) {
        this.displayAspectRatioDen = displayAspectRatioDen;
    }
    public int getSampleFormat() {
        return sampleFormat;
    }
    public void setSampleFormat(int sampleFormat) {
        this.sampleFormat = sampleFormat;
    }
    public String getCodec() {
        return codec;
    }
    public void setCodec(String codec) {
        this.codec = codec;
    }
    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }
    public String getPixelFormat() {
        return pixelFormat;
    }
    public void setPixelFormat(String pixelFormat) {
        this.pixelFormat = pixelFormat;
    }
    public String getChannelFormat() {
        return channelFormat;
    }
    public void setChannelFormat(String channelFormat) {
        this.channelFormat = channelFormat;
    }
    public boolean isClosedCaptions() {
        return closedCaptions;
    }
    public void setClosedCaptions(boolean closedCaptions) {
        this.closedCaptions = closedCaptions;
    }
    
    

}
