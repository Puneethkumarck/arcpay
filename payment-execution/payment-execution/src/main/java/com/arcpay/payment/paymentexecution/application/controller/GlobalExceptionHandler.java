package com.arcpay.payment.paymentexecution.application.controller;

import com.arcpay.payment.paymentexecution.api.ErrorCodes;
import com.arcpay.payment.paymentexecution.domain.exception.AgentNotActiveException;
import com.arcpay.payment.paymentexecution.domain.exception.AgentNotFoundException;
import com.arcpay.payment.paymentexecution.domain.exception.AgentNotOwnedException;
import com.arcpay.payment.paymentexecution.domain.exception.IdempotencyConflictException;
import com.arcpay.payment.paymentexecution.domain.exception.IdentityServiceUnavailableException;
import com.arcpay.payment.paymentexecution.domain.exception.InvalidPaymentRequestException;
import com.arcpay.payment.paymentexecution.domain.exception.PaymentAccessDeniedException;
import com.arcpay.payment.paymentexecution.domain.exception.PaymentNotFoundException;
import com.arcpay.platform.api.ApiError;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PaymentNotFoundException.class)
    public ResponseEntity<ApiError> handlePaymentNotFound(PaymentNotFoundException ex) {
        return toError(ex, ErrorCodes.PAYMENT_NOT_FOUND, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(PaymentAccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(PaymentAccessDeniedException ex) {
        return toError(ex, ErrorCodes.PAYMENT_ACCESS_DENIED, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(InvalidPaymentRequestException.class)
    public ResponseEntity<ApiError> handleInvalidRequest(InvalidPaymentRequestException ex) {
        return toError(ex, ErrorCodes.INVALID_PAYMENT_REQUEST, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @ExceptionHandler({AgentNotFoundException.class, AgentNotOwnedException.class})
    public ResponseEntity<ApiError> handleAgentNotFound(RuntimeException ex) {
        return toError(ex, ErrorCodes.AGENT_NOT_FOUND, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @ExceptionHandler(AgentNotActiveException.class)
    public ResponseEntity<ApiError> handleAgentNotActive(AgentNotActiveException ex) {
        return toError(ex, ErrorCodes.AGENT_NOT_ACTIVE, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<ApiError> handleIdempotencyConflict(IdempotencyConflictException ex) {
        return toError(ex, ErrorCodes.IDEMPOTENCY_CONFLICT, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @ExceptionHandler(IdentityServiceUnavailableException.class)
    public ResponseEntity<ApiError> handleIdentityUnavailable(IdentityServiceUnavailableException ex) {
        log.error("Identity Service unavailable: {}", ex.getMessage(), ex);
        return toError(ex, ErrorCodes.IDENTITY_UNAVAILABLE, HttpStatus.SERVICE_UNAVAILABLE);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        var errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.groupingBy(
                        fe -> fe.getField(),
                        Collectors.mapping(fe -> fe.getDefaultMessage(), Collectors.toList())));
        return toErrorWithDetail("Validation failed", ErrorCodes.INVALID_PAYMENT_REQUEST,
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
        return toErrorWithDetail("Validation failed", ErrorCodes.INVALID_PAYMENT_REQUEST,
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
