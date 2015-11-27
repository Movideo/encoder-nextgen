package com.movideo.nextgen.common.encoder.models;

import java.util.List;

public class StreamInfo{
    
    private List<String> manifestTypes;
    private boolean protectionRequired;
    private List<AudioConfig> audioConfig;
    private List<VideoConfig> videoConfig;
    
    public List<String> getManifestType() {
        return manifestTypes;
    }
    public void setManifestType(List<String> manifestTypes) {
        this.manifestTypes = manifestTypes;
    }
    public boolean isProtectionRequired() {
        return protectionRequired;
    }
    public void setProtectionRequired(boolean protectionRequired) {
        this.protectionRequired = protectionRequired;
    }
    public List<AudioConfig> getAudioConfig() {
        return audioConfig;
    }
    public void setAudioConfig(List<AudioConfig> audioConfig) {
        this.audioConfig = audioConfig;
    }
    public List<VideoConfig> getVideoConfig() {
        return videoConfig;
    }
    public void setVideoConfig(List<VideoConfig> videoConfig) {
        this.videoConfig = videoConfig;
    }
    
}
