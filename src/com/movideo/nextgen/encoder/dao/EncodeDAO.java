package com.movideo.nextgen.encoder.dao;

import java.net.MalformedURLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ektorp.CouchDbConnector;
import org.ektorp.CouchDbInstance;
import org.ektorp.http.HttpClient;
import org.ektorp.http.StdHttpClient;
import org.ektorp.impl.StdCouchDbConnector;
import org.ektorp.impl.StdCouchDbInstance;

public class EncodeDAO {
    
    private static final Logger log = LogManager.getLogger();
    
    public void storeEncodeSummary(String encodeSummary){
	HttpClient httpClient;
	try {
	    httpClient = new StdHttpClient.Builder()
	            .url("http://localhost:5984")
	            .build();
	} catch (MalformedURLException e) {
	    log.fatal("Unable to connect to couchdb", e);
	    return;
	}
	
	CouchDbInstance dbInstance = new StdCouchDbInstance(httpClient);
	//TODO: DB Name?
	CouchDbConnector db = new StdCouchDbConnector("encoding", dbInstance);

	db.createDatabaseIfNotExists();
	
	db.create(encodeSummary);

    }
    
    public static void main(String[] args) {
	EncodeDAO dao = new EncodeDAO();
	String encodeSummary = "{\"manifests\":{\"mpdUrl\":\"https://eu-storage-bitcodin.storage.googleapis.com/bitStorage/2232_5dbb991ee5d11f1a3f8dd3e9898c8f46/38701_a050aadcbedfb232fb41d1a05a6d2de4/38701.mpd?cb=1448490722\",\"m3u8Url\":\"https://eu-storage-bitcodin.storage.googleapis.com/bitStorage/2232_5dbb991ee5d11f1a3f8dd3e9898c8f46/38701_a050aadcbedfb232fb41d1a05a6d2de4/38701.m3u8?cb=1448490722\"},\"product_id\":\"1234567890\",\"media_id\":848044,\"object\":\"encoding\",\"mediaConfigurations\":[{\"displayAspectRatioNum\":16,\"width\":640,\"codec\":\"h264\",\"type\":\"video\",\"pixelFormat\":\"yuv420p\",\"streamId\":0,\"duration\":0,\"rate\":23.962059620596,\"height\":360,\"sampleAspectRatioNum\":1,\"displayAspectRatioDen\":9,\"bitrate\":0,\"sampleAspectRatioDen\":1,\"closedCaptions\":false},{\"duration\":0,\"streamId\":1,\"rate\":22050,\"bitrate\":0,\"codec\":\"aac\",\"channelFormat\":\"stereo\",\"type\":\"audio\",\"sampleFormat\":2}],\"variant\":\"HD\"}";
	dao.storeEncodeSummary(encodeSummary);
    }

}
