package com.restaurant.config;

import com.restaurant.interceptor.JwtInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

/**
 * Web MVC 配置
 * 注册 JWT 拦截器，放行公开接口
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Resource
    private JwtInterceptor jwtInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/**")            // 拦截所有 API
                .excludePathPatterns(                   // 放行公开接口
                        "/api/user/login",
                        "/api/user/register",
                        "/api/dish/list",
                        "/api/dish/detail/**",
                        "/api/category/list",
                        "/api/review/list/**",
                        "/error",
                        "/static/**"
                );
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 静态资源映射 (上传图片) — 使用 user.dir 绝对路径
        String uploadPath = "file:" + System.getProperty("user.dir") + "/uploads/";
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadPath);
    }
}
