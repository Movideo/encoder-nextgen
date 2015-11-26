package com.movideo.nextgen.common.encoder.models;

public class EncodeInfo{
    
    private int encodingProfileId;
    private StreamInfo streamsList;
    
    public int getEncodingProfileId() {
        return encodingProfileId;
    }
    public void setEncodingProfileId(int encodingProfileId) {
        this.encodingProfileId = encodingProfileId;
    }
    public StreamInfo getStreamsList() {
        return streamsList;
    }
    public void setStreamsList(StreamInfo streamsList) {
        this.streamsList = streamsList;
    }
   
}
