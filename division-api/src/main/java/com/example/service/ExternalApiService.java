package com.example.service;

import com.example.exception.DataAccessException;
import com.example.exception.ExternalServiceException;
import com.example.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;

/**
 * 模拟外部服务调用层
 */
@Service
public class ExternalApiService {

    private static final Logger logger = LoggerFactory.getLogger(ExternalApiService.class);

    /**
     * 场景：调用外部风控系统超时
     */
    public boolean checkRisk(String orderId, double amount) {
        logger.info("[ExternalAPI] 调用风控系统检查: orderId={}, amount={}", orderId, amount);

        // 模拟调用超时
        Duration timeout = Duration.ofSeconds(5);
        logger.error("[ExternalAPI] 风控系统 {} 响应超时 ({})", "risk-check-service", timeout);
        throw new ExternalServiceException("risk-check-service", timeout);
    }

    /**
     * 场景：外部库存系统返回异常数据
     */
    public int queryStock(String productId) {
        logger.info("[ExternalAPI] 查询库存: productId={}", productId);

        try {
            // 模拟外部返回异常数据导致处理出错
            int result = 100 / 0; // ArithmeticException
            return result;
        } catch (ArithmeticException e) {
            logger.error("[ExternalAPI] 库存查询计算异常", e);
            throw new ExternalServiceException("inventory-service",
                    "库存计算错误: division by zero while computing available stock", e);
        }
    }

    /**
     * 场景：外部支付网关返回错误
     */
    public String processPayment(String orderId, double amount) {
        logger.info("[ExternalAPI] 发起支付: orderId={}, amount={}", orderId, amount);

        // 模拟下游系统网络不可达
        RuntimeException networkError = new RuntimeException(
                "java.net.ConnectException: Connection refused (Connection refused)");
        throw new ExternalServiceException("payment-gateway",
                "支付网关不可达: POST https://pay.internal.com/v1/charge timeout after 10000ms",
                networkError);
    }
}
