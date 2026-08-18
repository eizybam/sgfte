# SGFTE — Sistema de Gestión de Fondos y Tarjetas Empresariales

Proyecto Integrador · Equipo 4.

Una empresa fondea una **Cuenta Concentradora**, dispersa dinero a cuentas de
empleados etiquetadas por propósito, expide tarjetas contra esas cuentas y
**reintegra el saldo automáticamente** cuando una cuenta o un empleado se dan de
baja.

---

## Levantar el sistema

Requisitos: **Docker** y **Docker Compose**. (Sólo para desarrollo: JDK 21.)

```bash
cp .env.example .env      # y rellena las variables (ver abajo)
docker compose up --build
```

- Aplicación: <http://localhost:8080>
- Administrador de prueba: `admin@empresa.com` / `sgfte`
- Empleado de prueba: `juanlopez@empresa.com` / `sgfte`

La base se crea sola desde `docker/init-schema.sql` y se llena con
`docker/demo-data.sql`.

> **Esos scripts sólo corren con el volumen vacío.** Si cambias el esquema:
> `docker compose down -v && docker compose up --build`, o los cambios se
> ignoran en silencio y la aplicación falla contra una base vieja.

### Variables de entorno (`.env`)

| Variable | Para qué |
|---|---|
| `ORACLE_PASSWORD` | Contraseña de `SYSTEM` en el contenedor de Oracle |
| `APP_USER` / `APP_USER_PASSWORD` | Usuario de aplicación y su contraseña |
| `SGFTE_MAIL_HOST` · `_PORT` · `_SSL` | Servidor SMTP de notificaciones |
| `SGFTE_MAIL_USER` · `_PASSWORD` | Credenciales SMTP |
| `SGFTE_APP_BASE_URL` | Base de los enlaces de activación que se mandan por correo |

Ningún secreto está versionado: `.env` está en `.gitignore` y
`docker-compose.yaml` sólo lee `${VARIABLES}`.

### Compilar y probar sin Docker

```bash
cd core
./mvnw clean package     # genera target/core-1.0-SNAPSHOT.war
./mvnw test              # 80 pruebas unitarias, sin base de datos
```

`mvn clean` no es opcional: una clase vieja en `target/` con el mismo
`@WebServlet` que una nueva impide que Tomcat arranque, y el error habla de
mapeos duplicados, no del cambio que lo provocó.

---

## Arquitectura

Híbrida, como pide la asignatura: **monolito modular** para todo lo que toca
dinero, más **microservicios periféricos** (notificaciones, auditoría,
analíticas) que pueden fallar sin tumbar el núcleo. Un solo esquema Oracle.

Cada módulo del núcleo repite las mismas cinco capas:

```
Vista (JSP) → Controlador (Servlet) → Servicio → DAO → Modelo
```

| Capa | Hace | Nunca hace |
|---|---|---|
| JSP | pintar, formularios | lógica de negocio, SQL |
| Servlet | leer parámetros, invocar al servicio, elegir vista | reglas de negocio, SQL |
| Servicio | reglas **+ transacciones**, llama a microservicios | HTTP, HTML |
| DAO | JDBC tonto | reglas, decisiones |

**Regla de oro: el dinero sólo se mueve dentro de un Servicio, dentro de una
transacción.** El DAO no decide nada; sólo el Servicio coordina varios DAOs de
forma atómica. La base refuerza la integridad por su cuenta: restricciones
`CHECK`, y **triggers de inmutabilidad** sobre `account_movement`,
`notification` y `audit_log` — la bitácora no se puede editar ni borrar, ni
siquiera desde SQL.

### Privacidad en el área del empleado (RNF-05)

El área de administración puede leer cualquier cuenta. El portal del empleado no.
La propiedad se comprueba **dentro del `WHERE`**, nunca con un `if`:

```java
// ✅ PortalDao
"SELECT ... FROM account a WHERE a.id = ? AND a.cardholder_id = ? AND a.status = 'ACTIVE'"
```

Si se olvida el `if`, el dato ya se leyó; si se olvida el `WHERE`, la consulta no
devuelve nada. El `cardholderId` sale **siempre de la sesión**
(`PortalSupport.cardholderId(req)`), nunca de un parámetro. La cuenta de otro
devuelve cero filas → 404, igual que una que no existe: un 403 confirmaría que
esa cuenta existe.

---

## Mapa de URLs

| Área | URL | Pantalla |
|---|---|---|
| Pública | `/login` · `/forgot-password` · `/set-password` | Acceso y contraseñas |
| Admin | `/admin/home` | Vista general y dispersión |
| | `/admin/concentradora` | Concentradora: fondeo y ledger |
| | `/admin/cuentas` · `/admin/cuenta?id=` | Cuentas y detalle |
| | `/admin/empleados` · `/admin/empleado?id=` | Empleados y detalle |
| | `/admin/cards` | Expedición y ciclo de vida de tarjetas |
| | `/admin/categorias` | Catálogos: propósitos y departamentos |
| | `/admin/departamentos` | Escrituras del catálogo de áreas (POST) |
| | `/admin/dashboard` · `/admin/analytics.csv` | Analíticas y exportación |
| | `/admin/logs` · `/admin/logs.csv` | Bitácora y exportación |
| | `/admin/movimientos` | Ledger global: los dos libros, con filtros |
| | `/admin/ajustes` | Perfil, contraseña y sesión |
| Portal | `/app/home` · `/app/cuenta` · `/app/tarjetas` | Empleado |
| | `/app/movimientos` · `/app/notificaciones` · `/app/gasto` | |
| | `/app/ajustes` | Perfil, contraseña y sesión |

`/admin/*` está protegido por `AuthFilter` (sesión + rol ADMIN) y `/app/*` por
`AppAuthFilter` (sesión + login ligado a una ficha de empleado).

---

## Estados y ciclos de vida

| Entidad | Estados | Notas |
|---|---|---|
| Cuenta | `ACTIVE` → `INACTIVE` | Se cierra, no se borra ni se reabre. Al cerrarla el saldo vuelve a la Concentradora |
| Tarjeta | `ACTIVE` ↔ `BLOCKED` → `INACTIVE` | Bloquear es temporal y lo puede hacer el propio empleado; invalidar es definitivo |
| Empleado | `ACTIVE` ↔ `INACTIVE` | Al darlo de baja se reintegran todas sus cuentas; al reincorporarlo vuelve sin cuentas ni tarjetas |
| Categoría / Departamento | `ACTIVE` ↔ `INACTIVE` | Retirar los saca de los desplegables; lo que ya los usaba los conserva |

---

## Base de datos

- `docs/schema.sql` — el esquema completo y comentado. `docker/init-schema.sql`
  es el mismo archivo con dos `ALTER SESSION` delante.
- `docs/V*.sql` — migraciones en orden, para una base que ya existe.
- `docker/demo-data.sql` — seis meses de movimientos de demostración.

---

## Documentación

`docs/` contiene también las historias de usuario, los planes de prueba
(`QA_*.md`) y la matriz de trazabilidad `MATRIZ_QA.md`, que enlaza cada caso de
prueba con su requerimiento y su evidencia.
