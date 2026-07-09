# AETHERIS — Backend Spring Boot

> **Sistema Gestor Financiero Corporativo**  
> API REST · Spring Boot 3.3 · Java 17 · MariaDB (AlwaysData) · Desplegado en Railway

---

## 🟢 Estado actual — PRODUCCIÓN ACTIVA

| Componente | Estado | URL |
|---|---|---|
| Backend Spring Boot | ✅ Online | `https://aetheris-production-3f46.up.railway.app` |
| Base de datos | ✅ MariaDB AlwaysData | `mysql-roberth.alwaysdata.net` / `roberth_is` |
| Healthcheck | ✅ `{"status":"UP"}` | `/api/actuator/health` |

---

## ⚠️ PENDIENTES — Continuar desde aquí

### 1. Crear usuario administrador (URGENTE — sin esto no hay login)
Ejecutar este SQL directamente en MariaDB AlwaysData:

```sql
-- Paso 1: Asegurarse de que existe el rol ADMIN
INSERT IGNORE INTO rol (nombre, descripcion) VALUES ('ADMIN', 'Administrador del sistema');

-- Paso 2: Crear el usuario admin
-- La contraseña 'Admin2024!' hasheada con BCrypt:
INSERT INTO usuario (
  username, password, nombre_completo, email,
  activo, intentos_fallidos, id_rol
) VALUES (
  'admin',
  '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TiGqKbB8P1RtGf5K9mN2oV7sZeXi',
  'Administrador AETHERIS',
  'admin@aetheris.com',
  1, 0,
  (SELECT id_rol FROM rol WHERE nombre = 'ADMIN' LIMIT 1)
);
```
> **Contraseña de acceso:** `Admin2024!`  
> Si la tabla `usuario` tiene nombres de columna distintos, adaptar según el esquema real.

Verificar login:
```bash
curl -X POST https://aetheris-production-3f46.up.railway.app/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"Admin2024!"}'
```
Debe devolver un JWT token.

---

### 2. Chatbot ARIA — Express/TypeScript (en el monorepo Replit)
El monorepo Replit contiene un servidor Express en `artifacts/api-server/` con el chatbot ARIA (IA financiera basada en Gemini + function calling).

**Estado:** El chatbot consulta MariaDB directamente con `mysql2`. La mejora pendiente es que use la API REST del backend en vez de la BD directamente.

**Archivos clave en Replit:**
```
artifacts/api-server/src/routes/gemini/
├── tools.ts        ← 6 herramientas de function calling (consultan MariaDB)
├── chat.ts         ← Loop agéntico SSE con Gemini (MAX_ITER=5)
├── conversations.ts ← CRUD de conversaciones (PostgreSQL Replit)
└── index.ts        ← Router Express
```

**Variables de entorno necesarias en Replit:**
- `GEMINI_API_KEY` — Google AI Studio
- `MYSQL_PASSWORD` — AlwaysData
- `JWT_SECRET` — misma clave que Railway

---

### 3. Frontend (NO construido aún)
El dashboard web AETHERIS y la UI del chatbot ARIA están pendientes de desarrollo.

**Tecnología sugerida:** React + Vite + Tailwind  
**Conectar a:** `https://aetheris-production-3f46.up.railway.app/api`

---

## 🏗️ Arquitectura

```
Replit (monorepo pnpm)
├── artifacts/api-server/     → Express + TypeScript (puerto 8080)
│   └── src/routes/gemini/    → Chatbot ARIA (Gemini + MariaDB)
├── artifacts/mockup-sandbox/ → Sandbox de componentes UI
└── lib/
    ├── integrations-gemini-ai/ → Cliente Gemini (usa GEMINI_API_KEY directo)
    └── db/                     → Helpers de BD

GitHub (este repo) → Railway
└── backend/  →  Spring Boot REST API (puerto 8080 en Railway)
```

---

## 🔧 Variables de entorno — Railway

| Variable | Valor |
|---|---|
| `MYSQL_HOST` | `mysql-roberth.alwaysdata.net` |
| `MYSQL_USER` | `roberth` |
| `MYSQL_DATABASE` | `roberth_is` |
| `MYSQL_PASSWORD` | *(secret — configurado en Railway)* |
| `JWT_SECRET` | *(secret — mínimo 64 caracteres)* |
| `PORT` | Railway la inyecta automáticamente → app escucha en `${PORT:8080}` |

---

## 📡 Endpoints de la API

### Autenticación
| Método | Ruta | Auth | Descripción |
|--------|------|------|-------------|
| POST | `/api/auth/login` | ❌ | Login → devuelve JWT |
| POST | `/api/auth/logout` | ✅ | Cerrar sesión |
| GET  | `/api/auth/validar` | ✅ | Validar token activo |

