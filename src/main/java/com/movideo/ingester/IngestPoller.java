package com.movideo.ingester;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlobDirectory;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.azure.storage.blob.CopyStatus;
import com.microsoft.azure.storage.blob.ListBlobItem;
import com.movideo.nextgen.common.queue.QueueManager;
import com.movideo.nextgen.encoder.common.Util;
import com.movideo.nextgen.encoder.models.AzureBlobInfo;

public class IngestPoller implements Runnable
{

	QueueManager queueManager;

	public IngestPoller(QueueManager queueManager)
	{
	this.queueManager = queueManager;
	}

	@Override
	public void run()
	{

	AzureBlobInfo source = new AzureBlobInfo();

	source.setAccountKey(Util.getConfigProperty("azure.blob.input.account.key"));
	source.setAccountName(Util.getConfigProperty("azure.blob.input.account.name"));
	//source.setContainer(Util.getConfigProperty("azure.blob.input.container"));
	final String sourceStorageConnectionString = Util.buildAzureConnectionString(source);

	try
	{

		CloudStorageAccount storageAccount = CloudStorageAccount.parse(sourceStorageConnectionString);

		CloudBlobClient blobClient = storageAccount.createCloudBlobClient();
		CloudBlobContainer container = blobClient.getContainerReference(Util.getConfigProperty("azure.blob.input.container"));

		container.createIfNotExists();

		System.out.println("container uri " + container.getUri());
		
		for(ListBlobItem blobItem : container.listBlobs("processordone", true, null, null, null))
		{

		CloudBlockBlob blob = (CloudBlockBlob) blobItem;

		System.out.println("blob name " + blob.getName());
		}
		
		// loop over blobs, if we have an xml we put it in to processing, 
		// put the xml blob name and container uri and the xml uri on a queue
		// next job picks up the uri downloads the xml, checks for the media file
		
		// then parses the xml and uses existing dropbox stuff
		
		
		
		
		
		// Loop over blobs within the container and output the URI to each of them.
		for(ListBlobItem blobItem : container.listBlobs("landing", true, null, null, null))
		{

		CloudBlockBlob blob = (CloudBlockBlob) blobItem;

		System.out.println("blob name " + blob.getName());

		// ---------------
		String copyDestination = blob.getName().replace("landing", "processordone");
		System.out.println("copyDestination " + copyDestination);
		CloudBlockBlob destination = container.getBlockBlobReference(copyDestination);

		destination.startCopyFromBlob(blob);
		System.out.println("Destingation: " + destination.getUri());
		if(destination.exists() && destination.getCopyState().getStatus().equals(CopyStatus.SUCCESS))
		{
			System.out.println("destination file " + destination.getUri() + " exists");
			blob.deleteIfExists();
		}

		// --------------

		}

	}
	catch(InvalidKeyException | URISyntaxException | StorageException e)
	{
		//			log.error("Unable to transfer subtitle files specified for job" + job.getEncodingJobId(), e);
		//			job.setErrorType(Util.getConfigProperty("job.status.failed"));
		//			queueManager.moveQueues(workingListName, errorListName, jobString, job.toString());
		return;
	}

	System.out.println("calling run");
	}

}
