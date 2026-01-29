package com.codeanalyzer.backend.config; // ⚠️ Make sure this matches your folder!

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // "/**" means apply this rule to ALL endpoints in the app
        registry.addMapping("/**")
                // "*" means allow ANY website to connect (e.g., localhost, Vercel, etc.)
                .allowedOrigins("*")
                // Allow these HTTP actions
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                // Allow all headers
                .allowedHeaders("*");
    }
}