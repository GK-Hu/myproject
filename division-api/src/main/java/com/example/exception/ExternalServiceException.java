package com.example.exception;

import java.time.Duration;

/**
 * 外部服务异常 —— 模拟外部 API 调用失败/超时
 */
public class ExternalServiceException extends RuntimeException {

    private final String serviceName;
    private final Duration timeout;

    public ExternalServiceException(String serviceName, Duration timeout) {
        super("调用外部服务 [" + serviceName + "] 超时，耗时 " + timeout.toMillis() + "ms");
        this.serviceName = serviceName;
        this.timeout = timeout;
    }

    public ExternalServiceException(String serviceName, String message, Throwable cause) {
        super("外部服务 [" + serviceName + "] 异常: " + message, cause);
        this.serviceName = serviceName;
        this.timeout = null;
    }

    public String getServiceName() {
        return serviceName;
    }

    public Duration getTimeout() {
        return timeout;
    }
}
