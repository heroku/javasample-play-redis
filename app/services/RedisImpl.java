/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package services;

import java.security.ProviderException;

import redis.clients.jedis.Jedis;

/**
 *
 * @author luciano
 */
public class RedisImpl implements Redis{

	static Jedis jedis;
//    static JRedis jredis;

    public Jedis connect() {
        if (jedis == null) {
        	jedis = new Jedis("localhost", 6379);
        	jedis.ping();
        }
        
        return this.jedis;
    }
}
