package com.movideo.nextgen.common.encoder.models;

import java.util.List;

public class EncodeInfo{
    
    private int encodingProfileId;
    private List<StreamInfo> streamsList;
    
    public int getEncodingProfileId() {
        return encodingProfileId;
    }
    public void setEncodingProfileId(int encodingProfileId) {
        this.encodingProfileId = encodingProfileId;
    }
    public List<StreamInfo> getStreamsList() {
        return streamsList;
    }
    public void setStreamsList(List<StreamInfo> streamsList) {
        this.streamsList = streamsList;
    }
   
}
