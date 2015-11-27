package com.movideo.nextgen.encoder.bitcodin.tasks;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.movideo.nextgen.common.encoder.models.EncodeInfo;
import com.movideo.nextgen.common.encoder.models.EncodeRequest;
import com.movideo.nextgen.common.encoder.models.StreamInfo;
import com.movideo.nextgen.common.multithreading.Task;
import com.movideo.nextgen.common.queue.QueueException;
import com.movideo.nextgen.common.queue.QueueManager;
import com.movideo.nextgen.encoder.common.Util;
import com.movideo.nextgen.encoder.config.Constants;
import com.movideo.nextgen.encoder.models.EncodingJob;

public class ProcessEncodeRequestTask extends Task {

    private static final Logger log = LogManager.getLogger();

    private String workingListName = Constants.REDIS_ENCODE_REQUEST_WORKING_LIST,
	    errorListName = Constants.REDIS_ENCODE_REQUEST_ERROR_LIST, successListName = Constants.REDIS_INPUT_LIST;

    public ProcessEncodeRequestTask(QueueManager manager, String jobString) {
	super(manager, jobString);
    }

    @Override
    public void run() {
	
	log.debug("In ProcessEncodeTask");
	EncodeRequest encodeRequest;
	
	try {
	    encodeRequest = new Gson().fromJson(jobString, EncodeRequest.class);
	    log.debug("Encode Request Received is: \n" + new Gson().toJson(encodeRequest));
	} catch (JsonSyntaxException e) {
	    log.error("Unable to create EncodeRequest object from input message. Exception is: " + e.getMessage());
	    try {
		queueManager.moveQueues(workingListName, errorListName, jobString, jobString);
	    } catch (QueueException e1) {
		log.error("Unable to move erroneous message to the error list");
	    }
	    return;
	}
	
	log.debug("Length of encode info is: " + encodeRequest.getEncodeInfo().size());
	
	List<EncodeInfo> encodeList = encodeRequest.getEncodeInfo();
	
	for (EncodeInfo encodeInfo : encodeList) {
	    log.debug("In encode info loop");
	    EncodingJob job = new EncodingJob();
	    job.setClientId(encodeRequest.getClientId());
	    job.setMediaId(encodeRequest.getMediaId());
	    job.setProductId(encodeRequest.getProductId());
	    job.setVariant(encodeRequest.getVariant());
	    log.debug("Basic params set");
	    job.setEncodingProfileId(encodeInfo.getEncodingProfileId());
	    log.debug("Encoding profile id set");
	    job.setInputFileName(encodeRequest.getInputFilename());
	    log.debug("filename set");
	    job.setInputFileUrl(Util.getMediaUrlFromSegments(encodeRequest.getClientId(), encodeRequest.getMediaId(),
		    encodeRequest.getInputFilename()));
	    log.debug("Input file url set");
	    StreamInfo streams = encodeInfo.getStreamInfo();
	    log.debug("Got streams");
	    job.setManifestTypes(streams.getManifestType().toArray(new String[streams.getManifestType().size()]));
	    log.debug("Got manifest types");
	    job.setStatus(Constants.STATUS_NEW);
	    log.debug("Ready to post job request to the next list. Job string is: " + job.toString());
	    try {
		queueManager.moveQueues(workingListName, successListName, jobString, job.toString());
	    } catch (QueueException e) {
		log.error("Unable to move message to the next list! Exception is: " + e.getMessage());
	    }
	}

    }

}