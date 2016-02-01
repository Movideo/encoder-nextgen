package com.movideo.nextgen.encoder.bitcodin.tasks;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.movideo.nextgen.common.encoder.models.SubtitleInfo;
import com.movideo.nextgen.common.multithreading.Task;
import com.movideo.nextgen.common.queue.QueueException;
import com.movideo.nextgen.common.queue.QueueManager;
import com.movideo.nextgen.encoder.bitcodin.BitcodinDRMConfigBuilder;
import com.movideo.nextgen.encoder.bitcodin.BitcodinException;
import com.movideo.nextgen.encoder.bitcodin.BitcodinProxy;
import com.movideo.nextgen.encoder.common.Util;
import com.movideo.nextgen.encoder.dao.EncodeDAO;
import com.movideo.nextgen.encoder.models.EncodeSummary;
import com.movideo.nextgen.encoder.models.EncodingJob;
import com.movideo.nextgen.encoder.models.InputConfig;
import com.movideo.nextgen.encoder.models.OutputConfig;

import net.minidev.json.JSONArray;

/**
 * Runnable class that submits a new job to Bitcodin and queues it into the pending list for subsequent polling
 *
 * @author yramasundaram
 */
public class CreateBitcodinJobTask extends Task
{

	private static final Logger log = LogManager.getLogger();
	private EncodeDAO encodeDao;

	private String workingListName = Util.getConfigProperty("redis.encoder.working.list"), errorListName = Util.getConfigProperty("redis.encoder.error.list"), successListName = Util.getConfigProperty("redis.encoder.success.list");

	public CreateBitcodinJobTask(QueueManager queueManager, EncodeDAO encodeDao, String jobString)
	{
		super(queueManager, jobString);
		this.encodeDao = encodeDao;
	}

	private EncodeSummary getEncodeSummary(EncodingJob job, JSONObject createJobResponse) throws JSONException, IOException
	{

		JSONObject encodeSummary = new JSONObject();

		encodeSummary.put("object", "encoding");
		encodeSummary.put("product_id", job.getProductId());
		encodeSummary.put("media_id", job.getMediaId());
		encodeSummary.put("variant", job.getVariant());
		encodeSummary.put("mediaConfigurations", createJobResponse.getJSONObject("input").getJSONArray("mediaConfigurations"));
		JSONArray manifests = new JSONArray();

		List<SubtitleInfo> subtitles = job.getSubtitleList();

		// No subtitles, need to set the encode summary output path here

		if(subtitles == null || subtitles.size() == 0)
		{
			String outputPath = createJobResponse.getString("outputPath");

			for(String manifestType : job.getManifestTypes())
			{
				String manifestPath;
				JSONObject manifestLocation = new JSONObject();
				if(manifestType.equalsIgnoreCase(Util.getConfigProperty("stream.mpd.manifest.type")))
				{
					manifestLocation.put("type", manifestType);
					manifestPath = outputPath + "/" + Util.getConfigProperty("bitcodin.job.output.path.mpd.prefix");
				}
				else
				{
					manifestLocation.put("type", Util.getConfigProperty("stream.hls.type"));
					manifestPath = outputPath + "/" + Util.getConfigProperty("bitcodin.job.output.path.hls.prefix");
				}

				manifestLocation.put("url", manifestPath + "/" + job.getEncodingJobId() + "." + manifestType);
				manifests.add(manifestLocation);
			}

		}

		encodeSummary.put("manifests", manifests);
		encodeSummary.put("streamType", job.isProtectionRequired() ? "protected" : "unprotected");

		String json = encodeSummary.toString();
		ObjectMapper objectMapper = new ObjectMapper();
		return objectMapper.reader().withType(EncodeSummary.class).readValue(json);
	}

