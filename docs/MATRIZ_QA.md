# Matriz de ejecución de pruebas — SGFTE

**Entregables:** T21 (Ejecución de casos de prueba) y T22 (Pruebas de integración y regresión)
**Responsable de QA:** Salvador · **Versión del sistema:** rama `develop`
**Última actualización:** _(anotar en cada corrida)_

---

## Cómo se usa este documento

1. Ejecuta los casos siguiendo el plan de cada módulo (columna *Plan*).
2. Llena **Resultado** con `PASA`, `NO PASA` o `BLOQUEADO`.
3. Si es `NO PASA`, abre una fila en la [sección 4](#4--bugs-encontrados) con el detalle.
4. Anota fecha y quién ejecutó. Sin eso no es evidencia.
5. Guarda la captura de pantalla como `evidencias/<caso>.png` y referénciala.

> **Regla:** un caso `BLOQUEADO` no es lo mismo que `NO PASA`. Bloqueado = no se
> pudo probar porque la funcionalidad todavía no existe. No pasa = existe y está mal.

---

## 1 · Trazabilidad del DFR (CP-01 … CP-09)

Estos son los casos que el DFR compromete formalmente en su matriz de
trazabilidad. Son los que hay que poder demostrar el día de la entrega.

| CP | Objetivo | HU | RF | Caso concreto | Plan | Resultado | Fecha | Ejecutó |
|----|----------|----|----|---------------|------|-----------|-------|---------|
| CP-01 | OBJ-01 | HU-01 | RF-02 | Registrar tarjetahabiente con cuenta | QA_M2 · E1, C1 | | | |
| CP-02 | OBJ-01 | HU-02 | RF-06 | Dispersar fondos a una cuenta | QA_M1 · D1, D2 | | | |
| CP-03 | OBJ-02 | HU-03 | RF-04 | Emitir tarjeta ligada a una cuenta | QA_M2 · T1, T4 | | | |
| CP-04 | OBJ-03 | HU-04 | RF-07 | Cerrar cuenta y verificar reintegración | QA_M4 · R1–R4 | | | |
| CP-05 | OBJ-03 | HU-05 | RF-07 | Dar de baja a un empleado e invalidar sus tarjetas | QA_M4 · R5–R8 | | | |
| CP-06 | OBJ-03 | HU-07 | RF-08 | Transferir P2P del mismo propósito | QA_M3 · T1, T2 | | | |
| CP-07 | OBJ-04 | HU-06 | RF-10 | Notificar depósito, acceso y eliminación | QA_MICRO · N1–N3 | | | |
| CP-08 | OBJ-04 | HU-08 | RF-09 | Verificar inmutabilidad de los logs | QA_MICRO · A1–A4 | | | |
| CP-09 | OBJ-04 | HU-09 | RF-11 | Analítica por propósito y periodo | QA_MICRO · G1–G3, G7 | | | |

✅ **CP-04 y CP-05 ya son ejecutables desde la interfaz** (2026-08-17). La
reintegración estaba implementada en `DeletionService` desde hace semanas, pero
ninguna pantalla podía dispararla: el cierre de cuenta no tenía botón. Hoy vive
en el badge de ESTADO de `/admin/cuentas`, y la baja de empleado en el de
`/admin/empleados`, las dos con confirmación previa.

**Cómo se verifican:** apunta el saldo de la Concentradora antes, ejecuta, y
comprueba que subió *exactamente* el saldo que tenía la cuenta (o la suma de
todas las del empleado), que quedó un movimiento `REINTEGRATION` por cuenta con
saldo, y que ninguna de sus tarjetas sigue activa.

---

## 2 · Cobertura por requerimiento funcional

| RF | Requerimiento | Plan de pruebas | Casos | Resultado global |
|----|---------------|-----------------|-------|------------------|
| RF-01 | Autenticación | QA_SEGURIDAD_ROLES | S1, S7–S12 | login · activación · recuperación · **cambio de contraseña en /ajustes** |
| RF-02 | Gestión de tarjetahabientes | QA_M2 | E1–E8 | alta · **edición de ficha** · baja/reincorporación · catálogo de departamentos |
| RF-03 | Gestión de cuentas | QA_M2 | C1–C10 | |
| RF-04 | Gestión de tarjetas | QA_M2 | T1–T9 | expedir · **bloquear/reactivar** · invalidar |
| RF-05 | Cuenta Concentradora | QA_M1 | F1–F7 | |
| RF-06 | Dispersión de fondos | QA_M1 | D1–D8 | |
| RF-07 | **Reintegración automática** | QA_M4 | R1–R8 | ✅ implementada y disparable desde la UI |
| RF-08 | Transferencias P2P | QA_M3 + QA_PORTAL | T1–T8 | |
| RF-09 | Auditoría / Logs | QA_MICROSERVICIOS | A1–A11 | |
| RF-10 | Notificaciones | QA_MICROSERVICIOS | N1–N5 | |
| RF-11 | Analítica | QA_MICROSERVICIOS | G1–G8 | filtro por periodo + exportación CSV |
| RF-12 | Categorías / propósitos | QA_M2 | — | ✅ ABC completo: alta, edición, retiro y reactivación |
| RF-13 | Usuarios y roles | QA_SEGURIDAD_ROLES | S1–S6 | |

---

## 3 · Requerimientos no funcionales

| RNF | Criterio | Cómo se comprueba | Resultado |
|-----|----------|-------------------|-----------|
| RNF-01 | Contraseñas nunca en texto plano | `SELECT password_hash FROM app_user;` → todos empiezan con `$2a$` | |
| RNF-03 | **Integridad financiera** | La invariante de QA_M1 §3: el dinero total no cambia con transferencias ni con operaciones fallidas | |
| RNF-04 | Trazabilidad | QA_MICROSERVICIOS A1–A4 (inmutabilidad) | |
| RNF-05 | Privacidad | QA_PORTAL §3 (aislamiento entre empleados) + QA_SEGURIDAD_ROLES S4–S5 | |
| RNF-06 | Rendimiento | Buscar un usuario/cuenta responde en < 3 s con datos de prueba | |
| RNF-09 | Solo MXN | `SELECT DISTINCT currency FROM account;` → únicamente `MXN` | |
| RNF-02 | Usabilidad | Recorrer las 11 pantallas en escritorio y tablet, **pulsando todo lo que parezca un enlace**: ningún elemento debe quedarse quieto | |

### Invariante financiera — ejecutar al inicio y al final de cada corrida

```sql
SELECT (SELECT balance FROM concentrator_account WHERE singleton = 'Y')
     + (SELECT NVL(SUM(balance), 0) FROM account WHERE status = 'ACTIVE')
       AS dinero_total
FROM dual;
```

| Momento | Valor | Notas |
|---------|-------|-------|
| Inicio de la corrida | | |
| Después de las pruebas de dispersión | | Sube solo por fondeo |
| Después de las pruebas P2P | | **No debe cambiar** |
| Después de los casos fallidos | | **No debe cambiar** |
| Fin de la corrida | | |

---

## 4 · Bugs encontrados

| # | Caso | Descripción | Severidad | Estado | Corregido en |
|---|------|-------------|-----------|--------|--------------|
| B-01 | — | `/accounts` y `/cardholders` respondían sin sesión: cualquiera podía dar de alta cuentas y empleados | **Crítica** | Corregido | `894a9e7` |
| B-02 | — | Un tarjetahabiente alcanzaba toda el área de administración escribiendo la URL | **Crítica** | Corregido | `894a9e7` |
| B-03 | — | El trigger de inmutabilidad de `audit_log` nunca se creaba (faltaba el `/`): la bitácora era editable | **Alta** | Corregido | `51df6a7` |
| B-04 | — | StackOverflowError al abrir cualquier pantalla: los fragmentos compartidos se incluían a sí mismos | **Crítica** | Corregido | `76be4a5` |
| B-05 | — | La raíz de la aplicación mostraba el "Hello World" del arquetipo de IntelliJ | Baja | Corregido | `7429c95` |
| | | | | | |

Severidades: **Crítica** (dinero o seguridad) · **Alta** (funcionalidad comprometida) ·
**Media** (funciona con rodeos) · **Baja** (cosmético).

---

## 5 · Pruebas automatizadas (JUnit)

`cd core && mvn test`

| Clase | Qué cubre | Casos |
|-------|-----------|-------|
| `AccountTest` | Depósito y retiro sobre el POJO | 2 |
| `AccountServiceTest` | Alta de cuenta, propósito duplicado, generación del código | 9 |
| `CardholderServiceTest` | Validación y correo duplicado | 6 |
| `CardServiceTest` | Expedición, tipos, PAN enmascarado, invalidación | 9 |
| `ConcentratorServiceTest` | Fondeo y validación de dispersión | 6 |
| `TransferServiceValidationTest` | Validación de entrada de la transferencia P2P | 6 |
| `PortalServiceTest` | **Aislamiento entre empleados** (no leer ni transferir desde cuentas ajenas) | 12 |
| | **Total** | **50** |

### Alcance: qué NO cubren y por qué

`TransferService` y `DispersionService` validan sus argumentos y **después**
llaman a `Db.getConnection()`, que es una fábrica estática. No hay forma de
inyectarle una conexión falsa, así que todo lo que ocurre después de esa llamada
queda fuera del alcance automatizable:

| Regla | Dónde se verifica en su lugar |
|-------|-------------------------------|
| Solo transferir entre el mismo propósito (RN-07) | QA_M3 · T2 (manual) |
| Saldo suficiente (RN-08) | QA_M3 · T3 y QA_M1 · D6 (manual) |
| Atomicidad / rollback (RNF-03) | QA_M3 · T7 y QA_M1 · D8 (manual) |
| Doble asiento en el ledger | QA_M3 · H1, H2 (manual) |

**Recomendación para después de la entrega:** darle a los dos servicios una
fuente de conexión inyectable (un `Supplier<Connection>` con `Db::getConnection`
por defecto). Son unas 10 líneas y volvería automatizables las cuatro reglas de
arriba, que son justamente las que sostienen RNF-03. No se hizo ahora porque
implica tocar código que mueve dinero a pocos días de la entrega.

---

## 6 · Registro de corridas

| Fecha | Versión / commit | Ejecutó | Casos | Pasa | No pasa | Bloqueados | Notas |
|-------|------------------|---------|-------|------|---------|------------|-------|
| | | | | | | | |
