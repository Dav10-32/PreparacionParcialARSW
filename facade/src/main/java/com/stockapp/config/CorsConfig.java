package com.stockapp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Clase de configuración para determinar las políticas de intercambio de
 * recursos
 * de origen cruzado (CORS). Permite que el Dashboard (React) se comunique con
 * este API.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    // Orígenes permitidos (ej. http://localhost:3000) leídos desde la configuración
    @Value("${cors.allowed-origins}")
    private String allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // Configuramos CORS para todos los endpoints bajo /api/**
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins.split(","))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600); // 1 hora de caché para la pre-flight request (OPTIONS)
    }
}
