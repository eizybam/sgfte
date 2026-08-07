# Plan de pruebas QA — Área del Tarjetahabiente (`/app/*`)

Cubre **HU-07** (transferencia P2P), **HU-08** (historial), **RN-07** (mismo propósito),
**RNF-05** (privacidad) y **RNF-03** (integridad financiera).

---

## Datos de prueba

Necesitas **dos empleados distintos**, cada uno con cuentas, y al menos un
propósito compartido entre ambos más uno que no compartan.

```sql
-- Logins para los dos tarjetahabientes del seed (contraseña: Sgfte2026$)
INSERT INTO app_user (email, password_hash, full_name, role, cardholder_id)
VALUES ('juanlopez@empresa.com',
        '$2a$12$P6s7lL4fFj3Qcy0Szxi1g.mKC1LD6DlVRn6xKyiNTD7yK4am6T2ky',
        'Juan Lopez', 'TARJETAHABIENTE',
        (SELECT id FROM cardholder WHERE email = 'juanlopez@empresa.com'));

INSERT INTO app_user (email, password_hash, full_name, role, cardholder_id)
VALUES ('raultorres@empresa.com',
        '$2a$12$P6s7lL4fFj3Qcy0Szxi1g.mKC1LD6DlVRn6xKyiNTD7yK4am6T2ky',
        'Raul Torres', 'TARJETAHABIENTE',
        (SELECT id FROM cardholder WHERE email = 'raultorres@empresa.com'));
COMMIT;
```

Después, **desde la interfaz de administrador** (para que los datos nazcan por el
flujo real, no por INSERT a mano):

1. Crea a Juan una cuenta de **Gasolina** y otra de **Alimentos**.
2. Crea a Raúl una cuenta de **Gasolina** (propósito compartido) y una de **Viajes** (no compartido).
3. Dispersa saldo a las cuentas de Gasolina de ambos (ej. $1,000 cada una).
4. Emite una tarjeta física y una digital a la cuenta de Gasolina de Juan.

Anota los `id` que vas a necesitar:

```sql
SELECT a.id, c.first_name, cat.name AS proposito, a.account_number, a.balance
FROM   account a
JOIN   cardholder c ON c.id = a.cardholder_id
JOIN   category cat ON cat.id = a.category_id
ORDER  BY c.first_name, cat.name;
```

---

## 1 · Casos funcionales

| # | Caso | Pasos | Resultado esperado |
|---|------|-------|--------------------|
| P1 | Panel principal | Login como Juan → `/app/home` | Ve **sus dos** cuentas (Gasolina, Alimentos) con propósito, código, saldo y número de tarjetas |
| P2 | Saldo total | Misma pantalla | El total mostrado es la suma exacta de los saldos de sus cuentas |
| P3 | Empleado sin cuentas | Login con un tarjetahabiente sin cuentas | Mensaje "Todavía no tienes cuentas asignadas"; **sin error** |
| P4 | Detalle de cuenta | Clic en la tarjeta de Gasolina | Saldo, las 2 tarjetas con tipo y estado, y el historial de esa cuenta |
| P5 | Tarjeta enmascarada | Misma pantalla | Solo se ven los últimos 4 dígitos (`**** **** **** 1234`) |
| P6 | Historial legible | Misma pantalla | Los tipos salen en español (Depósito, Transferencia enviada…), entradas en verde con `+`, salidas con `−` |
| P7 | Cuenta sin movimientos | Abre la cuenta de Alimentos | "Sin movimientos todavía"; sin tabla vacía ni error |
| P8 | Navegación | Usa el encabezado | "Mis cuentas" y "Transferir" funcionan desde cualquier pantalla; "Cerrar sesión" cierra |

---

## 2 · Transferencia P2P (HU-07, RN-07)

| # | Caso | Pasos | Resultado esperado |
|---|------|-------|--------------------|
| T1 | Solo mis cuentas como origen | Juan → `/app/transferencia` | El desplegable de origen lista **únicamente** las cuentas de Juan |
| T2 | Destinos del mismo propósito | Elige Gasolina como origen | Aparece la cuenta de Gasolina **de Raúl**; **no** aparece la de Viajes de Raúl ni ninguna de Juan |
| T3 | Sin destinos posibles | Elige Alimentos como origen (nadie más lo tiene) | Mensaje "Ningún compañero tiene una cuenta con este propósito"; no se muestra el formulario |
| T4 | Transferencia válida | Gasolina de Juan → Gasolina de Raúl, $200 | Mensaje de éxito; Juan −200, Raúl +200 |
| T5 | Doble asiento en el ledger | Tras T4, revisa el historial de ambos | Juan ve "Transferencia enviada −$200"; Raúl ve "Transferencia recibida +$200" |
| T6 | Saldo insuficiente | Monto mayor al saldo de Juan | Error "saldo insuficiente"; **ningún** saldo cambia |
| T7 | Monto inválido | Monto `0` o negativo | Error de validación; nada cambia |
| T8 | Atomicidad | Tras cada error de T6/T7, consulta ambos saldos | Idénticos a antes del intento |

