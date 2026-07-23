package com.example.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 计算服务 —— 包含多种计算异常场景
 */
@Service
public class CalculatorService {

    private static final Logger logger = LoggerFactory.getLogger(CalculatorService.class);

    /**
     * 正常除法
     */
    public double divide(double a, double b) {
        logger.info("[Calculator] 执行除法: {} / {}", a, b);
        if (b == 0) {
            logger.error("[Calculator] 除数为零拒绝计算");
            throw new IllegalArgumentException("除数不可为零");
        }
        double result = a / b;
        logger.info("[Calculator] 结果: {}", result);
        return result;
    }

    /**
     * 数组平均值 —— 可能越界或空数组
     */
    public double average(int[] numbers) {
        logger.info("[Calculator] 计算平均值, 数组长度={}", numbers == null ? "null" : numbers.length);
        if (numbers == null) {
            throw new IllegalArgumentException("数组不能为 null");
        }
        if (numbers.length == 0) {
            throw new IllegalArgumentException("数组不能为空");
        }
        int sum = 0;
        for (int num : numbers) {
            sum += num;
        }
        return (double) sum / numbers.length;
    }

    /**
     * 加权计算 —— 参数校验场景
     */
    public double weightedScore(double score, double weight) {
        logger.info("[Calculator] 加权计算: score={}, weight={}", score, weight);
        if (weight < 0 || weight > 1) {
            throw new IllegalArgumentException("权重必须在 [0, 1] 范围内, 当前值: " + weight);
        }
        if (score < 0 || score > 100) {
            throw new IllegalArgumentException("分数必须在 [0, 100] 范围内, 当前值: " + score);
        }
        return score * weight;
    }

    /**
     * 字符串解析为数字 —— NumberFormatException 场景
     */
    public int parseIntSafely(String raw) {
        logger.info("[Calculator] 解析字符串: '{}'", raw);
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("输入不能为空");
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            logger.error("[Calculator] 数字格式错误: '{}'", raw, e);
            throw new NumberFormatException("无法将 '" + raw + "' 解析为整数");
        }
    }

    /**
     * 复杂计算 —— 深层调用链场景
     */
    public double complexFormula(double x, double y, double z, String mode) {
        logger.info("[Calculator] 复杂公式计算: x={}, y={}, z={}, mode={}", x, y, z, mode);

        // 第1层
        double step1 = step1Normalize(x);

        // 第2层
        double step2 = step2Transform(step1, y);

        // 第3层
        double step3 = step3ApplyMode(step2, z, mode);

        // 第4层
        double step4 = step4Finalize(step3);

        logger.info("[Calculator] 最终结果: {}", step4);
        return step4;
    }

    private double step1Normalize(double x) {
        logger.debug("[Calculator.step1] 归一化输入: {}", x);
        if (Double.isNaN(x) || Double.isInfinite(x)) {
            throw new IllegalArgumentException("输入值无效: " + x);
        }
        return Math.log(Math.abs(x) + 1);
    }

    private double step2Transform(double value, double y) {
        logger.debug("[Calculator.step2] 变换: value={}, y={}", value, y);
        if (y == 0) {
            throw new ArithmeticException("变换参数 y 不能为零");
        }
        return value / y;
    }

    private double step3ApplyMode(double value, double z, String mode) {
        logger.debug("[Calculator.step3] 应用模式: value={}, z={}, mode={}", value, z, mode);

        switch (mode) {
            case "sqrt":
                if (value < 0) {
                    throw new ArithmeticException("无法对负数开平方: " + value);
                }
                return Math.sqrt(value);
            case "scale":
                return value * z;
            case "reciprocal":
                if (value == 0) {
                    throw new ArithmeticException("倒數計算：值为零");
                }
                return 1.0 / value;
            default:
                throw new IllegalArgumentException("未知计算模式: " + mode +
                        " (支持: sqrt, scale, reciprocal)");
        }
    }

    private double step4Finalize(double value) {
        logger.debug("[Calculator.step4] 最终处理: {}", value);
        if (Double.isInfinite(value)) {
            throw new ArithmeticException("结果超出可表示范围 (正无穷)");
        }
        if (Double.isNaN(value)) {
            throw new ArithmeticException("计算结果为 NaN");
        }
        return Math.round(value * 10000.0) / 10000.0;
    }
}
