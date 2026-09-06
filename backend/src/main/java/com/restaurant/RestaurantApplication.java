package com.restaurant;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 餐厅点餐与评价系统 - 启动类
 */
@SpringBootApplication
@MapperScan("com.restaurant.mapper")
public class RestaurantApplication {

    public static void main(String[] args) {
        SpringApplication.run(RestaurantApplication.class, args);
        System.out.println("===========================================");
        System.out.println("  餐厅点餐与评价系统启动成功！");
        System.out.println("  后端地址: http://localhost:8080");
        System.out.println("===========================================");
    }
}
