# 📈 StockApp - Guía Suprema de Preparación para el Parcial (ARSW)

Este proyecto no es solo una aplicación de escritorio; es una arquitectura de referencia para sistemas distribuidos, concurrentes y escalables. Está diseñado para demostrar el dominio de patrones de diseño, integración de APIs externas y manejo de concurrencia.

---

## 📂 Estructura del Proyecto y Propósito

```text
stockapp/
├── facade/                  # Microservicio Backend (El mediador)
│   ├── src/main/java/com/stockapp/
│   │   ├── cache/           # Persistencia volátil (ConcurrentHashMap)
│   │   ├── config/          # Infraestructura (CORS, Seguridad)
│   │   ├── controller/      # Exposición de recursos (REST)
│   │   └── service/         # Lógica de negocio (Strategy + Facade)
├── frontend/                # Aplicación Web (Visualización)
│   ├── src/App.js           # Orquestador visual y fetcher de datos
├── test-client/             # Herramienta de validación técnica
└── .github/workflows/       # Automatización (CI/CD)
```

---

## 🛠️ Endpoints: Local vs Remoto

Es fundamental entender qué hace la "Fachada". El frontend **NUNCA** ve los parámetros complejos de Alpha Vantage.

### API Local (Nuestra Fachada en `localhost:8080`)
| Recurso | Parámetros | Propósito |
|---------|----------|-----------|
| `/api/stock/{sym}/intraday` | `interval` | Datos minuto a minuto. |
| `/api/stock/{sym}/daily` | n/a | Cierre diario de los últimos meses. |
| `/api/stock/{sym}/weekly` | n/a | Histórico semanal. |
| `/api/stock/health` | n/a | Estado del sistema y tamaño de la caché. |

### API Externa (Alpha Vantage - Consumida por el Backend)
| Function | Mapeo Local | Complejidad técnica |
|----------|-------------|---------------------|
| `TIME_SERIES_INTRADAY` | `/intraday` | Requiere API Key y `interval`. |
| `TIME_SERIES_DAILY` | `/daily` | Retorna ~100 puntos en modo compact. |

> [!IMPORTANT]
> **Aislamiento de API Keys**: Nota que el frontend solo envía el símbolo. El backend añade la `apikey`. Esto evita que tu clave sea robada desde el navegador del usuario.

---

## 🎨 Patrones de Diseño (Explicación para el Examen)

### 1. Patrón Facade (Fachada)
**Propósito**: Proporcionar una interfaz unificada y simplificada.
- **Implementación**: `StockController` y `StockFacadeService`.
- **Ventaja**: Si Alpha Vantage cambiara su formato de JSON a XML, el frontend no se enteraría; solo actualizamos el Facade.

### 2. Patrón Strategy (Estrategia)
**Propósito**: Definir una familia de algoritmos y hacerlos intercambiables.
- **Implementación**: Interfaz `StockDataProvider` y sus implementaciones.
- **Código para extender**:
```java
@Service
@Primary // Esto hace que Spring elija esta clase por defecto
public class BloombergProvider implements StockDataProvider {
    @Override
    public String getDaily(String symbol) {
        // Lógica para llamar a Bloomberg en lugar de AlphaVantage
        return "{}";
    }
    // ... otros métodos
}
```

### 3. Patrón Singleton (Bean Scope)
**Propósito**: Asegurar una única instancia de una clase.
- **Implementación**: `StockCache`.
- **Relevancia**: Permite que las peticiones del Usuario A guarden datos que el Usuario B puede aprovechar (caché global).

---

## 🚀 Casos de Extensión Posibles (Laboratorio/Parcial)

### A. Agregar Filtros de Datos (Server-Side)
Si te piden que el Facade solo devuelva datos con volumen mayor a X:
1. En `StockFacadeService`, antes de guardar en caché, parsea el JSON (con Jackson).
2. Filtra los items.
3. Convierte de nuevo a JSON y guarda el resultado filtrado.

### B. Agregar Soporte para Criptomonedas
Alpha Vantage usa funciones como `DIGITAL_CURRENCY_DAILY`:
```java
// En AlphaVantageProvider.java
@Override
public String getCrypto(String symbol) {
    String url = String.format("?function=DIGITAL_CURRENCY_DAILY&symbol=%s&market=USD&apikey=%s", symbol, apiKey);
    return fetchData(url);
}
```

### C. Implementar TTL (Time-To-Live) en la Caché
Actualmente la caché es eterna hasta que reinicias. Para que expire:
1. Cambia `ConcurrentHashMap<String, String>` por un mapa que guarde un objeto `CacheEntry { String data, long timestamp }`.
2. En `get(key)`, verifica: `System.currentTimeMillis() - entry.timestamp > 3600000`. Si es mayor, bórralo y retorna null (Cache Miss).

---

## 🔐 Concurrencia Bajo la Lupa

En ARSW, el manejo de hilos es clave. El `ConcurrentTestClient` usa:
- **`ExecutorService`**: Para gestionar un pool de hilos y no saturar el sistema.
- **`CountDownLatch`**: Para que todos los hilos arranquen al mismo tiempo (simular ataque o pico de tráfico).
- **`HttpClient` (nativo)**: Más eficiente que librerías viejas para llamadas asíncronas.

```java
// Truco de concurrencia en el test:
CountDownLatch latch = new CountDownLatch(1);
// ... crear 20 hilos que esperan latch.await() ...
latch.countDown(); // DISPARA TODOS LOS HILOS AL MISMO TIEMPO
```

---

## 🏁 Checklist de Verificación Final
1. **CORS**: ¿Está configurado para el puerto 3000? (Sí, en `CorsConfig`).
2. **Performance**: ¿Mejora el tiempo en la segunda llamada? (Sí, por la caché).
3. **Escalabilidad**: ¿Puedo meter Redis en lugar del Mapa? (Sí, solo cambio `StockCache`).
4. **Despliegue**: ¿Está el `Dockerfile` optimizado? (Sí, multi-stage para pesar menos).