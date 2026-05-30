package com.arcpay.settlement.application.controller;

import com.arcpay.settlement.api.ErrorCodes;
import com.arcpay.settlement.application.webhook.CircleNotificationException;
import com.arcpay.settlement.domain.InsufficientBalanceException;
import com.arcpay.settlement.domain.TransferNotFoundException;
import com.arcpay.settlement.domain.WebhookSignatureException;
import com.arcpay.platform.api.ApiError;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(WebhookSignatureException.class)
    public ResponseEntity<ApiError> handleInvalidSignature(WebhookSignatureException ex) {
        log.warn("Rejected Circle webhook: {}", ex.getMessage());
        return toError(ex.getMessage(), ErrorCodes.INVALID_WEBHOOK_SIGNATURE, UNAUTHORIZED);
    }

    @ExceptionHandler(CircleNotificationException.class)
    public ResponseEntity<ApiError> handleInvalidNotification(CircleNotificationException ex) {
        return toError(ex.getMessage(), ErrorCodes.INVALID_REQUEST, BAD_REQUEST);
    }

    @ExceptionHandler(TransferNotFoundException.class)
    public ResponseEntity<ApiError> handleTransferNotFound(TransferNotFoundException ex) {
        return toError(ex.getMessage(), ErrorCodes.TRANSFER_NOT_FOUND, NOT_FOUND);
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<ApiError> handleInsufficientBalance(InsufficientBalanceException ex) {
        return toError(ex.getMessage(), ErrorCodes.INSUFFICIENT_BALANCE, UNPROCESSABLE_ENTITY);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableBody(HttpMessageNotReadableException ex) {
        return toError("Malformed request body", ErrorCodes.INVALID_REQUEST, BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return toError("Malformed request parameter", ErrorCodes.INVALID_REQUEST, BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        var errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.groupingBy(
                        fe -> fe.getField(),
                        Collectors.mapping(fe -> fe.getDefaultMessage(), Collectors.toList())));
        return toErrorWithDetail("Validation failed", ErrorCodes.INVALID_REQUEST,
                UNPROCESSABLE_ENTITY, errors);
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
        return toErrorWithDetail("Validation failed", ErrorCodes.INVALID_REQUEST,
                UNPROCESSABLE_ENTITY, errors);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return toError(INTERNAL_SERVER_ERROR.getReasonPhrase(),
                ErrorCodes.INTERNAL_ERROR, INTERNAL_SERVER_ERROR);
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
