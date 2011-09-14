package controllers;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import play.modules.redis.Redis;
import play.mvc.Controller;

public class Application extends Controller {

    public static void index() {
        render();
    }
    
    public static void persist(String key, String value) {
    	Redis.set(key, value);
    	index();
    }
    
    public static void clear() {
    	Redis.flushAll();
    	index();
    }
    
    public static void contents() {
    	Set<String> keys = Redis.keys("*");
    	
    	Map<String, Object> redisContents = new TreeMap<String, Object>();
    	for (String key : keys) {
    		redisContents.put(key, Redis.get(key));
    	}
    	
    	renderJSON(redisContents);
    }
}