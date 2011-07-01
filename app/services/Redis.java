/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package services;
import redis.clients.jedis.Jedis;
/**
 *
 * @author luciano
 */
public interface Redis {
    public Jedis connect();
}
