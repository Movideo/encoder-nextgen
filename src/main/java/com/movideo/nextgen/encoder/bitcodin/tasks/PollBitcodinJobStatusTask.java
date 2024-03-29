package com.movideo.nextgen.encoder.bitcodin.tasks;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.movideo.nextgen.common.encoder.models.SubtitleInfo;
import com.movideo.nextgen.common.multithreading.Task;
import com.movideo.nextgen.common.queue.QueueException;
import com.movideo.nextgen.common.queue.QueueManager;
import com.movideo.nextgen.encoder.bitcodin.BitcodinException;
import com.movideo.nextgen.encoder.bitcodin.BitcodinProxy;
import com.movideo.nextgen.encoder.common.Util;
import com.movideo.nextgen.encoder.dao.EncodeDAO;
import com.movideo.nextgen.encoder.models.AzureBlobInfo;
import com.movideo.nextgen.encoder.models.EncodeSummary;
import com.movideo.nextgen.encoder.models.EncodingJob;
import com.movideo.nextgen.encoder.models.FtpInfo;
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

	private String pendingListName = Util.getConfigProperty("redis.poller.input.list"), workingListName = Util.getConfigProperty("redis.poller.working.list"), errorListName = Util.getConfigProperty("redis.poller.error.list"), successListName = Util.getConfigProperty("redis.poller.success.list");
	private String ftpRetryListName = Util.getConfigProperty("redis.poller.ftp.retry.list"), subtitleRetryListName = Util.getConfigProperty("redis.poller.subtitles.retry.list");

	private EncodeDAO encodeDAO;

	public PollBitcodinJobStatusTask(QueueManager manager, EncodeDAO encodeDAO, String jobString)
	{
		super(manager, jobString);
		this.encodeDAO = encodeDAO;
	}

	@Override
	public void run()
	{

		String jobString = new String(jobStringAsBytes, StandardCharsets.UTF_8);

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
			catch(JsonSyntaxException | JsonIOException | UnsupportedEncodingException e)
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
				job.setErrorType(Util.getConfigProperty("job.status.failed"));
				queueManager.moveQueues(workingListName, errorListName, jobString, job.toString());
				return;
			}

			if(status != null && (status.equalsIgnoreCase("Created") || status.equalsIgnoreCase("Enqueued") || status.equalsIgnoreCase("In Progress")))
			{
				//If there are no other items on the input queue, prevent this message from flooding bitcodin
				if(queueManager.getQueueLength(pendingListName) == 0)
				{
					try
					{
						Thread.sleep(Integer.parseInt(Util.getConfigProperty("thread.sleep.time")));
					}
					catch(InterruptedException e)
					{
						log.info("Thread wait time interrupted");
					}
				}

				// Put back in the pending list to check back later
				queueManager.moveQueues(workingListName, pendingListName, jobString, job.toString());

			}
			else if(status != null && status.equalsIgnoreCase("Finished"))
			{
				//Job completed. Process subtitles if needed

				List<SubtitleInfo> subtitles = job.getSubtitleList();
				if(subtitles != null && subtitles.size() > 0)
				{
					long jobId = job.getEncodingJobId();
					String[] manifestTypes = job.getManifestTypes();
					if(manifestTypes == null || manifestTypes.length == 0)
					{
						log.error("Unable to create manifest with subs for job id " + job.getEncodingJobId(), new BitcodinException(Integer.parseInt(Util.getConfigProperty("error.codes.bad.request")), "Manifest array is empty", null));
						job.setErrorType(Util.getConfigProperty("job.status.failed"));
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

							if(manifestType.equalsIgnoreCase(Util.getConfigProperty("stream.mpd.manifest.type")))
							{
								urlKey = manifestType + "Url";
								manifest.setType(manifestType);
							}
							else if(manifestType.equalsIgnoreCase(Util.getConfigProperty("stream.hls.manifest.type")))
							{
								urlKey = Util.getConfigProperty("stream.hls.type") + "Url";
								manifest.setType(Util.getConfigProperty("stream.hls.manifest.type"));
							}
							else
							{
								throw new BitcodinException(Integer.parseInt(Util.getConfigProperty("error.codes.bad.request")), "Unsupported manifest type", null);
							}

							if(response.has(urlKey))
							{
								tempUrl = response.getString(urlKey);
								String manifestUrl = Util.getManifestUrl(job, tempUrl);
								manifest.setUrl(manifestUrl);
								manifestUrlList.add(manifest);

								// Copy subtitle files over from origin blob to destination blob + CDN FTP if configured 
								try
								{
									AzureBlobInfo input = new AzureBlobInfo();
									AzureBlobInfo output = new AzureBlobInfo();

									input.setAccountKey(Util.getConfigProperty("azure.blob.input.account.key"));
									input.setAccountName(Util.getConfigProperty("azure.blob.input.account.name"));
									input.setContainer(Util.getConfigProperty("azure.blob.input.container.prefix") + job.getClientId());

									output.setAccountKey(Util.getConfigProperty("azure.blob.output.account.key"));
									output.setAccountName(Util.getConfigProperty("azure.blob.output.account.name"));
									output.setContainer(Util.getConfigProperty("azure.blob.output.container.prefix") + job.getClientId());

									input.setBlobReferences(getSubFilenames(job, true, null));
									//output.setBlobReferences(getSubFilenames(job, false, Util.getBitcodinFolderHash(tempUrl)));
									output.setBlobReferences(Util.getSubtitleOutputBlobReferences(job.getSubtitleList(), manifestUrl));

									boolean result = Util.copySubtitles(input, output, job, manifestUrl);
									//Blob copy went through, but FTP failed
									if(!result && job.isCdnSyncRequired())
									{
										job.setStatus("WARN");
										job.setErrorType("SUBS_COPY_FTP_FAILED");
										queueManager.push(subtitleRetryListName, job.toString());
									}

								}
								catch(Exception e)
								{
									//Blob copy failed
									log.error("Unable to transfer subtitle files specified for job: " + job.getEncodingJobId() + ", product: " + job.getProductId() + ", variant" + job.getVariant() + ", mediaId: " + job.getMediaId(), e);
									job.setStatus("WARN");
									job.setErrorType("SUBS_COPY_FTP_AND_BLOB_FAILED");
									queueManager.push(subtitleRetryListName, job.toString());

									//job.setErrorType(Util.getConfigProperty("job.status.failed"));
									//queueManager.moveQueues(workingListName, errorListName, jobString, job.toString());
									//return;
								}
							}
							else
							{
								throw new BitcodinException(Integer.parseInt(Util.getConfigProperty("error.codes.internal.server.error")), "Unable to retrieve manifest url with subtitles!", null);
							}
						}
						catch(BitcodinException | JSONException e)
						{
							log.error("Unable to create manifest with subs for job id " + job.getEncodingJobId(), e);
							job.setErrorType(Util.getConfigProperty("job.status.failed"));
							queueManager.moveQueues(workingListName, errorListName, jobString, job.toString());
							return;
						}
					}
					// Copy bitcodin output to Azure blob
					try
					{
						BitcodinProxy.transferJobOutput(jobId, job.getOutputId());
					}
					catch(BitcodinException e)
					{
						log.error("Unable to transfer files after subtitle processing for job" + job.getEncodingJobId(), e);
						job.setErrorType(Util.getConfigProperty("job.status.failed"));
						queueManager.moveQueues(workingListName, errorListName, jobString, job.toString());
						return;
					}

					EncodeSummary encodeSummary = job.getEncodeSummary();
					encodeSummary.setManifests(manifestUrlList.toArray(new Manifest[manifestUrlList.size()]));
					log.info("Encode summary for this job is: " + job.getEncodeSummary());
					job.setEncodeSummary(encodeSummary);
					//TODO: Poll Bitcodin to check transfer status

				}

				// Copy bitcodin output to CDN FTP if required
				if(job.isCdnSyncRequired())
				{
					FtpInfo ftpInfo = Util.getFtpOutputInfo(job);
					job.setCdnFtpInfo(ftpInfo);

					try
					{
						response = BitcodinProxy.createFTPOutput(ftpInfo);
						if(response.has("outputId"))
						{
							try
							{
								long outputId = response.getLong("outputId");
								ftpInfo.setOutputId(outputId);
								job.setCdnFtpInfo(ftpInfo);
								response = BitcodinProxy.transferJobOutput(job.getEncodingJobId(), outputId);
							}
							catch(BitcodinException | JSONException e)
							{
								log.error("Unable to start transfer to FTP job");
								queueManager.push(ftpRetryListName, job.toString());
								//queueManager.moveQueues(workingListName, errorListName, jobString, job.toString());
								//return;
							}
						}
						else
						{
							log.error("Error in creating FTP output for mediaId: " + job.getMediaId());
							queueManager.push(ftpRetryListName, job.toString());
							//queueManager.moveQueues(workingListName, errorListName, jobString, job.toString());
							//return;
						}
					}
					catch(BitcodinException | NumberFormatException e1)
					{
						log.error("Unable to create FTP output");
						queueManager.push(ftpRetryListName, job.toString());
						//queueManager.moveQueues(workingListName, errorListName, jobString, job.toString());
						//return;
					}

				}

				queueManager.moveQueues(workingListName, successListName, jobString, job.toString());
				encodeDAO.storeEncodeSummary(job.getEncodeSummary());
				log.info("Successfully created encode summary for job: " + job.getEncodingJobId() + ", productId: " + job.getProductId() + ", variant: " + job.getVariant() + ", mediaId: " + job.getMediaId() + " created successfully");

			}
			else
			{
				log.error("Job failed");
				job.setStatus(Util.getConfigProperty("job.status.failed"));
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
}
