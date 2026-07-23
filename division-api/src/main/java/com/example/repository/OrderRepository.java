package com.example.repository;

import com.example.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 模拟数据访问层 —— 覆盖各类数据库错误场景
 */
@Repository
public class OrderRepository {

    private static final Logger logger = LoggerFactory.getLogger(OrderRepository.class);

    private final Map<String, Object> fakeDb = new HashMap<>();

    public OrderRepository() {
        // 初始化一些假数据
        fakeDb.put("ORDER_001", Map.of("id", "ORDER_001", "amount", 100.0, "status", "PAID"));
        fakeDb.put("ORDER_002", Map.of("id", "ORDER_002", "amount", 250.0, "status", "SHIPPED"));
        fakeDb.put("ORDER_003", null); // 模拟数据损坏（查询返回 null）
    }

    /**
     * 场景1：查询不存在的数据 —— 返回 null → Service 层 NPE
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> findById(String orderId) {
        logger.info("[DAO] 查询订单: orderId={}", orderId);
        Object data = fakeDb.get(orderId);
        if (data == null) {
            logger.warn("[DAO] 订单不存在: orderId={}", orderId);
        }
        return (Map<String, Object>) data;
    }

    /**
     * 场景2：数据库连接失败 —— 模拟 SQLException
     */
    public List<String> findAllOrderIds() {
        logger.info("[DAO] 查询所有订单ID...");
        try {
            // 模拟 SQL 执行异常
            throw new SQLException("Connection refused: connect to mysql://10.0.1.99:3306/orders " +
                    "failed after 30000ms. Cause: Communications link failure");
        } catch (SQLException e) {
            logger.error("[DAO] 数据库连接失败", e);
            throw new DataAccessException("查询订单列表失败，数据库连接异常", e);
        }
    }

    /**
     * 场景3：并发修改冲突
     */
    public void updateOrderStatus(String orderId, String newStatus) {
        logger.info("[DAO] 更新订单状态: orderId={}, newStatus={}", orderId, newStatus);
        Object existing = fakeDb.get(orderId);

        // 模拟版本冲突
        if (existing == null) {
            throw new DataAccessException("更新失败: 订单 " + orderId + " 版本冲突，" +
                    "当前数据已被其他事务修改 (optimistic lock)");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> order = (Map<String, Object>) existing;
        logger.debug("[DAO] 更新前状态: {} → 更新后: {}", order.get("status"), newStatus);
    }

    /**
     * 场景4：批量操作部分失败
     */
    public void batchInsert(List<Map<String, Object>> orders) {
        logger.info("[DAO] 批量插入 {} 条订单", orders.size());
        for (int i = 0; i < orders.size(); i++) {
            Map<String, Object> order = orders.get(i);
            String id = (String) order.get("id");
            if (fakeDb.containsKey(id)) {
                logger.error("[DAO] 批量插入第 {}/{} 条失败: 主键冲突 id={}", i + 1, orders.size(), id);
                throw new DataAccessException("批量插入失败: 订单 " + id +
                        " 主键冲突 (Duplicate entry '" + id + "' for key 'PRIMARY')");
            }
            fakeDb.put(id, order);
        }
        logger.info("[DAO] 批量插入成功");
    }

    /**
     * 场景5：数据类型转换错误
     */
    public double queryOrderTotal(String orderId) {
        logger.info("[DAO] 查询订单总额: orderId={}", orderId);
        Object data = fakeDb.get(orderId);
        if (data == null) {
            // 返回特殊值导致后续计算错误
            return -1;
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> order = (Map<String, Object>) data;
        Object amountObj = order.get("amount");
        if (amountObj == null) {
            logger.error("[DAO] 订单金额字段为 null: orderId={}", orderId);
            throw new DataAccessException("数据完整性异常: 订单 " + orderId + " 的 amount 字段为 NULL");
        }
        return (Double) amountObj;
    }

    /**
     * 场景6：关联查询 —— 跨表查询失败
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> findOrderWithDetails(String orderId) {
        logger.info("[DAO] 关联查询订单和明细: orderId={}", orderId);
        Map<String, Object> order = (Map<String, Object>) fakeDb.get(orderId);

        if (order == null) {
            throw new DataAccessException("订单 " + orderId + " 不存在");
        }

        try {
            // 模拟关联查询 order_items 表异常
            String[] items = {"item-001", "item-002", "item-003"};
            String item = items[5]; // 故意数组越界
            logger.debug("查询到明细: {}", item);
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new DataAccessException(
                    "关联查询失败: order_items 表索引异常 (SQL: SELECT * FROM order_items " +
                            "WHERE order_id='" + orderId + "' ORDER BY seq ASC)", e);
        }
        return order;
    }

    /**
     * 场景7：拿到损坏数据后在 Service 层处理出错
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> findRawById(String orderId) {
        logger.info("[DAO] 原始查询: orderId={}", orderId);
        return (Map<String, Object>) fakeDb.get(orderId);
    }
}
