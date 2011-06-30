package services;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import models.Post;
import org.jredis.JRedis;
import org.jredis.RedisException;
/**
 *
 * @author luciano
 */
public class TwayisImpl implements Twayis{
    private static final String GLOBALTIMELINE = "global:timeline";
    @Inject
    private Redis redis;

    public List<Post> getUserPosts(String username, int maxPosts) {
        List<Post> ret = new ArrayList<Post>();
        try {
            final String key = username.equals(GLOBALTIMELINE)?GLOBALTIMELINE:"uid:" + getUserId(username) + ":posts";
            
            final List<byte[]> posts = redis.connect().lrange(key, 0, maxPosts);
            if (posts!=null) {
                for(byte[] p:posts) {
                    final String postid = new String(p);
                    byte[] postdata = redis.connect().get("post:"+postid);
                    if (postdata!=null) {
                        String[] postInfo = new String(postdata).split("\\|");
                        ret.add(new Post(Long.parseLong(postid),
                                postInfo[2],
                                new Date(Long.parseLong(postInfo[1])),
                                new String(redis.connect().get("uid:" + postInfo[0] + ":username"))) );
                    }

                }
            }
        } catch (RedisException ex) {
            //Logger.getLogger(Engine.class.getName()).log(Level.SEVERE, null, ex);
            ex.printStackTrace(); // TODO
        }
        return ret;
    }

    public void follow(String currentUser, String userToFollow) {
        JRedis jRedis = redis.connect();
        try {
            jRedis.sadd("uid:" + getUserId(userToFollow)+":followers", getUserId(currentUser));
            jRedis.sadd("uid:" + getUserId(currentUser)+":following", getUserId(userToFollow));
            System.out.println(currentUser + " is now following " + userToFollow);
        } catch (RedisException ex) {
            Logger.getLogger(TwayisImpl.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public void unfollow(String currentUser, String userToUnFollow) {
        JRedis jRedis = redis.connect();
        try {
            jRedis.srem("uid:" + getUserId(userToUnFollow)+":followers", getUserId(currentUser));
            jRedis.srem("uid:" + getUserId(currentUser)+":following", getUserId(userToUnFollow));
            System.out.println(currentUser + " is not following " + userToUnFollow + " anymore");
        } catch (RedisException ex) {
            Logger.getLogger(TwayisImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public long getFollowersCount(final String username) {
        try {
            return redis.connect().scard("uid:" + getUserId(username) + ":followers");
        } catch (RedisException ex) {
            ex.printStackTrace();
        }
        return 0;
    }

    public long getFollowingCount(final String username) {
        try {
            return redis.connect().scard("uid:"+getUserId(username)+":following");
        } catch (RedisException ex) {
            ex.printStackTrace();
        }
        return 0;
    }

    public List<Post> timeline(int maxTweets) {

        return getUserPosts(GLOBALTIMELINE, 50);

    }

    public String getUserId(String username) {
        byte[] userid = null;
        try {
            userid = redis.connect().get("username:" + username + ":id");
        } catch (RedisException ex) {
            Logger.getLogger(TwayisImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
        return userid!=null?new String(userid):null;
    }
    
    public boolean isFollowing(String username, String followingWho) {

        try {
            return redis.connect().sismember("uid:" + getUserId(username) + ":following", getUserId(followingWho));
        } catch (RedisException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public void register(String username, String pazz) {
        try {
            JRedis jredis = redis.connect();
            byte[] user = jredis.get("username:" + username + ":id");
            if (user != null) {
                throw new UsernameInUseException("Username " + username + " is already in use");
            } else {

                final long userid = jredis.incr("global:nextUserId");
                jredis.set("username:" + username + ":id", userid);
                jredis.set("uid:" + userid + ":username", username);
                jredis.set("uid:" + userid + ":password", pazz);
            }
        } catch (RedisException ex) {
            ex.printStackTrace();
        }
    }

    public long post(String username, String status) {
        Long postid = 0L;
        try {
            JRedis jredis = redis.connect();
            // increment global posts counter (sequence)
            postid = jredis.incr("global:nextPostId");
            String userid = getUserId(username);
            // Create string to post to redis (userid, timestamp, tweet)
            final String post = userid+"|"+System.currentTimeMillis()+"|"+status;
            // Add the post to redis
            jredis.set("post:"+postid, post);
            
            List<byte[]> followers = jredis.smembers("uid:"+userid + ":followers");
            if (followers==null || followers.isEmpty()) {
                followers = new ArrayList<byte[]>();
            }
            followers.add(userid.getBytes()); /* Add the post to our own posts too */
            for (byte[] b:followers) {
                jredis.lpush("uid:"+new String(b)+":posts", postid);
            }

            // Push the post on the timeline, and trim the timeline to the
            //newest 1000 elements.
            jredis.lpush("global:timeline", postid);
            jredis.ltrim("global:timeline", 0, 1000);


        } catch (RedisException ex) {
            
            ex.printStackTrace();
        }
        return postid;


    }

}
