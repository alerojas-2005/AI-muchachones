# Resultados de las pruebas RAG

Documento: Reglamento Estudiantil, Universidad El Bosque (Acuerdo 18573 de 2026), 43 páginas.
Modelo de chat: llama3 (Ollama, CPU). Embeddings: nomic-embed-text (768 dim). Base vectorial: PGVector.

**Criterio de calificación (usarlo igual siempre):** Sí = 1, Parcial = 0,5, No = 0.
**Tiempo:** el valor "ms en el modelo" que muestra el chat debajo de cada respuesta.
**Tipo de fallo:** recuperación (el artículo correcto no llegó al prompt) o generación (llegó, pero el modelo respondió mal).

## Preguntas y respuestas esperadas

| # | Pregunta (idéntica en todas las rondas) | Respuesta esperada | Referencia |
|---|---|---|---|
| 1 | ¿Qué porcentaje de inasistencias hace que un estudiante pierda una asignatura por fallas? | 20 % o más de las sesiones obligatorias (fracciones se redondean al entero superior) | Art. 24.1, pág. 12 |
| 2 | ¿Cuál es la nota mínima para aprobar una asignatura en pregrado y en posgrado? | Pregrado 3,0. Posgrado 3,5 | Art. 29, pág. 14 |
| 3 | ¿Qué promedio debe tener un estudiante de pregrado para no entrar en prueba académica, y qué otra situación también lleva a prueba académica? | Promedio ponderado por período y acumulado igual o mayor a 3,3. También perder el 60 % o más de los créditos cursados en un período | Arts. 32.2 y 33, págs. 16-17 |
| 4 | Si no estoy de acuerdo con la nota que me puso un profesor, ¿cuántos días tengo para objetarla y ante quién? | 3 días hábiles, por escrito, ante el mismo docente | Art. 31, pág. 16 |
| 5 | Si me transfiero desde otra universidad, ¿qué parte de la carrera tengo que cursar obligatoriamente en El Bosque? | Mínimo el 50 % del total de créditos del programa | Art. 10, pág. 6 |
| 6 | ¿Qué requisitos debo cumplir para obtener mi título? | 100 % de los créditos, demás requisitos académicos y administrativos del Consejo Directivo, y sin sanciones disciplinarias en ejecución | Art. 41, pág. 20 |
| 7 | ¿Cuánto cuesta la matrícula de un semestre de Ingeniería de Sistemas? | No está en el documento (solo hay porcentajes, ningún valor en pesos) | Fuera del documento |

Nota: la P7 se formuló con "Ingeniería de Sistemas" (no "Medicina") y se mantuvo igual en todas las rondas.

---

## Resumen comparativo

### Equipos usados

| Rondas | Persona | Equipo | Procesador | RAM |
|---|---|---|---|---|
| Sin RAG, A, A2, B, C (P1) | Maida | Lenovo IdeaPad Slim 3 15ARP10 | AMD Ryzen 7 7735HS (8 núcleos, 16 hilos) | 24 GB |
| Calibración, C, D | Paula | Lenovo ThinkCentre M720q | Intel Core i7-8700T (6 núcleos, 12 hilos, 2,4 GHz) | 8 GB |

Los puntajes de calidad son comparables entre equipos; los tiempos no, porque el segundo equipo tiene menos núcleos y un tercio de la RAM. Por eso se midió una calibración con la configuración de A2 en el equipo de Paula.

| Ronda | Chunk | topK | Umbral | Plantilla | Fragmentos | Correctas /7 | Tiempo prom. | Estado |
|---|---|---|---|---|---|---|---|---|
| Sin RAG | - | - | - | sin advisor | - | 0 | ~20 s | Completa |
| A | 500 | 4 | 0,5 | v1 (estricta) | 55 | 3,5 | ~122 s | Completa |
| A2 | 500 | 4 | 0,5 | v2 (suavizada) | 55 | 2,0 | ~190 s | Completa |
| B | 250 | 4 | 0,5 | v2 | 112 | 2,0 | ~87 s | Completa |
| C | 500 | 6 | 0,5 | v2 | 55 | No medido | P1: ~301 s | Parcial (limitación de hardware) |
| D | 500 | 4 | 0,7 | v2 | 55 | No medido | No medido | Parcial (limitación de hardware) |

