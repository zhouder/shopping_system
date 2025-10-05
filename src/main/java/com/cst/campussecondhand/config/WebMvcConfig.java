package com.cst.campussecondhand.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 当浏览器访问 /uploads/** 的URL时
        registry.addResourceHandler("/uploads/**")
                // 将其映射到服务器文件系统的绝对路径上
                // "file:" 是必需的前缀，表示这是一个文件系统路径
                .addResourceLocations("file:" + System.getProperty("user.home") + "/app-uploads/");
    }
}