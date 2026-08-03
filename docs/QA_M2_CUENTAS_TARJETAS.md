# Plan de pruebas QA — Módulo 2 (Tarjetahabientes, Cuentas y Tarjetas)

Cubre **RF-02** (tarjetahabientes), **RF-03** (cuentas por propósito),
**RF-04** (tarjetas), **RN-01** (el dinero vive en la cuenta),
**RN-03** (jerarquía 1-N-N) y **RN-06** (borrar tarjeta no mueve dinero).

Pantallas: `/cardholders`, `/accounts`, `/admin/cards`.

---

## 1 · Alta de tarjetahabiente (RF-02)

| # | Caso | Pasos | Resultado esperado |
|---|------|-------|--------------------|
| E1 | Alta válida | Nombre, apellido, correo y teléfono correctos | Mensaje con el ID nuevo; fila en `cardholder` con status ACTIVE |
| E2 | Teléfono opcional | Alta sin teléfono | Se registra igual; `phone` queda NULL |
| E3 | Nombre vacío | Dejar el nombre en blanco | Error "El nombre es obligatorio" |
| E4 | Apellido vacío | Dejar el apellido en blanco | Error "El apellido es obligatorio" |
| E5 | Correo inválido | `juan@`, `juan`, `@empresa.com` | Error "El correo no es válido" |
| E6 | **Correo duplicado** | Repetir un correo ya registrado | Error "El correo ya está registrado"; **no** se inserta |
| E7 | Varios errores juntos | Nombre vacío + correo inválido | Se listan los dos a la vez, no de uno en uno |
| E8 | Espacios en blanco | Nombre `"   "` | Se trata como vacío y se rechaza |

**Verificación tras E1**

```sql
SELECT id, first_name, last_name, email, status FROM cardholder ORDER BY id DESC FETCH FIRST 1 ROWS ONLY;
```

---

## 2 · Alta de cuenta por propósito (RF-03)

| # | Caso | Pasos | Resultado esperado |
|---|------|-------|--------------------|
| C1 | Alta válida | Elegir tarjetahabiente + propósito | Cuenta creada con código público (p. ej. `GAS-48HSY`) y **saldo $0.00** |
| C2 | Saldo inicial | Revisar la cuenta recién creada | `balance = 0`; el dinero solo entra por dispersión (RN-02) |
| C3 | Código legible | Ver el `account_number` generado | 3 letras del propósito + guion + 5 caracteres, sin O/0 ni I/1 |
| C4 | Prefijo con acento | Crear una cuenta de "Viáticos" | Código empieza con `VIA-`, nunca con `VIÁ` |
| C5 | Sin tarjetahabiente | Enviar sin elegir empleado | Error "Debes elegir un tarjetahabiente" |
| C6 | Sin propósito | Enviar sin elegir categoría | Error "Debes elegir un propósito" |
| C7 | **Propósito duplicado** | Crear una segunda cuenta de Gasolina al mismo empleado | Error "ya tiene una cuenta con ese propósito"; no se inserta |
| C8 | Mismo propósito, otro empleado | Crear Gasolina para un empleado distinto | **Sí** se permite: la restricción es por empleado, no global |
| C9 | Varias cuentas por empleado | Crear Gasolina y Alimentos al mismo empleado | Ambas conviven (jerarquía 1-N, RN-03) |
| C10 | Moneda | Revisar la fila creada | `currency = 'MXN'` (RNF-09); el `CHECK` impide otra |

**Verificación tras C1**

```sql
SELECT a.id, a.account_number, a.balance, a.currency, cat.name AS proposito
FROM   account a JOIN category cat ON cat.id = a.category_id
ORDER  BY a.id DESC FETCH FIRST 1 ROWS ONLY;
```

---

## 3 · Expedición de tarjetas (RF-04)

| # | Caso | Pasos | Resultado esperado |
|---|------|-------|--------------------|
| T1 | Tarjeta física | Expedir PHYSICAL sobre una cuenta activa | Tarjeta creada, estado ACTIVA, ligada a esa cuenta |
| T2 | Tarjeta digital | Expedir DIGITAL | Igual, con tipo DIGITAL |
| T3 | Varias tarjetas por cuenta | Expedir 3 tarjetas a la misma cuenta | Las tres conviven (jerarquía N, RN-03) |
| T4 | **PAN enmascarado** | Ver cualquier tarjeta | Solo `**** **** **** 1234`; nunca un número completo |
| T5 | Sin cuenta | Enviar sin elegir cuenta | Error "Debes elegir una cuenta" |
| T6 | Cuenta inactiva | Expedir sobre una cuenta INACTIVE | Error "La cuenta no existe o está inactiva" |
| T7 | Invalidar tarjeta | Invalidar una tarjeta activa | Pasa a INACTIVA; desaparece el botón de invalidar |
| T8 | Invalidar dos veces | Invalidar una ya inactiva | Error "no existe o ya estaba inactiva" |
| T9 | **RN-06: invalidar NO mueve dinero** | Anotar el saldo, invalidar una tarjeta, volver a consultarlo | Saldo **idéntico**; ningún movimiento nuevo en el ledger |

**Verificación tras T9 — la prueba clave del módulo**

```sql
-- Antes y después de invalidar: los dos valores deben coincidir.
SELECT balance FROM account WHERE id = <cuenta>;
SELECT COUNT(*) FROM account_movement WHERE account_id = <cuenta>;
```

---

## 4 · Vista del tarjetahabiente

| # | Caso | Pasos | Resultado esperado |
|---|------|-------|--------------------|
| V1 | Tarjetas visibles | Entrar como el empleado dueño y abrir la cuenta | Aparecen sus tarjetas con tipo y estado |
| V2 | Tarjeta inactiva | Con una tarjeta invalidada | Se muestra atenuada y marcada como Inactiva |
| V3 | Conteo en el panel | Ver `/app/home` | El número de "tarjetas activas" concuerda con las ACTIVE |

---

## Notas de implementación

- El `account_number` se genera en Java y se valida contra el `UNIQUE` de la
  base: si colisiona, el servicio regenera y reintenta hasta 10 veces.
- El alfabeto del código excluye O/0 e I/1 a propósito, para que se pueda dictar
  por teléfono sin ambigüedad.
- `CardService` no tiene acceso a `AccountDao`: por construcción no puede tocar
  un saldo, que es la razón estructural por la que RN-06 no se puede romper por
  descuido.
- Cobertura automatizada: `CardholderServiceTest` (E1–E8), `AccountServiceTest`
  (C1, C3–C7), `CardServiceTest` (T1–T8). Los casos con base de datos real
  (C8–C10, T9, V1–V3) son manuales.
