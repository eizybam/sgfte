# Historias de Usuario — Módulo 3 (Transferencias P2P + Historial)

## HU-05 · Transferencia entre pares (mismo propósito)
**Como** tarjetahabiente
**quiero** transferir fondos a la cuenta de un compañero con el mismo propósito
**para** compartir recursos destinados al mismo fin (p. ej. gasolina).

**Criterios de aceptación**
- Dado dos cuentas ACTIVAS del **mismo** `category_id`, cuando transfiero un monto > 0 con
  saldo suficiente, entonces: origen baja, destino sube, y se registran DOS movimientos
  (`TRANSFER_OUT` y `TRANSFER_IN`), todo en una sola transacción.
- Si las cuentas son de **distinto** propósito, veo "solo se permite transferir entre cuentas
  del mismo propósito" y nada cambia.
- Si el saldo del origen es insuficiente, veo "saldo insuficiente" y nada cambia.
- Si origen = destino, o el monto ≤ 0, veo el error de validación.

## HU-06 · Consultar historial de movimientos
**Como** tarjetahabiente (o administrador)
**quiero** ver la lista de movimientos de una cuenta
**para** dar seguimiento y trazabilidad a los fondos.

**Criterios de aceptación**
- Al elegir una cuenta, veo sus movimientos ordenados del más reciente al más antiguo, con
  fecha, tipo, descripción y monto.
- Los movimientos no se pueden editar ni borrar (ledger inmutable).
