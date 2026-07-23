package com.example.service;

import com.example.exception.BusinessException;
import com.example.exception.DataAccessException;
import com.example.exception.ExternalServiceException;
import com.example.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 订单服务 —— 最深调用链场景：
 * Controller → OrderService → ExternalApiService / CalculatorService / OrderRepository
 * 通过多层嵌套调用展现跨层根因追踪能力
 */
@Service
public class OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final ExternalApiService externalApiService;
    private final CalculatorService calculatorService;

    private final Map<String, Object> cache = new ConcurrentHashMap<>();

    public OrderService(OrderRepository orderRepository,
                        ExternalApiService externalApiService,
                        CalculatorService calculatorService) {
        this.orderRepository = orderRepository;
        this.externalApiService = externalApiService;
        this.calculatorService = calculatorService;
    }

    // ========================
    // 场景1: DAO层错误 → Service封装 → 抛出BusinessException
    // 调用链: Controller → OrderService.createOrder → OrderRepository.batchInsert → DataAccessException
    // ========================
    public Map<String, Object> createOrder(Map<String, Object> orderData) {
        logger.info("[OrderService] 创建订单: data={}", orderData);

        // 参数校验
        String orderId = (String) orderData.get("id");
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("订单ID不能为空");
        }

        try {
            // 调用 DAO 层 —— 如果ID重复会抛 DataAccessException
            orderRepository.batchInsert(List.of(orderData));
        } catch (DataAccessException e) {
            logger.error("[OrderService] 数据访问层异常，封装为业务异常", e);
            throw new BusinessException("ORDER_CREATE_FAILED",
                    "订单创建失败 (主键冲突): " + orderId, e);
        }

        logger.info("[OrderService] 订单创建成功: {}", orderId);
        return Map.of("orderId", orderId, "status", "CREATED");
    }

    // ========================
    // 场景2: DAO层返回null → Service层NPE
    // 调用链: Controller → OrderService.getCorruptedOrder → OrderRepository.findRawById → null → NPE
    // ========================
    @SuppressWarnings("unchecked")
    public Map<String, Object> getCorruptedOrder(String orderId) {
        logger.info("[OrderService] 查询订单: orderId={}", orderId);

        // DAO返回null(ORDER_003)，Service直接用导致NPE
        Map<String, Object> order = orderRepository.findRawById(orderId);
        // 这里故意不判空 —— 模拟开发者疏忽
        String status = (String) order.get("status"); // NPE here!
        String amount = String.valueOf(order.get("amount"));

        logger.info("[OrderService] 订单查询成功: status={}, amount={}", status, amount);
        return order;
    }

    // ========================
    // 场景3: 跨层调用链 —— 外部服务 + DAO 联合场景
    // 调用链: Controller → OrderService.processOrderWithRisk
    //         → ExternalApiService.checkRisk → ExternalServiceException (timeout)
    //         → wrapped in BusinessException
    // ========================
    public Map<String, Object> processOrderWithRisk(String orderId) {
        logger.info("[OrderService] 风控处理订单: orderId={}", orderId);

        Map<String, Object> order = orderRepository.findById(orderId);
        if (order == null) {
            throw new BusinessException("ORDER_NOT_FOUND", "订单不存在: " + orderId);
        }

        double amount = (Double) order.get("amount");

        try {
            // 调用外部风控 —— 会超时
            boolean passed = externalApiService.checkRisk(orderId, amount);
            logger.info("[OrderService] 风控结果: {}", passed);
        } catch (ExternalServiceException e) {
            logger.error("[OrderService] 外部风控调用失败，订单处理中断", e);
            throw new BusinessException("RISK_CHECK_FAILED",
                    "订单 " + orderId + " 风控检查失败，上游服务不可用", e);
        }

        return Map.of("orderId", orderId, "riskPassed", true);
    }

    // ========================
    // 场景4: DAO关联查询失败 —— ArrayIndexOutOfBounds 根源在 DAO
    // 调用链: Controller → OrderService.getOrderDetails → OrderRepository.findOrderWithDetails
    //         → ArrayIndexOutOfBoundsException → DataAccessException
    // ========================
    public Map<String, Object> getOrderDetails(String orderId) {
        logger.info("[OrderService] 查询订单详情: orderId={}", orderId);

        try {
            Map<String, Object> order = orderRepository.findOrderWithDetails(orderId);
            return order;
        } catch (DataAccessException e) {
            logger.error("[OrderService] DAO层关联查询异常", e);
            throw new BusinessException("ORDER_DETAIL_FAILED",
                    "查询订单 " + orderId + " 详情失败，数据库关联查询异常", e);
        }
    }

    // ========================
    // 场景5: 支付流程 —— 三层错误嵌套
    // 调用链: Controller → OrderService.payOrder
    //         → ExternalApiService.processPayment → ExternalServiceException(网络异常)
    //         → ExternalApiService.queryStock → ExternalServiceException(计算异常)
    //         → wrapped in BusinessException
    // ========================
    public Map<String, Object> payOrder(String orderId) {
        logger.info("[OrderService] 支付订单: orderId={}", orderId);

        // 查订单
        Map<String, Object> order = orderRepository.findById(orderId);
        if (order == null) {
            throw new BusinessException("ORDER_NOT_FOUND", "支付失败: 订单 " + orderId + " 不存在");
        }

        double amount = (Double) order.get("amount");

        try {
            // 先查库存
            int stock = externalApiService.queryStock("PROD_001");
            logger.info("[OrderService] 库存充足: {}", stock);

            // 再发起支付
            String payResult = externalApiService.processPayment(orderId, amount);
            logger.info("[OrderService] 支付结果: {}", payResult);
        } catch (ExternalServiceException e) {
            logger.error("[OrderService] 支付流程异常: {}", e.getServiceName(), e);
            throw new BusinessException("PAYMENT_FAILED",
                    "订单 " + orderId + " 支付失败: 外部服务 [" + e.getServiceName() + "] 异常", e);
        }

        return Map.of("orderId", orderId, "paymentStatus", "SUCCESS");
    }

    // ========================
    // 场景6: 深层嵌套计算错误
    // 调用链: Controller → OrderService.calculateDiscount
    //         → CalculatorService.complexFormula → ArithmeticException (step2 y=0)
    //         → step3 NaN → step4 NaN → ArithmeticException
    // ========================
    public Map<String, Object> calculateDiscount(String orderId, double basePrice, String discountMode) {
        logger.info("[OrderService] 折扣计算: orderId={}, basePrice={}, mode={}",
                orderId, basePrice, discountMode);

        try {
            // 多层计算: 参数故意设置为 y=0 触发深层异常
            double factor = calculatorService.complexFormula(basePrice, 0, 1.5, discountMode);
            double finalPrice = basePrice * factor;
            logger.info("[OrderService] 折扣后价格: {}", finalPrice);
            return Map.of("orderId", orderId, "finalPrice", finalPrice);
        } catch (ArithmeticException e) {
            logger.error("[OrderService] 折扣计算异常", e);
            throw new BusinessException("DISCOUNT_CALC_ERROR",
                    "订单 " + orderId + " 折扣计算失败: 公式运算异常", e);
        }
    }

    // ========================
    // 场景7: 批量订单处理 —— 缓存 + 并发问题
    // 调用链: Controller → OrderService.batchProcess
    //         → 缓存操作 → ConcurrentModificationException
    // ========================
    @SuppressWarnings("unchecked")
    public Map<String, Object> batchProcess(List<String> orderIds) {
        logger.info("[OrderService] 批量处理订单: count={}", orderIds.size());

        List<Map<String, Object>> results = new ArrayList<>();
        for (String id : orderIds) {
            // 先查缓存
            if (cache.containsKey(id)) {
                logger.info("[OrderService] 缓存命中: {}", id);
                results.add((Map<String, Object>) cache.get(id));
                continue;
            }

            Map<String, Object> order = orderRepository.findById(id);
            if (order == null) {
                logger.error("[OrderService] 批量处理中订单不存在: orderId={}", id);
                throw new BusinessException("BATCH_PROCESS_FAILED",
                        "批量处理失败: 订单 " + id + " 不存在，已处理 " + results.size() + " 条", null);
            }

            // 写入缓存
            cache.put(id, order);
            results.add(order);
        }

        logger.info("[OrderService] 批量处理完成: {} 条", results.size());
        return Map.of("processedCount", results.size(), "orders", results);
    }

    // ========================
    // 场景8: 数据损坏 —— ORDER_003的amount为null
    // 调用链: Controller → OrderService.getOrderAmount → OrderRepository.queryOrderTotal → DataAccessException
    // ========================
    public Map<String, Object> getOrderAmount(String orderId) {
        logger.info("[OrderService] 查询订单金额: orderId={}", orderId);

        try {
            double amount = orderRepository.queryOrderTotal(orderId);
            if (amount < 0) {
                logger.warn("[OrderService] 订单金额异常: orderId={}, amount={}", orderId, amount);
                throw new BusinessException("INVALID_AMOUNT",
                        "订单 " + orderId + " 金额数据异常: " + amount);
            }
            return Map.of("orderId", orderId, "amount", amount);
        } catch (DataAccessException e) {
            logger.error("[OrderService] 数据完整性异常", e);
            throw new BusinessException("DATA_CORRUPTED",
                    "订单 " + orderId + " 数据损坏: 必填字段为空", e);
        }
    }

    // ========================
    // 场景9: DAO层全部订单查询失败 → DB连接失败
    // 调用链: Controller → OrderService.getAllOrders
    //         → OrderRepository.findAllOrderIds → SQLException → DataAccessException
    //         → wrapped in BusinessException
    // ========================
    public Map<String, Object> getAllOrders() {
        logger.info("[OrderService] 查询全部订单列表");

        try {
            List<String> orderIds = orderRepository.findAllOrderIds();
            return Map.of("count", orderIds.size(), "orderIds", orderIds);
        } catch (DataAccessException e) {
            logger.error("[OrderService] 查询订单列表失败", e);
            throw new BusinessException("DB_CONNECTION_FAILED",
                    "系统暂时不可用: 数据库连接失败，请稍后重试", e);
        }
    }

    // ========================
    // 场景10: 状态机校验 —— IllegalStateException
    // ========================
    @SuppressWarnings("unchecked")
    public Map<String, Object> shipOrder(String orderId) {
        logger.info("[OrderService] 发货订单: orderId={}", orderId);

        Map<String, Object> order = orderRepository.findById(orderId);
        if (order == null) {
            throw new BusinessException("ORDER_NOT_FOUND", "订单不存在: " + orderId);
        }

        String currentStatus = (String) order.get("status");
        logger.info("[OrderService] 当前订单状态: {}", currentStatus);

        // ORDER_001: PAID → 可以发货
        // ORDER_002: SHIPPED → 不能重复发货
        if ("SHIPPED".equals(currentStatus)) {
            logger.error("[OrderService] 状态校验失败: 订单已发货, orderId={}", orderId);
            throw new IllegalStateException(
                    "订单 " + orderId + " 当前状态为 [" + currentStatus + "]，不允许执行发货操作。" +
                            "允许发货的状态: [PAID]");
        }

        if (!"PAID".equals(currentStatus)) {
            throw new BusinessException("ORDER_STATUS_INVALID",
                    "订单 " + orderId + " 状态 [" + currentStatus + "] 不允许发货");
        }

        orderRepository.updateOrderStatus(orderId, "SHIPPED");
        logger.info("[OrderService] 发货成功: orderId={}", orderId);
        return Map.of("orderId", orderId, "status", "SHIPPED");
    }
}
