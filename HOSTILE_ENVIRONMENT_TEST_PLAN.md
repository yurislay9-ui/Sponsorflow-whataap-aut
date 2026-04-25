# 🛡️ PLAN DE PRUEBAS QA: Entorno de Ejecución Hostil (HOSTILE_ENVIRONMENT_TEST_PLAN)

Como Ingeniero especializado en Sistemas Distribuidos y QA, he diseñado la siguiente batería de ataques para evaluar nuestro `Local First Architecture` frente a un dispositivo "Basura" o "Catastrófico". Sponsorflow debe poder mantenerse a flote incluso en un teléfono barato con condiciones infrahumanas.

---

## 1. ESTRANGULAMIENTO DE CPU (CPU Throttling al 90%)
El enemigo es el temido "Jank" (congelamiento de UI).
*   **Vector de Prueba:** Forzaremos al procesador de Android a trabajar bajo estrés simulado.
*   **Cómo Ejecutar (Herramientas Externas):**
    1.  Abre el **Android Profiler** en Android Studio.
    2.  Navega a la sección `CPU`. Activa el rastro de la aplicación.
    3.  Abre una terminal y ejecuta el comando de tortura ABD: `adb shell "while true; do cat /dev/urandom > /dev/null; done"` (Esto satura deliberadamente los núcleos del SoC).
    4.  Navega a la pestaña de **CRM** y de **Pedidos** deslizando *LazyColumns*.
*   **Criterio de Éxito:** Sponsorflow no colapsa porque la vista está desarrollada con *Jetpack Compose*. Las Listas diferidas *LazyColumn* solo renderizan los objetos visibles, ignorando el resto del DOM. *Sin embargo*, para que Compose no malgaste ciclos CPU, **hemos implementado llaves únicas (`key`)** en las iteraciones para ayudar a Compose a no recalcular listas bajo esta presión de procesador.

---

## 2. PÉRDIDA DE RED Y LATENCIA (5,000ms & Dropped Packets)
El enemigo es el *Estado Zombie* y operaciones *Double-Billing*.
*   **Vector de Prueba:** El cliente camina por un túnel e intenta subir una campaña o procesar órdenes por IA mientras el Wi-Fi parpadea en `Edge`.
*   **Cómo Ejecutar (Herramientas Externas):**
    1.  Abre un Emulador de Android oficial y ve al panel **Extended Controls -> Cellular**.
    2.  Configura *Network Speed* en **Edge** o GPRS.
    3.  Configura *Signal Strength* en **Poor**.
    4.  Como alternativa, usar *Toxiproxy* en el host de tu ordenador.
*   **Criterio de Éxito:** Sponsorflow sobrevive y responde con integridad absoluta gracias al código Resiliente (Test `ResilienceTest.kt` y `Order Duplication Idempotency`) insertado en los pasos anteriores:
    1. El motor *Exponential Backoff* asume el timeout tras fallar una promesa y re-intenta hasta 3 veces automáticamente guardando el historial temporal si se recupera.
    2. Eventuales órdenes que pudiesen chocar en el vacío, están protegidas por el componente *Idempotency SQLite*. El cobro no se realizará dos veces.

---

## 3. ASFIXIA DE MEMORIA (Low Memory Killer Test)
El enemigo principal son las "Fugas de Memoria (Memory Leaks)".
*   **Vector de Prueba:** Empujar a Android a niveles donde el sistema operativo entra en Pánico y mata aplicaciones indiscriminadamente (LMK).
*   **Cómo Ejecutar (Herramientas Externas):**
    1. Lanza la aplicación Sponsorflow en un emulador o equipo físico de bajos recursos.
    2. Usa la consola de tu computadora para engañar al sistema obligándolo a creer que no hay RAM:
       `adb shell am send-trim-memory com.sponsorflow RUNNING_CRITICAL`
    3. Realiza Swipes salvajes deslizando todas las pestañas velozmente de izquierda a derecha.
*   **Manejo Interno Optimizado:** He configurado un **Garbage Collector Nivel OS** inyectándolo directamente en el Master Activity de Sponsorflow. Cuándo el Sistema Operativo entre en la alerta `RUNNING_CRITICAL`, Sponsorflow escuchará el grito del sistema y cortará en seco un `System.gc()` junto a la purga volátil de los HashMap del *AntiSpamGuardian*, ahorrando hasta 80 MB de RAM y salvando la interfaz de ser asesinada abruptamente por Android.

---
### 🛠️ IMPLEMENTACIONES A NIVEL DE CÓDIGO
Para garantizar que estas pruebas logren sus objetivos sin destruir al usuario, he adaptado la Arquitectura ahora mismo con:
1.  **LazyColumn "Keying" Constrain:** Las litas ya no pierden el control y se reensamblan. Ahora la Memoria caché de UI es diferida porque `key={it.senderId}` o `key={it.id}` informa al Virtual DOM qué elementos ignorar al hacer Scroll.
2.  **OS Memory Listener (ComponentCallbacks2):** El puente vitalicio que se comunica con el Kernel de Android está registrado y vivo en la aplicación para vaciar la sangre de la memoria no crítica y sobrevivir a las tormentas.
