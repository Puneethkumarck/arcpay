package com.arcpay.compliance.application.controller;

import com.arcpay.compliance.api.ErrorCodes;
import com.arcpay.compliance.domain.exception.HoldAlreadyDecidedException;
import com.arcpay.compliance.domain.exception.HoldNotFoundException;
import com.arcpay.compliance.domain.exception.IdentityServiceUnavailableException;
import com.arcpay.compliance.domain.exception.MalformedAddressException;
import com.arcpay.compliance.domain.exception.ReviewReasonInvalidException;
import com.arcpay.compliance.domain.exception.ScreeningNotFoundException;
import com.arcpay.compliance.domain.exception.UnauthorizedException;
import com.arcpay.platform.api.ApiError;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ScreeningNotFoundException.class)
    public ResponseEntity<ApiError> handleScreeningNotFound(ScreeningNotFoundException ex) {
        return toError(ex, ErrorCodes.SCREENING_NOT_FOUND, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(HoldNotFoundException.class)
    public ResponseEntity<ApiError> handleHoldNotFound(HoldNotFoundException ex) {
        return toError(ex, ErrorCodes.HOLD_NOT_FOUND, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler({UnauthorizedException.class, AccessDeniedException.class})
    public ResponseEntity<ApiError> handleUnauthorized(RuntimeException ex) {
        return toError(ex, ErrorCodes.NOT_AUTHORIZED, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(HoldAlreadyDecidedException.class)
    public ResponseEntity<ApiError> handleHoldAlreadyDecided(HoldAlreadyDecidedException ex) {
        return toError(ex, ErrorCodes.HOLD_ALREADY_DECIDED, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(IdentityServiceUnavailableException.class)
    public ResponseEntity<ApiError> handleIdentityUnavailable(IdentityServiceUnavailableException ex) {
        log.error("Identity service unavailable: {}", ex.getMessage(), ex);
        return toError(ex, ErrorCodes.IDENTITY_UNAVAILABLE, HttpStatus.SERVICE_UNAVAILABLE);
    }

    @ExceptionHandler(MalformedAddressException.class)
    public ResponseEntity<ApiError> handleMalformedAddress(MalformedAddressException ex) {
        return toError(ex, ErrorCodes.MALFORMED_ADDRESS, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @ExceptionHandler(ReviewReasonInvalidException.class)
    public ResponseEntity<ApiError> handleReviewReasonInvalid(ReviewReasonInvalidException ex) {
        return toError(ex, ErrorCodes.REVIEW_REASON_INVALID, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableBody(HttpMessageNotReadableException ex) {
        return toError("Malformed request body", ErrorCodes.MALFORMED_REQUEST,
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return toError("Malformed request parameter", ErrorCodes.MALFORMED_REQUEST,
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        var errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.groupingBy(
                        fe -> fe.getField(),
                        Collectors.mapping(fe -> fe.getDefaultMessage(), Collectors.toList())));
        return toErrorWithDetail("Validation failed", ErrorCodes.MALFORMED_ADDRESS,
                HttpStatus.UNPROCESSABLE_ENTITY, errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex) {
        var errors = ex.getConstraintViolations().stream()
                .collect(Collectors.groupingBy(
                        cv -> {
                            var path = cv.getPropertyPath().toString();
                            var dot = path.lastIndexOf('.');
                            return dot >= 0 ? path.substring(dot + 1) : path;
                        },
                        Collectors.mapping(cv -> cv.getMessage(), Collectors.toList())));
        return toErrorWithDetail("Validation failed", ErrorCodes.MALFORMED_ADDRESS,
                HttpStatus.UNPROCESSABLE_ENTITY, errors);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return toError(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                ErrorCodes.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<ApiError> toError(Exception ex, String code, HttpStatus status) {
        return toError(ex.getMessage(), code, status);
    }

    private ResponseEntity<ApiError> toError(String message, String code, HttpStatus status) {
        var error = ApiError.builder()
                .code(code)
                .status(status.getReasonPhrase())
                .message(message)
                .build();
        return ResponseEntity.status(status).body(error);
    }

    private ResponseEntity<ApiError> toErrorWithDetail(String message, String code, HttpStatus status,
                                                       Map<String, List<String>> errors) {
        var detail = ApiError.Detail.builder().errors(errors).build();
        var error = ApiError.builder()
                .code(code)
                .status(status.getReasonPhrase())
                .message(message)
                .details(detail)
                .build();
        return ResponseEntity.status(status).body(error);
    }
}
