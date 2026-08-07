-- SGFTE · V7 · Vencimiento de la tarjeta
-- Oracle 23. Ejecutar conectado al esquema SGFTE.
--
-- Por qué: los marcos piden la fecha de vencimiento en TRES sitios —"VÁLIDA
-- HASTA" en la cara de la tarjeta (263:58 y 285:47) y "Fecha de expiración" en
-- el detalle— y la tabla card no la guardaba: id, cuenta, tipo, PAN
-- enmascarado, estado y alta. Hasta ahora esos huecos salían con un guion.
--
-- La vigencia son cuatro años desde la emisión. Es una convención, no una ley,
-- así que vive en CardService.VALIDITY_YEARS y no repartida por consultas: el
-- día que la empresa decida otra cosa se cambia en un sitio.
--
-- Se rellena a partir de created_at para que las tarjetas ya expedidas tengan
-- una fecha coherente con su alta, y no todas la misma.

ALTER TABLE card ADD (expires_at DATE);

UPDATE card
   SET expires_at = ADD_MONTHS(TRUNC(created_at), 48)
 WHERE expires_at IS NULL;

-- NOT NULL después del relleno: antes, las filas existentes lo violarían.
ALTER TABLE card MODIFY (expires_at DATE NOT NULL);

-- Una tarjeta no puede vencer antes de existir.
ALTER TABLE card ADD CONSTRAINT chk_card_expiry CHECK (expires_at > created_at);

COMMIT;

-- Comprobación:
-- SELECT id, card_type, TO_CHAR(created_at, 'MM/YY') AS emitida,
--        TO_CHAR(expires_at, 'MM/YY') AS vence FROM card ORDER BY id;
