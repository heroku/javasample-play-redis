/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package services;

import org.jredis.ClientRuntimeException;
import org.jredis.JRedis;
import org.jredis.ProviderException;
import org.jredis.ri.alphazero.JRedisClient;

/**
 *
 * @author luciano
 */
public class RedisImpl implements Redis{

    static JRedis jredis;

    public JRedis connect() {
        if (jredis == null) {
            try {
                jredis = new JRedisClient("localhost", 6379);
                //jredis.ping();
            } catch (ProviderException e) {
                System.out.format("Oh no, an 'un-documented feature':  %s\nKindly report it.", e.getMessage());
            } catch (ClientRuntimeException e) {
                System.out.format("%s\n", e.getMessage());
            }
        }
        return this.jredis;
    }
}
