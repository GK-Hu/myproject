package com.example.controller;

import com.example.service.CalculatorService;
import com.example.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 根因分析测试控制器 —— 覆盖各类异常场景
 *
 * 错误分类：
 *   NPE           - 空指针
 *   IAE           - 参数校验失败
 *   ARE           - 算术异常
 *   AIOOB         - 数组越界
 *   NFE           - 数字格式异常
 *   ISE           - 非法状态
 *   BE            - 业务异常 (带 errorCode)
 *   DBE           - 数据库异常 (跨层)
 *   EXT           - 外部服务异常 (跨层)
 *   CHAINED       - 多层异常嵌套
 */
@RestController
@RequestMapping("/api/root-cause")
public class RootCauseTestController {

    private static final Logger logger = LoggerFactory.getLogger(RootCauseTestController.class);

    private final OrderService orderService;
    private final CalculatorService calculatorService;

    public RootCauseTestController(OrderService orderService, CalculatorService calculatorService) {
        this.orderService = orderService;
        this.calculatorService = calculatorService;
    }

    // ===========================================
    // 场景1: NPE —— 空指针 (跨3层)
    // Controller → OrderService.getCorruptedOrder → OrderRepository.findRawById → null → NPE
    // ===========================================
    @GetMapping("/null-pointer")
    public Map<String, Object> testNullPointer() {
        logger.info("[RootCauseTest] ===== 测试场景1: 深层NPE =====");
        return orderService.getCorruptedOrder("ORDER_003"); // ORDER_003数据为null
    }

    // ===========================================
    // 场景2: DB主键冲突 (跨2层)
    // Controller → OrderService.createOrder → OrderRepository.batchInsert → DataAccessException
    //   → BusinessException("ORDER_CREATE_FAILED")
    // ===========================================
    @GetMapping("/db-duplicate-key")
    public Map<String, Object> testDbDuplicateKey() {
        logger.info("[RootCauseTest] ===== 测试场景2: DB主键冲突 =====");
        return orderService.createOrder(Map.of(
                "id", "ORDER_001",  // 已存在
                "amount", 999.0,
                "status", "NEW"
        ));
    }

    // ===========================================
    // 场景3: 外部服务超时 (跨3层)
    // Controller → OrderService.processOrderWithRisk → ExternalApiService.checkRisk → ExternalServiceException(timeout)
    //   → BusinessException("RISK_CHECK_FAILED")
    // ===========================================
    @GetMapping("/external-timeout")
    public Map<String, Object> testExternalTimeout() {
        logger.info("[RootCauseTest] ===== 测试场景3: 外部服务超时 =====");
        return orderService.processOrderWithRisk("ORDER_001");
    }

    // ===========================================
    // 场景4: DAO关联查询越界 (跨2层)
    // Controller → OrderService.getOrderDetails → OrderRepository.findOrderWithDetails
    //   → ArrayIndexOutOfBoundsException → DataAccessException → BusinessException
    // ===========================================
    @GetMapping("/db-join-error")
    public Map<String, Object> testDbJoinError() {
        logger.info("[RootCauseTest] ===== 测试场景4: DB关联查询异常 =====");
        return orderService.getOrderDetails("ORDER_001");
    }

    // ===========================================
    // 场景5: 支付流程多层嵌套错误 (跨3层)
    // Controller → OrderService.payOrder
    //   → ExternalApiService.queryStock → ArithmeticException → ExternalServiceException
    //   → BusinessException("PAYMENT_FAILED")
    // ===========================================
    @GetMapping("/payment-chain-error")
    public Map<String, Object> testPaymentChainError() {
        logger.info("[RootCauseTest] ===== 测试场景5: 支付链多层错误 =====");
        return orderService.payOrder("ORDER_001");
    }

    // ===========================================
    // 场景6: 深层计算错误 (跨3层, 5个方法调用)
    // Controller → OrderService.calculateDiscount → CalculatorService.complexFormula
    //   → step2Transform(y=0) → ArithmeticException → BusinessException("DISCOUNT_CALC_ERROR")
    // ===========================================
    @GetMapping("/deep-calc-error")
    public Map<String, Object> testDeepCalcError() {
        logger.info("[RootCauseTest] ===== 测试场景6: 深层公式计算错误 =====");
        return orderService.calculateDiscount("ORDER_001", 100.0, "scale");
    }

