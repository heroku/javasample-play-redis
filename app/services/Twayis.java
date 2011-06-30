package services;

import java.util.List;
import models.Post;

/**
 * Service exposing basic Twitter-like mehods.
 * @author luciano
 */
public interface Twayis {
    List<Post> getUserPosts(String username,int maxPosts);
    void follow(String currentUser, String userToFollow);
    void unfollow(String currentUser, String userToUnFollow);
    long getFollowersCount(String username);
    long getFollowingCount(String username);
    boolean isFollowing(String username, String followingWho);
    String getUserId(String username);
    List<Post> timeline(int maxTweets);
    void register(String username, String pazz);
    long post(String username, String status);
    
}
