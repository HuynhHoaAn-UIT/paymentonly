package com.shopsphere.payment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;
@Configuration
public class CorsConfig implements WebMvcConfigurer {
 @Value("${app.cors:http://localhost:5500,http://127.0.0.1:5500}") private String origins;
 public void addCorsMappings(CorsRegistry registry){
  registry.addMapping("/api/**").allowedOrigins(origins.split(","))
   .allowedMethods("GET","POST","OPTIONS").allowedHeaders("Content-Type","Idempotency-Key");
 }
}