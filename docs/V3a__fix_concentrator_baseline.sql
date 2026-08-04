-- SGFTE · V3a · Corrige la fila de apertura del ledger de la concentradora
--
-- Sólo hace falta si ya se corrió la primera versión de V3, que fechaba la
-- apertura con SYSTIMESTAMP. Quien ejecute V3 desde cero ya no la necesita.
--
-- El problema: el saldo "de hace un periodo" se busca como el último movimiento
-- ANTERIOR al inicio de la ventana. Una apertura fechada hoy no es anterior a
-- ninguna ventana, así que la tarjeta "SALDO CONCENTRADORA" mostraba un guion
-- en vez de su comparación, para siempre.
--
-- La tabla es inmutable a propósito, así que hay que desactivar el disparador
-- durante la corrección y volver a activarlo. Es una operación de migración,
-- no algo que la aplicación pueda hacer.

ALTER TRIGGER trg_concentrator_movement_immutable DISABLE;

UPDATE concentrator_movement
   SET created_at = SYSDATE - 400,
       actor      = 'saldo inicial'
 WHERE actor IN ('migración V3', 'saldo inicial')
   AND movement_type = 'FUNDING';

ALTER TRIGGER trg_concentrator_movement_immutable ENABLE;

COMMIT;

-- Comprobación: la apertura debe quedar claramente en el pasado.
-- SELECT movement_type, balance_after, actor, created_at
--   FROM concentrator_movement ORDER BY created_at;
