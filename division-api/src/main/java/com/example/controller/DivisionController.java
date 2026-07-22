package com.example.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class DivisionController {

    @GetMapping("/divide")
    public Map<String, Object> divide(
            @RequestParam double a,
            @RequestParam double b) {


        double result = a / b;

        return Map.of(
                "dividend", a,
                "divisor", b,
                "result", result
        );
    }
}
