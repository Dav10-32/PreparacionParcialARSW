import React, { useState, useEffect, useCallback } from 'react';
import axios from 'axios';
import {
    LineChart, Line, AreaChart, Area, BarChart, Bar,
    XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer
} from 'recharts';

// --- FUNCIONES DE APOYO (HELPERS) ---

/**
 * Parsea el JSON crudo de Alpha Vantage para convertirlo en un array plano
 * que la librería de gráficos (Recharts) pueda entender.
 */
const parseAlphaVantageData = (json, maxPoints = 60) => {
    if (!json) return [];

    // Detectar errores o límites de la API (Crucial para manejo de errores)
    if (json['Note'] || json['Information']) {
        throw new Error("Límite de API alcanzado o aviso: " + (json['Note'] || json['Information']));
    }
    if (json['Error Message']) {
        throw new Error("Error en la petición: Símbolo no encontrado.");
    }

    // Buscamos la llave dinámica que contiene los datos ("Time Series (Daily)", etc.)
    const timeSeriesKey = Object.keys(json).find(key => key.includes('Time Series'));
    if (!timeSeriesKey) return [];

    const timeSeries = json[timeSeriesKey];
    return Object.entries(timeSeries)
        .map(([date, values]) => ({
            date: date.includes(' ') ? date.split(' ')[1] : date, // Acortar la fecha para gráficos intradía
            fullDate: date,
            open: parseFloat(values['1. open']),
            high: parseFloat(values['2. high']),
            low: parseFloat(values['3. low']),
            close: parseFloat(values['4. close']),
            volume: parseInt(values['5. volume'] || values['6. volume']),
        }))
        .reverse() // Alpha Vantage entrega los datos del más reciente al más antiguo, necesitamos reversarlos
        .slice(-maxPoints); // Tomamos solo los últimos N puntos para no saturar el gráfico
};

// --- SUB-COMPONENTES VISUALES ---

/**
 * Selector de tipo de gráfico con estilo segmentado.
 */
const ChartTypeSelector = ({ current, onChange }) => {
    const types = ['Line', 'Area', 'Bar'];
    return (
        <div style={{ display: 'flex', gap: '8px', background: '#1e1e24', padding: '4px', borderRadius: '12px' }}>
            {types.map(type => (
                <button
                    key={type}
                    onClick={() => onChange(type)}
                    style={{
                        padding: '8px 16px',
                        border: 'none',
                        borderRadius: '8px',
                        cursor: 'pointer',
                        fontSize: '14px',
                        fontWeight: 500,
                        transition: 'all 0.2s',
                        background: current === type ? '#3b82f6' : 'transparent',
                        color: current === type ? '#fff' : '#94a3b8',
                    }}
                >
                    {type}
                </button>
            ))}
        </div>
    );
};

/**
 * Barra de métricas con tarjetas calculadas dinámicamente.
 */
const MetricsBar = ({ data }) => {
    if (!data || data.length === 0) return null;

    const last = data[data.length - 1]; // El punto más reciente
    const first = data[0]; // El punto más antiguo de la serie actual
    const change = last.close - first.close;
    const changePercent = (change / first.close) * 100;
    const high = Math.max(...data.map(d => d.high));
    const low = Math.min(...data.map(d => d.low));

    const cardStyle = {
        background: '#1e1e24',
        padding: '16px 24px',
        borderRadius: '16px',
        border: '1px solid #2d2d35',
        flex: 1,
        minWidth: '150px'
    };

    return (
        <div style={{ display: 'flex', gap: '16px', flexWrap: 'wrap', marginBottom: '24px' }}>
            <div style={cardStyle}>
                <div style={{ color: '#94a3b8', fontSize: '12px', marginBottom: '8px' }}>ÚLTIMO PRECIO</div>
                <div style={{ color: '#f8fafc', fontSize: '20px', fontWeight: 600 }}>${last.close.toFixed(2)}</div>
            </div>
            <div style={cardStyle}>
                <div style={{ color: '#94a3b8', fontSize: '12px', marginBottom: '8px' }}>VARIACIÓN</div>
                <div style={{ color: change >= 0 ? '#10b981' : '#ef4444', fontSize: '20px', fontWeight: 600 }}>
                    {change >= 0 ? '+' : ''}{change.toFixed(2)} ({changePercent.toFixed(2)}%)
                </div>
            </div>
            <div style={cardStyle}>
                <div style={{ color: '#94a3b8', fontSize: '12px', marginBottom: '8px' }}>MÁXIMO (PERIODO)</div>
                <div style={{ color: '#f8fafc', fontSize: '20px', fontWeight: 600 }}>${high.toFixed(2)}</div>
            </div>
            <div style={cardStyle}>
                <div style={{ color: '#94a3b8', fontSize: '12px', marginBottom: '8px' }}>MÍNIMO (PERIODO)</div>
                <div style={{ color: '#f8fafc', fontSize: '20px', fontWeight: 600 }}>${low.toFixed(2)}</div>
            </div>
        </div>
    );
};