### Verificación en BD tras T4

```sql
SELECT account_id, movement_type, amount, related_account_id
FROM   account_movement
ORDER  BY id DESC FETCH FIRST 2 ROWS ONLY;
-- Esperado: (cuenta de Raúl, TRANSFER_IN, 200, cuenta de Juan)
--           (cuenta de Juan, TRANSFER_OUT, 200, cuenta de Raúl)
```

---

## 3 · Aislamiento entre empleados (lo más importante)

Estos casos verifican que **un empleado no puede ver ni tocar el dinero de otro**
manipulando la URL o el formulario. Es la clase de falla que en un sistema
financiero real es crítica, y la única forma de detectarla es probándola a mano.

> Para ejecutarlos necesitas el `id` de una cuenta **de Raúl** y una sesión
> iniciada **como Juan**.

| # | Caso | Pasos | Resultado esperado |
|---|------|-------|--------------------|
| A1 | Leer la cuenta de otro | Como Juan, abre `/app/cuenta?id=<cuenta_de_Raul>` | **404**. No se muestra ni saldo, ni tarjetas, ni movimientos |
| A2 | Sin distinguir inexistente | Como Juan, abre `/app/cuenta?id=999999` | **404 idéntico** al de A1 (la respuesta no revela si la cuenta existe) |
| A3 | Parámetro ausente | `/app/cuenta` sin `?id=` | Redirige a `/app/home`; sin excepción |
| A4 | Parámetro basura | `/app/cuenta?id=abc` | Redirige a `/app/home`; sin excepción |
| A5 | **Transferir desde la cuenta de otro** | Como Juan, `POST /app/transferencia` con `sourceId` = cuenta de Raúl | **404**; el saldo de Raúl **no cambia** |
| A6 | Origen ajeno manipulado en el HTML | Edita el `<input type="hidden" name="sourceId">` con DevTools y envía | Igual que A5: 404 y sin movimiento |
| A7 | Sesión ajena tras cerrar sesión | Juan cierra sesión, botón Atrás, recarga `/app/home` | Redirige a `/login` |
| A8 | Admin en el área de empleado | Login como admin → `/app/home` | Redirige a `/admin/home` (no tiene ficha de tarjetahabiente) |

### Cómo ejecutar A5 sin herramientas extra

```bash
# 1) Inicia sesión y guarda la cookie
curl -c /tmp/juan.txt -X POST http://localhost:8080/core/login \
     -d "email=juanlopez@empresa.com&password=Sgfte2026\$"

# 2) Intenta transferir DESDE la cuenta de Raúl (sustituye los ids)
curl -b /tmp/juan.txt -i -X POST http://localhost:8080/core/app/transferencia \
     -d "sourceId=<cuenta_de_Raul>&destId=<cuenta_de_Juan>&amount=500"
# Esperado: HTTP/1.1 404
```

Después confirma que no se movió nada:

```sql
SELECT id, balance FROM account WHERE id IN (<cuenta_de_Raul>, <cuenta_de_Juan>);
SELECT COUNT(*) FROM account_movement WHERE amount = 500;  -- debe ser 0
```

---

## 4 · Invariante financiera (RNF-03)

Vale la pena correrla al final de toda la sesión de pruebas:

```sql
-- El dinero no se crea ni se destruye: este total debe ser el mismo
-- antes y después de CUALQUIER cantidad de transferencias P2P.
SELECT (SELECT balance FROM concentrator_account WHERE singleton = 'Y')
     + (SELECT NVL(SUM(balance), 0) FROM account WHERE status = 'ACTIVE')
       AS dinero_total
FROM dual;
```

Las transferencias P2P mueven dinero entre cuentas, así que **este total no debe
cambiar nunca** por una transferencia. Solo cambia al fondear la Concentradora.

---

## Notas de implementación

- Toda consulta de `/app` está acotada por `cardholder_id` **dentro del `WHERE`**
  (`PortalDao`), no por un `if` en el servlet. Pedir una cuenta ajena no devuelve
  filas, así que el caso A1 no depende de que nadie olvide una validación.
- El `cardholderId` sale **siempre de la sesión** (`SessionUser.getCardholderId()`),
  nunca de un parámetro de la petición.
- `PortalService.transfer` solo agrega la comprobación de propiedad del origen;
  la regla de mismo propósito, el saldo y la atomicidad siguen viviendo en
  `TransferService`, que es el mismo que usa la pantalla de administrador.
