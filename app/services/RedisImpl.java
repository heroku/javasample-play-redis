/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package services;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.pool.impl.GenericObjectPool.Config;

import com.google.inject.Inject;

import play.Logger;
import play.Play;
import play.mvc.Controller;
import play.mvc.Finally;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.exceptions.JedisConnectionException;

/**
 *
 * @author luciano
 */
// TODO: Play! module
public class RedisImpl extends Controller implements Redis {

	private static final Pattern REDIS_URL_PATTERN = Pattern.compile("^redis://([^:]*):([^@]*)@([^:]*):([^/]*)(/)?");
	private static final ThreadLocal<Jedis> TL_JEDIS = new ThreadLocal<Jedis>();
	
	static JedisPool jedisPool;

    public Jedis connect() {
    	// We'll maintain the same connection
    	// throughout the request
    	if (TL_JEDIS.get() != null) {
    		return TL_JEDIS.get();
    	}
    	
    	Jedis jedis = getPool().getResource();
    	TL_JEDIS.set(jedis);
        return jedis;
    }
    
    public void disconnect(Jedis jedis) {
    	if (jedisPool != null) {
    		jedisPool.returnResource(jedis);
    	}
    }
    
    JedisPool getPool() {
    	if (jedisPool == null) {
    		jedisPool = initPool();
    		
    		// Make sure the pool is destroyed during shutdown
    		// (as per https://github.com/xetorthio/jedis/wiki)
            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    jedisPool.destroy();
                }
            });
    	}
    	
    	return jedisPool;
    }
    
    JedisPool initPool() {
    	
    	String password;
    	String host;
    	int port;
    	
    	String redisUrl;
    	Matcher redisUrlMatcher;
    	if ((redisUrl = Play.configuration.getProperty("redis.url")) != null
    			&& (redisUrlMatcher = REDIS_URL_PATTERN.matcher(redisUrl)).matches()) {
    		
    		Logger.info("Connecting to redis using url: " + redisUrl);
    		
    		password = redisUrlMatcher.group(2);
    		host = redisUrlMatcher.group(3);
    		port = Integer.parseInt(redisUrlMatcher.group(4));
    		
    	} else if ((host = Play.configuration.getProperty("redis.host")) != null) {
    		
    		password = Play.configuration.getProperty("redis.password");
    		
    		if (Play.configuration.containsKey("redis.port")) {
    			port = Integer.parseInt(Play.configuration.getProperty("redis.port"));
    		} else {
    			port = Protocol.DEFAULT_PORT;
    		}        		
    		
    	} else {
    		throw new JedisConnectionException("No redis configuration found. You must specify redis.url or redis.host");
    	}
    	
    	int timeout;
    	if (Play.configuration.containsKey("redis.timeout")) {
    		timeout = Integer.parseInt(Play.configuration.getProperty("redis.timeout"));
    	} else {
    		timeout = Protocol.DEFAULT_TIMEOUT;
    	}
    	
    	// Ensure we get a healthy connection each time
    	Config config = new Config();
    	config.testOnBorrow = true;
    	
    	Logger.info("Creating jedis pool using: host -> " + host + ", port -> " + port + ", timeout -> " + timeout);
    	return new JedisPool(config, host, port, timeout, password);
    }
    
    // This is run at the end of each request to ensure that
    // the connection is returned to the pool.
    // (as per https://github.com/xetorthio/jedis/wiki)
	@Finally
	static void disconnectRemainingConnection() {
    	if (TL_JEDIS.get() != null) {
    		
    		if (jedisPool != null) {
    			jedisPool.returnResource(TL_JEDIS.get());
    		}
    		
    		TL_JEDIS.remove();
    	}
	}
}
