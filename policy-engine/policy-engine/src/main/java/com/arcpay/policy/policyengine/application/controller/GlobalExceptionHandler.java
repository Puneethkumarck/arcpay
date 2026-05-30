package com.arcpay.policy.policyengine.application.controller;

import com.arcpay.platform.api.ApiError;
import com.arcpay.policy.policyengine.api.ErrorCodes;
import com.arcpay.policy.policyengine.domain.exception.AgentNotActiveException;
import com.arcpay.policy.policyengine.domain.exception.AgentNotFoundException;
import com.arcpay.policy.policyengine.domain.exception.AgentOwnershipException;
import com.arcpay.policy.policyengine.domain.exception.IdentityServiceUnavailableException;
import com.arcpay.policy.policyengine.domain.exception.IllegalReservationStateException;
import com.arcpay.policy.policyengine.domain.exception.InvalidPolicyException;
import com.arcpay.policy.policyengine.domain.exception.PolicyHashMismatchException;
import com.arcpay.policy.policyengine.domain.exception.PolicyNotFoundException;
import com.arcpay.policy.policyengine.domain.exception.PolicyViolationException;
import com.arcpay.policy.policyengine.domain.exception.ReservationNotFoundException;
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

    @ExceptionHandler({PolicyNotFoundException.class, AgentNotFoundException.class})
    public ResponseEntity<ApiError> handleNotFound(RuntimeException ex) {
        var code = ex instanceof AgentNotFoundException
                ? ErrorCodes.AGENT_NOT_FOUND
                : ErrorCodes.POLICY_NOT_FOUND;
        return toError(ex, code, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(InvalidPolicyException.class)
    public ResponseEntity<ApiError> handleInvalidPolicy(InvalidPolicyException ex) {
        return toError(ex, ErrorCodes.INVALID_POLICY, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(PolicyViolationException.class)
    public ResponseEntity<ApiError> handlePolicyViolation(PolicyViolationException ex) {
        return toError(ex, ErrorCodes.POLICY_VIOLATION, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @ExceptionHandler(AgentNotActiveException.class)
    public ResponseEntity<ApiError> handleAgentNotActive(AgentNotActiveException ex) {
        return toError(ex, ErrorCodes.AGENT_NOT_ACTIVE, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @ExceptionHandler(AgentOwnershipException.class)
    public ResponseEntity<ApiError> handleAgentOwnership(AgentOwnershipException ex) {
        return toError(ex, ErrorCodes.AGENT_OWNERSHIP, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(PolicyHashMismatchException.class)
    public ResponseEntity<ApiError> handlePolicyHashMismatch(PolicyHashMismatchException ex) {
        return toError(ex, ErrorCodes.POLICY_HASH_MISMATCH, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(ReservationNotFoundException.class)
    public ResponseEntity<ApiError> handleReservationNotFound(ReservationNotFoundException ex) {
        return toError(ex, ErrorCodes.RESERVATION_NOT_FOUND, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(IllegalReservationStateException.class)
    public ResponseEntity<ApiError> handleIllegalReservationState(IllegalReservationStateException ex) {
        return toError(ex, ErrorCodes.ILLEGAL_RESERVATION_STATE, HttpStatus.CONFLICT);
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
        return toErrorWithDetail("Validation failed", ErrorCodes.INVALID_POLICY, HttpStatus.BAD_REQUEST, errors);
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
        return toErrorWithDetail("Validation failed", ErrorCodes.INVALID_POLICY, HttpStatus.BAD_REQUEST, errors);
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
