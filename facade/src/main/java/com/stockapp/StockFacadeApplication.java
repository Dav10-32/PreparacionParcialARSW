package com.stockapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class for the Stock Facade service.
 * This service acts as a gateway and cache for stock market data.
 */
@SpringBootApplication
public class StockFacadeApplication {

    public static void main(String[] args) {
        SpringApplication.run(StockFacadeApplication.class, args);
    }
}
