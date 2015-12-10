package com.movideo.nextgen.encoder.dao;

import java.io.IOException;
import java.net.MalformedURLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ektorp.CouchDbConnector;
// import org.ektorp.CouchDbConnector;
// import org.ektorp.http.HttpClient;
// import org.ektorp.http.StdHttpClient;
import org.ektorp.CouchDbInstance;
import org.ektorp.http.HttpClient;
import org.ektorp.http.StdHttpClient;
import org.ektorp.impl.StdCouchDbConnector;
import org.ektorp.impl.StdCouchDbInstance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.movideo.nextgen.encoder.models.EncodeSummary;

public class EncodeDAO
{

	private static final Logger log = LogManager.getLogger();

	private CouchDbConnector db;
	private static EncodeDAO instance;
	private final String connectionString;
	private final String databaseName;

	public EncodeDAO(String connectionString, String databaseName)
	{
		this.connectionString = connectionString;
		this.databaseName = databaseName;
		//		HttpClient httpClient;
		//		try
		//		{
		//			httpClient = new StdHttpClient.Builder()
		//					.url("http://192.168.1.10:5984")
		//					.url(connectionString)
		//					.build();
		//		}
		//		catch(MalformedURLException e)
		//		{
		//			log.fatal("Unable to connect to couchdb", e);
		//			return;
		//		}
		//		CouchDbInstance dbInstance = new StdCouchDbInstance(httpClient);
		//		db = new StdCouchDbConnector(databaseName, dbInstance);
		//		db.createDatabaseIfNotExists();
	}

	public void storeEncodeSummary(EncodeSummary encodeSummary)
	{
		HttpClient httpClient;
		try
		{
			httpClient = new StdHttpClient.Builder()
					.url(connectionString).build();
		}
		catch(MalformedURLException e)
		{
			log.fatal("Unable to connect to couchdb", e);
			return;
		}

		CouchDbInstance dbInstance = new StdCouchDbInstance(httpClient);
		StdCouchDbConnector db = new StdCouchDbConnector(databaseName, dbInstance);
		db.createDatabaseIfNotExists();
		db.create(encodeSummary);
		db.ensureFullCommit();
		httpClient.shutdown();
	}

	public static void main(String[] args) throws IOException
	{
		EncodeDAO dao = new EncodeDAO("http://192.168.1.10:5984", "hub/media");
		String encodeSummaryStr = "{\n" + " \"product_id\" : \"93024\",\n" + " \"media_id\" : 12345,\n" + " \"variant\" : \"SD\",\n" + " \"manifests\": [\n" + "   {\n" + "     \"type\": \"hls\",\n" + "     \"url\": \"http://movideo.com/encoded-524/media/848044/IOS_IPAD/stream_ipad.m3u8\"\n" + "   },\n" + "   {\n" + "     \"type\": \"mpd\",\n"
				+ "     \"url\": \"http://movideo.com/encoded-524/media/848044/IOS_IPAD/stream_ipad.mpd\"\n" + "   }\n" + " ],\n" + " \"mediaConfigurations\": [\n" + "      {\n" + "        \"streamId\": 0,\n" + "        \"duration\": 0,\n" + "        \"rate\": 29.97002997003,\n" + "        \"codec\": \"h264\",\n" + "        \"type\": \"video\",\n" + "        \"bitrate\": 0,\n"
				+ "        \"width\": 512,\n" + "        \"height\": 288,\n" + "        \"pixelFormat\": \"yuv420p\",\n" + "        \"sampleAspectRatioNum\": 1,\n" + "        \"sampleAspectRatioDen\": 1,\n" + "        \"displayAspectRatioNum\": 16,\n" + "        \"displayAspectRatioDen\": 9,\n" + "        \"closedCaptions\": false\n" + "      },\n" + "      {\n" + "        \"streamId\": 1,\n"
				+ "        \"duration\": 0,\n" + "        \"rate\": 44100,\n" + "        \"codec\": \"aac\",\n" + "        \"type\": \"audio\",\n" + "        \"bitrate\": 0,\n" + "        \"sampleFormat\": 2,\n" + "        \"channelFormat\": \"stereo\"\n" + "      }\n" + "    ]\n" + "}\n";
		ObjectMapper objectMapper = new ObjectMapper();
		EncodeSummary encodeSummary = objectMapper.reader().withType(EncodeSummary.class).readValue(encodeSummaryStr);
		dao.storeEncodeSummary(encodeSummary);
	}

}
