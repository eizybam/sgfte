# Plan de pruebas QA — Módulo 4 (Eliminación + Reintegración)

| # | Caso | Pasos | Resultado esperado |
|---|------|-------|--------------------|
| E1 | Eliminar cuenta con saldo | cuenta con $500, `/admin/cuentas` → Eliminar | Concentradora +500; cuenta balance 0 + INACTIVE; tarjetas INACTIVE; movimiento REINTEGRATION 500 |
| E2 | Eliminar cuenta sin saldo | cuenta con $0 → Eliminar | Cuenta INACTIVE; tarjetas INACTIVE; **sin** movimiento (amount debe ser > 0) |
| E3 | Cuenta ya inactiva | eliminar una INACTIVE | Error "no existe o ya está inactiva"; nada cambia |
| E4 | Eliminar tarjetahabiente | TH con 2 cuentas con saldo | Ambas reintegradas; sus tarjetas INACTIVE; TH INACTIVE; Concentradora sube la suma |
| E5 | TH inexistente | id inválido (Postman) | Error; nada cambia |
| E6 | Atomicidad | TH con varias cuentas; si una fallara, revisar rollback | O se reintegran todas o ninguna |
| E7 | Sin sesión | abrir `/admin/cuentas` sin login | Redirige a `/login` |

## Verificación en BD (tras E1)
```sql
SELECT balance FROM concentrator_account;                     -- subió 500
SELECT balance, status FROM account WHERE id = <cuenta>;      -- 0, INACTIVE
SELECT status FROM card WHERE account_id = <cuenta>;          -- INACTIVE (todas)
SELECT movement_type, amount FROM account_movement
  WHERE account_id = <cuenta> ORDER BY id DESC FETCH FIRST 1 ROWS ONLY;  -- REINTEGRATION, 500
```

## Consistencia global (regla de oro)
La suma de (saldo Concentradora + saldos de todas las cuentas activas) debe permanecer
**constante** ante fondeos internos, dispersiones, transferencias y reintegraciones.
Solo el **fondeo externo** (HU-01) cambia ese total.
