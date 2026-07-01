package com.eval.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 暂时注释掉拦截器，方便调试
        // registry.addInterceptor(new LoginInterceptor())
        //         .addPathPatterns("/**")
        //         .excludePathPatterns("/login", "/api/login", "/css/**", "/js/**", "/images/**");
    }
}
