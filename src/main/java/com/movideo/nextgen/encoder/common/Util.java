package com.movideo.nextgen.encoder.common;

import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.amazonaws.util.json.JSONException;
import com.google.gson.Gson;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.movideo.nextgen.common.encoder.models.SubtitleInfo;
import com.movideo.nextgen.encoder.bitcodin.BitcodinException;
import com.movideo.nextgen.encoder.config.Constants;
import com.movideo.nextgen.encoder.models.AzureBlobInfo;
import com.movideo.nextgen.encoder.models.EncodingJob;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * Utility class with static methods across the board
 * 
 * @author yramasundaram
 */
public class Util
{

	private static final Logger log = LogManager.getLogger();

	/**
	 * Constructs a Bitcodin Job object from input JSON string Primarily avoids serialization and de-serialization
	 * 
	 * @param json
	 *            - JSON string representing the job
	 * @return
	 * @throws JSONException
	 */
	public static EncodingJob getBitcodinJobFromJSON(String jsonString)
	{

		Gson gson = new Gson();
		EncodingJob job = gson.fromJson(jsonString, EncodingJob.class);

		return job;
	}

	/**
	 * Used to move a specific item from one Redis list to another by the value This is different from the pop operations, because pop always gets the
	 * first or last element out and since the list is constantly changing, the only way to identify a specific element uniquely, is its value
	 * 
	 * @param pool
	 *            - Redis connection pool
	 * @param fromListName
	 *            - Source list
	 * @param toListName
	 *            - Target list
	 * @param originalValue
	 *            - This is used to locate the message to delete
	 * @param newValue
	 *            - This is the value to be posted to the new queue (may be having extra parameters)
	 */
	public static void moveJobToNextList(JedisPool pool, String fromListName, String toListName, String originalValue,
			String newValue)
	{

		/*
		 * Jedis escapes JSON string when storing, but doesn't escape it when
		 * matching values for LREM. For now, escaping double quotes seems to be
		 * sufficient. Will have to revisit if there are other problematic
		 * characters
		 */
		String valueToBeRemoved = originalValue.replaceAll("\"", "\\\"");
		Jedis jedis = pool.getResource();
		jedis.lpush(toListName, newValue);
		Long response = jedis.lrem(fromListName, 1, valueToBeRemoved);
		log.debug("Util : moveJobToNextList() -> IN MOVE TO NEXT LIST: Number of entries deleted is: " + response);
		jedis.close();
		// TODO: Messages pushed into the error list are not processed at the
		// moment. Need to implement error handling and
		// selective re-tries based on error types.
	}

	// public static void main(String[] args) throws JSONException {
	// String json =
	// "{\"mediaId\":837935,\"encodingProfileId\":35364,\"inputFileName\":\"ForYourIceOnly.mp4\",\"status\":\"NEW\",\"speed\":\"premium\",\"bitcodinJobId\":0,\"serialversionuid\":-2746341744995209121,\"outputId\":19496,\"clientId\":524,\"inputId\":0,\"inputFileUrl\":\"http://movideoqaoriginal1.blob.core.windows.net/original-524/media/837935/ForYourIceOnly.mp4\",\"manifestTypes\":[\"mpd\"],\"retryCount\":0}";
	// getBitcodinJobFromJSON(json);
	// }

	public static String getMediaUrlFromSegments(int clientId, int mediaId, String fileName, boolean isInput, String outputPath, boolean includePrefix)
	{
		StringBuffer buffer = new StringBuffer();
		if(includePrefix)
		{
			if(isInput)
			{
				buffer.append(Constants.AZURE_INPUT_URL_PREFIX).append(Constants.AZURE_INPUT_BLOB_CONTAINER_PREFIX).append(clientId).append("/");
			}
			else
			{
				buffer.append(Constants.AZURE_OUTPUT_URL_PREFIX).append(Constants.AZURE_OUTPUT_BLOB_CONTAINER_PREFIX).append(clientId).append("/");
			}
		}
		//Media path prefix is the same for input and output, for now.
		buffer.append(Constants.AZURE_INPUT_BLOB_MEDIA_PATH_PREFIX).append("/").append(mediaId).append("/").append(outputPath != null ? outputPath + "/" : "").append(fileName);
		return buffer.toString();
	}

