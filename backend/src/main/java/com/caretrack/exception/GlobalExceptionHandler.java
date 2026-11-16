package com.caretrack.exception;

import com.caretrack.dto.ApiErrorResponse;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Turns exceptions into the consistent {@link ApiErrorResponse} JSON shape.
 *
 * Clients only ever see a short, safe message. Unexpected errors are logged on the
 * server with their stack trace and returned to the client as a generic 500.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                             HttpServletRequest request) {
        List<ApiErrorResponse.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ApiErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage()))
                .distinct()
                .sorted(Comparator.comparing(ApiErrorResponse.FieldError::field)
                        .thenComparing(ApiErrorResponse.FieldError::message))
                .toList();
        return build(HttpStatus.BAD_REQUEST, "Validation failed.", request, fieldErrors);
    }

    /** Malformed JSON, or a value that cannot be converted (e.g. an unknown category). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadable(HttpMessageNotReadableException ex,
                                                             HttpServletRequest request) {
        if (ex.getCause() instanceof InvalidFormatException ife
                && ife.getTargetType() != null
                && ife.getTargetType().isEnum()) {
            String field = ife.getPath().stream()
                    .map(JsonMappingException.Reference::getFieldName)
                    .filter(name -> name != null)
                    .collect(Collectors.joining("."));
            String allowed = Arrays.stream(ife.getTargetType().getEnumConstants())
                    .map(Object::toString)
                    .collect(Collectors.joining(", "));
            String message = "Invalid value. Allowed values: " + allowed + ".";
            return build(HttpStatus.BAD_REQUEST, "Validation failed.", request,
                    List.of(new ApiErrorResponse.FieldError(field.isEmpty() ? "body" : field, message)));
        }
        return build(HttpStatus.BAD_REQUEST, "Request body is missing or is not valid JSON.", request, List.of());
    }

    /** A query parameter of the wrong type, e.g. {@code ?status=DONE}. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                               HttpServletRequest request) {
        String message = "Invalid value for parameter '" + ex.getName() + "'.";
        Class<?> type = ex.getRequiredType();
        if (type != null && type.isEnum()) {
            message += " Allowed values: " + Arrays.stream(type.getEnumConstants())
                    .map(Object::toString)
                    .collect(Collectors.joining(", ")) + ".";
        }
        return build(HttpStatus.BAD_REQUEST, message, request, List.of());
    }

    @ExceptionHandler(RequestNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(RequestNotFoundException ex,
                                                           HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(InvalidStatusTransitionException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidTransition(InvalidStatusTransitionException ex,
                                                                    HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiErrorResponse> handleConcurrentUpdate(ObjectOptimisticLockingFailureException ex,
                                                                   HttpServletRequest request) {
        return build(HttpStatus.CONFLICT,
                "This request was changed by someone else at the same time. Refresh and try again.",
                request, List.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNoResource(NoResourceFoundException ex,
                                                             HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "Endpoint not found.", request, List.of());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex,
                                                                   HttpServletRequest request) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, "HTTP method not supported for this endpoint.",
                request, List.of());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMediaType(HttpMediaTypeNotSupportedException ex,
                                                            HttpServletRequest request) {
        return build(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Content-Type must be application/json.",
                request, List.of());
    }

    /** Last resort. Log the details server-side; tell the client nothing internal. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unhandled error on {} {}", request.getMethod(), request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong. Please try again.",
                request, List.of());
    }

    private static ResponseEntity<ApiErrorResponse> build(HttpStatus status, String message,
                                                          HttpServletRequest request,
                                                          List<ApiErrorResponse.FieldError> fieldErrors) {
        ApiErrorResponse body = new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI(),
                fieldErrors);
        return ResponseEntity.status(status).body(body);
    }
}
