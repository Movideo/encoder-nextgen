package com.movideo.nextgen.encoder.bitcodin.tasks;

import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.gson.JsonSyntaxException;
import com.microsoft.azure.storage.StorageException;
import com.movideo.nextgen.common.encoder.models.SubtitleInfo;
import com.movideo.nextgen.common.multithreading.Task;
import com.movideo.nextgen.common.queue.QueueException;
import com.movideo.nextgen.common.queue.QueueManager;
import com.movideo.nextgen.encoder.bitcodin.BitcodinException;
import com.movideo.nextgen.encoder.bitcodin.BitcodinProxy;
import com.movideo.nextgen.encoder.common.Util;
import com.movideo.nextgen.encoder.config.Constants;
import com.movideo.nextgen.encoder.dao.EncodeDAO;
import com.movideo.nextgen.encoder.models.AzureBlobInfo;
import com.movideo.nextgen.encoder.models.EncodeSummary;
import com.movideo.nextgen.encoder.models.EncodingJob;
import com.movideo.nextgen.encoder.models.Manifest;

/**
 * Polls Bitcodin for the status of the id specified in the input If the status is completed or errored, moves it to the appropriate list If not,
 * replaces the message back in the list for round-robin polling
 *
 * @author yramasundaram
 */
public class PollBitcodinJobStatusTask extends Task
{

	private static final Logger log = LogManager.getLogger();

	private String pendingListName = Constants.REDIS_PENDING_LIST, workingListName = Constants.REDIS_PENDING_WORKING_LIST, errorListName = Constants.REDIS_POLL_ERROR_LIST, successListName = Constants.REDIS_FINISHED_LIST;

	private EncodeDAO encodeDAO;

	public PollBitcodinJobStatusTask(QueueManager manager, EncodeDAO encodeDAO, String jobString)
	{
		super(manager, jobString);
		this.encodeDAO = encodeDAO;
	}

	@Override
	public void run()
	{

		log.debug("PollBitcodinJobStatus : run() -> Executing poller");

		String status;
		JSONObject response;
		EncodingJob job;

		log.debug("PollBitcodinJobStatus : run() -> Job string is: " + jobString);
		try
		{

			try
			{
				job = Util.getBitcodinJobFromJSON(jobString);
			}
			catch(JsonSyntaxException e)
			{
				log.error("Could not extract bitcodin job from JSON Object", e);
				queueManager.moveQueues(workingListName, errorListName, jobString, null);
				return;
			}

			log.debug("PollBitcodinJobStatus : run() -> Now polling Job id: " + job.getEncodingJobId());

			try
			{
				response = BitcodinProxy.getJobStatus(job.getEncodingJobId());
				status = response.getString("status");
				log.debug("PollBitcodinJobStatus : run() -> Response Status is: " + status);

			}
			catch(BitcodinException | JSONException e)
			{
				log.error("Encoding failed for the job id " + job.getEncodingJobId(), e);
				job.setErrorType(Constants.STATUS_JOB_FAILED);
				queueManager.moveQueues(workingListName, errorListName, jobString, job.toString());
				return;
			}

			if(status != null && (status.equalsIgnoreCase("Created") || status.equalsIgnoreCase("Enqueued") || status.equalsIgnoreCase("In Progress")))
			{
				// Put back in the pending list to check back later
				queueManager.moveQueues(workingListName, pendingListName, jobString, job.toString());

			}
			else if(status != null && status.equalsIgnoreCase("Finished"))
			{
				//Job completed. Process subtitles if needed

				List<SubtitleInfo> subtitles = job.getSubtitleList();
				if(subtitles != null)
				{
					long jobId = job.getEncodingJobId();
					String[] manifestTypes = job.getManifestTypes();
					if(manifestTypes == null || manifestTypes.length == 0)
					{
						log.error("Unable to create manifest with subs for job id " + job.getEncodingJobId(), new BitcodinException(Constants.STATUS_CODE_BAD_REQUEST, "Manifest array is empty", null));
						job.setErrorType(Constants.STATUS_JOB_FAILED);
						queueManager.moveQueues(workingListName, errorListName, jobString, job.toString());
						return;
					}
					List<Manifest> manifestUrlList = new ArrayList<>();
					String tempUrl = "";

					for(String manifestType : manifestTypes)
					{
						//TODO: Find a way to generalize processing of other subtitle types
						try
						{
							String outputManifestName = jobId + "_subs." + manifestType;
							response = BitcodinProxy.createManifestWithSubs(jobId, subtitles, outputManifestName, manifestType, "vtt");
							log.debug("Response from create subtitle call: " + response);
							String urlKey;
							Manifest manifest = new Manifest();

							switch(manifestType)
							{
								case Constants.MPEG_DASH_MANIFEST_TYPE:
								{
									urlKey = manifestType + "Url";
									manifest.setType(manifestType);
									break;

								}
								case Constants.HLS_MANIFEST_TYPE:
								{
									urlKey = Constants.HLS_STREAM_TYPE + "Url";
									manifest.setType(Constants.HLS_MANIFEST_TYPE);
									break;
								}
								default:
								{
									throw new BitcodinException(Constants.STATUS_CODE_BAD_REQUEST, "Unsupported manifest type", null);
								}
							}
							if(response.has(urlKey))
							{
								tempUrl = response.getString(urlKey);
								manifest.setUrl(Util.getManifestUrl(job, tempUrl));
								manifestUrlList.add(manifest);
							}
							else
							{
								throw new BitcodinException(Constants.STATUS_CODE_SERVER_ERROR, "Unable to retrieve manifest url with subtitles!", null);
							}
						}
						catch(BitcodinException | JSONException e)
						{
							log.error("Unable to create manifest with subs for job id " + job.getEncodingJobId(), e);
							job.setErrorType(Constants.STATUS_JOB_FAILED);
							queueManager.moveQueues(workingListName, errorListName, jobString, job.toString());
							return;
						}
					}
					// Copy bitcodin output to Azure blob
					try
					{
						BitcodinProxy.transferToAzure(jobId, job.getOutputId());
					}
					catch(BitcodinException e)
					{
						log.error("Unable to transfer files after subtitle processing for job" + job.getEncodingJobId(), e);
						job.setErrorType(Constants.STATUS_JOB_FAILED);
						queueManager.moveQueues(workingListName, errorListName, jobString, job.toString());
						return;
					}
					// Copy subtitle files over from origin blob to destination blob
					try
					{
						AzureBlobInfo input = new AzureBlobInfo();
						AzureBlobInfo output = new AzureBlobInfo();

						input.setAccountKey(Constants.AZURE_INPUT_ACCOUNT_KEY);
						input.setAccountName(Constants.AZURE_INPUT_ACCOUNT_NAME);
						input.setContainer(Constants.AZURE_INPUT_BLOB_CONTAINER_PREFIX + job.getClientId());

						output.setAccountKey(Constants.AZURE_OUPUT_ACCOUNT_KEY);
						output.setAccountName(Constants.AZURE_OUTPUT_ACCOUNT_NAME);
						output.setContainer(Constants.AZURE_OUTPUT_BLOB_CONTAINER_PREFIX + job.getClientId());

						input.setBlobReferences(getSubFilenames(job, true, null));
						output.setBlobReferences(getSubFilenames(job, false, Util.getBitcodinFolderHash(tempUrl)));

						Util.copyAzureBlob(input, output);

					}
					catch(InvalidKeyException | URISyntaxException | StorageException e)
					{
						log.error("Unable to transfer subtitle files specified for job" + job.getEncodingJobId(), e);
						job.setErrorType(Constants.STATUS_JOB_FAILED);
						queueManager.moveQueues(workingListName, errorListName, jobString, job.toString());
						return;
					}
					EncodeSummary encodeSummary = job.getEncodeSummary();
					encodeSummary.setManifests(manifestUrlList.toArray(new Manifest[manifestUrlList.size()]));
					log.info("Encode Summary is: " + encodeSummary);
					job.setEncodeSummary(encodeSummary);
					//TODO: Poll Bitcodin to check transfer status

				}

				queueManager.moveQueues(workingListName, successListName, jobString, job.toString());
				log.debug("Encode summary for this job is: " + job.getEncodeSummary());
				encodeDAO.storeEncodeSummary(job.getEncodeSummary());

			}
			else
			{
				log.error("Job failed");
				job.setStatus(Constants.STATUS_JOB_FAILED);
				queueManager.moveQueues(workingListName, errorListName, jobString, job.toString());
				return;
			}
		}
		catch(QueueException e)
		{
			log.error("PollBitcodinJob :: Queue Exception when trying to process job " + e.getMessage());
			return;
		}
	}

