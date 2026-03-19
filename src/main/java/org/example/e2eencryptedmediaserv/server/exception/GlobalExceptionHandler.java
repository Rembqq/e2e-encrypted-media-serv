package org.example.e2eencryptedmediaserv.server.exception;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 401 — AuthenticationException (неверный или просроченный токен)
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthenticationException(AuthenticationException ex) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Authentication failed", ex.getMessage());
    }

    // 403 — AccessDeniedException (нет прав, попытка доступа к чужим данным)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDeniedException(AccessDeniedException ex) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, "Access denied", ex.getMessage());
    }

    // 400 — Валидация (@Valid) не прошла
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", Instant.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Validation failed");
        body.put("details", errors);

        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    // 400 — IllegalArgumentException (наши кастомные проверки)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Invalid request", ex.getMessage());
    }

    // 404 — EntityNotFoundException
    @ExceptionHandler(jakarta.persistence.EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleEntityNotFoundException(jakarta.persistence.EntityNotFoundException ex) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, "Resource not found", ex.getMessage());
    }

    // 500 — Все остальные непредвиденные ошибки
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error",
                "An unexpected error occurred. Please try again later.");
    }

    // Вспомогательный метод для формирования ответа
    private ResponseEntity<Map<String, Object>> buildErrorResponse(HttpStatus status, String error, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", Instant.now());
        body.put("status", status.value());
        body.put("error", error);
        body.put("message", message);

        return new ResponseEntity<>(body, status);
    }
}
