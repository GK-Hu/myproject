package com.example.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 参数校验异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        logger.warn("[ExceptionHandler] 参数校验失败: {}", ex.getMessage());
        return ResponseEntity
                .badRequest()
                .body(Map.of(
                        "error", "Invalid input",
                        "type", "IllegalArgumentException",
                        "message", ex.getMessage()
                ));
    }

    /**
     * 业务异常 —— 带 errorCode，支持根因链追踪
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessException(BusinessException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "Business error");
        body.put("type", "BusinessException");
        body.put("errorCode", ex.getErrorCode());
        body.put("message", ex.getMessage());

        // 递归解包根因链
        Throwable root = getRootCause(ex);
        if (root != ex) {
            body.put("rootCause", root.getClass().getSimpleName() + ": " + root.getMessage());
            logger.error("[ExceptionHandler] 业务异常 [{}], 根因: {}",
                    ex.getErrorCode(), root.getClass().getSimpleName(), ex);
        } else {
            logger.error("[ExceptionHandler] 业务异常 [{}]: {}", ex.getErrorCode(), ex.getMessage());
        }

        // 添加完整异常链
        body.put("exceptionChain", buildExceptionChain(ex));

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(body);
    }

    /**
     * 数据访问异常
     */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, Object>> handleDataAccessException(DataAccessException ex) {
        Throwable root = getRootCause(ex);
        logger.error("[ExceptionHandler] 数据库异常, 根因: {}", root.getClass().getSimpleName(), ex);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "Database error");
        body.put("type", "DataAccessException");
        body.put("message", ex.getMessage());
        if (root != ex) {
            body.put("rootCause", root.getClass().getSimpleName() + ": " + root.getMessage());
        }
        body.put("exceptionChain", buildExceptionChain(ex));

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(body);
    }

    /**
     * 外部服务异常
     */
    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<Map<String, Object>> handleExternalServiceException(ExternalServiceException ex) {
        logger.error("[ExceptionHandler] 外部服务异常 [{}]: {}", ex.getServiceName(), ex.getMessage(), ex);

        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(Map.of(
                        "error", "External service error",
                        "type", "ExternalServiceException",
                        "serviceName", ex.getServiceName(),
                        "message", ex.getMessage(),
                        "exceptionChain", buildExceptionChain(ex)
                ));
    }

    /**
     * 算术异常
     */
    @ExceptionHandler(ArithmeticException.class)
    public ResponseEntity<Map<String, Object>> handleArithmeticException(ArithmeticException ex) {
        logger.error("[ExceptionHandler] 算术异常: {}", ex.getMessage(), ex);
        return ResponseEntity
                .badRequest()
                .body(Map.of(
                        "error", "Arithmetic error",
                        "type", "ArithmeticException",
                        "message", ex.getMessage(),
                        "exceptionChain", buildExceptionChain(ex)
                ));
    }

    /**
     * 数字格式异常
     */
    @ExceptionHandler(NumberFormatException.class)
    public ResponseEntity<Map<String, Object>> handleNumberFormatException(NumberFormatException ex) {
        logger.error("[ExceptionHandler] 数字格式异常: {}", ex.getMessage(), ex);
        return ResponseEntity
                .badRequest()
                .body(Map.of(
                        "error", "Number format error",
                        "type", "NumberFormatException",
                        "message", ex.getMessage()
                ));
    }

    /**
     * 非法状态异常
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalStateException(IllegalStateException ex) {
        logger.warn("[ExceptionHandler] 状态异常: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(Map.of(
                        "error", "State conflict",
                        "type", "IllegalStateException",
                        "message", ex.getMessage()
                ));
    }

    /**
     * 数组越界异常
     */
    @ExceptionHandler(IndexOutOfBoundsException.class)
    public ResponseEntity<Map<String, Object>> handleIndexOutOfBounds(IndexOutOfBoundsException ex) {
        logger.error("[ExceptionHandler] 数组越界: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "error", "Index out of bounds",
                        "type", "IndexOutOfBoundsException",
                        "message", ex.getMessage(),
                        "exceptionChain", buildExceptionChain(ex)
                ));
    }

    /**
     * 运行异常兜底
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        Throwable root = getRootCause(ex);
        logger.error("[ExceptionHandler] 运行时异常, 根因: {} - {}", root.getClass().getSimpleName(), root.getMessage(), ex);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "Internal server error");
        body.put("type", ex.getClass().getSimpleName());
        body.put("message", ex.getMessage());
        if (root != ex && !root.getClass().equals(ex.getClass())) {
            body.put("rootCause", root.getClass().getSimpleName() + ": " + root.getMessage());
        }
        body.put("exceptionChain", buildExceptionChain(ex));

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(body);
    }

    /**
     * 所有未捕获异常兜底
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAll(Exception ex) {
        logger.error("[ExceptionHandler] 未捕获异常: {}", ex.getClass().getName(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "error", "Unexpected error",
                        "type", ex.getClass().getSimpleName(),
                        "message", ex.getMessage()
                ));
    }

    // ========================
    // 工具方法
    // ========================

    /**
     * 递归查找根因
     */
    private Throwable getRootCause(Throwable t) {
        Throwable cause = t.getCause();
        if (cause == null || cause == t) {
            return t;
        }
        return getRootCause(cause);
    }

    /**
     * 构建异常链信息
     */
    private String buildExceptionChain(Throwable t) {
        StringBuilder sb = new StringBuilder();
        Throwable current = t;
        int level = 0;
        while (current != null) {
            if (level > 0) sb.append(" → ");
            sb.append(current.getClass().getSimpleName())
                    .append("(").append(current.getMessage()).append(")");
            current = current.getCause();
            level++;
            if (level > 10) break; // 防止循环
        }
        return sb.toString();
    }
}
