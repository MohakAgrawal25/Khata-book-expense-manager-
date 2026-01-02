package com.expensetracker.app.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Custom exception to indicate that a resource (like a User or Group) 
 * was not found in the system.
 * Spring automatically maps this exception to an HTTP 404 Not Found status.
 */
@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    
    /**
     * Constructs a ResourceNotFoundException with a detailed error message.
     * * @param resourceName The name of the resource (e.g., "User", "Group")
     * @param fieldName The field used for the search (e.g., "username", "id")
     * @param fieldValue The value of the field that was not found
     */
    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s : '%s'", resourceName, fieldName, fieldValue));
    }
    // Optional: Useful constructor if you want to include the cause
    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
