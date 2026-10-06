package com.cp.party_trip.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

    // เว็บที่เรียก API ได้: ตั้งใน application.properties (app.cors.allowed-origin-patterns)
    // ค่าเริ่มต้นรวม Live Server บนเครื่องนี้ + เครื่องในวง LAN (เปิดจากมือถือด้วย IP ของคอม)
    @Value("${app.cors.allowed-origin-patterns:http://localhost:*,http://127.0.0.1:*,http://192.168.*.*:*,http://10.*.*.*:*}")
    private String[] allowedOriginPatterns;

    @Bean
    public WebMvcConfigurer corsConfigurer(AuthInterceptor authInterceptor) {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOriginPatterns(allowedOriginPatterns)
                        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }

            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(authInterceptor).addPathPatterns("/api/**");
            }

            // หน้าเว็บอยู่ใน resources/static (Spring Boot เสิร์ฟเอง): เปิด http://<เครื่อง>:8090/ = หน้าแรก
            @Override
            public void addViewControllers(ViewControllerRegistry registry) {
                registry.addRedirectViewController("/", "/home.html");
            }
        };
    }
}
