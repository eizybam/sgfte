# Plan de pruebas QA — Microservicios (Auditoría, Notificaciones, Analítica)

Cubre **RF-09** (bitácora inmutable), **RF-10** (notificaciones),
**RF-11** (analítica en JSON), **RNF-04** (trazabilidad) y **RNF-10**
(los microservicios fallan de forma independiente sin tumbar el core).

Pantallas / endpoints: `/admin/logs`, `/admin/dashboard`, `/admin/analytics.json`.

---

> ⚠️ **Estado al 2 de agosto de 2026.** El código de los tres microservicios está
> integrado, pero `AuditLogService.record(...)` y `NotificationService.send(...)`
> **todavía no se llaman desde ningún módulo**. Hasta que se cableen (tarea en
> curso), los casos A3–A6 y N1–N4 fallarán por diseño: la bitácora se verá vacía.
> No los marques como defecto, márcalos como **bloqueados** hasta ese momento.

---

## 1 · Auditoría / Logs (RF-09, RNF-04)

### 1.1 Inmutabilidad — el criterio de aceptación literal

Es el caso **CP-08** de la matriz de trazabilidad del DFR: *"los registros de
auditoría no pueden editarse ni eliminarse"*.

```sql
-- El trigger debe existir y estar habilitado
SELECT trigger_name, status FROM user_triggers
WHERE  trigger_name IN ('TRG_AUDIT_LOG_IMMUTABLE', 'TRG_ACCOUNT_MOVEMENT_IMMUTABLE');
-- Esperado: 2 filas, ambas ENABLED
```

| # | Caso | Pasos | Resultado esperado |
|---|------|-------|--------------------|
| A1 | **No se puede modificar un log** | `UPDATE audit_log SET detail = 'x' WHERE id = <n>;` | `ORA-20002: audit_log es inmutable...` |
| A2 | **No se puede borrar un log** | `DELETE FROM audit_log WHERE id = <n>;` | `ORA-20002` |
| A3 | No se puede modificar un movimiento | `UPDATE account_movement SET amount = 1 WHERE id = <n>;` | `ORA-20001` |
| A4 | No se puede borrar un movimiento | `DELETE FROM account_movement WHERE id = <n>;` | `ORA-20001` |

> Si A1 o A2 **funcionan** en vez de fallar, la base está desactualizada: falta el
> `/` de cierre del trigger. Vuelve a correr `docs/schema.sql`.

### 1.2 Registro de eventos

| # | Caso | Pasos | Resultado esperado |
|---|------|-------|--------------------|
| A5 | Vista de logs | Abrir `/admin/logs` | Tabla con los eventos más recientes primero |
| A6 | Login exitoso | Iniciar sesión y revisar la bitácora | Evento tipo LOGIN con el correo como actor |
| A7 | Login fallido | Fallar la contraseña a propósito | Queda registrado el intento (criterio de RF-01) |
| A8 | Dispersión | Dispersar fondos y revisar | Evento con monto y cuenta destino |
| A9 | Eliminación | Eliminar una cuenta (cuando exista RF-07) | Evento de baja con el actor que la ejecutó |
| A10 | Bitácora vacía | Con la tabla recién creada | Mensaje "Sin eventos registrados", sin error |
| A11 | Tolerancia a fallos | Provocar un fallo al escribir el log | La operación financiera **sí** se completa: el log es best-effort y no revierte dinero (RNF-10) |

---

## 2 · Notificaciones (RF-10)

| # | Caso | Pasos | Resultado esperado |
|---|------|-------|--------------------|
| N1 | Aviso de depósito | Dispersar a una cuenta | El tarjetahabiente recibe la notificación |
| N2 | Aviso de acceso | Iniciar sesión | Se genera el aviso de acceso |
| N3 | Aviso de eliminación | Eliminar cuenta o usuario | Se genera el aviso correspondiente |
| N4 | Registro cruzado | Tras N1, revisar `/admin/logs` | La notificación queda también en la bitácora como NOTIFICATION |
| N5 | Fallo del servicio | Con el correo mal configurado, dispersar | La dispersión se completa igual; solo se pierde el aviso (RNF-10) |

> **Alcance actual:** `NotificationService` es un stub que escribe en consola y en
> la bitácora. El envío real por correo (JavaMail) está pendiente; hasta entonces
> N1–N3 se verifican mirando la salida de consola de Tomcat, no una bandeja.

---

## 3 · Analítica (RF-11)

| # | Caso | Pasos | Resultado esperado |
|---|------|-------|--------------------|
| G1 | JSON válido | Abrir `/admin/analytics.json` | Responde `application/json` bien formado |
| G2 | Campos esperados | Revisar el cuerpo | `concentratorBalance`, `totalInAccounts`, `activeCardholders`, `activeAccounts`, `activeCards`, `movementsByType` |
| G3 | Concuerda con la BD | Comparar cada cifra con su consulta SQL | Coinciden |
| G4 | Gráfica | Abrir `/admin/dashboard` | La gráfica de barras se dibuja con los datos del JSON |
| G5 | Sin movimientos | Con `account_movement` vacía | Mensaje "no hay movimientos que graficar", sin error de JS |
| G6 | Servicio caído | Detener el endpoint y recargar el dashboard | Aviso "el servicio de analítica no está disponible"; la página no se rompe (RNF-10) |
| G7 | **Filtros** | Pedir por propósito y rango de fechas | ⚠️ **No implementado.** Es el criterio de aceptación de RF-11 y queda como defecto abierto |
| G8 | Solo administradores | Pedir el JSON con sesión de tarjetahabiente | Redirige a `/app/home`; no entrega datos (RNF-05) |

**Verificación de G3**

```sql
SELECT (SELECT balance FROM concentrator_account WHERE singleton='Y')   AS concentradora,
       (SELECT NVL(SUM(balance),0) FROM account WHERE status='ACTIVE')  AS en_cuentas,
       (SELECT COUNT(*) FROM cardholder WHERE status='ACTIVE')          AS tarjetahabientes,
       (SELECT COUNT(*) FROM account   WHERE status='ACTIVE')           AS cuentas,
       (SELECT COUNT(*) FROM card      WHERE status='ACTIVE')           AS tarjetas
FROM dual;
```

---

## Notas de implementación

- El ledger de dinero (`account_movement`) se escribe **dentro** de la
  transacción del core; la bitácora de sistema (`audit_log`) se escribe
  **después del commit**. Esa separación es intencional: un fallo al registrar un
  evento nunca debe revertir una operación con dinero.
- Ambas tablas están protegidas por triggers `BEFORE UPDATE OR DELETE`. La
  inmutabilidad es de la base, no de la aplicación: ni siquiera un `UPDATE`
  manual desde SQL Developer puede alterarlas.
- `AuditLogService.record(...)` traga sus propias excepciones a propósito
  (best-effort), que es lo que hace pasar el caso A11.
