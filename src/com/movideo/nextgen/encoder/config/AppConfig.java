package com.movideo.nextgen.encoder.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.log4j.Logger;

public class AppConfig {

    private static final Logger log = Logger.getLogger(AppConfig.class);

    private int clientId;
    private int corePoolSize;
    private int maxPoolSize;
    private long keepAliveTime;
    private String redisConnectionString;
    private String encodedOutputPrefix;
    private String encodedOutputStorageType;

    private int sampleJobMediaId;
    private int sampleJobencProfileId;
    private String sampleJobSpeed;
    private String sampleJobInputFile;
    private String sampleJobStatus;
    private int sampleJobDefOutputId;
    private int paralleljobCountforTest;

    public int getClientId() {
	return clientId;
    }

    public int getCorePoolSize() {
	return corePoolSize;
    }

    public int getMaxPoolSize() {
	return maxPoolSize;
    }

    public long getKeepAliveTime() {
	return keepAliveTime;
    }

    public String getRedisConnectionString() {
	return redisConnectionString;
    }

    public String getEncodedOutputPrefix() {
	return encodedOutputPrefix;
    }

    public String getEncodedOutputStorageType() {
	return encodedOutputStorageType;
    }

    public int getSampleJobMediaId() {
	return sampleJobMediaId;
    }

    public int getSampleJobencProfileId() {
	return sampleJobencProfileId;
    }

    public String getSampleJobSpeed() {
	return sampleJobSpeed;
    }

    public String getSampleJobInputFile() {
	return sampleJobInputFile;
    }

    public String getSampleJobStatus() {
	return sampleJobStatus;
    }

    public int getSampleJobDefOutputId() {
	return sampleJobDefOutputId;
    }

    public int getParalleljobCountforTest() {
	return paralleljobCountforTest;
    }

    public AppConfig(String configFile) {

	InputStream inStream = null;

	try {
	    Properties prop = new Properties();
	    inStream = new FileInputStream(configFile);
	    prop.load(inStream);

	    clientId = Integer.parseInt(prop.getProperty("app.clientid"));
	    corePoolSize = Integer.parseInt(prop.getProperty("threadpool.corePoolSize"));
	    maxPoolSize = Integer.parseInt(prop.getProperty("threadpool.maxPoolSize"));
	    keepAliveTime = Long.parseLong(prop.getProperty("threadpool.keepAliveTime"));

	    redisConnectionString = prop.getProperty("redis.connectionString");
	    encodedOutputPrefix = prop.getProperty("encoded.output.prefix");
	    encodedOutputStorageType = prop.getProperty("encoded.output.storage.type");

	    sampleJobMediaId = Integer.parseInt(prop.getProperty("sample.test.job.mediaId"));
	    sampleJobencProfileId = Integer.parseInt(prop.getProperty("sample.test.job.encProfileId"));
	    sampleJobSpeed = prop.getProperty("sample.test.job.speed");
	    ;
	    sampleJobInputFile = prop.getProperty("sample.test.job.inputFileName");
	    sampleJobStatus = prop.getProperty("sample.test.job.status");
	    sampleJobDefOutputId = Integer.parseInt(prop.getProperty("sample.test.job.default.output.id"));
	    paralleljobCountforTest = Integer.parseInt(prop.getProperty("test.app.num.paralleljobs"));

	} catch (IOException e) {
	    log.error("Could not find the configuration file " + configFile, e);
	} finally {

	    if (inStream != null) {
		try {
		    inStream.close();
		} catch (IOException e) {
		    log.error("An error occured while closing the input stream " + configFile, e);
		}
	    }

	}

    }

}
