package com.movideo.nextgen.common.encoder.models;

public abstract class MediaConfig{
    private long bitRate;
    private String codec;
    public long getBitRate() {
        return bitRate;
    }
    public void setBitRate(long bitRate) {
        this.bitRate = bitRate;
    }
    public String getCodec() {
        return codec;
    }
    public void setCodec(String codec) {
        this.codec = codec;
    }
    
}