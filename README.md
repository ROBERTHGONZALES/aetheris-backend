<div align="center">

# AETHERIS
### Sistema Gestor Financiero Corporativo

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/17/)
[![MariaDB](https://img.shields.io/badge/MariaDB-003545?style=for-the-badge&logo=mariadb&logoColor=white)](https://mariadb.org/)
[![Railway](https://img.shields.io/badge/Railway-0B0D0E?style=for-the-badge&logo=railway&logoColor=white)](https://railway.app/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg?style=for-the-badge)](LICENSE)

**API REST corporativa para gestión de transacciones financieras, conciliación bancaria, presupuesto y auditoría.**

[Producción](#-estado-de-producción) • [Endpoints](#-referencia-de-la-api) • [Instalación](#-instalación-local) • [Variables de entorno](#-variables-de-entorno) • [Arquitectura](#-arquitectura)

</div>

---

## 🟢 Estado de producción

| Componente | Estado | URL |
|---|---|---|
| API REST | ✅ Online | `https://aetheris-production-3f46.up.railway.app` |
| Healthcheck | ✅ `{"status":"UP"}` | [`/api/actuator/health`](https://aetheris-production-3f46.up.railway.app/api/actuator/health) |
| Base de datos | ✅ MariaDB AlwaysData | `mysql-roberth.alwaysdata.net / roberth_is` |
| Chatbot ARIA | ✅ Online | `POST /api/aria/chat` |

> **Nota:** Los endpoints protegidos requieren JWT. Usa **Postman** o **curl** — el navegador no puede enviar headers `Authorization` en navegación directa.

---

## 📐 Arquitectura

```
┌─────────────────────────────────────────────────────────────┐
│                     CLIENTE / FRONTEND                      │
│                  (React + Vite — en desarrollo)             │
└────────────────────────┬────────────────────────────────────┘
                         │ HTTPS
         ┌───────────────┴───────────────┐
         │                               │
         ▼                               ▼
┌─────────────────┐            ┌──────────────────────┐
│  Chatbot ARIA   │            │  API REST AETHERIS   │
│  Express + TS   │ ──JWT──►  │  Spring Boot 3.3     │
│  Gemini 2.0     │            │  Context path: /api  │
│  SSE streaming  │            │  (Railway)           │
└─────────────────┘            └──────────┬───────────┘
                                          │ JPA / Hibernate
                                          ▼
                               ┌──────────────────────┐
                               │  MariaDB             │
                               │  AlwaysData          │
                               │  DB: roberth_is      │
                               └──────────────────────┘
```

### Stack tecnológico

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
| Chatbot IA | Express + TypeScript + Gemini 2.0 Flash + function calling |

---

## 🚀 Instalación local

### Prerrequisitos

- Java 17+
- Maven 3.9+
- MariaDB o MySQL corriendo localmente (o acceso a AlwaysData)

### 1. Clonar el repositorio

```bash
git clone https://github.com/ROBERTHGONZALES/aetheris-backend.git
cd aetheris-backend
```

### 2. Configurar variables de entorno

Crea un archivo `.env` o exporta las variables en tu shell:

```bash
export MYSQL_HOST=mysql-roberth.alwaysdata.net
export MYSQL_DATABASE=roberth_is
export MYSQL_USER=roberth
export MYSQL_PASSWORD=tu_password_alwaysdata
export JWT_SECRET=cambia_esto_por_64_caracteres_aleatorios_minimo_aqui
export PORT=8080

# Opcionales
export JWT_EXPIRATION_MS=1800000          # 30 min (default)
export SESION_INACTIVIDAD_MIN=30          # 30 min (default)
export CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173
```

### 3. Compilar y ejecutar

```bash
# Compilar (omite tests en primer arranque)
mvn clean package -DskipTests

# Ejecutar
java -jar target/aetheris-backend-1.0.0.jar
```

La API queda disponible en `http://localhost:8080/api`

### 4. Cargar datos de prueba

El script `scripts/seed-aetheris.sql` carga 5 usuarios, 3 sedes, 15 transacciones y toda la data necesaria para probar el sistema. Es **idempotente** (usa `INSERT IGNORE` con UUIDs fijos, se puede ejecutar varias veces).

```bash
# Con mysql CLI
mysql -h $MYSQL_HOST -u $MYSQL_USER -p$MYSQL_PASSWORD $MYSQL_DATABASE < scripts/seed-aetheris.sql

# O con Node.js + mysql2
npm install mysql2
node -e "
const mysql = require('mysql2/promise');
const fs = require('fs');
const sql = fs.readFileSync('scripts/seed-aetheris.sql', 'utf8');
mysql.createConnection({
  host: process.env.MYSQL_HOST,
  user: process.env.MYSQL_USER,
  password: process.env.MYSQL_PASSWORD,
  database: process.env.MYSQL_DATABASE,
  ssl: { rejectUnauthorized: false },
  multipleStatements: true
}).then(c => c.query(sql).then(() => { console.log('Seed OK'); c.end(); }));
"
```

---

## 🔑 Autenticación

AETHERIS usa **JWT stateless** con invalidación por base de datos. El flujo completo es:

```
1. POST /api/auth/login  →  { token, sesionId, usuario, rol }
                                │
                   Guardar el token localmente
                                │
2. GET|POST /api/...    →   Header: Authorization: Bearer <token>
                                │
3. POST /api/auth/logout →  Invalida sesión en BD
```

Los tokens expiran en **30 minutos**. Las sesiones quedan en la tabla `sesion`, lo que permite logout forzado incluso antes de que expire el JWT.

### Usuarios de prueba (cargados por el seed)

> La contraseña de todos los usuarios se genera con BCrypt desde la variable de entorno `AETHERIS_ADMIN_PASSWORD`.

| Email | Rol | Descripción |
|---|---|---|
| admin@aetheris.com | ADMIN | Acceso total al sistema |
| marta.fernandez@aetheris.com | CONTADOR | Registra transacciones, Lima y Arequipa |
| carlos.medina@aetheris.com | APROBADOR | Aprueba/rechaza transacciones en todas las sedes |
| lucia.torres@aetheris.com | AUDITOR | Audita todos los módulos en todas las sedes |
| pedro.santos@aetheris.com | CONTADOR | Registra transacciones, Lima y Trujillo |

---

## 📡 Referencia de la API

> **Base URL producción:** `https://aetheris-production-3f46.up.railway.app`  
> **Context path:** `/api`  
> 🔒 = requiere `Authorization: Bearer <JWT>`

---

### 🔐 Autenticación — `/api/auth`

#### `POST /api/auth/login`
Inicia sesión y devuelve un JWT.

**Body:**
```json
{
  "correo": "admin@aetheris.com",
  "password": "TuPassword123!"
}
```

**Respuesta 200:**
```json
{
  "token": "eyJhbGciOiJIUzM4NCJ9...",
  "sesionId": "e062e6a8-f756-4c14-957b-03dff9c1d947",
  "usuario": "Roberto González",
  "rol": "ADMIN"
}
```

**Respuesta 401:**
```json
{ "error": "Credenciales inválidas" }
```

---

#### `GET /api/auth/validar` 🔒
Verifica si el token actual sigue siendo válido.

**Respuesta 200:**
```json
{ "valido": true }
```

---

#### `POST /api/auth/logout` 🔒
Cierra la sesión e invalida el token en base de datos.

**Respuesta 200:**
```json
{ "mensaje": "Sesión cerrada correctamente" }
```

---

### 🏢 Sedes — `/api/sedes`

#### `GET /api/sedes` 🔒
Lista todas las sedes activas.

**Respuesta 200:**
```json
[
  {
    "id": "30000000-0000-0000-0000-000000000001",
    "nombre": "Sede Lima — Oficina Central",
    "codigo": "SEDE-LIM",
    "pais": "Perú",
    "moneda": "PEN",
    "montoLimiteAprobacion": 10000.00,
    "estado": true
  }
]
```

#### `GET /api/sedes/{id}` 🔒
Obtiene una sede por ID.

#### `POST /api/sedes` 🔒
Crea una nueva sede.

**Body:**
```json
{
  "nombre": "Sede Cusco",
  "codigo": "SEDE-CUS",
  "pais": "Perú",
  "moneda": "PEN",
  "montoLimiteAprobacion": 5000.00
}
```

#### `PUT /api/sedes/{id}` 🔒
Actualiza una sede existente. Mismo body que POST.

#### `PUT /api/sedes/{id}/limite?monto=8000` 🔒
Actualiza el límite de aprobación de una sede.

---

### 💸 Transacciones — `/api/transacciones`

#### `GET /api/transacciones?sedeId={id}` 🔒
Lista todas las transacciones de una sede.

**Respuesta 200:**
```json
[
  {
    "id": "60000000-0000-0000-0000-000000000001",
    "tipo": "INGRESO",
    "monto": 45000.00,
    "moneda": "PEN",
    "fecha": "2025-04-10",
    "descripcion": "Venta de software corporativo a cliente minero",
    "estadoAprobacion": "APROBADA",
    "referencia": "TXN-LIM-2025-001",
    "sede": { "id": "...", "nombre": "Sede Lima — Oficina Central" },
    "categoria": { "nombre": "Ventas de Productos", "tipo": "INGRESO" }
  }
]
```

#### `GET /api/transacciones/{id}` 🔒
Obtiene una transacción por ID.

#### `GET /api/transacciones/pendientes` 🔒
Lista todas las transacciones con estado `PENDIENTE` (esperando aprobación).

#### `GET /api/transacciones/periodo?inicio=2025-05-01&fin=2025-05-31` 🔒
Lista transacciones en un rango de fechas (formato `YYYY-MM-DD`).

#### `POST /api/transacciones` 🔒
Registra una nueva transacción financiera.

**Body:**
```json
{
  "tipo": "EGRESO",
  "monto": 3500.00,
  "moneda": "PEN",
  "fecha": "2025-07-09",
  "descripcion": "Compra de equipos de cómputo para oficina central",
  "sede": { "id": "30000000-0000-0000-0000-000000000001" },
  "categoria": { "id": "40000000-0000-0000-0000-000000000006" },
  "cuentaBancaria": { "id": "50000000-0000-0000-0000-000000000001" }
}
```

> ℹ️ Si el monto supera el `montoLimiteAprobacion` de la sede, el estado será `PENDIENTE` y se creará automáticamente un flujo de aprobación.

**Tipos de transacción:** `INGRESO` · `EGRESO`  
**Estados:** `PENDIENTE` · `APROBADA` · `RECHAZADA`

---

### ✅ Aprobaciones — `/api/aprobaciones`

#### `GET /api/aprobaciones/pendientes` 🔒
Lista todos los flujos de aprobación pendientes.

**Respuesta 200:**
```json
[
  {
    "id": "A0000000-0000-0000-0000-000000000005",
    "estado": "PENDIENTE",
    "montoLimite": 10000.00,
    "fechaSolicitud": "2025-06-02T09:00:00",
    "observacion": null,
    "transaccion": {
      "id": "60000000-0000-0000-0000-000000000009",
      "monto": 15000.00,
      "descripcion": "Consultoría BI — cliente financiero",
      "sede": { "nombre": "Sede Lima — Oficina Central" }
    }
  }
]
```

#### `PUT /api/aprobaciones/{id}/aprobar` 🔒
Aprueba una transacción pendiente. Cambia el estado de la transacción a `APROBADA`.

**Body:**
```json
{ "observacion": "Contrato revisado y validado. Procede el pago." }
```

#### `PUT /api/aprobaciones/{id}/rechazar` 🔒
Rechaza una transacción pendiente. Cambia el estado a `RECHAZADA`.

**Body:**
```json
{ "observacion": "Documentación incompleta. Se requieren 3 cotizaciones." }
```

---

### 📊 Presupuesto — `/api/presupuesto`

#### `GET /api/presupuesto?sedeId={id}&periodo=2025-06` 🔒
Lista las partidas presupuestarias de una sede para un periodo (`YYYY-MM`).

**Respuesta 200:**
```json
[
  {
    "id": "80000000-0000-0000-0000-000000000001",
    "periodo": "2025-06",
    "montoPresupuestado": 50000.00,
    "montoEjecutado": 15000.00,
    "porcentajeEjecucion": 30.00,
    "categoria": {
      "nombre": "Ventas de Productos",
      "tipo": "INGRESO"
    }
  }
]
```

> `porcentajeEjecucion` es una columna STORED generada automáticamente por MariaDB — no se envía en el body.

#### `POST /api/presupuesto` 🔒
Crea una nueva partida presupuestaria.

**Body:**
```json
{
  "periodo": "2025-07",
  "montoPresupuestado": 55000.00,
  "sede": { "id": "30000000-0000-0000-0000-000000000001" },
  "categoria": { "id": "40000000-0000-0000-0000-000000000001" }
}
```

#### `PUT /api/presupuesto/{id}/ejecucion?monto=5000` 🔒
Actualiza el monto ejecutado de una partida presupuestaria.

---

### 🏦 Conciliación Bancaria — `/api/conciliacion`

#### `POST /api/conciliacion?cuentaId={id}&periodo=2025-06` 🔒
Inicia una nueva conciliación bancaria para una cuenta y periodo.

**Respuesta 201:**
```json
{ "id": "C0000000-0000-0000-0000-000000000004", "estado": "INICIADA" }
```

#### `POST /api/conciliacion/{id}/movimientos` 🔒
Importa movimientos bancarios a una conciliación (CSV/JSON del extracto bancario).

**Body:**
```json
[
  {
    "fecha": "2025-06-10",
    "monto": 45000.00,
    "referencia": "BCP-TX-00421",
    "descripcionBanco": "ABONO TRANSFERENCIA CORPORATIVA"
  }
]
```

#### `POST /api/conciliacion/{id}/cruce` 🔒
Ejecuta el cruce automático entre movimientos bancarios y transacciones registradas.

**Respuesta 200:**
```json
{ "mensaje": "Cruce automático ejecutado correctamente" }
```

#### `GET /api/conciliacion/{id}/discrepancias` 🔒
Lista las discrepancias encontradas (movimientos sin transacción match o con diferencia de monto).

**Respuesta 200:**
```json
[
  {
    "id": "90000000-0000-0000-0000-000000000002",
    "estado": "ABIERTA",
    "fechaCreacion": "2025-06-04T09:30:00",
    "movimientoBancario": {
      "referencia": "IB-TX-00165",
      "monto": 1200.00,
      "descripcionBanco": "CARGO MANTENIMIENTO AC"
    }
  }
]
```

#### `PUT /api/conciliacion/discrepancias/{id}/resolver` 🔒
Resuelve una discrepancia con una justificación documentada.

**Body:**
```json
{
  "tipo": "DOCUMENTADO",
  "justificacion": "Cargo de mantenimiento de aire acondicionado. Factura N° 001-0423 adjunta."
}
```

**Tipos de resolución:** `AJUSTE_MANUAL` · `DOCUMENTADO` · `PENDIENTE`

---

### 👥 Usuarios — `/api/usuarios`

#### `GET /api/usuarios?rolId={id}` 🔒
Lista usuarios filtrados por ID de rol.

#### `GET /api/usuarios/{id}` 🔒
Obtiene un usuario por ID.

#### `POST /api/usuarios` 🔒
Crea un nuevo usuario en el sistema.

**Body:**
```json
{
  "nombreCompleto": "Ana Quispe",
  "correoElectronico": "ana.quispe@aetheris.com",
  "password": "Password123!"
}
```

#### `PUT /api/usuarios/{id}/rol?rolId={rolId}` 🔒
Asigna un rol a un usuario existente.

#### `PUT /api/usuarios/{id}/activar` 🔒
Activa un usuario previamente desactivado.

#### `PUT /api/usuarios/{id}/desactivar` 🔒
Desactiva un usuario (no lo elimina).

---

### 📋 Auditoría — `/api/auditoria`

Todos los endpoints registran automáticamente cada acción en la tabla `log_auditoria`.

#### `GET /api/auditoria?usuarioId={id}` 🔒
Lista los logs de auditoría de un usuario específico.

#### `GET /api/auditoria/modulo?modulo=TRANSACCIONES` 🔒
Lista los logs de un módulo del sistema.

**Módulos disponibles:** `TRANSACCIONES` · `CONCILIACION` · `PRESUPUESTO` · `USUARIOS` · `APROBACIONES` · `REPORTES`

#### `GET /api/auditoria/periodo?inicio=2025-06-01T00:00:00&fin=2025-06-30T23:59:59` 🔒
Lista logs en un rango de fechas y horas (formato ISO 8601).

---

### 📄 Reportes — `/api/reportes`

#### `POST /api/reportes/ingresos-egresos?periodo=2025-06&sedeId={id}&formato=PDF` 🔒
Genera reporte de ingresos vs. egresos para una sede y periodo.

#### `POST /api/reportes/presupuestal?periodo=2025-06&sedeId={id}&formato=EXCEL` 🔒
Genera reporte de ejecución presupuestal.

#### `POST /api/reportes/conciliacion?idConciliacion={id}&formato=PDF` 🔒
Genera reporte de una conciliación bancaria específica.

#### `POST /api/reportes/auditoria?inicio=2025-06-01T00:00:00&fin=2025-06-30T23:59:59&formato=PDF` 🔒
Genera reporte de logs de auditoría en un rango de fechas.

#### `GET /api/reportes?usuarioId={id}` 🔒
Lista los reportes generados por un usuario.

**Formatos disponibles:** `PDF` · `EXCEL`

---

### ❤️ Sistema — `/api/actuator`

#### `GET /api/actuator/health`
Healthcheck del servicio — **sin autenticación**.

**Respuesta 200:**
```json
{ "status": "UP" }
```

---

## 🤖 Chatbot ARIA

ARIA (**A**sistente de **R**eportes e **I**nteligencia de **A**etheris) es un chatbot financiero que usa **Gemini 2.0 Flash** con *function calling*. Consulta esta API en tiempo real, sin acceso directo a la base de datos.

### Endpoint

```
POST https://<replit-url>/api/aria/chat
Content-Type: application/json
```

### Request

```json
{
  "message": "¿Cuántas transacciones están pendientes de aprobación?",
  "history": []
}
```

### Response (SSE — `text/event-stream`)

```
event: tool_call
data: {"name":"listar_transacciones_pendientes","args":{}}

event: text
data: {"text":"Hay **3 transacciones** pendientes:\n\n| Sede | Monto | Descripción |\n|---|---|---|\n..."}

event: done
data: {"done":true}
```

**Eventos SSE posibles:**

| Evento | Descripción |
|---|---|
| `text` | Fragmento de respuesta generado por Gemini |
| `tool_call` | Herramienta invocada con sus argumentos |
| `tool_error` | Error al ejecutar una herramienta |
| `done` | Fin del stream |
| `error` | Error general (ej. cuota de API agotada) |

### Herramientas disponibles

| Herramienta | Parámetros | Descripción |
|---|---|---|
| `listar_sedes` | — | Lista todas las sedes activas |
| `listar_transacciones_sede` | `sedeId` | Transacciones de una sede |
| `listar_transacciones_pendientes` | — | Transacciones pendientes de aprobación |
| `listar_transacciones_periodo` | `inicio`, `fin` (YYYY-MM-DD) | Transacciones en rango de fechas |
| `listar_presupuesto` | `sedeId`, `periodo` (YYYY-MM) | Partidas presupuestarias |
| `listar_aprobaciones_pendientes` | — | Flujos de aprobación sin resolver |
| `listar_usuarios_por_rol` | `rolId` | Usuarios filtrados por rol |

### Probar ARIA con curl

```bash
curl -N -X POST https://<tu-replit-url>/api/aria/chat \
  -H "Content-Type: application/json" \
  -d '{"message":"Dame un resumen de las sedes de AETHERIS"}'
```

---

## 🗄️ Esquema de base de datos

```
rol ─────────────── permiso (via rol_permiso)
 │
 └── usuario ─────── sesion
       │
       └── usuario_sede ─────── sede ──── cuenta_bancaria
       │                          │
       │                          └── partida_presupuestaria ─── categoria_contable
       │                          │
       │                          └── conciliacion_bancaria
       │                                │
       └── transaccion_financiera ──────┤
             │  └── flujo_aprobacion    └── movimiento_bancario
             │                                └── discrepancia
             └── log_auditoria
             └── reporte
```

### Tablas principales

| Tabla | Descripción |
|---|---|
| `rol` | Roles del sistema (ADMIN, CONTADOR, APROBADOR, AUDITOR) |
| `permiso` | Permisos granulares por módulo y acción |
| `rol_permiso` | Relación N:M entre roles y permisos |
| `usuario` | Usuarios con hash BCrypt (costo 12) |
| `sesion` | Sesiones activas — permite logout forzado |
| `usuario_sede` | Asignación de sedes por usuario (N:M) |
| `sede` | Sucursales con moneda y límite de aprobación |
| `cuenta_bancaria` | Cuentas bancarias asociadas a cada sede |
| `categoria_contable` | Categorías de INGRESO/EGRESO |
| `transaccion_financiera` | Core del sistema: ingresos y egresos |
| `partida_presupuestaria` | Presupuesto por sede + categoría + periodo; `porcentaje_ejecucion` es columna STORED |
| `conciliacion_bancaria` | Procesos de conciliación por cuenta y periodo |
| `movimiento_bancario` | Movimientos importados del extracto bancario |
| `discrepancia` | Diferencias entre banco y sistema |
| `flujo_aprobacion` | Aprobaciones requeridas por monto |
| `log_auditoria` | Registro inmutable de todas las acciones |
| `reporte` | Metadatos de PDFs/Excel generados |

---

## 🌍 Variables de entorno

| Variable | Requerida | Default | Descripción |
|---|---|---|---|
| `MYSQL_HOST` | ✅ | — | Host de la base de datos |
| `MYSQL_USER` | ✅ | — | Usuario de la base de datos |
| `MYSQL_DATABASE` | ✅ | — | Nombre de la base de datos |
| `MYSQL_PASSWORD` | ✅ | — | Contraseña de la base de datos |
| `JWT_SECRET` | ✅ | — | Clave secreta JWT (mínimo 64 caracteres) |
| `PORT` | ✅ | — | Puerto HTTP (Railway lo inyecta automáticamente) |
| `JWT_EXPIRATION_MS` | ❌ | `1800000` | Expiración del JWT en ms (30 min) |
| `SESION_INACTIVIDAD_MIN` | ❌ | `30` | Tiempo de inactividad de sesión en minutos |
| `CORS_ALLOWED_ORIGINS` | ❌ | `http://localhost:3000,http://localhost:5173` | Orígenes CORS permitidos |

---

## 🐳 Docker

El proyecto incluye un `Dockerfile` multi-stage optimizado:

```bash
# Construir imagen
docker build -t aetheris-backend .

# Ejecutar
docker run -p 8080:8080 \
  -e MYSQL_HOST=mysql-roberth.alwaysdata.net \
  -e MYSQL_DATABASE=roberth_is \
  -e MYSQL_USER=roberth \
  -e MYSQL_PASSWORD=tu_password \
  -e JWT_SECRET=tu_jwt_secret_64_chars_minimo \
  aetheris-backend
```

- **Stage 1:** `maven:3.9-eclipse-temurin-17` — compila y genera el JAR
- **Stage 2:** `eclipse-temurin:17-jre-alpine` — imagen mínima de producción (~180MB)

---

## 🚀 Despliegue en Railway

Railway redespliega automáticamente con cada `git push` a la rama `main`.

**Redespliegue manual:** Railway Dashboard → tu proyecto → **Deployments** → **Deploy Now**

**Variables a configurar en Railway:**
```
MYSQL_HOST         = mysql-roberth.alwaysdata.net
MYSQL_USER         = roberth
MYSQL_DATABASE     = roberth_is
MYSQL_PASSWORD     = (configurar como secret en Railway)
JWT_SECRET         = (configurar como secret en Railway)
```

> Railway inyecta `PORT` automáticamente — no la configures manualmente.

---

## 📁 Estructura del proyecto

```
aetheris-backend/
├── src/
│   └── main/
│       ├── java/com/aetheris/
│       │   ├── AetherisApplication.java        ← Entry point
│       │   ├── config/
│       │   │   ├── SecurityConfig.java          ← Spring Security + CORS
│       │   │   ├── JwtAuthenticationFilter.java ← Filtro JWT en cada request
│       │   │   └── GlobalExceptionHandler.java  ← Manejo global de errores
│       │   ├── modelo/                          ← Entidades JPA
│       │   │   └── enums/                       ← Enumeraciones del dominio
│       │   ├── dao/                             ← Repositorios Spring Data JPA
│       │   ├── servicio/                        ← Lógica de negocio
│       │   └── presentacion/                   ← Controllers REST
│       └── resources/
│           └── application.properties           ← Configuración (lee de env vars)
├── scripts/
│   └── seed-aetheris.sql                        ← Datos de prueba (idempotente)
├── Dockerfile                                   ← Multi-stage build
├── railway.toml                                 ← Config de despliegue Railway
└── pom.xml                                      ← Dependencias Maven
```

---

## 🔒 Seguridad

- 🔐 Contraseñas hasheadas con **BCrypt** (costo 12, nunca texto plano)
- 🎟️ Tokens **JWT HS384** firmados, expiración configurable (default 30 min)
- 🚪 Sesiones persistidas en BD para **invalidación forzada** (logout seguro aunque el JWT siga vigente)
- 🛡️ **Spring Security** protege todos los endpoints excepto `/auth/login` y `/actuator/health`
- 🌐 **CORS** configurado por variable de entorno
- 📝 **Auditoría automática** de todas las operaciones en `log_auditoria`

---

## 🤝 Contribuir

1. Haz fork del repositorio
2. Crea tu rama: `git checkout -b feature/nueva-funcionalidad`
3. Haz commit con mensaje descriptivo: `git commit -m 'feat: descripción corta'`
4. Push a tu rama: `git push origin feature/nueva-funcionalidad`
5. Abre un **Pull Request** hacia `main`

---

## 📜 Licencia

MIT © [ROBERTHGONZALES](https://github.com/ROBERTHGONZALES)
