/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package services;
import org.jredis.JRedis;
/**
 *
 * @author luciano
 */
public interface Redis {
    public JRedis connect();
}
