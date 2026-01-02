package com.expensetracker.app.exception;

/**
 * Custom exception to indicate a validation failure, such as invalid
 * data input or a violation of business rules (e.g., total owed != total amount).
 *
 * It is an unchecked exception (extends RuntimeException) so it doesn't
 * have to be explicitly declared in method signatures.
 */
public class ValidationException extends RuntimeException {

    /**
     * Constructs a new ValidationException with the specified detail message.
     * @param message the detail message (which is saved for later retrieval by the getMessage() method).
     */
    public ValidationException(String message) {
        super(message);
    }

    /**
     * Constructs a new ValidationException with the specified detail message and cause.
     * @param message the detail message.
     * @param cause the cause (which is saved for later retrieval by the getCause() method).
     */
    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}