# AETHERIS — Backend Spring Boot

Sistema Gestor Financiero Corporativo · API REST

## Requisitos

| Herramienta | Versión |
|---|---|
| Java | 17+ |
| Maven | 3.8+ |

## Configuración

Edita `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://mysql-roberth.alwaysdata.net:3306/roberth_is?...
spring.datasource.username=roberth
spring.datasource.password=TU_PASSWORD
aetheris.jwt.secret=CambiarEstaClavePorUnaSeguraEnProduccion256Bits
```

## Compilar y ejecutar

```bash
# Compilar
mvn clean package -DskipTests

# Ejecutar
java -jar target/aetheris-backend-1.0.0.jar

# O en modo desarrollo
mvn spring-boot:run
```

El servidor arranca en `http://localhost:8080/api`

## Estructura del proyecto

```
src/main/java/com/aetheris/
├── AetherisApplication.java        ← Punto de entrada
├── config/
│   ├── SecurityConfig.java         ← JWT + CORS + Spring Security
│   └── GlobalExceptionHandler.java ← Manejo centralizado de errores
├── modelo/                         ← 12 Entidades JPA
│   ├── enums/                      ← 9 Enumeraciones
│   ├── Usuario.java
│   ├── Rol.java / Permiso.java
│   ├── Sesion.java
│   ├── LogAuditoria.java           ← Inmutable (sin setters)
│   ├── Sede.java
│   ├── CategoriaContable.java
│   ├── PartidaPresupuestaria.java
│   ├── CuentaBancaria.java
│   ├── TransaccionFinanciera.java
│   ├── FlujodeAprobacion.java
│   ├── Reporte.java
│   ├── ConciliacionBancaria.java
│   ├── MovimientoBancario.java
│   └── Discrepancia.java
├── dao/                            ← 12 Repositorios JPA (Spring Data)
├── servicio/                       ← 9 Services + JwtService
└── presentacion/                   ← 9 Controllers REST
```

## Endpoints principales

| Método | Ruta | Descripción |
|--------|------|-------------|
| POST | `/api/auth/login` | Iniciar sesión (devuelve JWT) |
| POST | `/api/auth/logout` | Cerrar sesión |
| GET  | `/api/auth/validar` | Validar token |
| POST | `/api/transacciones` | Registrar transacción |
| GET  | `/api/transacciones/pendientes` | Listar pendientes de aprobación |
| PUT  | `/api/aprobaciones/{id}/aprobar` | Aprobar transacción (CFO) |
| PUT  | `/api/aprobaciones/{id}/rechazar` | Rechazar transacción (CFO) |
| GET  | `/api/presupuesto/alerta` | Partidas en alerta (≥90%) |
| POST | `/api/conciliacion` | Iniciar conciliación bancaria |
| POST | `/api/conciliacion/{id}/cruce` | Ejecutar cruce automático |
| GET  | `/api/auditoria` | Consultar log de auditoría |
| POST | `/api/reportes/ingresos-egresos` | Generar reporte |

## Seguridad

- Autenticación stateless con **JWT (HS256)**
- Contraseñas hasheadas con **BCrypt**
- Bloqueo de cuenta tras **5 intentos fallidos**
- Cierre automático de sesión por **30 min de inactividad**
- **Segregación de funciones**: quien registra no puede aprobar

## Base de datos

Las tablas ya fueron creadas en MariaDB (AlwaysData).  
JPA usa `ddl-auto=validate` — no modifica el esquema existente.
