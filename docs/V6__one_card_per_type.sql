-- SGFTE · V6 · Una tarjeta activa de cada tipo por cuenta
-- Oracle 23. Ejecutar conectado al esquema SGFTE.
--
-- Por qué: la regla del negocio es que una cuenta tiene como mucho UNA tarjeta
-- física y UNA digital. Estaba sólo en la cabeza de quien la contó: la tabla
-- aceptaba cuantas quisieras y CardService.issue únicamente comprobaba que el
-- tipo fuera válido. La vista de cuenta del tarjetahabiente dibuja exactamente
-- dos tarjetas, así que una tercera la rompía.
--
-- Índice ÚNICO y no CHECK: una restricción CHECK sólo puede mirar la fila que
-- se está insertando, y aquí hay que mirar las demás filas de la misma cuenta.
--
-- El CASE es lo que lo limita a las ACTIVAS. En Oracle una entrada de índice
-- con TODAS sus columnas en NULL no se guarda, así que las tarjetas inactivas
-- quedan fuera del índice y no estorban: cancelar una física y expedir otra
-- sigue siendo posible, que es justo lo que tiene que pasar cuando se pierde.

CREATE UNIQUE INDEX uq_card_active_type ON card (
    CASE WHEN status = 'ACTIVE' THEN account_id END,
    CASE WHEN status = 'ACTIVE' THEN card_type  END
);

-- Comprobación: no debe devolver ninguna fila.
-- SELECT account_id, card_type, COUNT(*)
--   FROM card WHERE status = 'ACTIVE'
--  GROUP BY account_id, card_type HAVING COUNT(*) > 1;
