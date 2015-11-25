package com.movideo.nextgen.common.encoder.models;

public abstract class MediaConfig{
    private String bitRate;
    private String codec;
    public String getBitRate() {
        return bitRate;
    }
    public void setBitRate(String bitRate) {
        this.bitRate = bitRate;
    }
    public String getCodec() {
        return codec;
    }
    public void setCodec(String codec) {
        this.codec = codec;
    }
    
}