Comparaciones limpias (una sola variable cambia):
- A frente a A2: efecto de la plantilla (v1 a v2). Reto 1.
- A2 frente a B: efecto del tamaño de fragmento (500 a 250).
- A2 frente a C: efecto de topK (4 a 6).
- A2 frente a D: efecto del umbral (0,5 a 0,7).

Variabilidad: los tiempos dependen de la carga del equipo (la misma configuración A tardó ~122 s y A2 ~190 s; la carga del PDF pasó de 34 s a 58 s). Con una sola ejecución por pregunta y temperatura 0,7, diferencias de 1 punto pueden ser azar.

---

## Ronda SIN RAG (línea base)

Método: chat sin documento ni recuperación, solo conocimiento de llama3.
Resultado global: 0 / 7. Tiempo promedio ~20 s (139.546 ms en total).

| # | Respuesta (resumen) | Tiempo (ms) | Calif. | Observación |
|---|---|---|---|---|
| 1 | "20-25 %", genérico, depende de la institución | 42742 | No | Respuesta vaga, no del reglamento |
| 2 | Escala española 4-4,5 / 5-5,5 sobre 10 | 16832 | No | Escala equivocada |
| 3 | Promedio mínimo 2,5 y "No Aprobado" | 12085 | No | Inventado |
| 4 | 5 días hábiles, profesor o jefe de departamento | 17895 | No | Inventado |
| 5 | Módulos genéricos, sin dar el 50 % | 24459 | No | Genérico |
| 6 | Lista genérica ("2.0 o C+") | 19771 | No | No es el Art. 41 |
| 7 | Inventa "$12,000 MXN" | 5762 | No | Alucinación |

Conclusión: sin RAG el modelo generaliza o inventa, incluso con valores que parecen plausibles.

---

## Ronda A: chunk 500, topK 4, umbral 0,5, plantilla v1

Fragmentos indexados: 55 (34475 ms). Puntaje: **3,5 / 7**. Tiempo promedio: ~122 s (857.102 ms en total).

| # | Respuesta (resumen) | Tiempo (ms) | Calif. | Tipo de fallo |
|---|---|---|---|---|
| 1 | Art. 24.1, 20 %, pero dice "más del 20 %" y mezcla sujeto con el Art. 57 | 133832 | Parcial | Generación |
| 2 | 3,0 y 3,5 correctos | 116821 | Sí | - |
| 3 | Dice "menor a 3,3" (invertido); otra causa inventada (afiliación a salud) | 123931 | No | Generación |
| 4 | 3 días hábiles correcto, pero cita Art. 57 y no dice ante quién | 124580 | Parcial | Generación |
| 5 | No da el 50 %, cita Art. 55 | 119566 | No | Recuperación |
| 6 | 100 % de créditos y requisitos del Art. 41, con línea contradictoria "no encuentro..." | 123111 | Sí | - |
| 7 | No inventa cifra pero responde con el 30 % de matrícula reducida | 115261 | No | Generación |

Observación: prompt de ~2.100 tokens (2064 en la primera prueba).

---

## Ronda A2: chunk 500, topK 4, umbral 0,5, plantilla v2

Fragmentos indexados: 55 (58674 ms). Puntaje: **2,0 / 7**. Tiempo promedio: ~190 s (1.331.769 ms en total).

| # | Respuesta (resumen) | Tiempo (ms) | Calif. | Tipo de fallo |
|---|---|---|---|---|
| 1 | Art. 24.1, 20 %, pero "más del 20 %" | 190059 | Parcial | Generación |
| 2 | 3,0 y 3,5 | 181312 | Sí | - |
| 3 | "Menor a 3,3" (invertido); otra causa vaga | 176343 | No | Generación |
| 4 | 5 días naturales, ante el Decano, cita Art. 80 | 188319 | No | Recuperación (Art. 31 no llegó) |
| 5 | Habla de movilidad (Art. 47) y dice que no hay información | 229776 | No | Recuperación (Art. 10 no llegó) |
| 6 | Art. 41: 100 % y requisitos del Consejo; omite sanciones; añade "no encuentro" | 183630 | Parcial | Generación |
| 7 | Responde con matrícula reducida (30 %) | 182330 | No | Generación |

---

## Ronda B: chunk 250, topK 4, umbral 0,5, plantilla v2

Fragmentos indexados: 112 (31276 ms). Puntaje: **2,0 / 7**. Tiempo promedio: ~87 s (610.498 ms en total).

