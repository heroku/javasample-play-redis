
package controllers.twayis;

import play.mvc.Controller;
import java.util.List;
import javax.inject.Inject;
import models.Post;
import services.Twayis;

/**
 *
 * @author luciano
 */
public class Application extends Controller {

    @Inject
    static Twayis twayis;
    
    public static void index() {
        if (session.contains("username")) {
            // we shouldnt really do that
            // I don't like calling Engine.home() because it makes the url horrible
            // Best thing would be to simply have the default login page to change to Application/index.html
            // but I don't know how to do that...
            
            //List<Post> posts = twayis.getUserPosts(session.get("username"));
            //render("twayis/Engine/home.html", posts);
            Engine.home();
        } else {
            render();
        }
    }

    public static void register(String username, String password, String password2) {
        try {
            // TODO check that passwords match..

            twayis.register(username, password);
            // login the user..
            //controllers.Secure.auth(username, password);

        } catch (Throwable e) {
            // TODO ... handle exception
            e.printStackTrace();
        }
        index();
    }

    public static void profile(String username) {
        
        if (username != null) {
            final List<Post> posts = twayis.getUserPosts(username,10);
            boolean isFollowing = twayis.isFollowing(session.get("username"), username);
            boolean displayFollow = !username.equals(session.get("username"));
            render(posts, isFollowing, username, displayFollow);

        } else {
            // show some message maybe...
            index();
        }

        
        
    }

    public static void timeline() {
        final List<Post> posts = twayis.timeline(50);
        render(posts);
    }
}
