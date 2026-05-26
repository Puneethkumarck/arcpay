package com.arcpay.identity.agentidentity.application.controller;

import com.arcpay.identity.agentidentity.api.ErrorCodes;
import com.arcpay.identity.agentidentity.api.model.ApiError;
import com.arcpay.identity.agentidentity.domain.exception.AgentNameDuplicateException;
import com.arcpay.identity.agentidentity.domain.exception.AgentNotFoundException;
import com.arcpay.identity.agentidentity.domain.exception.AgentNotInExpectedStateException;
import com.arcpay.identity.agentidentity.domain.exception.BlockchainRegistrationException;
import com.arcpay.identity.agentidentity.domain.exception.ForbiddenException;
import com.arcpay.identity.agentidentity.domain.exception.InvalidAgentNameException;
import com.arcpay.identity.agentidentity.domain.exception.InvalidEmailException;
import com.arcpay.identity.agentidentity.domain.exception.InvalidPolicyHashException;
import com.arcpay.identity.agentidentity.domain.exception.InvalidPurposeException;
import com.arcpay.identity.agentidentity.domain.exception.InvalidWalletAddressException;
import com.arcpay.identity.agentidentity.domain.exception.MissingIdempotencyKeyException;
import com.arcpay.identity.agentidentity.domain.exception.OwnerEmailAlreadyExistsException;
import com.arcpay.identity.agentidentity.domain.exception.OwnerWalletAlreadyExistsException;
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

    @ExceptionHandler(AgentNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(AgentNotFoundException ex) {
        return toError(ex, ErrorCodes.NOT_FOUND, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiError> handleForbidden(ForbiddenException ex) {
        return toError(ex, ErrorCodes.FORBIDDEN, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler({
            OwnerEmailAlreadyExistsException.class,
            OwnerWalletAlreadyExistsException.class,
            AgentNameDuplicateException.class,
            AgentNotInExpectedStateException.class
    })
    public ResponseEntity<ApiError> handleConflict(RuntimeException ex) {
        return toError(ex, ErrorCodes.CONFLICT, HttpStatus.CONFLICT);
    }

    @ExceptionHandler({
            InvalidEmailException.class,
            InvalidWalletAddressException.class,
            InvalidAgentNameException.class,
            InvalidPolicyHashException.class,
            InvalidPurposeException.class
    })
    public ResponseEntity<ApiError> handleBadRequest(RuntimeException ex) {
        return toError(ex, ErrorCodes.BAD_REQUEST, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MissingIdempotencyKeyException.class)
    public ResponseEntity<ApiError> handleMissingIdempotencyKey(MissingIdempotencyKeyException ex) {
        return toError(ex, ErrorCodes.IDEMPOTENCY, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        var errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.groupingBy(
                        fe -> fe.getField(),
                        Collectors.mapping(fe -> fe.getDefaultMessage(), Collectors.toList())));
        return toErrorWithDetail("Validation failed", ErrorCodes.BAD_REQUEST, HttpStatus.BAD_REQUEST, errors);
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
        return toErrorWithDetail("Validation failed", ErrorCodes.BAD_REQUEST, HttpStatus.BAD_REQUEST, errors);
    }

    @ExceptionHandler(BlockchainRegistrationException.class)
    public ResponseEntity<ApiError> handleExternalService(BlockchainRegistrationException ex) {
        log.error("External service failure: {}", ex.getMessage(), ex);
        return toError("External service error", ErrorCodes.EXTERNAL_SERVICE_ERROR, HttpStatus.BAD_GATEWAY);
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
