package com.omar.postify.exception;

import com.omar.postify.dto.ApiErrorResponse;
import com.omar.postify.dto.ApiErrorResponse.ValidationError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.time.LocalDateTime;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String PROBLEM_BASE = "/problems/";

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(ConflictException ex) {
        return buildErrorResponse(
                problemType("conflict"),
                HttpStatus.CONFLICT,
                "Conflict",
                ex.getMessage()
        );
    }
    @ExceptionHandler(UnauthorizedActionException.class)
    public ResponseEntity<ApiErrorResponse> handleUnauthorizedAction(UnauthorizedActionException ex) {
        return buildErrorResponse(
                problemType("unauthorized"),
                HttpStatus.UNAUTHORIZED,
                "Unauthorized",
                ex.getMessage()
        );
    }
    @ExceptionHandler(InvalidUserDataException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidUserData(InvalidUserDataException ex) {
        return buildErrorResponse(
                problemType("bad-request"),
                HttpStatus.BAD_REQUEST,
                "Bad Request",
                ex.getMessage()
        );
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleResourceNotFound(ResourceNotFoundException ex) {
        return buildErrorResponse(
                problemType("not-found"),
                HttpStatus.NOT_FOUND,
                "Not Found",
                ex.getMessage()
        );
    }
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiErrorResponse> handleBadRequest(BadRequestException ex) {
        return buildErrorResponse(
                problemType("bad-request"),
                HttpStatus.BAD_REQUEST,
                "Bad Request",
                ex.getMessage()
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return buildErrorResponse(
                problemType("bad-request"),
                HttpStatus.BAD_REQUEST,
                "Bad Request",
                ex.getMessage()
        );
    }
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthentication(AuthenticationException ex) {
        return buildErrorResponse(
                problemType("unauthorized"),
                HttpStatus.UNAUTHORIZED,
                "Not authenticated",
                ex.getMessage()
        );
    }

    @ExceptionHandler(RateLimitException.class)
    public ResponseEntity<ApiErrorResponse> handleRateLimit(RateLimitException ex) {
        return buildErrorResponse(
                problemType("too-many-requests"),
                HttpStatus.TOO_MANY_REQUESTS,
                "Too Many Requests",
                ex.getMessage()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<ValidationError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> new ValidationError(fieldError.getField(), fieldError.getDefaultMessage()))
                .toList();

        ApiErrorResponse body = new ApiErrorResponse(
                problemType("validation-error"),
                "Validation Failed",
                HttpStatus.BAD_REQUEST.value(),
                "One or more fields are invalid",
                null,
                LocalDateTime.now(),
                errors
        );

        return ResponseEntity.badRequest().body(body);
    }
    @ExceptionHandler(BadCredentialsCustomException.class)
    public ResponseEntity<ApiErrorResponse> handleBadCredentials(BadCredentialsCustomException ex) {
        return buildErrorResponse(
                problemType("unauthorized"),
                HttpStatus.UNAUTHORIZED,
                "Invalid username or password",
                ex.getMessage()
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleNotReadable(HttpMessageNotReadableException ex) {
        return buildErrorResponse(
                problemType("malformed-json"),
                HttpStatus.BAD_REQUEST,
                "Bad Request",
                "The request body is invalid or malformed."
        );
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleResponseStatusException(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        String title = status.getReasonPhrase();
        String detail = ex.getReason() != null ? ex.getReason() : status.getReasonPhrase();

        return buildErrorResponse(
                problemType(status.name().toLowerCase().replace('_', '-')),
                status,
                title,
                detail
        );
    }

    @ExceptionHandler(JwtAuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleJwtAuthentication(JwtAuthenticationException ex) {
        return buildErrorResponse(
                problemType("unauthorized"),
                HttpStatus.UNAUTHORIZED,
                "Invalid or expired token",
                ex.getMessage()
        );
    }

    @ExceptionHandler(InsufficientAuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleInsufficientAuthentication(InsufficientAuthenticationException ex) {
        return buildErrorResponse(
                problemType("unauthorized"),
                HttpStatus.UNAUTHORIZED,
                "Full authentication is required to access this resource",
                ex.getMessage()
        );
    }

    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleRequestMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return buildErrorResponse(
                problemType("method-not-allowed"),
                HttpStatus.METHOD_NOT_ALLOWED,
                "Method Not Allowed",
                "The requested method is not allowed for this endpoint."
        );
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiErrorResponse> handleForbidden(ForbiddenException ex) {
        return buildErrorResponse(
                problemType("forbidden"),
                HttpStatus.FORBIDDEN,
                "Forbidden. The user does not have permission to access this resource.",
                ex.getMessage()
        );
    }

    private ResponseEntity<ApiErrorResponse> buildErrorResponse(
            String type,
            HttpStatus status,
            String title,
            String detail
    ) {

        ApiErrorResponse body = new ApiErrorResponse(
                type,
                title,
                status.value(),
                detail,
                null,
                LocalDateTime.now(),
                null
        );

        return ResponseEntity.status(status).body(body);
    }

    private String problemType(String slug) {
        return PROBLEM_BASE + slug;
    }
}