	@Override
	public void run()
	{

		String jobString = new String(jobStringAsBytes, StandardCharsets.UTF_8);

		log.debug("CreateBitcodinJob : run() -> Executing job creator");

		JSONObject response;
		Map<String, JSONObject> drmConfigMap = null;
		EncodingJob job;

		// int mediaId;
		// TODO: Replace all Sysouts with proper log statements. Retain key
		// information for debug purposes

		log.info("CreateBitcodinJob : run() -> Job string is: " + jobString);

		try
		{

			try
			{
				job = Util.getBitcodinJobFromJSON(jobString);
			}
			catch(JsonSyntaxException | JsonIOException | UnsupportedEncodingException e)
			{
				log.error("Could not extract bitcodin job from job string", e);
				queueManager.moveQueues(workingListName, errorListName, jobString, null);
				return;
			}

			List<EncodeSummary> mediaEncodings = encodeDao.getExistingMedia(job.getMediaId(), job.getVariant());

			// Delete previous encoding summaries if it's a reprocess request
			if(job.isReprocess() && !mediaEncodings.isEmpty())
			{
				log.info("Media already exists and this is a reprocess job. Removing previous encoding summary: \n" + mediaEncodings.get(0));
				EncodeSummary summary = mediaEncodings.get(0);
				encodeDao.deleteExistingMedia(summary.getId(), summary.getRevision());
			}
			else if(!mediaEncodings.isEmpty())
			{
				log.error("Media already exists, but this is NOT a reprocess job. Previous encoding summary: \n" + mediaEncodings.get(0));
				job.setStatus(Util.getConfigProperty("job.status.failed"));
				queueManager.moveQueues(workingListName, errorListName, jobString, job.toString());
			}

			InputConfig inputConfig = new InputConfig(Util.getConfigProperty("bitcodin.input.azure.type"), Util.getConfigProperty("azure.blob.input.account.name"), Util.getConfigProperty("azure.blob.input.account.key"), Util.getConfigProperty("azure.blob.input.container.prefix") + job.getClientId());

			OutputConfig outputConfig = new OutputConfig(Util.getConfigProperty("bitcodin.output.azure.type"), Util.getConfigProperty("bitcodin.output.name.prefix") + job.getClientId() + "-" + job.getMediaId(), Util.getConfigProperty("azure.blob.output.account.name"), Util.getConfigProperty("azure.blob.output.account.key"), Util.getConfigProperty("azure.blob.output.container.prefix") + job.getClientId(), Util.getConfigProperty("azure.blob.media.path.prefix") + "/" + job.getMediaId() + "/");

			// TODO: Track these statuses by Media Id. Dropbox processor creates
			// the
			// first entry
			// which needs to be subsquently updated at each point.
			job.setStatus(Util.getConfigProperty("job.status.submitted"));
			boolean hasSubs = (job.getSubtitleList() != null && job.getSubtitleList().size() > 0) ? true : false;

			if(job.isProtectionRequired())
			{
				log.debug("Protection required for the current encoding");

				try
				{
					drmConfigMap = BitcodinDRMConfigBuilder.getDRMConfigMap(job);
					if(drmConfigMap.isEmpty())
					{
						throw new BitcodinException(Integer.parseInt(Util.getConfigProperty("error.codes.internal.server.error")), "Could not construct DRM Config", null);
					}
				}
				catch(BitcodinException e)
				{
					// TODO: Define an error handler to avoid repetition
					log.error("An error occured while fetching DRM configuration", e);
					job.setStatus(Util.getConfigProperty("job.status.failed"));
					queueManager.moveQueues(workingListName, errorListName, jobString, job.toString());

					return;
				}
			}

			try
			{
				log.debug("About to call createJob");
				response = BitcodinProxy.createJob(inputConfig, outputConfig, job, drmConfigMap);
				log.debug("CreateBitcodinJob : run() -> Got back the response from Bitcodin");
				job.setStatus(Util.getConfigProperty("job.status.submitted"));

				if(hasSubs)
				{
					try
					{
						job.setOutputId(response.getLong("outputId"));
						log.debug("Job has subtitles and the outputid from response is: " + response.getLong("outputId"));
					}
					catch(JSONException e)
					{
						log.error("Unable to get outputId for processing subtitles", e);
						job.setStatus(Util.getConfigProperty("job.status.failed"));
						queueManager.moveQueues(workingListName, errorListName, jobString, job.toString());
					}
				}
			}
			catch(BitcodinException e)
			{
				log.error("Job creation failed", e);
				job.setStatus(Util.getConfigProperty("job.status.failed"));
				queueManager.moveQueues(workingListName, errorListName, jobString, job.toString());

				return;
			}

			log.info("Response string is: " + response.toString());

			//TODO: Error handling. Assumes Bitcodin will always return success response if response code is a non-error code

			try
			{
				job.setEncodingJobId(response.getInt("jobId"));
			}
			catch(JSONException e)
			{
				// This shouldn't happen either. Implies we got a 200 from
				// Bitcodin but no jobId
				log.error("An error occured while fetching jobId from the response", e);
				job.setStatus(Util.getConfigProperty("job.status.failed"));
				queueManager.moveQueues(workingListName, errorListName, jobString, job.toString());
				return;
			}

			try
			{
				job.setEncodeSummary(getEncodeSummary(job, response));
			}
			catch(JSONException | IOException e)
			{
				log.error("An error occured while creating the job summary", e);
				job.setStatus(Util.getConfigProperty("error.codes.internal.server.error"));
				queueManager.moveQueues(workingListName, errorListName, jobString, job.toString());
				return;
			}

			queueManager.moveQueues(workingListName, successListName, jobString, job.toString());
		}
		catch(QueueException e)
		{
			log.error("CreateBitcodinJob :: Queue Exception when trying to process job", e);
			return;
		}
	}
}
