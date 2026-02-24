package com.stockapp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Implementación de StockDataProvider que utiliza la API externa de Alpha
 * Vantage.
 * Utiliza WebClient para realizar peticiones HTTP.
 */
@Service
public class AlphaVantageProvider implements StockDataProvider {

    private static final Logger logger = LoggerFactory.getLogger(AlphaVantageProvider.class);

    private final WebClient webClient;
    private final String apiKey;
    private final String baseUrl;

    /**
     * Constructor que inyecta la configuración desde application.properties.
     */
    public AlphaVantageProvider(WebClient.Builder webClientBuilder,
            @Value("${alphavantage.apikey}") String apiKey,
            @Value("${alphavantage.base-url}") String baseUrl) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
    }

    @Override
    public String getIntraday(String symbol, String interval) {
        // Construcción de URL para datos intradía
        String url = String.format("?function=TIME_SERIES_INTRADAY&symbol=%s&interval=%s&outputsize=compact&apikey=%s",
                symbol, interval, apiKey);
        return fetchData(url);
    }

    @Override
    public String getDaily(String symbol) {
        // Construcción de URL para datos diarios
        String url = String.format("?function=TIME_SERIES_DAILY&symbol=%s&outputsize=compact&apikey=%s",
                symbol, apiKey);
        return fetchData(url);
    }

    @Override
    public String getWeekly(String symbol) {
        // Construcción de URL para datos semanales
        String url = String.format("?function=TIME_SERIES_WEEKLY&symbol=%s&apikey=%s",
                symbol, apiKey);
        return fetchData(url);
    }

    @Override
    public String getMonthly(String symbol) {
        // Construcción de URL para datos mensuales
        String url = String.format("?function=TIME_SERIES_MONTHLY&symbol=%s&apikey=%s",
                symbol, apiKey);
        return fetchData(url);
    }

    /**
     * Método auxiliar para realizar la petición HTTP y registrar logs.
     * 
     * @param queryParams Parámetros de la URL.
     * @return El cuerpo de la respuesta como String JSON.
     */
    private String fetchData(String queryParams) {
        // Ocultamos la API Key en los logs por seguridad usando regex
        logger.info("Solicitando datos a Alpha Vantage: {}", queryParams.replaceAll("apikey=[^&]+", "apikey=***"));

        return webClient.get()
                .uri(queryParams)
                .retrieve()
                .bodyToMono(String.class)
                .block(); // Bloqueante por simplicidad en esta arquitectura de fachada
    }
}