| # | Respuesta (resumen) | Tiempo (ms) | Calif. | Tipo de fallo |
|---|---|---|---|---|
| 1 | "Igual o superior al 20 %", Art. 24 | 79222 | Sí | - |
| 2 | Pregrado 3,0 correcto; posgrado 3,0 (incorrecto) | 71576 | Parcial | Recuperación/generación |
| 3 | No da 3,3; otra causa: Art. 81 (inventada) | 100430 | No | Recuperación |
| 4 | 15 días calendario, Art. 51 | 87793 | No | Recuperación |
| 5 | "Curso Básico (Art. 12)" | 87944 | No | Recuperación |
| 6 | 100 % de créditos; omite sanciones; añade "nivel solicitado" (Art. 39) | 93628 | Parcial | Generación |
| 7 | Responde con el 70 % de la matrícula | 89905 | No | Generación |

Lectura: fragmentos más pequeños dieron prompts más cortos y respuestas más rápidas (~87 s frente a ~190 s), pero no mejoraron la calidad (2,0 igual que A2). Un artículo se parte en varios trozos y cada uno pierde contexto.

---

## Calibración en el segundo equipo (configuración A2)

Antes de las rondas C y D se midió el rendimiento del segundo equipo (ThinkCentre M720q, 8 GB de RAM) con la misma configuración de A2 (chunk 500, topK 4, umbral 0,5, plantilla v2), para poder comparar tiempos entre equipos.

| Medición | Equipo 1 (24 GB) | Equipo 2 (8 GB) |
|---|---|---|
| Indexación del PDF (55 fragmentos) | 34 a 58 s | 39 a 153 s |
| P1 con configuración A2 | 190 s | Más de 600 s (superó el límite del frontend) |
| Respuestas completadas en la sesión de prueba | 7 de 7 | 0 |

Configuración verificada en el log del backend: `RAG config: topK=4 threshold=0.5` y `chunkSize=500`.

**Conclusión:** con la misma configuración, el segundo equipo es al menos 3,2 veces más lento (más de 600 s frente a 190 s) y no completó ninguna respuesta durante la sesión de prueba.

### Estimación de tiempos en el segundo equipo (no medida)

La estimación toma los tiempos medidos en el equipo 1 y los multiplica por el factor mínimo de lentitud del equipo 2 (3,2 = 600 s / 190 s). Como el equipo 2 superó los 600 s, el factor real es igual o mayor, así que estos valores son un mínimo.

| Ronda | Tiempo medido en el equipo 1 (por pregunta) | Tiempo estimado en el equipo 2 (por pregunta) | Tiempo estimado de la ronda completa (7 preguntas) |
|---|---|---|---|
| A2 (topK 4, umbral 0,5) | ~190 s | ≥ 10 min | ≥ 1 h 10 min |
| B (chunk 250) | ~87 s | ≥ 4,6 min | ≥ 32 min |
| C (topK 6) | ~301 s (P1) | ≥ 16 min | ≥ 1 h 52 min |
| D (umbral 0,7) | Entre ~20 s (sin contexto, como sin RAG) y ~190 s | Entre ≥ 1 min y ≥ 10 min | Entre ≥ 7 min y ≥ 1 h 10 min |

Para D se da un rango: si ningún fragmento pasa el umbral, el prompt queda casi del tamaño de la ronda sin RAG y el tiempo se acerca al límite inferior; si pasa alguno, se acerca al de A2.

Completar las rondas C y D en el equipo 2 habría requerido, como mínimo, entre 2 y 3 horas de inferencia continua.

### Problemas presentados en el segundo equipo

1. **Memoria insuficiente.** Docker usa por defecto la mitad de la RAM (4 GB), y llama3 necesita entre 5 y 6 GB. Se aumentó a 6 GB con un archivo `.wslconfig` (`memory=6GB`, `swap=8GB`), pero Windows quedó con unos 2 GB y el sistema recurrió al disco como memoria de intercambio, lo que vuelve la inferencia mucho más lenta.
2. **Inferencia solo en CPU.** El equipo no tiene GPU dedicada; llama3 usa todos los núcleos al 100 % durante la generación.
3. **Límite de espera.** El frontend tenía `TIMEOUT_MS = 300000` (300 s). Se subió a 600 s en el frontend (`TIMEOUT_MS = 600000`) y en el backend (`spring.http.client.read-timeout=600s`), y aun así las respuestas superaron ese límite.
4. **Configuración por variables de entorno.** El cambio de umbral solo se aplica si la variable `RAG_SIMILARITY_THRESHOLD` se define en la misma ventana de PowerShell y se recrea el backend con `--force-recreate`; hubo que verificarlo en el log antes de cada ronda.

