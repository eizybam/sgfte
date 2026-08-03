# Plan de pruebas QA — Módulo 1 (Concentradora y Dispersión)

Cubre **RF-05** (fondear y consultar la Concentradora), **RF-06** (dispersión),
**RN-02** (fuente única de fondos), **RN-08** (sin sobregiro) y **RNF-03** (integridad).

Pantallas: `/admin/concentradora`, `/admin/dispersion`.

---

## Datos de prueba

El seed de `docs/schema.sql` ya crea la Concentradora con $1,000,000.00. Antes de
empezar, anota el estado inicial — lo vas a comparar al final:

```sql
SELECT balance FROM concentrator_account WHERE singleton = 'Y';
SELECT NVL(SUM(balance), 0) FROM account WHERE status = 'ACTIVE';
```

Necesitas además una cuenta activa (creada desde `/accounts`) y, para D7, una
cuenta puesta a INACTIVE a mano:

```sql
UPDATE account SET status = 'INACTIVE' WHERE id = <id>;
COMMIT;
```

---

## 1 · Fondeo de la Concentradora (RF-05)

| # | Caso | Pasos | Resultado esperado |
|---|------|-------|--------------------|
| F1 | Consulta de saldo | Abrir `/admin/concentradora` | Muestra el saldo actual en MXN |
| F2 | Fondeo válido | Fondear $50,000 | Saldo sube exactamente $50,000; mensaje de éxito |
| F3 | Monto cero | Fondear `0` | Error "debe ser mayor a 0"; saldo sin cambio |
| F4 | Monto negativo | Fondear `-100` | Error de validación; saldo sin cambio |
| F5 | Campo vacío | Enviar sin monto | El navegador exige el campo (required) |
| F6 | Decimales | Fondear `1234.56` | Se registra con dos decimales exactos |
| F7 | Texto no numérico | Escribir `abc` | El campo `type=number` lo rechaza; nada se envía |

**Verificación tras F2**

```sql
SELECT balance FROM concentrator_account WHERE singleton = 'Y';
```

---

## 2 · Dispersión a una cuenta (RF-06)

| # | Caso | Pasos | Resultado esperado |
|---|------|-------|--------------------|
| D1 | Dispersión válida | $500 de la Concentradora a una cuenta activa | Concentradora −500, cuenta +500, mensaje de éxito |
| D2 | Movimiento en el ledger | Tras D1, revisar `account_movement` | Aparece un `DEPOSIT` de 500 con la descripción capturada |
| D3 | Descripción por defecto | Dispersar sin escribir descripción | El movimiento queda como "Dispersión desde Concentradora" |
| D4 | Sin cuenta destino | Enviar sin elegir cuenta | Error "Debes elegir una cuenta destino"; nada cambia |
| D5 | Monto no positivo | Monto `0` o negativo | Error de validación; nada cambia |
| D6 | **Saldo insuficiente** | Dispersar un monto mayor al saldo de la Concentradora | Error "La Concentradora no tiene saldo suficiente"; **ningún** saldo cambia |
| D7 | Cuenta inactiva | Dispersar a una cuenta con status INACTIVE | Error "no existe o está inactiva"; rollback completo |
| D8 | **Atomicidad** | Tras D6 y D7, comparar ambos saldos con los de antes | Idénticos: o pasa todo, o no pasa nada |

**Verificación tras D1**

```sql
SELECT account_id, movement_type, amount, description
FROM   account_movement
ORDER  BY id DESC FETCH FIRST 1 ROWS ONLY;
-- Esperado: (cuenta destino, DEPOSIT, 500, '...')
```

---

## 3 · Invariante financiera (RNF-03) — la prueba que resume todo

El dinero no se crea ni se destruye: **la dispersión mueve saldo, no lo genera**.

```sql
SELECT (SELECT balance FROM concentrator_account WHERE singleton = 'Y')
     + (SELECT NVL(SUM(balance), 0) FROM account WHERE status = 'ACTIVE')
       AS dinero_total
FROM dual;
```

| # | Caso | Resultado esperado |
|---|------|--------------------|
| I1 | Total antes vs. después de D1 | **Idéntico** (solo se movió entre bolsas) |
| I2 | Total antes vs. después de D6 y D7 (fallidos) | **Idéntico** |
| I3 | Total después de F2 (fondeo) | Sube exactamente el monto fondeado — es la única entrada de dinero al sistema (RN-02) |

---

## Notas de implementación

- La dispersión es una sola transacción: débito a la Concentradora, abono a la
  cuenta y escritura en el ledger. Si cualquier paso falla se revierte todo
  (`DispersionService`, `conn.setAutoCommit(false)` → `commit`/`rollback`).
- El débito usa `WHERE ... AND balance >= ?`, así que la base misma impide el
  sobregiro aunque la validación en Java fallara.
- El `CHECK chk_concentrator_balance` (>= 0) es la última red de seguridad.
- Cobertura automatizada: `ConcentratorServiceTest` cubre F3–F5 y D4–D5. Los
  casos con saldo real (D1, D6, D7, D8) son manuales — ver la nota de alcance en
  `MATRIZ_QA.md`.
