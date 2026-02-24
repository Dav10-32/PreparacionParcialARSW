package com.stockapp.controller;

import com.stockapp.service.StockFacadeService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controlador REST que expone la API de acciones hacia el Frontend.
 */
@RestController
@RequestMapping("/api/stock")
public class StockController {

    private final StockFacadeService facadeService;

    public StockController(StockFacadeService facadeService) {
        this.facadeService = facadeService;
    }

    /**
     * Endpoint para datos intradía.
     * Ej: GET /api/stock/AAPL/intraday?interval=5min
     */
    @GetMapping("/{symbol}/intraday")
    public ResponseEntity<String> getIntraday(@PathVariable String symbol,
            @RequestParam(defaultValue = "5min") String interval) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(facadeService.getIntraday(symbol, interval));
    }

    /**
     * Endpoint para datos diarios.
     * Ej: GET /api/stock/MSFT/daily
     */
    @GetMapping("/{symbol}/daily")
    public ResponseEntity<String> getDaily(@PathVariable String symbol) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(facadeService.getDaily(symbol));
    }

    /**
     * Endpoint para datos semanales.
     * Ej: GET /api/stock/IBM/weekly
     */
    @GetMapping("/{symbol}/weekly")
    public ResponseEntity<String> getWeekly(@PathVariable String symbol) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(facadeService.getWeekly(symbol));
    }

    /**
     * Endpoint para datos mensuales.
     * Ej: GET /api/stock/GOOGL/monthly
     */
    @GetMapping("/{symbol}/monthly")
    public ResponseEntity<String> getMonthly(@PathVariable String symbol) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(facadeService.getMonthly(symbol));
    }

    /**
     * Comprobación de salud (Health Check).
     * Retorna el estado del sistema, tamaño de la caché y el proveedor activo.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getHealth() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "UP");
        status.put("cacheSize", facadeService.getCacheSize());
        status.put("provider", "AlphaVantage");
        return ResponseEntity.ok(status);
    }
}
