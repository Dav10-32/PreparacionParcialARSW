package com.stockapp.service;

/**
 * Interfaz de estrategia para los proveedores de datos de acciones.
 * Define el contrato para obtener diferentes tipos de series de tiempo
 * bursátiles.
 * <p>
 * Para agregar un nuevo proveedor (ej. Yahoo Finance, Bloomberg):
 * 1. Cree una nueva clase que implemente esta interfaz.
 * 2. Implemente todos los métodos requeridos.
 * 3. Anote la clase con @Service (y @Primary para que sea la predeterminada).
 * </p>
 */
public interface StockDataProvider {

    /**
     * Obtiene datos intradía para un símbolo y un intervalo específico.
     * 
     * @param symbol   Símbolo de la acción (ej. AAPL).
     * @param interval Intervalo entre puntos (ej. 5min, 15min).
     * @return String en formato JSON con los datos intradía.
     */
    String getIntraday(String symbol, String interval);

    /**
     * Obtiene datos diarios (cierre de mercado) de los últimos meses.
     * 
     * @param symbol Símbolo de la acción (ej. MSFT).
     * @return String en formato JSON con los datos diarios.
     */
    String getDaily(String symbol);

    /**
     * Obtiene datos históricos con periodicidad semanal.
     * 
     * @param symbol Símbolo de la acción (ej. IBM).
     * @return String en formato JSON con los datos semanales.
     */
    String getWeekly(String symbol);

    /**
     * Obtiene datos históricos con periodicidad mensual.
     * 
     * @param symbol Símbolo de la acción (ej. GOOGL).
     * @return String en formato JSON con los datos mensuales.
     */
    String getMonthly(String symbol);
}
