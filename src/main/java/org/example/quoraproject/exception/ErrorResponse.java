package org.example.quoraproject.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/**
 * Consistent error body:
 * {
 *   "timestamp": "...", "status": 404, "error": "Not Found",
 *   "message": "...", "path": "...", "errors": [...]
 * }
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<String> errors
) {

    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(Instant.now(), status, error, message, path, null);
    }

    public static ErrorResponse of(int status, String error, String message, String path, List<String> errors) {
        return new ErrorResponse(Instant.now(), status, error, message, path, errors);
    }
}
