package services;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import models.Post;
import play.Logger;
import redis.clients.jedis.Jedis;
import services.exception.RegistrationPasswordException;
import services.exception.RegistrationUsernameException;
import services.exception.UsernameInUseException;

import com.google.inject.Inject;
/**
 *
 * @author luciano
 */
public class TwayisImpl implements Twayis {
	
	private static final Pattern PATTERN_WHITESPACE = Pattern.compile("\\s");
	private static final int MIN_USERNAME_CHARS = 5;
	
    private static final String GLOBALTIMELINE = "global:timeline";
    @Inject
    private Redis redis;

    public List<Post> getUserPosts(String username, int maxPosts) {
        List<Post> ret = new ArrayList<Post>();
        final String key = username.equals(GLOBALTIMELINE)?GLOBALTIMELINE:"uid:" + getUserId(username) + ":posts";
            
        final List<String> postids = redis.connect().lrange(key, 0, maxPosts);
        if (postids!=null) {
        	for(String postid : postids) {
        		String postdata = redis.connect().get("post:"+postid);
        		if (postdata!=null) {
        			String[] postInfo = postdata.split("\\|");
        			ret.add(new Post(Long.parseLong(postid),
        					postInfo[2],
        					new Date(Long.parseLong(postInfo[1])),
        					new String(redis.connect().get("uid:" + postInfo[0] + ":username"))) );
                }

            }
        }
        return ret;
    }

    public void follow(String currentUser, String userToFollow) {
        Jedis jedis = redis.connect();
        jedis.sadd("uid:" + getUserId(userToFollow)+":followers", getUserId(currentUser));
        jedis.sadd("uid:" + getUserId(currentUser)+":following", getUserId(userToFollow));
        Logger.info(currentUser + " is now following " + userToFollow);
    }

    public void unfollow(String currentUser, String userToUnFollow) {
        Jedis jedis = redis.connect();
        jedis.srem("uid:" + getUserId(userToUnFollow)+":followers", getUserId(currentUser));
        jedis.srem("uid:" + getUserId(currentUser)+":following", getUserId(userToUnFollow));
        Logger.info(currentUser + " is not following " + userToUnFollow + " anymore");
    }

    public long getFollowersCount(final String username) {
    	return redis.connect().scard("uid:" + getUserId(username) + ":followers");
    }

    public long getFollowingCount(final String username) {
    	return redis.connect().scard("uid:"+getUserId(username)+":following");
    }

    public List<Post> timeline(int maxTweets) {

        return getUserPosts(GLOBALTIMELINE, 50);

    }

    public String getUserId(String username) {
    	return redis.connect().get("username:" + username + ":id");
    }
    
    public boolean isFollowing(String username, String followingWho) {
    	return redis.connect().sismember("uid:" + getUserId(username) + ":following", getUserId(followingWho));
    }

    public void checkUsername(String username) {
    	Matcher whitespaceMatcher = PATTERN_WHITESPACE.matcher(username);
    	if (whitespaceMatcher.find()) {
    		throw new RegistrationUsernameException("username cannot contain whitespace");
    	} else if (username.length() < MIN_USERNAME_CHARS) {
    		throw new RegistrationUsernameException("username must be at least " + MIN_USERNAME_CHARS + " characters long");
    	}
    	
    	Jedis jedis = redis.connect();
    	String user = jedis.get("username:" + username + ":id");
    	if (user != null) {
    		throw new UsernameInUseException("username '" + username + "' is already in use");    		
    	}
    }
    
    public void checkPassword(String password, String password2) {
    	if (password == null || password.isEmpty()) {
    		throw new RegistrationPasswordException("password cannot be empty");
    	} else if (!password.equals(password2)) {
    		throw new RegistrationPasswordException("passwords do not match");
    	}
    }
    
    public void register(String username, String pazz) {
    	Jedis jedis = redis.connect();
    	
    	final long userid = jedis.incr("global:nextUserId");
    	jedis.set("username:" + username + ":id", Long.toString(userid));
    	jedis.set("uid:" + userid + ":username", username);
    	jedis.set("uid:" + userid + ":password", pazz);
    }

    public long post(String username, String status) {
        Long postid = 0L;

        Jedis jedis = redis.connect();
        // increment global posts counter (sequence)
        postid = jedis.incr("global:nextPostId");
        String userid = getUserId(username);
        
        // Create string to post to redis (userid, timestamp, tweet)
        final String post = userid+"|"+System.currentTimeMillis()+"|"+status;
        // Add the post to redis
        jedis.set("post:"+postid, post);
        
        Set<String> followers = jedis.smembers("uid:"+userid + ":followers");
        if (followers==null || followers.isEmpty()) {
        	followers = new HashSet<String>();
        }
        followers.add(userid); /* Add the post to our own posts too */
        for (String follower:followers) {
        	jedis.lpush("uid:"+follower+":posts", Long.toString(postid));
        }

        // Push the post on the timeline, and trim the timeline to the
        //newest 1000 elements.
        jedis.lpush("global:timeline", Long.toString(postid));
        jedis.ltrim("global:timeline", 0, 1000);

        return postid;
    }

}
