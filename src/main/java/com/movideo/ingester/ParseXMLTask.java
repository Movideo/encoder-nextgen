package com.movideo.ingester;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.movideo.nextgen.common.multithreading.Task;
import com.movideo.nextgen.common.queue.QueueManager;
import com.movideo.nextgen.encoder.common.Util;
import com.movideo.nextgen.encoder.models.AzureBlobInfo;

public class ParseXMLTask extends Task
{
	private static final Logger log = LogManager.getLogger();

	private String workingListName = Util.getConfigProperty("redis.xml.parser.working.list"), errorListName = Util.getConfigProperty("redis.xml.parser.error.list"), successListName = Util.getConfigProperty("redis.xml.parser.success.list");

	public ParseXMLTask(QueueManager queueManager, String jobString)
	{
		super(queueManager, jobString);
	}
	
	
	  @Override
	    public void run() {
		  
		  
		  
		  
		  
		  System.out.println("parse xml called " + jobString);
		  Gson gson = new Gson();
		  IngesterTransferJson ingesterTransferJson =  gson.fromJson(jobString, IngesterTransferJson.class);
			
		  System.out.println("Recieved " + ingesterTransferJson.getUri());
		  
		  
		  
			AzureBlobInfo source = ingesterTransferJson.getAzureBlobInfo();
		final String sourceStorageConnectionString = Util.buildAzureConnectionString(source );


				CloudStorageAccount storageAccount;
		try
		{
			storageAccount = CloudStorageAccount.parse(sourceStorageConnectionString);

			CloudBlobClient blobClient = storageAccount.createCloudBlobClient();
			CloudBlobContainer container = blobClient.getContainerReference(source.getContainer());
			
			CloudBlockBlob xmlToParse = container.getBlockBlobReference(ingesterTransferJson.getBlobName());
			if(xmlToParse.exists()){
				
			
			String xmlText = xmlToParse.downloadText();
			//xmlToParse.downloadToFile(xmlToParse.getName());
			System.out.println("=========================================================");
			System.out.println(xmlText);
			System.out.println("=========================================================");
			
			}
		}
		catch(InvalidKeyException | URISyntaxException | StorageException | IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


		  
		  
		  
		  
		  // download and parse the xml- neeed container name etc
		  // check media exists
		  // if yes move media to processing
		  // - another task tocreate entities?
		  // persist entities
		  // call redis queue with encoding jobs
		  // any errors move to the error queue
		  
		 
		  
		  
	    }
	
}