### Operaciones (todas requieren `Authorization: Bearer <token>`)
| Método | Ruta | Descripción |
|--------|------|-------------|
| GET/POST | `/api/transacciones` | Listar / Registrar transacción |
| GET | `/api/transacciones/pendientes` | Pendientes de aprobación |
| PUT | `/api/aprobaciones/{id}/aprobar` | Aprobar (CFO) |
| PUT | `/api/aprobaciones/{id}/rechazar` | Rechazar (CFO) |
| GET | `/api/presupuesto` | Consultar presupuesto |
| GET | `/api/presupuesto/alerta` | Partidas en alerta ≥90% |
| GET | `/api/sedes` | Listar sedes |
| POST | `/api/conciliacion` | Iniciar conciliación bancaria |
| POST | `/api/conciliacion/{id}/cruce` | Cruce automático |
| GET | `/api/auditoria` | Log de auditoría |
| POST | `/api/reportes/ingresos-egresos` | Generar reporte |
| GET/POST | `/api/usuarios` | Gestión de usuarios |

---

## 🗄️ Estructura del proyecto

```
src/main/java/com/aetheris/
├── AetherisApplication.java
├── config/
│   ├── SecurityConfig.java           ← JWT stateless + CORS
│   ├── JwtAuthenticationFilter.java  ← Valida JWT contra tabla sesion en BD
│   └── GlobalExceptionHandler.java   ← Sin stack traces al cliente
├── modelo/                           ← 14 entidades JPA
│   ├── enums/                        ← 9 enums
│   ├── Usuario.java / Rol.java / Permiso.java / Sesion.java
│   ├── Sede.java / CategoriaContable.java
│   ├── TransaccionFinanciera.java / FlujodeAprobacion.java
│   ├── PartidaPresupuestaria.java
│   ├── CuentaBancaria.java / MovimientoBancario.java
│   ├── ConciliacionBancaria.java / Discrepancia.java
│   ├── Reporte.java
│   └── LogAuditoria.java             ← Inmutable (sin setters)
├── dao/                              ← 13 repositorios Spring Data JPA
├── servicio/                         ← 10 servicios
│   ├── AutenticacionService.java     ← BCrypt + JWT + sesión en BD
│   ├── JwtService.java
│   ├── TransaccionService.java / AprobacionService.java
│   ├── PresupuestoService.java / SedeService.java
│   ├── ConciliacionService.java / ReporteService.java
│   ├── AuditoriaService.java / UsuarioService.java
└── presentacion/                     ← 9 controllers REST
```

---

## 🔐 Decisiones técnicas importantes

| Decisión | Motivo |
|---|---|
| `ddl-auto=none` | La BD tiene columnas `CHAR` donde JPA espera `VARCHAR`. Con `none` no valida y las queries funcionan. |
| `server.port=${PORT:8080}` | Railway inyecta `PORT`, no `SPRING_PORT`. |
| Context path `/api` | Todos los endpoints van bajo `/api/...` |
| Healthcheck en `/api/actuator/health` | Por el context path `/api` |
| JWT stateless | Sesiones guardadas en tabla `sesion` en BD para poder invalidarlas |
| Gemini directo con `GEMINI_API_KEY` | No usa proxy de Replit (requería upgrade de cuenta) |

---

## 🚀 Redesplegar en Railway

Railway redespliega automáticamente con cada push a `main` en este repo.

Para redesplegar manualmente: Railway dashboard → Deployments → Deploy.

---

## 💻 Desarrollo local

```bash
# Clonar
git clone https://github.com/ROBERTHGONZALES/aetheris-backend.git
cd aetheris-backend

# Variables de entorno
export MYSQL_HOST=mysql-roberth.alwaysdata.net
export MYSQL_DATABASE=roberth_is
export MYSQL_USER=roberth
export MYSQL_PASSWORD=tu_password
export JWT_SECRET=tu_jwt_secret_minimo_64_chars
export PORT=8080

# Compilar y ejecutar
mvn clean package -DskipTests
java -jar target/aetheris-backend-1.0.0.jar
# → http://localhost:8080/api
```

---

## 📋 Stack tecnológico

| Capa | Tecnología |
|---|---|
| Framework | Spring Boot 3.3 |
| Java | 17 (eclipse-temurin) |
| Build | Maven 3.9 |
| Seguridad | Spring Security + JWT (jjwt 0.12.5) + BCrypt |
| Persistencia | Spring Data JPA + Hibernate 6.5 |
| Base de datos | MariaDB / MySQL (AlwaysData) |
| Pool conexiones | HikariCP |
| Observabilidad | Spring Boot Actuator |
| Despliegue | Docker (multistage) → Railway |
