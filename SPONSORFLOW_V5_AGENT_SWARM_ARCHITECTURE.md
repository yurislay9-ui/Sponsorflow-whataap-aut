# 🚀 DOCUMENTO ESTRATÉGICO: TRANSICIÓN A ECOSISTEMA MULTI-AGENTE (SWARM)
**Proyecto:** Sponsorflow (Android Native)
**Versión de Arquitectura:** 5.0 (Inspirada en CLAW-LITE Phoenix Mythos)
**Estado:** Propuesta de Refactorización Aprobada
**Objetivo:** Migrar de un bot monolítico y sistemas aislados a un **Sistema Operativo Agéntico Ligero**, basado puramente en un enjambre de agentes especialistas.

---

## 1. RESUMEN EJECUTIVO

Este documento detalla la reestructuración al 100% del núcleo y sistemas de **Sponsorflow**. El objetivo es abandonar la arquitectura tradicional de "código espagueti" (donde archivos como `Orchestrator.kt`, `CoreEngine.kt`, `SocialMediaPublisher.kt` y `MessageQueueManager.kt` están acoplados y hacen múltiples tareas a la vez) para adoptar un **Ecosistema Multi-Agente (Swarm Architecture) Puro**.

Basado en los principios de *CLAW-LITE "Fénix Edition"*, este nuevo paradigma elimina la enorme cantidad de archivos desorganizados y divide **cada tarea funcional** en un "Micro-Agente" altamente especializado (una clase independiente). Estos agentes colaboran entre sí, se ejecutan en paralelo (usando Corrutinas), consumen mínima RAM y aseguran un mantenimiento a futuro de "cero fricción" (Plug & Play).

---

## 2. ANÁLISIS: CÓDIGO ACTUAL VS. SWARM AGENTS

| Problema Actual (Sponsorflow v4) | Solución Swarm (Sponsorflow v5) | Beneficio |
| :--- | :--- | :--- |
| **Monolitos Cognitivos:** `CoreEngine` tiene toda la lógica de qué responder y cómo analizar (Spintax, Fuzzy, Reglas). | **División Cognitiva:** Un agente para Catálogo, otro para Soporte, otro para Extraer Ventas. | Modificar cómo se vende no rompe cómo se saluda. |
| **Acoplamiento de Acción:** `SocialMediaPublisher` y `MessageQueueManager` son servicios sueltos y difíciles de rastrear. | **Escuadrón de Acción:** Agentes especialistas (`PublisherAgent`, `CommsAgent`) con contratos claros. | Menos archivos, más fáciles de actualizar si cambia una API. |
| **Falta de Validación:** El sistema genera una respuesta y la envía ciegamente. | **Buddy Reviewer:** Un agente auditor (`ReviewerAgent`) revisa todo antes de publicarlo/enviarlo. | Cero errores vergonzosos de cara al cliente. |
| **Mantenimiento Pesado:** `MemoryConsolidatorWorker` y otros workers ensucian el paquete `services`. | **KAIROS Daemon:** Un escuadrón de agentes (`MemoryAgent`, `PrivacyAgent`) gestionan el estado en idle. | Base de datos limpia y rápida automáticamente. |

---

## 3. LA ALINEACIÓN DEL ENJAMBRE (ROSTER DE AGENTES)

El nuevo sistema Sponsorflow se organizará en **4 Escuadrones Estratégicos**, eliminando decenas de archivos viejos y centralizando la inteligencia. Todos los agentes (sin excepción) implementarán una interfaz común (`SponsorflowAgent`).

### 🏛️ ESCUADRÓN 1: DIRECCIÓN Y CONTROL (Routing & Quality)
El cerebro logístico. No generan respuestas finales, controlan el flujo del enjambre.

*   **`RouterAgent` (El Despachador):** 
    *   *Objetivo:* Leer el mensaje entrante (ej. "precio zapatos negros").
    *   *Acción:* No formula respuestas. Analiza la intención (Intent) en milisegundos y decide **qué agentes deben despertar**. En este caso, llama al `CatalogAgent`.
*   **`BuddyReviewerAgent` (El Auditor / Self-Healing):**
    *   *Objetivo:* Ser el último filtro antes de que el teléfono actúe.
    *   *Acción:* Revisa la respuesta propuesta por el escuadrón cognitivo o la publicación propuesta por el escuadrón de acción. *¿Promete un producto sin stock? ¿Falta el nombre para el envío? ¿El link está roto?* Si el Score es bajo, fuerza una corrección interna (Self-Healing).

### 🧠 ESCUADRÓN 2: INTELIGENCIA COGNITIVA (Los Especialistas)
Reemplazan el viejo `CoreEngine` y la IA pesada por lógica atómica ultra-rápida. Trabajan en paralelo.

*   **`CatalogAgent` (Consultor de BD):** 
    *   Su única misión es consultar `Room DB` (Catálogo/Inventario). Conoce precios, stock y descripciones.
*   **`PolicyAgent` (Soporte Técnico):** 
    *   Experto en reglas de negocio. Responde exclusivamente preguntas sobre horarios, ubicación, envíos y garantías basadas en la configuración del usuario.
*   **`OrderParsingAgent` (El Cajero / NER):** 
    *   Analiza el texto del cliente para extraer "Nombre", "Dirección" y "Producto". Su objetivo es armar el `JSON_ORDER` para cerrar la venta.
*   **`SynthesizerAgent` (El Maestro de Ceremonias):**
    *   Recibe la "materia prima" de los agentes anteriores (ej. datos del catálogo + datos de políticas) y ensambla un mensaje de respuesta humano y coherente usando *Spintax*.

