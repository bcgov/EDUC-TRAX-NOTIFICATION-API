package ca.bc.gov.educ.api.trax.exception;

/**
 * Exception thrown when there's an error with the TRAX-NOTIFICATION-API
 */
public class NotificationApiException extends RuntimeException {
    
    public NotificationApiException(String message) {
        super(message);
    }
    
    public NotificationApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
