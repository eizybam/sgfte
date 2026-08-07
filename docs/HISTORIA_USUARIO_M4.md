# Historias de Usuario — Módulo 4 (Eliminación + Reintegración)

## HU-07 · Eliminar cuenta con reintegración
**Como** administrador
**quiero** eliminar una cuenta y que su saldo regrese a la Concentradora
**para** no perder dinero y mantener la trazabilidad.

**Criterios de aceptación**
- Al eliminar una cuenta ACTIVA con saldo, en una sola transacción: (1) su saldo se suma a la
  Concentradora, (2) se registra un movimiento `REINTEGRATION`, (3) sus tarjetas pasan a
  INACTIVE, (4) la cuenta queda en balance 0 e INACTIVE.
- Si cualquier paso falla, se revierte todo (ningún saldo cambia).
- Eliminar una cuenta ya inactiva o inexistente muestra un error.

## HU-08 · Eliminar tarjetahabiente con reintegración total
**Como** administrador
**quiero** eliminar un tarjetahabiente y que todos sus saldos regresen a la Concentradora
**para** dar de baja al empleado sin dejar fondos huérfanos.

**Criterios de aceptación**
- Al eliminar un tarjetahabiente ACTIVO, todas sus cuentas activas se reintegran (como HU-07),
  todas sus tarjetas se invalidan, y el tarjetahabiente queda INACTIVE — todo en una sola
  transacción (todo o nada).
- Eliminar un tarjetahabiente inexistente o inactivo muestra un error.

## Nota (regla de negocio 4)
Eliminar una **tarjeta** NO mueve dinero; el saldo permanece en la cuenta. (Eso vive en el Módulo 2.)
