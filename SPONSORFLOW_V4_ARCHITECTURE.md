# SPONSORFLOW V4.0 "FÉNIX EDITION" 
## Especificación de Arquitectura Empresarial y Manual Estratégico (2026)

---

## 1. RESUMEN EJECUTIVO (EL REINICIO CONCEPTUAL)
Sponsorflow v4.0 representa un salto evolutivo en la automatización de WhatsApp, Facebook e Instagram para negocios B2B y B2C. 

Tras descartar la dependencia de modelos de Inteligencia Artificial pesados (LLMs locales como Llama/Qwen que consumen gigabytes de RAM y drena batería), el sistema ha sido reconstruido bajo la **Arquitectura Híbrida Determinista (RAG-M)**. Ahora es **100% Nativo en Android (Kotlin)**, opera con una latencia de **< 2 milisegundos**, consume un **0% de batería adicional**, es inmune a las "alucinaciones" de la IA y es capaz de instalarse en los dispositivos más económicos del mercado sin sufrir cuelgues (crashes).

El enfoque de Sponsorflow v4.0 no es simular consciencia, sino ejercer una automatización industrial infalible, combinando un **Motor de NLP Ligero (Fuzzy Matching)**, flujos de ventas automatizados (Kanban), y **Controladores del Sistema Operativo de Android** (`AccessibilityService` e `Intent.ACTION_SEND`) para crear un "vendedor mecánico" perfecto y blindado contra baneos de Meta.

---

## 2. SUBSISTEMAS PRINCIPALES DEL SISTEMA

### A. MOTOR CENTRAL DE RESPUESTA (`CoreEngine.kt`)
Sustituto de la IA. Analiza las intenciones del cliente y dispara respuestas usando algoritmos matemáticos en lugar de redes neuronales, garantizando cero latencia.

*   **Nivel 1 (Regex & Exact Match):** Filtrado ultrarrápido de palabras clave comerciales ("precio", "catálogo", "comprar").
*   **Nivel 2 (Algoritmo de Levenshtein - Fuzzy Logic):** Capaz de detectar intenciones incluso si el cliente comete horrores ortográficos (ej. "kiero preZio de los sapatos" asimila a `Intent.PRICE` con 87% de similitud).
*   **Nivel 3 (Motor Spintax):** Para evitar sonar robótico, las plantillas de respuesta utilizan *Spintax* (ej. `{¡Hola|Qué tal|Saludos} {nombre}!`). Cada mensaje enviado es gramaticalmente distinto, evadiendo los filtros anti-spam de WhatsApp.

### B. ESCUDO TÁCTICO ANTI-BAN (`AntiSpamGuardian.kt`)
Protección de Nivel Kernel contra la desactivación de tu cuenta de WhatsApp / Instagram.

*   **Cooldown Térmico (Anti-Metralleta):** Obliga al bot a esperar 2.5 segundos mínimos entre mensajes, simulando la velocidad de tipeo de un ser humano.
*   **Caja de Castigo (Penalty Box):** Si interactúa con otro bot o un troll (más de 6 mensajes en 1 minuto), el Guardián aísla a ese usuario. Sponsorflow lo ignorará categóricamente durante 5 minutos, rompiendo los bucles infinitos que causan el baneo comercial en Meta.

### C. MÁQUINA DE BOMBARDERO OMNI-CANAL (`SocialMediaPublisher.kt`)
El módulo de marketing proactivo más agresivo del mercado para publicar a través de múltiples cuentas simuladas o clonadas.

*   **Escaneo de Clones:** En lugar de lanzar la publicación a una App física, pide una "Palabra Clave" (ej: *instagram*). Usando el `PackageManager` de Android, escanea e identifica la App Original, las versiones Lite, y clones de terceros (Dual Messenger, Parallel Space).
*   **Ráfaga Secuencial Segura:** Envía el contenido a Clon #1 ➔ Espera 12 segundos ➔ Envía al Clon #2 ➔ Espera 12 segundos ➔ repite. Evita que el sistema operativo se congele por exceso de intents simultáneos.
*   **Mano Fantasma (`AccessibilityMimic.kt`):** El Agente Biomecánico que espera sigilosamente la aparición de los botones "Publicar", "Compartir", "Tweet" o "Crear" en el clon, hace el clic táctil de forma autónoma, y ordena el retroceso a la Pantalla de Inicio (`GLOBAL_ACTION_HOME`).

