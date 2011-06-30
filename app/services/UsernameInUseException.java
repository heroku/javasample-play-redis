

package services;

/**
 *
 * @author luciano
 */
public class UsernameInUseException extends RuntimeException {
    
    public UsernameInUseException(String message) {
	super(message);
    }
}