---

## Ronda C: chunk 500, topK 6, umbral 0,5, plantilla v2

**Estado: parcial, por limitación de hardware.**

### Datos medidos

| Medición | Equipo | Resultado |
|---|---|---|
| P1 con topK 6 | Equipo 1 (24 GB) | 301.424 ms; la respuesta llegó 1,4 s después del límite de 300 s del frontend |
| P1 con topK 4 (A2) | Equipo 1 (24 GB) | 190.059 ms |

Pasar de topK 4 a topK 6 aumentó el tiempo de la P1 en un 58 % en el mismo equipo. En el segundo equipo, que ya superaba los 600 s con topK 4, la ronda no se pudo completar.

### Estimación (no medida)

El tiempo estimado multiplica el tiempo medido en el equipo 1 por el factor mínimo de lentitud del equipo 2 (3,2).

| # | Tiempo estimado en el equipo 2 | Resultado esperado y justificación |
|---|---|---|
| 1 | ≥ 16 min (301 s × 3,2) | Se espera "20 %" con el Art. 24, como en A y A2; el error "más de" en lugar de "igual o superior" es de generación y no depende de topK |
| 2 | ≥ 16 min | Se espera 3,0 y 3,5, correcto en A y A2 |
| 3 | ≥ 16 min | Más fragmentos pueden traer el Art. 33 (60 % de créditos), pero el error de invertir la condición del 3,3 es de generación |
| 4 | ≥ 16 min | Posible mejora: el Art. 31 no llegó en A2 y con 6 fragmentos tiene más probabilidad de recuperarse; riesgo de confusión con otros plazos (Arts. 27, 51 y 77) |
| 5 | ≥ 16 min | Posible mejora: el Art. 10 no llegó en A2; con 6 fragmentos tiene más probabilidad de entrar al contexto |
| 6 | ≥ 16 min | Se espera Parcial, como en A2 |
| 7 | ≥ 16 min | Se espera que siga respondiendo con porcentajes (Arts. 13 y 18): más contexto trae más porcentajes de matrícula |

**Tiempo total estimado de la ronda en el equipo 2:** más de 1 hora y 50 minutos.

**Riesgo adicional:** con 6 fragmentos de unos 500 tokens, el prompt ronda los 3.000 tokens. Sumado a `num-predict=1024`, queda cerca del límite de `num-ctx=4096`, y Ollama puede recortar el prompt sin avisar.

---

## Ronda D: chunk 500, topK 4, umbral 0,7, plantilla v2

**Estado: parcial, por limitación de hardware.**

### Datos medidos

| Medición | Resultado |
|---|---|
| Configuración aplicada (log del backend) | `RAG config: topK=4 threshold=0.7`, `chunkSize=500` |
| Indexación del PDF | 55 fragmentos en 44.362 ms |
| Respuestas completadas | 0 (la generación no terminó dentro del tiempo de la prueba) |

### Base para la estimación

En los logs de las rondas anteriores, los fragmentos recuperados tuvieron scores entre 0,65 y 0,67 (por ejemplo, el fragmento del Art. 24 con `score=0.6552895307540894`). Con un umbral de 0,7, esos fragmentos no pasarían el filtro.

### Estimación (no medida)

| # | Resultado esperado y justificación |
|---|---|
| 1 | "No encuentro esta información en el documento": el Art. 24 tuvo score 0,655, menor que 0,7 |
| 2 | Igual: sin contexto, el modelo no tiene de dónde responder |
| 3 | Igual |
| 4 | Igual |
| 5 | Igual |
| 6 | Igual |
| 7 | "No encuentro...": sería la única respuesta "correcta", pero por falta de contexto, no porque el modelo reconozca que el dato no está |

**Puntaje esperado:** cerca de 1/7, y por la razón equivocada.

**Tiempo esperado:** menor que en A2, porque el prompt queda casi vacío (solo la pregunta y las instrucciones).

