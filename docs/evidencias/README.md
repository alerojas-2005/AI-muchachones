# Evidencias del taller de agentes con Spring AI + Ollama

Equipo: Paula Alejandra Arias Buitrago, Maida Alexandra Rojas Morato y Jesús Daniel Torralba Cárdenas.
Modelo: `qwen2.5:3b` (Ollama), temperatura 0. Bitácora: [`../bitacora.pdf`](../bitacora.pdf).

| Archivo | Qué contiene |
|---|---|
| `salida_gradle_test.txt`, `reportes_junit/` | Pruebas JUnit de las herramientas y del reto: 14 de 14 aprobadas (BUILD SUCCESSFUL). |
| `resultados_api_springai.csv`, `resultados_api_springai.json` | Las seis preguntas y el «confirmo» enviados a `POST /api/v1/chat` con `scripts_pruebas/ejecutar_pruebas.ps1`: respuesta, tiempo y estado HTTP. |
| `log_backend_springai.txt` | Log del backend durante esa ejecución: decisiones del supervisor (`[SUPERVISOR] -> ...`) y llamadas a herramientas. |
| `consola_tiempos_seis_preguntas.png`, `consola_confirmo_p5_p6.png` | Pantallazos de la consola con los tiempos y las respuestas. |
| `frontend_p3_p4.png`, `frontend_confirmo_res001.png` | Ejecución desde el frontend (http://localhost:4200): preguntas 3 y 4 y confirmación humana con código RES-001. |

## Cómo reproducir

```powershell
docker compose -f docker\docker-compose.yml up -d
powershell -NoProfile -ExecutionPolicy Bypass -File scripts_pruebas\ejecutar_pruebas.ps1 -Backend springai -Salida docs\evidencias
```

Resultado por API: 1 correcta, 2 parciales y 3 incorrectas; promedio de 146,5 s (63,1 s sin la primera llamada, que cargó el modelo).
En el frontend, la pregunta 4 completó el flujo de reserva con confirmación humana (RES-001).