    // ===========================================
    // 场景7: 批量操作部分失败 (跨2层)
    // Controller → OrderService.batchProcess → OrderRepository.findById → null → BusinessException
    // ===========================================
    @GetMapping("/batch-partial-fail")
    public Map<String, Object> testBatchPartialFail() {
        logger.info("[RootCauseTest] ===== 测试场景7: 批量操作部分失败 =====");
        return orderService.batchProcess(List.of("ORDER_001", "ORDER_999", "ORDER_002"));
    }

    // ===========================================
    // 场景8: 非法状态 (单层 → 跨层)
    // Controller → OrderService.shipOrder("ORDER_002") → IllegalStateException(已发货)
    // 对比: Controller → OrderService.shipOrder("ORDER_001") → 正常
    // ===========================================
    @GetMapping("/illegal-state")
    public Map<String, Object> testIllegalState(
            @RequestParam(defaultValue = "ORDER_002") String orderId) {
        logger.info("[RootCauseTest] ===== 测试场景8: 非法状态校验 =====");
        return orderService.shipOrder(orderId);
    }

    // ===========================================
    // 场景9: DB连接失败 (跨2层)
    // Controller → OrderService.getAllOrders → OrderRepository.findAllOrderIds
    //   → SQLException → DataAccessException → BusinessException("DB_CONNECTION_FAILED")
    // ===========================================
    @GetMapping("/db-connection-fail")
    public Map<String, Object> testDbConnectionFail() {
        logger.info("[RootCauseTest] ===== 测试场景9: DB连接失败 =====");
        return orderService.getAllOrders();
    }

    // ===========================================
    // 场景10: 数据损坏 (跨2层)
    // Controller → OrderService.getOrderAmount → OrderRepository.queryOrderTotal → null字段 → DataAccessException
    //   → BusinessException("DATA_CORRUPTED")
    // ===========================================
    @GetMapping("/data-corrupted")
    public Map<String, Object> testDataCorrupted() {
        logger.info("[RootCauseTest] ===== 测试场景10: 数据损坏 =====");
        return orderService.getOrderAmount("ORDER_003");
    }

    // ===========================================
    // 场景11: NumberFormatException (单层计算)
    // Controller → CalculatorService.parseIntSafely → NumberFormatException
    // ===========================================
    @GetMapping("/number-format")
    public Map<String, Object> testNumberFormat() {
        logger.info("[RootCauseTest] ===== 测试场景11: 数字格式错误 =====");
        try {
            int val = calculatorService.parseIntSafely("abc123");
            return Map.of("result", val);
        } catch (NumberFormatException e) {
            throw new RuntimeException("解析数字失败", e);
        }
    }

    // ===========================================
    // 场景12: 复杂公式未知模式
    // Controller → CalculatorService.complexFormula → step3ApplyMode → IAE(未知模式)
    // ===========================================
    @GetMapping("/unknown-mode")
    public Map<String, Object> testUnknownMode() {
        logger.info("[RootCauseTest] ===== 测试场景12: 未知计算模式 =====");
        try {
            double result = calculatorService.complexFormula(10, 5, 2, "magic");
            return Map.of("result", result);
        } catch (Exception e) {
            throw new RuntimeException("公式计算失败: " + e.getMessage(), e);
        }
    }

    // ===========================================
    // 场景13: 参数范围校验
    // Controller → CalculatorService.weightedScore → IAE(超出范围)
    // ===========================================
    @GetMapping("/param-out-of-range")
    public Map<String, Object> testParamOutOfRange() {
        logger.info("[RootCauseTest] ===== 测试场景13: 参数范围超限 =====");
        double result = calculatorService.weightedScore(150, 0.5);
        return Map.of("result", result);
    }

    // ===========================================
    // 场景14: 数组越界 (带上下文)
    // ===========================================
    @GetMapping("/array-index")
    public Map<String, Object> testArrayIndex() {
        logger.info("[RootCauseTest] ===== 测试场景14: 数组越界 =====");
        int[] data = {10, 20, 30, 40, 50};
        // 用 "index=10" 越界
        return Map.of("value", data[10]);
    }

    // ===========================================
    // 场景15: 级联错误 —— 多层Wrap后的深层根因
    // Controller (wrap1) → Service (wrap2) → Repository (root cause = SQLException)
    // ===========================================
    @GetMapping("/cascading-error")
    public Map<String, Object> testCascadingError() {
        logger.info("[RootCauseTest] ===== 测试场景15: 级联异常包裹 =====");
        return orderService.calculateDiscount("ORDER_001", 0.0, "reciprocal");
    }
}
