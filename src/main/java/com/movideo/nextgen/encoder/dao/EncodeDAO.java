package com.movideo.nextgen.encoder.dao;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.ektorp.CouchDbConnector;
// import org.ektorp.CouchDbConnector;
// import org.ektorp.http.HttpClient;
// import org.ektorp.http.StdHttpClient;
import org.ektorp.CouchDbInstance;
import org.ektorp.ViewQuery;
import org.ektorp.http.HttpClient;
import org.ektorp.http.StdHttpClient;
import org.ektorp.impl.StdCouchDbConnector;
import org.ektorp.impl.StdCouchDbInstance;

import com.movideo.nextgen.encoder.models.EncodeSummary;

public class EncodeDAO
{

	private static final Logger log = LogManager.getLogger();

	private CouchDbConnector db;
	private static EncodeDAO instance;
	private final String connectionString;
	private final String databaseName;
	private static HttpClient httpClient;

	private synchronized HttpClient getHttpClient()
	{
		if(httpClient == null)
		{
			try
			{
				httpClient = new StdHttpClient.Builder()
						.url(connectionString).build();
			}
			catch(MalformedURLException e)
			{
				log.fatal("Unable to connect to couchdb", e);
				return null;
			}
		}
		return httpClient;
	}

	public EncodeDAO(String connectionString, String databaseName)
	{
		this.connectionString = connectionString;
		this.databaseName = databaseName;
	}

	public List<EncodeSummary> getExistingMedia(long mediaId, String variant)
	{
		String key = mediaId + "#" + variant;

		CouchDbInstance dbInstance = new StdCouchDbInstance(getHttpClient());
		StdCouchDbConnector db = new StdCouchDbConnector(databaseName, dbInstance);
		ViewQuery query = new ViewQuery()
				.designDocId("_design/media")
				.viewName("encodingsByMediaAndVariantView")
				.key(key);
		List<EncodeSummary> result = db.queryView(query, EncodeSummary.class);
		log.info(result.size());
		if(result.size() > 0)
			log.info(result.get(0));
		return result;
	}

	public void deleteExistingMedia(String documentId, String revision)
	{
		CouchDbInstance dbInstance = new StdCouchDbInstance(getHttpClient());
		StdCouchDbConnector db = new StdCouchDbConnector(databaseName, dbInstance);
		db.delete(documentId, revision);
	}

	public void storeEncodeSummary(EncodeSummary encodeSummary)
	{
		CouchDbInstance dbInstance = new StdCouchDbInstance(getHttpClient());
		StdCouchDbConnector db = new StdCouchDbConnector(databaseName, dbInstance);
		db.createDatabaseIfNotExists();
		db.create(encodeSummary);
		db.ensureFullCommit();
	}

	public static void main(String[] args) throws IOException
	{
		EncodeDAO dao = new EncodeDAO("http://localhost:5984", "media");
		dao.getExistingMedia(1403922, "HD");
		dao.getExistingMedia(848095, "HD");

		String encodeSummaryStr1 = "{\"manifests\":[{\"type\":\"m3u8\",\"url\":\"https://movideoqaencoded1.blob.core.windows.net/encoded-524/media/848095/50437_9f1a1c4fca54ea6ed068006d927bd41f/50437_subs.m3u8\"},{\"type\":\"mpd\",\"url\":\"https://movideoqaencoded1.blob.core.windows.net/encoded-524/media/848095/50437_9f1a1c4fca54ea6ed068006d927bd41f/50437_subs.mpd\"}],\"product_id\":\"999999999\",\"streamType\":\"unprotected\",\"media_id\":\"848096\",\"object\":\"encoding\",\"mediaConfigurations\":[{\"displayAspectRatioNum\":16,\"width\":1280,\"codec\":\"h264\",\"type\":\"video\",\"pixelFormat\":\"yuv420p\",\"duration\":0,\"streamId\":0,\"rate\":25,\"height\":720,\"sampleAspectRatioNum\":1,\"displayAspectRatioDen\":9,\"bitrate\":0,\"sampleAspectRatioDen\":1,\"closedCaptions\":false,\"sampleFormat\":0},{\"displayAspectRatioNum\":0,\"width\":0,\"codec\":\"aac\",\"type\":\"audio\",\"duration\":0,\"streamId\":1,\"rate\":48000,\"height\":0,\"sampleAspectRatioNum\":0,\"displayAspectRatioDen\":0,\"bitrate\":0,\"channelFormat\":\"5.1\",\"sampleAspectRatioDen\":0,\"closedCaptions\":false,\"sampleFormat\":6}],\"variant\":\"SD\"}";
		String encodeSummaryStr2 = "{\"manifests\":[{\"type\":\"m3u8\",\"url\":\"https://movideoqaencoded1.blob.core.windows.net/encoded-524/media/848095/50437_9f1a1c4fca54ea6ed068006d927bd41f/50437_subs.m3u8\"},{\"type\":\"mpd\",\"url\":\"https://movideoqaencoded1.blob.core.windows.net/encoded-524/media/848095/50437_9f1a1c4fca54ea6ed068006d927bd41f/50437_subs.mpd\"}],\"product_id\":\"999999999\",\"streamType\":\"unprotected\",\"media_id\":\"848096\",\"object\":\"encoding\",\"mediaConfigurations\":[{\"displayAspectRatioNum\":16,\"width\":1280,\"codec\":\"h264\",\"type\":\"video\",\"pixelFormat\":\"yuv420p\",\"duration\":0,\"streamId\":0,\"rate\":25,\"height\":720,\"sampleAspectRatioNum\":1,\"displayAspectRatioDen\":9,\"bitrate\":0,\"sampleAspectRatioDen\":1,\"closedCaptions\":false,\"sampleFormat\":0},{\"displayAspectRatioNum\":0,\"width\":0,\"codec\":\"aac\",\"type\":\"audio\",\"duration\":0,\"streamId\":1,\"rate\":48000,\"height\":0,\"sampleAspectRatioNum\":0,\"displayAspectRatioDen\":0,\"bitrate\":0,\"channelFormat\":\"5.1\",\"sampleAspectRatioDen\":0,\"closedCaptions\":false,\"sampleFormat\":6}],\"variant\":\"HD\"}";
		ObjectMapper objectMapper = new ObjectMapper();
		EncodeSummary encodeSummary1 = objectMapper.reader().withType(EncodeSummary.class).readValue(encodeSummaryStr1);
		EncodeSummary encodeSummary2 = objectMapper.reader().withType(EncodeSummary.class).readValue(encodeSummaryStr2);
		dao.storeEncodeSummary(encodeSummary1);
		dao.storeEncodeSummary(encodeSummary2);

	}

}