/**
 * Componente que renderiza el gráfico usando Recharts.
 */
const StockChart = ({ data, chartType }) => {
    const commonProps = {
        width: "100%",
        height: 380,
        data: data,
        margin: { top: 10, right: 30, left: 0, bottom: 0 }
    };

    const renderCurrentChart = () => {
        switch (chartType) {
            case 'Area':
                return (
                    <AreaChart {...commonProps}>
                        <defs>
                            <linearGradient id="colorPrice" x1="0" y1="0" x2="0" y2="1">
                                <stop offset="5%" stopColor="#3b82f6" stopOpacity={0.3} />
                                <stop offset="95%" stopColor="#3b82f6" stopOpacity={0} />
                            </linearGradient>
                        </defs>
                        <XAxis dataKey="date" stroke="#475569" fontSize={12} tickLine={false} axisLine={false} />
                        <YAxis stroke="#475569" fontSize={12} tickLine={false} axisLine={false} domain={['auto', 'auto']} />
                        <CartesianGrid strokeDasharray="3 3" stroke="#2d2d35" vertical={false} />
                        <Tooltip contentStyle={{ background: '#1e1e24', border: 'none', borderRadius: '8px' }} />
                        <Area type="monotone" dataKey="close" stroke="#3b82f6" strokeWidth={2} fillOpacity={1} fill="url(#colorPrice)" />
                    </AreaChart>
                );
            case 'Bar':
                return (
                    <BarChart {...commonProps}>
                        <XAxis dataKey="date" stroke="#475569" fontSize={12} tickLine={false} axisLine={false} />
                        <YAxis stroke="#475569" fontSize={12} tickLine={false} axisLine={false} domain={['auto', 'auto']} />
                        <CartesianGrid strokeDasharray="3 3" stroke="#2d2d35" vertical={false} />
                        <Tooltip contentStyle={{ background: '#1e1e24', border: 'none', borderRadius: '8px' }} />
                        <Bar dataKey="close" fill="#3b82f6" radius={[4, 4, 0, 0]} />
                    </BarChart>
                );
            default:
                return (
                    <LineChart {...commonProps}>
                        <XAxis dataKey="date" stroke="#475569" fontSize={12} tickLine={false} axisLine={false} />
                        <YAxis stroke="#475569" fontSize={12} tickLine={false} axisLine={false} domain={['auto', 'auto']} />
                        <CartesianGrid strokeDasharray="3 3" stroke="#2d2d35" vertical={false} />
                        <Tooltip contentStyle={{ background: '#1e1e24', border: 'none', borderRadius: '8px' }} />
                        <Line type="monotone" dataKey="close" stroke="#3b82f6" strokeWidth={2} dot={false} />
                    </LineChart>
                );
        }
    };

    return (
        <div style={{ background: '#1e1e24', padding: '24px', borderRadius: '24px', border: '1px solid #2d2d35' }}>
            <ResponsiveContainer width="100%" height={400}>
                {renderCurrentChart()}
            </ResponsiveContainer>
        </div>
    );
};