### ⚡ ESCUADRÓN 3: ACCIÓN Y COMUNICACIÓN (Los Ejecutores)
Reemplazan los sistemas desorganizados de `services` (SocialMediaPublisher, MessageQueue).

*   **`PublisherAgent` (Community Manager):** 
    *   *Objetivo:* Publicar contenido en redes. 
    *   *Acción:* Le dices: *"Publica esta oferta en Instagram a las 5 PM"*. Él gestiona la pantalla, realiza las interacciones de UI necesarias (Accesibility si es necesario) y reporta el éxito. Aisla toda la lógica de UI automator.
*   **`CommsAgent` (El Cartero):** 
    *   *Objetivo:* Entregar los mensajes de texto a los clientes de forma segura.
    *   *Acción:* Gestiona las pausas (Throttling) y las intenciones de respuesta (ReplyIntents) para evitar que WhatsApp/Instagram bloqueen la cuenta por SPAM. (Reemplaza a `MessageQueueManager`).

### 🧹 ESCUADRÓN 4: DAEMON KAIROS (Mantenimiento Background)
Trabajan "mientras el teléfono duerme" (Idle), reemplazando los WorkerManagers pesados.

*   **`MemoryAgent` (El Consolidador KAIROS):** 
    *   *Acción:* Corre de madrugada. Lee el historial de chat (`Room DB`), purga mensajes inútiles ("hola", "ok") y destila *Insights*: *"A Juan Pérez le interesan descuentos"*. Mantiene la base de datos veloz y el contexto de los clientes perfecto.
*   **`PrivacyAgent` (El Escudo PII):** 
    *   *Acción:* Analiza los flujos de datos entrantes y salientes para enmascarar (blur) contraseñas, números de tarjeta de crédito (16 dígitos) o datos sensibles antes de que se guarden en la base de datos, garantizando cumplimiento de privacidad "Zero-Data-Exfiltration" (Pilar 4 de tu documento).

---

## 4. ANATOMÍA TÉCNICA DEL AGENTE (El Contrato Universal)

Para que el Ecosistema funcione y la adición de futuros agentes no requiera modificar el sistema base, todos implementarán este contrato:

```kotlin
// Contrato Base para todos los agentes del Ecosistema
interface SponsorflowAgent {
    val agentName: String
    val squadron: SquadType // DIRECTION, COGNITIVE, ACTION, KAIROS
    val capabilities: List<String> // Ej: ["catalog_search", "price_check"]
    
    // Método universal de ejecución (asíncrono para paralelismo)
    suspend fun executeTask(taskPayload: AgentTask): AgentResult
}

// Estructura universal de respuesta entre agentes
data class AgentResult(
    val confidenceScore: Double, // 0.0 a 1.0 (Qué tan seguro está el agente)
    val extractedData: Map<String, Any>?, // Datos en crudo descubiertos
    val proposedAction: ActionIntent?, // Respuesta textual o acción a ejecutar
    val status: TaskStatus // SUCCESS, NEEDS_REVIEW, FAILED
)
```

---

## 5. ROADMAP DE IMPLEMENTACIÓN PURA (Refactorización a Agentes)

La transición implicará la eliminación progresiva del código antiguo ("Strangler Fig Pattern") hasta que solo queden agentes.

### 🟡 FASE 1: Foundation (El Cuartel General)
*   Crear la estructura de carpetas `com.sponsorflow.agents` con subcarpetas para cada escuadrón.
*   Crear la interface `SponsorflowAgent` y las clases base (`AgentResult`, `AgentTask`).
*   Construir el `RouterAgent` y conectarlo a las entradas existentes (Notificaciones).

### 🟢 FASE 2: Reemplazo Cognitivo (Adiós CoreEngine)
*   Desmontar el gigantesco `CoreEngine.kt`.
*   Migrar su lógica a los nuevos `CatalogAgent`, `PolicyAgent`, y `OrderParsingAgent`.
*   Implementar el `SynthesizerAgent` para unificar respuestas.
*   *Hito:* Todo el procesamiento de texto ahora es Multi-Agente (Swarm en paralelo).

### 🔵 FASE 3: Reemplazo de Acción & Auditoría (Adiós Services desordenados)
*   Desmontar `SocialMediaPublisher.kt` y `MessageQueueManager.kt`.
*   Migrar su funcionalidad a `PublisherAgent` y `CommsAgent`.
*   Implementar el `BuddyReviewerAgent` para auditar todo lo que el sistema intenta publicar o enviar.

### 🟣 FASE 4: Reemplazo KAIROS (Adiós Workers pesados)
*   Desmontar `MemoryConsolidatorWorker` y similares.
*   Implementar el `MemoryAgent` y el `PrivacyAgent` operando bajo la lógica del Daemon KAIROS (ejecución inteligente en idle).
*   *Hito Final:* El código Monolítico ha dejado de existir. Sponsorflow es 100% un Sistema Operativo de Agentes.

---

## 6. CONCLUSIÓN ARQUITECTÓNICA

Al aplicar esta estructura de 4 Escuadrones, Sponsorflow reduce drásticamente las líneas de código repetidas, elimina la dependencia de archivos fuertemente acoplados, y logra una organización impecable. Si mañana se requiere que el sistema reserve citas médicas, simplemente se agregará un `CalendarAgent` al Escuadrón Cognitivo, sin necesidad de reprogramar ni una sola línea del `RouterAgent` o del sistema de mensajería. Esto es verdadero software de "Grado Frontera".