---

## 3. SISTEMA CRM Y EMBUDO DE VENTAS (AUTOMATIZADO)
Sponsorflow reemplaza la lista plana de clientes con un **Dashboard estilo Kanban** con inteligencia de auto-movimiento.

*   **Fases del Embudo:** `NUEVO` ➔ `NEGOCIANDO` ➔ `CASI CIERRE` ➔ `COMPRÓ`.
*   **Pipeline Automático (`Orchestrator.kt`):** 
    *   El cliente pide catálogo = Pasa a `NEGOCIANDO`.
    *   El cliente envía palabras clave de ubicación ("calle", "avenida") = Pasa a `CASI CIERRE`. 
    *   El embudo dispara Notificaciones Android en tiempo real para que el Operador Humano (Dueño) intervenga en el último milímetro de la venta.

### D. REPORTE EJECUTIVO Y ANTI-GHOSTING (`DailyExecutiveWorker`)
*   Se ejecuta todos los días a través del `WorkManager`.
*   **Radar de Ghosting:** Busca clientes estancados en Negociación por más de 48 horas y alerta al dueño.
*   **Métricas Financieras:** Suma los ingresos diarios y cierra caja reportando la liquidez en una notificación formal.

---

## 4. GESTIÓN DE MEMORIA Y ALTO RENDIMIENTO (OOM Prevention)

### A. KAIROS DAEMON: MANTENIMIENTO NOCTURNO (`MemoryConsolidatorWorker.kt`)
Sponsorflow almacena todo lo que el cliente escribe durante el día. Si no se limpia, la base de datos (SQLite/Room) colapsaría.
*   **Extracción Determinista:** Durante la madrugada (o forzado vía botón), KAIROS entra en acción.
*   **Etiquetado Semántico (Tagging):** Si en el hilo el cliente dijo "excelente" o "estafa", extrae los Tags `[Satisfecho]` o `[Riesgo Alto]` matemáticamente, sin la impredecibilidad de una IA.
*   **Compresión de Memoria Rápida:** Una vez que absorbió el estado actual del cliente comercial, KAIROS **borra (PURGA)** todo el texto crudo del chat (`unconsolidatedHistory`), manteniendo el teléfono de 2GB de RAM corriendo como si fuera nuevo, sesión tras sesión.

### B. ENERGÍA INMORTAL (ANTI-DOZE KERNEL BYPASS)
Mitigación del "Modo Doze" de Android (muy agresivo en móviles como Xiaomi, Huawei o Samsung, que matan apps de fondo para ahorrar batería).
*   Sponsorflow invoca `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`.
*   El teléfono aloja a la aplicación en la **Whitelist del Kernel**, asegurando un Uptime del 99.9% para escuchar notificaciones e intercepciones de clientes.

---

## 5. CAPA DE SEGURIDAD EMPRESARIAL
A diferencia de sistemas SaaS Cloud, **Sponsorflow es TIER 1 ZERO-DATATRANSFER (Zero-Cloud)**.

1.  **Encriptación Local:** Base de datos nativa `Room` (`CustomerEntity`, `OrderEntity`). Ningún chat viaja a la API de OpenAI ni de Google. El servidor de Meta no puede auditar las ventas porque ocurren dentro de la matriz de memoria local.
2.  **Sala de Administración Condicional:** Limitada por el `DeviceUUID` del celular, cuenta con licencia que expira y deniega servicios a intrusos que logren copiar el APK a otro hardware no autorizado, manteniendo la monetización del creador segura.
3.  **Configuración Institucional:** Pantalla de "Cerebro logístico" para insertar Reglas de Negocio dinámicas y plantillas de proveedores sin hardcodear el sistema.

---
*Fin del Documento de Especificación V4.0.*
*Fecha de Expedición: Abril 2026. Target Build: ARM-v8a / Android 14+ Optimizado.*
