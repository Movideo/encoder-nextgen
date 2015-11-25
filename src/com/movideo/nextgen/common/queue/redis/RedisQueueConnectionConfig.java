package com.movideo.nextgen.common.queue.redis;

import com.movideo.nextgen.common.queue.QueueConnectionConfig;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class RedisQueueConnectionConfig extends QueueConnectionConfig {
    private static JedisPool pool;
    
    public RedisQueueConnectionConfig(String connectionString){
	if(pool == null){
	    pool = new JedisPool(new JedisPoolConfig(), connectionString);
	}
    }
    
    public RedisQueueConnectionConfig(){}
    
    public void setPool(JedisPool jedisPool){
	pool = jedisPool;
    }
    
    public JedisPool getPool(){
	return pool;
    }
}
