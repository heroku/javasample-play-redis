/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package controllers;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import org.jredis.JRedis;
import org.jredis.RedisException;
import services.Redis;


/**
 *
 * @author luciano
 */
public class Security extends Secure.Security {
    
    @Inject
    static Redis redis;

    static boolean authentify(String username, String password) {
        
        JRedis jredis = redis.connect();
        try {
            byte[] userid = jredis.get("username:" + username + ":id");
            if (userid!=null) {
                byte[] pw = jredis.get("uid:"+new String(userid)+":password");
                if (new String(pw).equals(password)) return true;
            }
        
        } catch (RedisException ex) {
            ex.printStackTrace();
        }
        return false;


    }



}
