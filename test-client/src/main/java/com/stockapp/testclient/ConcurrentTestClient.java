package com.stockapp.testclient;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Cliente de Pruebas Concurrentes.
 * Esta herramienta simula múltiples usuarios atacando el API simultáneamente
 * para validar el manejo de hilos y la eficiencia del caché en el servidor.
 */
public class ConcurrentTestClient {

    private static final String DEFAULT_BASE_URL = "http://localhost:8080";
    private static final int DEFAULT_THREADS = 10;

    // Cliente HTTP moderno de Java 11+
    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(10))
            .build();

    public static void main(String[] args) {
        String baseUrl = (args.length > 0) ? args[0] : DEFAULT_BASE_URL;
        int numThreads = (args.length > 1) ? Integer.parseInt(args[1]) : DEFAULT_THREADS;

        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("🚀 INICIANDO CLIENTE DE PRUEBAS CONCURRENTES");
        System.out.println("📍 URL Base: " + baseUrl);
        System.out.println("🧵 Hilos: " + numThreads);
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");

        try {
            runTest1Health(baseUrl);
            runTest2BasicEndpoints(baseUrl);
            runTest3Concurrency(baseUrl, numThreads);
            runTest4CacheEfficiency(baseUrl);
        } catch (Exception e) {
            System.err.println("❌ Error crítico en las pruebas: " + e.getMessage());
        }

        System.out.println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("🏁 PRUEBAS FINALIZADAS");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    /**
     * Test 1: Comprobar que el API está activa.
     */
    private static void runTest1Health(String baseUrl) throws Exception {
        System.out.println("Test 1: Health Check (Estado del Sistema)");
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(baseUrl + "/api/stock/health")).GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            System.out.println("  ✓ Salud Correcta: " + response.body());
        } else {
            System.out.println("  ✗ Error de conexión (Status: " + response.statusCode() + ")");
        }
        System.out.println();
    }

    /**
     * Test 2: Comprobar endpoints básicos de forma secuencial.
     */
    private static void runTest2BasicEndpoints(String baseUrl) {
        System.out.println("Test 2: Endpoints Básicos (Secuencial)");
        String[] endpoints = { "/daily", "/weekly", "/monthly" };

        for (String ep : endpoints) {
            try {
                HttpRequest request = HttpRequest.newBuilder().uri(URI.create(baseUrl + "/api/stock/IBM" + ep)).GET()
                        .build();
                long start = System.currentTimeMillis();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                long time = System.currentTimeMillis() - start;

                if (response.statusCode() == 200) {
                    System.out.println("  ✓ " + ep + " ... OK (" + time + "ms)");
                } else {
                    System.out.println("  ✗ " + ep + " ... ERROR (Status: " + response.statusCode() + ")");
                }
            } catch (Exception e) {
                System.out.println("  ✗ " + ep + " ... ERROR: " + e.getMessage());
            }
        }
        System.out.println();
    }

    /**
     * Test 3: Ataque concurrente.
     * Usa CountDownLatch para liberar todos los hilos EXACTAMENTE al mismo tiempo.
     */
    private static void runTest3Concurrency(String baseUrl, int numThreads) throws Exception {
        System.out.println("Test 3: Prueba de Concurrencia (" + numThreads + " hilos en paralelo)");
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);

        // El latch de inicio bloquea a los hilos hasta que todos estén listos
        CountDownLatch startLatch = new CountDownLatch(1);
        // El latch de fin espera a que todos los hilos terminen para mostrar resultados
        CountDownLatch finishLatch = new CountDownLatch(numThreads);

        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // Esperar la señal de salida
                    HttpRequest request = HttpRequest.newBuilder().uri(URI.create(baseUrl + "/api/stock/IBM/daily"))
                            .GET().build();
                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                    if (response.statusCode() == 200)
                        successCount.incrementAndGet();
                } catch (Exception ignored) {
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        long start = System.currentTimeMillis();
        startLatch.countDown(); // ¡FUEGO! (Libera todos los hilos)
        finishLatch.await(); // Esperar a que terminen
        long totalTime = System.currentTimeMillis() - start;

        executor.shutdown();

        System.out.println("  🚀 Hilos lanzados: " + numThreads);
        System.out.println("  ✓ Exitosos: " + successCount.get());
        System.out.println("  ⏱ Tiempo total del ataque: " + totalTime + "ms");
        System.out.println();
    }

    /**
     * Test 4: Eficiencia de la Caché.
     * Compara el tiempo de la primera petición vs la segunda (debe ser casi
     * instantánea).
     */
    private static void runTest4CacheEfficiency(String baseUrl) throws Exception {
        System.out.println("Test 4: Eficiencia de la Caché (AAPL)");
        String url = baseUrl + "/api/stock/AAPL/weekly";

        // Petición 1: Fallo de caché (debe llamar a la API externa)
        long start1 = System.currentTimeMillis();
        client.send(HttpRequest.newBuilder().uri(URI.create(url)).GET().build(), HttpResponse.BodyHandlers.ofString());
        long time1 = System.currentTimeMillis() - start1;
        System.out.println("  1ra Petición (MISS): " + time1 + "ms");

        // Petición 2: Acierto de caché (debe leer de memoria)
        long start2 = System.currentTimeMillis();
        client.send(HttpRequest.newBuilder().uri(URI.create(url)).GET().build(), HttpResponse.BodyHandlers.ofString());
        long time2 = System.currentTimeMillis() - start2;
        System.out.println("  2da Petición (HIT) : " + time2 + "ms");

        if (time2 < time1 * 0.5) {
            System.out.println("  ✓ Caché trabajando correctamente (Ahorro de tiempo masivo)");
        } else {
            System.out.println("  ⚠ La diferencia de tiempo no es concluyente.");
        }
        System.out.println();
    }
}