// --- APLICACIÓN PRINCIPAL ---

export default function App() {
    const [symbol, setSymbol] = useState('IBM');
    const [period, setPeriod] = useState('daily');
    const [interval, setInterval] = useState('5min');
    const [chartType, setChartType] = useState('Area');
    const [data, setData] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);

    /**
     * Lógica principal para obtener datos desde el Facade API.
     */
    const fetchData = useCallback(async () => {
        setLoading(true);
        setError(null);
        try {
            const baseUrl = process.env.REACT_APP_API_URL || '';
            let url = `${baseUrl}/api/stock/${symbol}/${period}`;
            if (period === 'intraday') {
                url += `?interval=${interval}`;
            }

            const response = await axios.get(url);
            const parsedData = parseAlphaVantageData(response.data);

            if (parsedData.length === 0) {
                throw new Error("Petición exitosa pero sin datos. Revisa el símbolo o límites.");
            }

            setData(parsedData);
        } catch (err) {
            console.error(err);
            setError(err.message || "Error al conectar con el servidor.");
        } finally {
            setLoading(false);
        }
    }, [symbol, period, interval]);

    // Se ejecuta al cargar la App o cuando cambia el periodo/intervalo
    useEffect(() => {
        fetchData();
    }, [period, interval]);

    return (
        <div style={{ maxWidth: '1200px', margin: '0 auto', padding: '40px 20px' }}>
            <header style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '40px' }}>
                <div>
                    <h1 style={{ margin: 0, fontSize: '28px', fontWeight: 700 }}>Market Terminal</h1>
                    <p style={{ color: '#64748b', fontSize: '14px' }}>Interfaz de consulta bursátil (ARSW)</p>
                </div>
                <div style={{ display: 'flex', gap: '12px' }}>
                    <input
                        style={{ background: '#1e1e24', border: '1px solid #2d2d35', padding: '12px', borderRadius: '12px', color: '#fff' }}
                        value={symbol}
                        onChange={(e) => setSymbol(e.target.value.toUpperCase())}
                        onKeyDown={(e) => e.key === 'Enter' && fetchData()}
                    />
                    <button onClick={fetchData} style={{ background: '#3b82f6', border: 'none', padding: '12px 24px', borderRadius: '12px', color: '#fff', fontWeight: 600, cursor: 'pointer' }}>
                        Buscar
                    </button>
                </div>
            </header>

            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '32px' }}>
                <div style={{ display: 'flex', gap: '8px' }}>
                    {['intraday', 'daily', 'weekly', 'monthly'].map(p => (
                        <button key={p} onClick={() => setPeriod(p)} style={{
                            padding: '10px 20px', border: 'none', borderRadius: '10px',
                            background: period === p ? '#2d2d35' : 'transparent',
                            color: period === p ? '#fff' : '#94a3b8', cursor: 'pointer'
                        }}>
                            {p.toUpperCase()}
                        </button>
                    ))}
                </div>

                {period === 'intraday' && (
                    <select value={interval} onChange={(e) => setInterval(e.target.value)} style={{ background: '#1e1e24', color: '#fff', border: '1px solid #2d2d35', borderRadius: '10px', padding: '0 10px' }}>
                        {['1min', '5min', '15min', '30min', '60min'].map(i => <option key={i} value={i}>{i}</option>)}
                    </select>
                )}

                <ChartTypeSelector current={chartType} onChange={setChartType} />
            </div>

            {loading ? (
                <div style={{ textAlign: 'center', padding: '100px', color: '#64748b' }}>Cargando datos del mercado...</div>
            ) : error ? (
                <div style={{ padding: '24px', background: 'rgba(239, 68, 68, 0.1)', border: '1px solid #ef4444', borderRadius: '16px', color: '#ef4444' }}>
                    <strong>Atención:</strong> {error}
                </div>
            ) : data.length > 0 ? (
                <>
                    <MetricsBar data={data} />
                    <StockChart data={data} chartType={chartType} />
                </>
            ) : null}
        </div>
    );
}
