package com.stockapp.service;

import com.stockapp.cache.StockCache;
import org.springframework.stereotype.Service;

/**
 * Servicio Orquestador (Fachada).
 * Coordina la lógica entre el proveedor de datos externo y la caché interna.
 */
@Service
public class StockFacadeService {

    private final StockDataProvider provider;
    private final StockCache cache;

    /**
     * Inyección por constructor (Práctica recomendada en Spring).
     */
    public StockFacadeService(StockDataProvider provider, StockCache cache) {
        this.provider = provider;
        this.cache = cache;
    }

    public String getIntraday(String symbol, String interval) {
        String key = buildKey("INTRADAY", symbol, interval);
        return getFromCacheOrFetch(key, () -> provider.getIntraday(symbol, interval));
    }

    public String getDaily(String symbol) {
        String key = buildKey("DAILY", symbol);
        return getFromCacheOrFetch(key, () -> provider.getDaily(symbol));
    }

    public String getWeekly(String symbol) {
        String key = buildKey("WEEKLY", symbol);
        return getFromCacheOrFetch(key, () -> provider.getWeekly(symbol));
    }

    public String getMonthly(String symbol) {
        String key = buildKey("MONTHLY", symbol);
        return getFromCacheOrFetch(key, () -> provider.getMonthly(symbol));
    }

    public int getCacheSize() {
        return cache.size();
    }

    /**
     * Genera llaves únicas para identificar el recurso en caché.
     */
    private String buildKey(String type, String symbol) {
        return String.format("%s_%s", type, symbol.toUpperCase());
    }

    private String buildKey(String type, String symbol, String interval) {
        return String.format("%s_%s_%s", type, symbol.toUpperCase(), interval);
    }

    /**
     * Lógica de Decisión:
     * 1. ¿Está en caché? -> Retornarlo.
     * 2. ¿No está? -> Llamar al proveedor externo.
     * 3. Validar respuesta (no guardar errores) y guardar si es válida.
     */
    private String getFromCacheOrFetch(String key, DataFetcher fetcher) {
        String cachedValue = cache.get(key);
        if (cachedValue != null) {
            return cachedValue;
        }

        String freshValue = fetcher.fetch();

        // REGLA DE ROBUSTEZ: Solo guardamos si contiene "Time Series" (evita cachear
        // errores de API)
        if (freshValue != null && freshValue.contains("Time Series")) {
            cache.put(key, freshValue);
        }
        return freshValue;
    }

    /**
     * Interfaz funcional interna para permitir pasar métodos como lambdas.
     */
    @FunctionalInterface
    private interface DataFetcher {
        String fetch();
    }
}
