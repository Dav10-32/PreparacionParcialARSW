package com.stockapp.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Componente de caché Singleton para almacenar datos de acciones.
 * <p>
 * ¿Por qué ConcurrentHashMap?
 * En un servidor web como Spring Boot, múltiples usuarios (hilos) acceden al
 * caché simultáneamente.
 * Un HashMap normal NO es seguro para hilos (thread-safe) y podría corromper
 * los datos o causar un bucle infinito.
 * ConcurrentHashMap permite accesos concurrentes eficientes sin necesidad de
 * bloquear todo el mapa.
 * </p>
 */
@Component
public class StockCache {

    private static final Logger logger = LoggerFactory.getLogger(StockCache.class);

    // Almacenamiento clave-valor: "DAILY_AAPL" -> JSON String
    private final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();

    /**
     * Recupera un valor del caché.
     * 
     * @param key Clave única.
     * @return El valor o null si no existe.
     */
    public String get(String key) {
        String value = cache.get(key);
        if (value != null) {
            logger.info("HIT de Caché para la clave: {}", key);
        } else {
            logger.info("MISS de Caché para la clave: {}", key);
        }
        return value;
    }

    /**
     * Guarda un valor en el caché.
     */
    public void put(String key, String value) {
        cache.put(key, value);
    }

    /**
     * Verifica si una clave ya existe.
     */
    public boolean contains(String key) {
        return cache.containsKey(key);
    }

    /**
     * Limpia todo el caché.
     */
    public void clear() {
        cache.clear();
    }

    /**
     * Retorna el número de elementos guardados.
     */
    public int size() {
        return cache.size();
    }
}
