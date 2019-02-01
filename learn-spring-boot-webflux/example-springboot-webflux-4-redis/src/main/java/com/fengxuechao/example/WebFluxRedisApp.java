package com.fengxuechao.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

/**
 * 用 Spring Boot WebFlux 的注解控制层技术创建一个 CRUD WebFlux 应用
 *
 * @author fengxuechao
 */
@SpringBootApplication
public class WebFluxRedisApp {

    public static void main(String[] args) {
        SpringApplication.run(WebFluxRedisApp.class, args);
    }

}
