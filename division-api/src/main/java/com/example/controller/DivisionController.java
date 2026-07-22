package com.example.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class DivisionController {

    private static final Logger logger = LoggerFactory.getLogger(DivisionController.class);

    @GetMapping("/divide")
    public Map<String, Object> divide(
            @RequestParam double a,
            @RequestParam double b) {

        logger.info("收到除法請求: a={}, b={}", a, b);

        if (b == 0) {
            logger.error("除數為零！a={}, b={}", a, b);
            throw new IllegalArgumentException("除數不可為零");
        }

        double result = a / b;
        logger.info("計算完成: {} / {} = {}", a, b, result);

        return Map.of(
                "dividend", a,
                "divisor", b,
                "result", result
        );
    }

    @GetMapping("/divide-error")
    public Map<String, Object> divideError(
            @RequestParam(defaultValue = "10") double a,
            @RequestParam(defaultValue = "0") double b) {

        logger.info("收到錯誤模擬請求: a={}, b={}", a, b);

        // 故意模擬一個 NullPointerException
        String test = null;
        try {
            test.length();
        } catch (NullPointerException e) {
            logger.error("模擬 NPE 錯誤: {}", e.getMessage(), e);
            throw new RuntimeException("故意觸發的錯誤，用於測試根因系統", e);
        }

        return Map.of("result", a / b);
    }
}