	private List<String> getSubFilenames(EncodingJob job, boolean isInput, String outputPath)
	{
		List<String> subFilenames = new ArrayList<>();

		List<SubtitleInfo> formattedInputSubs = Util.formatSubUrls(job.getSubtitleList(), job.getClientId(), job.getMediaId(), isInput, outputPath, false);
		for(SubtitleInfo info : formattedInputSubs)
		{
			log.debug("Current subtitle url: " + info.getUrl());
			subFilenames.add(info.getUrl());
		}

		return subFilenames;
	}

	//	public static void main(String[] args) throws InvalidKeyException, URISyntaxException, StorageException
	//	{
	//		AzureBlobInfo input = new AzureBlobInfo();
	//		AzureBlobInfo output = new AzureBlobInfo();
	//
	//		EncodingJob job = new EncodingJob();
	//
	//		List<SubtitleInfo> subList = new ArrayList<>();
	//
	//		SubtitleInfo subtitleEn = new SubtitleInfo();
	//		subtitleEn.setLangLong("English");
	//		subtitleEn.setLangShort("en");
	//		subtitleEn.setUrl("track_en.vtt");
	//		subList.add(subtitleEn);
	//
	//		SubtitleInfo subtitleVi = new SubtitleInfo();
	//		subtitleVi.setLangLong("Vietnamese");
	//		subtitleVi.setLangShort("vi");
	//		subtitleVi.setUrl("track_vi.vtt");
	//		subList.add(subtitleVi);
	//
	//		job.setSubtitleList(subList);
	//		job.setClientId(524);
	//		job.setMediaId(848095);
	//
	//		String tempUrl = "http://yadayada/yadayada/48275_695ab9d2816de1f352845233839a2d03/test.xyz";
	//
	//		input.setAccountKey(Constants.AZURE_INPUT_ACCOUNT_KEY);
	//		input.setAccountName(Constants.AZURE_INPUT_ACCOUNT_NAME);
	//		input.setContainer(Constants.AZURE_INPUT_BLOB_CONTAINER_PREFIX + job.getClientId());
	//
	//		output.setAccountKey(Constants.AZURE_OUPUT_ACCOUNT_KEY);
	//		output.setAccountName(Constants.AZURE_OUTPUT_ACCOUNT_NAME);
	//		output.setContainer(Constants.AZURE_OUTPUT_BLOB_CONTAINER_PREFIX + job.getClientId());
	//
	//		input.setBlobReferences(getSubFilenames(job, true, null));
	//		output.setBlobReferences(getSubFilenames(job, false, Util.getBitcodinFolderHash(tempUrl)));
	//
	//		Util.copyAzureBlob(input, output);
	//
	//	}
}
