package com.movideo.nextgen.encoder.tasks;

import redis.clients.jedis.JedisPool;

/**
 * Super class for all Tasks. Can implement Callable instead of Runnable, but we
 * don't have the need to return anything or throw checked exceptions. Might be
 * handy to implement Callable when converting this to a library.
 * 
 * @author yramasundaram
 *
 */
public class Task implements Runnable {

    protected JedisPool redisPool;
    protected String jobString;

    public Task(JedisPool pool, String jobString) {
	this.redisPool = pool;
	this.jobString = jobString;
    }

    @Override
    public void run() {
    }

}
