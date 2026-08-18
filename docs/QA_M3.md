# Plan de pruebas QA — Módulo 3 (Transferencias P2P + Historial)

Requisito previo: 2 cuentas del mismo propósito con saldo (fondéalas con el Módulo 1) y
1 cuenta de propósito distinto para casos de error.

| # | Caso | Pasos | Resultado esperado |
|---|------|-------|--------------------|
| T1 | Transferencia válida | A→B mismo propósito, monto 200 (A tiene ≥200) | A −200, B +200, 2 movimientos (OUT/IN); éxito |
| T2 | Distinto propósito | A→C (propósitos distintos), 100 | Error "mismo propósito"; nada cambia |
| T3 | Saldo insuficiente | A→B monto > saldo de A | Error "saldo insuficiente"; nada cambia |
| T4 | Misma cuenta | A→A | Error "no pueden ser la misma cuenta" |
| T5 | Monto ≤ 0 | A→B monto 0 o -5 | Error de validación |
| T6 | Cuenta inactiva | A→(cuenta INACTIVE) | Error "no existe o está inactiva"; rollback |
| T7 | Atomicidad | revisar que ante cualquier error NINGÚN saldo cambió | Consistencia total |
| H1 | Historial | `/admin/movimientos?cuenta=A` tras T1 | Aparece TRANSFER_OUT de 200 |
| H2 | Historial destino | `?cuenta=B` | Aparece TRANSFER_IN de 200 |

## Verificación en BD (tras T1)
```sql
SELECT id, balance FROM account WHERE id IN (A, B);
SELECT account_id, movement_type, amount, related_account_id
FROM account_movement ORDER BY id DESC FETCH FIRST 2 ROWS ONLY;
-- => (B, TRANSFER_IN, 200, A) y (A, TRANSFER_OUT, 200, B)
```