	public static List<SubtitleInfo> formatSubUrls(List<SubtitleInfo> inputSubsList, int clientId, int mediaId, boolean isInput, String outputPath, boolean includePrefix)
	{
		List<SubtitleInfo> outputSubsList = new ArrayList<>();

		for(SubtitleInfo inputSub : inputSubsList)
		{
			SubtitleInfo outputSub = new SubtitleInfo();
			outputSub.setLangLong(inputSub.getLangLong());
			outputSub.setLangShort(inputSub.getLangShort());
			outputSub.setType(inputSub.getType());
			outputSub.setUrl(getMediaUrlFromSegments(clientId, mediaId, inputSub.getUrl(), isInput, outputPath, includePrefix));
			outputSubsList.add(outputSub);
		}
		return outputSubsList;
	}

	public static String getManifestUrl(EncodingJob job, String tempUrl) throws BitcodinException
	{
		//"http://eu-storage-bitcodin.storage.googleapis.com/bitStorage/2232_5dbb991ee5d11f1a3f8dd3e9898c8f46/38228_2d5fbd0c6cba17ebe51f98d2b635c5dd/test1234.mpd"
		String[] tokens = tempUrl.split("/");
		int tokenCount = tokens.length;

		if(tokenCount < 2)
		{
			throw new BitcodinException(Constants.STATUS_CODE_BAD_REQUEST, "Bad temp URL. Could not get manifest url", null);
		}

		StringBuffer buffer = new StringBuffer(Constants.AZURE_OUTPUT_URL_PREFIX).append(Constants.AZURE_OUTPUT_BLOB_CONTAINER_PREFIX).append(job.getClientId()).append("/");
		buffer.append(Constants.AZURE_INPUT_BLOB_MEDIA_PATH_PREFIX).append("/").append(job.getMediaId()).append("/").append(tokens[tokenCount - 2]).append("/").append(tokens[tokenCount - 1]);

		log.debug("Manifest URL : " + buffer);
		return buffer.toString();
	}

	public static String getBitcodinFolderHash(String tempUrl)
	{
		//"http://eu-storage-bitcodin.storage.googleapis.com/bitStorage/2232_5dbb991ee5d11f1a3f8dd3e9898c8f46/38228_2d5fbd0c6cba17ebe51f98d2b635c5dd/test1234.mpd"
		String[] uriSegments = tempUrl.split("/");
		String bitcodinFolderHash = uriSegments[uriSegments.length - 2];

		log.debug("Folder hash is: " + bitcodinFolderHash);
		return bitcodinFolderHash;
	}

	public static void copyAzureBlob(AzureBlobInfo source, AzureBlobInfo destination) throws URISyntaxException, StorageException, InvalidKeyException
	{

		final String sourceStorageConnectionString = buildAzureConnectionString(source);
		final String destinationStorageConnectionString = buildAzureConnectionString(destination);

		log.debug("About to move blob files from source to destination. Params:\n");

		CloudStorageAccount sourceStorageAccount = CloudStorageAccount.parse(sourceStorageConnectionString);
		CloudBlobClient sourceBlobClient = sourceStorageAccount.createCloudBlobClient();
		CloudBlobContainer sourceContainer = sourceBlobClient.getContainerReference(source.getContainer());

		CloudStorageAccount destStorageAccount = CloudStorageAccount.parse(destinationStorageConnectionString);
		CloudBlobClient destBlobClient = destStorageAccount.createCloudBlobClient();
		CloudBlobContainer destContainer = destBlobClient.getContainerReference(destination.getContainer());

		log.debug("Input: Connection string: " + sourceStorageConnectionString + ", Container: " + source.getContainer());
		log.debug("Input: Connection string: " + destinationStorageConnectionString + ", Container: " + destination.getContainer());

		int countFiles = source.getBlobReferences().size();

		log.debug("Count of files to be moved: " + countFiles);

		for(int counter = 0; counter < countFiles; counter++)
		{
			CloudBlockBlob sourceBlob = sourceContainer.getBlockBlobReference(source.getBlobReferences().get(counter));
			log.debug("Source blob reference: " + source.getBlobReferences().get(counter));

			CloudBlockBlob destBlob = destContainer.getBlockBlobReference(destination.getBlobReferences().get(counter));
			log.debug("Destination blob reference: " + destination.getBlobReferences().get(counter));

			destBlob.startCopyFromBlob(sourceBlob);
			log.debug("Started copying " + source.getBlobReferences().get(counter) + "to " + destination.getContainer());

		}

	}

	private static String buildAzureConnectionString(AzureBlobInfo info)
	{
		return "DefaultEndpointsProtocol=http;" +
				"AccountName=" + info.getAccountName() + ";" +
				"AccountKey=" + info.getAccountKey();
	}

}