---

## Conclusiones del Reto 2

1. **Tamaño de fragmento (A2 frente a B, medido):** bajar de 500 a 250 tokens redujo el tiempo de ~190 s a ~87 s, pero no mejoró la calidad (2,0/7 en ambas). Un artículo partido en trozos pequeños pierde contexto.
2. **topK (A2 frente a C, medido parcialmente):** pasar de 4 a 6 fragmentos aumentó el tiempo de la P1 en un 58 %. Puede mejorar la recuperación de artículos que no llegaban (10 y 31), pero no corrige los errores de generación y acerca el prompt al límite del contexto.
3. **Umbral (A2 frente a D, estimado con los scores medidos):** con scores de 0,65 a 0,67, un umbral de 0,7 dejaría al modelo sin contexto. Un umbral demasiado alto anula el RAG.
4. **Hardware (medido):** el mismo sistema que en un equipo de 24 GB responde en unos 3 minutos, en uno de 8 GB no completó ninguna respuesta con más de 10 minutos de espera. Para usar RAG con llama3 en CPU hacen falta al menos 16 GB de RAM, o una GPU.

---

## Reto 1: plantilla del prompt (evidencia)

| Versión | Comportamiento observado |
|---|---|
| Plantilla por defecto del advisor (inglés) | Respuestas con "According to the Reglamento...", P1 con "más de 20 %" (155 s, prompt de 2064 tokens) |
| Plantilla propia v1, tabla vacía | "No encuentro esta información en el documento." (correcto, no había documento) |
| Plantilla propia v1, con documento (primera prueba) | P1: "No encuentro la respuesta en el documento." (falso negativo, el Art. 24 sí estaba en el contexto; 122 s) |
| Plantilla v1, ronda A | 3,5 / 7 |
| Plantilla v2 (suavizada), ronda A2 | 2,0 / 7 |

Conclusión: suavizar la regla de rechazo no mejoró los resultados con llama3 8B; la P7 no se rechazó en ninguna ronda con documento cargado, porque el contexto recuperado trae porcentajes de matrícula (Arts. 13 y 18) y el modelo los usa como respuesta. Mejora posible: una regla explícita para datos que no aparecen literalmente (precios, fechas, cifras), o un modelo más grande.

## Reto 3: evidencia de fuentes

**Estado: no implementado.** El diseño previsto: usar `.call().chatClientResponse()` en lugar de `.content()`, leer los documentos de `QuestionAnswerAdvisor.RETRIEVED_DOCUMENTS`, tomar el metadato `fuente` (guardado en la ingesta) y un extracto de cada fragmento, extender el DTO de respuesta y mostrarlo en el frontend. Los metadatos ya se guardan correctamente: el log del advisor muestra `source=reglamento-estudiantil-2026.pdf` y `fuente=reglamento-estudiantil-2026.pdf` en cada fragmento recuperado.

---

## Limitaciones observadas

- Respuestas lentas en CPU (2 a 5 min con RAG frente a ~20 s sin RAG en el equipo de 24 GB; más de 10 min en el de 8 GB), y el costo crece con `topK`.
- Dependencia fuerte del hardware: con 8 GB de RAM no fue posible completar respuestas.
- Errores de generación: invierte condiciones ("menor a 3,3"), cambia "igual o superior" por "más de".
- Fallos de recuperación: los artículos 10 y 31 no llegan al contexto en varias rondas.
- Confusión entre artículos con datos parecidos: varios plazos (3, 5 y 15 días) y varios porcentajes de matrícula.
- Chunking sin solapamiento: un artículo puede quedar partido (el Art. 24 llegó con el inicio de la frase cortado).
- Texto sucio de Tika: se cuelan letras sueltas del margen vertical del PDF ("@ U N I V E R S I D A D...").
- No rechaza bien preguntas fuera del documento (P7).
- Una sola ejecución por pregunta con temperatura 0,7: los puntajes tienen ruido.

**Mejoras propuestas:** chunking con solapamiento o por artículo, reranking, limpiar el texto extraído, mostrar la página de origen, temperatura 0 para los experimentos y un modelo más liviano o GPU para equipos con poca memoria.

## Capturas y logs

En `evidencias/`: capturas de la carga del documento, de las respuestas y de los logs del backend, y el PDF de evidencia del segundo equipo (`evidencia_paula.pdf`).
