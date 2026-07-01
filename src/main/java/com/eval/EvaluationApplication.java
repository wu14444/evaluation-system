package com.eval;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 学生综合素质评价管理系统 - 启动类
 */
@SpringBootApplication
@MapperScan("com.eval.mapper")
public class EvaluationApplication {
    public static void main(String[] args) {
        SpringApplication.run(EvaluationApplication.class, args);
    }
}
