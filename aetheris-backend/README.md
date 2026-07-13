<div align="center">

# AETHERIS — Backend

### API REST de Control Financiero Corporativo

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/17/)
[![MariaDB](https://img.shields.io/badge/MariaDB-003545?style=for-the-badge&logo=mariadb&logoColor=white)](https://mariadb.org/)
[![Railway](https://img.shields.io/badge/Railway-0B0D0E?style=for-the-badge&logo=railway&logoColor=white)](https://railway.app/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg?style=for-the-badge)](LICENSE)

**API REST corporativa para gestión de transacciones financieras, conciliación bancaria, presupuesto y auditoría — con chatbot financiero ARIA integrado.**

[Producción](#-estado-de-producción) • [Endpoints](#-referencia-de-la-api) • [Chatbot ARIA](#-chatbot-aria) • [Variables de entorno](#-variables-de-entorno) • [Instalación](#-instalación-local)

</div>

---

## 🟢 Estado de producción

| Componente | Estado | URL |
|---|---|---|
| API REST | ✅ Online | `https://aetheris-production-3f46.up.railway.app` |
| Healthcheck | ✅ `{"status":"UP"}` | [`/api/actuator/health`](https://aetheris-production-3f46.up.railway.app/api/actuator/health) |
| Base de datos | ✅ MariaDB — AlwaysData | `mysql-roberth.alwaysdata.net / roberth_is` |
| Chatbot ARIA | ✅ Online | `POST /api/aria/chat` |

> Los endpoints protegidos requieren JWT. Usa **Postman** o **curl** — el navegador no puede enviar el header `Authorization` en navegación directa.

---

## 📐 Arquitectura

```
┌─────────────────────────────────────────────────────────────────┐
│                        FRONTEND                                 │
│              React 19 + Vite + TypeScript (Replit)              │
└────────────────────────┬────────────────────────────────────────┘
                         │ HTTPS  (Bearer JWT)
                         ▼
         ┌───────────────────────────────┐
         │     PROXY INTERNO (Express)   │
         │  artifacts/api-server         │
         │  • Elimina Origin/Referer     │
         │  • Reenvía server-to-server   │
         └───────────────┬───────────────┘
                         │
                         ▼
         ┌───────────────────────────────┐
         │   API REST — Spring Boot 3.3  │
         │   Context path: /api          │
         │   Deploy: Railway (Docker)    │
         │                               │
         │  ┌─────────────────────────┐  │
         │  │   Chatbot ARIA          │  │
         │  │   Groq / Llama 3.3 70B  │  │
         │  │   Function calling SSE  │  │
         │  └─────────────────────────┘  │
         └───────────────┬───────────────┘
                         │ JPA / Hibernate
                         ▼
         ┌───────────────────────────────┐
         │   MariaDB — AlwaysData        │
         │   DB: roberth_is              │
         └───────────────────────────────┘
```

---

## 🛠 Stack tecnológico

| Capa | Tecnología |
|---|---|
| Framework | Spring Boot 3.3 |
| Lenguaje | Java 17 (Eclipse Temurin) |
| Build | Maven 3.9 |
| Seguridad | Spring Security + JWT HS384 (jjwt 0.12.5) + BCrypt costo 12 |
| Persistencia | Spring Data JPA + Hibernate 6.5 |
| Base de datos | MariaDB / MySQL (AlwaysData) — charset `utf8mb4_unicode_ci` |
| Pool de conexiones | HikariCP |
| Observabilidad | Spring Boot Actuator |
| Despliegue | Docker multi-stage → Railway (auto-deploy en push a `main`) |
| Chatbot IA | Groq API (Llama 3.3 70B Versatile) + function calling + SSE |

---

## 🤖 Chatbot ARIA

ARIA (Asistente de Reportes e Inteligencia de Aetheris) es un chatbot financiero integrado directamente en el portal. Consulta datos reales del sistema en tiempo real mediante *function calling*.

### Endpoint

```
POST /api/aria/chat
Authorization: Bearer <JWT>
Content-Type: application/json

{
  "message": "¿Cuántas transacciones están pendientes?",
  "history": [
    { "role": "user",  "text": "Hola" },
    { "role": "model", "text": "Hola, soy ARIA..." }
  ]
}
```

### Respuesta (Server-Sent Events)

| Evento | Payload | Descripción |
|---|---|---|
| `text` | `{ "text": "..." }` | Fragmento de la respuesta generada |
| `tool_call` | `{ "name": "listar_sedes", "args": "..." }` | Herramienta ejecutada |
| `tool_error` | `{ "name": "...", "error": "..." }` | Error en una herramienta |
| `done` | `{ "done": true }` | Fin del stream |
| `error` | `{ "error": "..." }` | Error general |

### Herramientas disponibles

| Herramienta | Descripción |
|---|---|
| `listar_sedes` | Lista todas las sedes activas |
| `listar_transacciones_sede` | Transacciones de una sede específica |
| `listar_transacciones_pendientes` | Transacciones en estado PENDIENTE |
| `listar_transacciones_periodo` | Transacciones en un rango de fechas |
| `listar_presupuesto` | Partidas presupuestarias por sede |
| `listar_aprobaciones_pendientes` | Flujos de aprobación sin resolver |
| `listar_usuarios_por_rol` | Usuarios filtrados por rol |

### Modelo utilizado

Por defecto usa `llama-3.3-70b-versatile` (Groq). Se puede cambiar con la variable `GROQ_MODEL`.  
Key gratuita disponible en [console.groq.com](https://console.groq.com) — 6 000 peticiones/día sin tarjeta.

---

## 📋 Referencia de la API

> Todos los endpoints (excepto `/auth/**`) requieren `Authorization: Bearer <JWT>`.

### Autenticación — `/api/auth`

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| `POST` | `/auth/login` | Público | Obtiene JWT |
| `GET` | `/auth/validate` | Autenticado | Valida token |
| `POST` | `/auth/logout` | Autenticado | Cierra sesión |

### Dashboard — `/api/dashboard`

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/dashboard` | Resumen financiero del mes actual |

### Transacciones — `/api/transacciones`

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/transacciones` | Lista todas las transacciones |
| `GET` | `/transacciones/sede/{id}` | Por sede |
| `GET` | `/transacciones/pendientes` | Solo pendientes |
| `GET` | `/transacciones/periodo?inicio=&fin=` | Por rango de fechas (YYYY-MM-DD) |
| `POST` | `/transacciones` | Registra nueva transacción |
| `PUT` | `/transacciones/{id}` | Actualiza transacción |

### Aprobaciones — `/api/aprobaciones`

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/aprobaciones/pendientes` | Flujos sin resolver |
| `PUT` | `/aprobaciones/{id}/aprobar` | Aprueba |
| `PUT` | `/aprobaciones/{id}/rechazar` | Rechaza |

### Presupuesto — `/api/presupuesto`

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/presupuesto/sede/{id}` | Partidas de una sede |
| `POST` | `/presupuesto` | Crea partida |

### Conciliación — `/api/conciliacion`

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/conciliacion` | Lista conciliaciones |
| `POST` | `/conciliacion` | Registra conciliación |

### Sedes — `/api/sedes`

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/sedes` | Lista sedes activas |
| `POST` | `/sedes` | Crea sede (ADMIN) |

### Auditoría — `/api/auditoria`

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/auditoria` | Registro de eventos de auditoría |

---

## 🔐 Seguridad

- **Autenticación:** JWT HS384 firmado con `JWT_SECRET`, expiración configurable (`JWT_EXPIRATION_MS`, default 30 min).
- **Hashing:** BCrypt con costo 12.
- **CORS:** `setAllowedOriginPatterns` acepta `https://*.replit.dev` y `https://*.repl.co` para desarrollo en Replit. En producción se puede restringir vía `CORS_ALLOWED_ORIGINS`.
- **Sesión inactiva:** se invalida automáticamente tras `SESION_INACTIVIDAD_MIN` minutos (default 30).

---

## ⚙️ Variables de entorno

| Variable | Requerida | Default | Descripción |
|---|---|---|---|
| `MYSQL_PASSWORD` | ✅ | — | Contraseña de la base de datos |
| `JWT_SECRET` | ✅ | — | Clave de firma JWT (mín. 32 chars) |
| `GROQ_API_KEY` | ✅ (ARIA) | — | API key de Groq para el chatbot |
| `MYSQL_HOST` | | `mysql-roberth.alwaysdata.net` | Host de MariaDB |
| `MYSQL_DATABASE` | | `roberth_is` | Nombre de la base de datos |
| `MYSQL_USER` | | `roberth` | Usuario de la base de datos |
| `JWT_EXPIRATION_MS` | | `1800000` | Expiración del JWT en ms (30 min) |
| `GROQ_MODEL` | | `llama-3.3-70b-versatile` | Modelo de Groq para ARIA |
| `CORS_ALLOWED_ORIGINS` | | `http://localhost:5173` | Orígenes CORS permitidos |
| `SESION_INACTIVIDAD_MIN` | | `30` | Minutos de inactividad hasta cierre |
| `PORT` | | `8080` | Puerto del servidor |

---

## 🚀 Instalación local

### Prerrequisitos

- Java 17+
- Maven 3.9+
- MariaDB o MySQL corriendo localmente

### 1. Clonar

```bash
git clone https://github.com/ROBERTHGONZALES/aetheris-backend.git
cd aetheris-backend
```

### 2. Variables de entorno

```bash
export MYSQL_HOST=localhost
export MYSQL_DATABASE=aetheris_dev
export MYSQL_USER=root
export MYSQL_PASSWORD=tu_password
export JWT_SECRET=una_clave_segura_de_minimo_32_caracteres
export GROQ_API_KEY=gsk_...          # obtén en console.groq.com
```

### 3. Ejecutar

```bash
mvn spring-boot:run
```

La API queda disponible en `http://localhost:8080/api`.

### 4. Verificar

```bash
curl http://localhost:8080/api/actuator/health
# → {"status":"UP"}
```

---

## 🔄 Despliegue en Railway

El proyecto usa Docker multi-stage. Railway auto-despliega al hacer push a `main`.

```bash
git push origin main
# Railway detecta el Dockerfile, compila y redespliega automáticamente
```

**Variables que debes configurar en Railway:**

```
MYSQL_PASSWORD=...
JWT_SECRET=...
GROQ_API_KEY=gsk_...
CORS_ALLOWED_ORIGINS=https://tu-frontend.replit.app
```

---

## 🤝 Contribuir

1. Fork del repositorio
2. Crea tu rama: `git checkout -b feature/nueva-funcionalidad`
3. Commit: `git commit -m 'feat: descripción corta'`
4. Push: `git push origin feature/nueva-funcionalidad`
5. Abre un **Pull Request** hacia `main`

---

## 📜 Licencia

MIT © [ROBERTHGONZALES](https://github.com/ROBERTHGONZALES)
