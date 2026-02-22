package com.vaultpay.common.exception;

import com.vaultpay.common.dto.ApiResponse;
import com.vaultpay.common.logging.MdcKeys;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex, HttpServletRequest request) {
        log.warn("Business exception: {} (code={})", ex.getMessage(), ex.getErrorCode());
        return ResponseEntity
                .status(ex.getStatus())
                .body(ApiResponse.<Void>builder()
                        .success(false)
                        .message(ex.getMessage())
                        .errorCode(ex.getErrorCode().name())
                        .path(request.getRequestURI())
                        .requestId(MDC.get(MdcKeys.REQUEST_ID))
                        .build());
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleUsernameNotFoundException(
            UsernameNotFoundException ex, HttpServletRequest request) {
        log.warn("Authentication failed: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.<Void>builder()
                        .success(false)
                        .message("Invalid credentials")
                        .errorCode(ErrorCode.UNAUTHORIZED.name())
                        .path(request.getRequestURI())
                        .requestId(MDC.get(MdcKeys.REQUEST_ID))
                        .build());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentialsException(
            BadCredentialsException ex, HttpServletRequest request) {
        log.warn("Bad credentials: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.<Void>builder()
                        .success(false)
                        .message("Invalid credentials")
                        .errorCode(ErrorCode.UNAUTHORIZED.name())
                        .path(request.getRequestURI())
                        .requestId(MDC.get(MdcKeys.REQUEST_ID))
                        .build());
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiResponse<Void>> handleDisabledException(
            DisabledException ex, HttpServletRequest request) {
        log.warn("Login attempt on disabled account: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.<Void>builder()
                        .success(false)
                        .message("Account is disabled. Please contact support.")
                        .errorCode(ErrorCode.UNAUTHORIZED.name())
                        .path(request.getRequestURI())
                        .requestId(MDC.get(MdcKeys.REQUEST_ID))
                        .build());
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ApiResponse<Void>> handleLockedException(
            LockedException ex, HttpServletRequest request) {
        log.warn("Login attempt on locked account: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.<Void>builder()
                        .success(false)
                        .message("Account is suspended. Please contact support.")
                        .errorCode(ErrorCode.ACCOUNT_LOCKED.name())
                        .path(request.getRequestURI())
                        .requestId(MDC.get(MdcKeys.REQUEST_ID))
                        .build());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });
        ApiResponse<Map<String, String>> response = ApiResponse.<Map<String, String>>builder()
                .success(false)
                .message("Validation failed")
                .errorCode(ErrorCode.VALIDATION_ERROR.name())
                .data(fieldErrors)
                .details(Map.of("fields", fieldErrors))
                .path(request.getRequestURI())
                .requestId(MDC.get(MdcKeys.REQUEST_ID))
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error: ", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<Void>builder()
                        .success(false)
                        .message("An unexpected error occurred")
                        .errorCode(ErrorCode.INTERNAL_ERROR.name())
                        .path(request.getRequestURI())
                        .requestId(MDC.get(MdcKeys.REQUEST_ID))
                        .build());
    }
}
