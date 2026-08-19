-- ============================================================
-- V13 · account_movement.card_id · con QUÉ tarjeta se gastó.
--
-- El sistema ya comprobaba la tarjeta antes de aceptar una compra:
-- PortalService.spend llama a cardUsable(), que exige que la tarjeta sea de esa
-- cuenta, que la cuenta sea de ese tarjetahabiente y que las dos estén ACTIVE.
-- Una tarjeta bloqueada no puede gastar, y eso funciona.
--
-- Lo que NO ocurría es guardarla. PurchaseService.spend recibía cuenta, monto y
-- comercio, y la tarjeta se quedaba en la puerta: se validaba y se tiraba. El
-- ledger podía responder "¿esta compra estaba autorizada?" pero no "¿con cuál
-- de las dos tarjetas se hizo?" — y como cada cuenta puede tener una física y
-- una digital a la vez, esa pregunta tiene respuesta y hasta ahora se perdía.
--
-- Es el mismo hueco que cerró V12 del lado del fondeo: el dinero se podía
-- rastrear hasta la cuenta, pero no hasta el instrumento.
--
-- ── La llave compuesta ──────────────────────────────────────
--
-- La FK no va sobre card_id a secas, va sobre (card_id, account_id). Eso obliga
-- a que la tarjeta pertenezca A LA CUENTA del movimiento, y lo obliga la base:
-- registrar una compra con la tarjeta de otra cuenta se vuelve imposible, no
-- "algo que el servicio revisa". Para que se pueda apuntar ahí, card necesita
-- un UNIQUE (id, account_id) — redundante como clave, porque id ya es única,
-- pero es lo que permite que otra tabla exija el par completo.
--
-- Con card_id en NULL la FK no se evalúa (basta con que una columna del par sea
-- nula), así que los movimientos que no son compras y las 396 compras que ya
-- existían no estorban.
--
-- ── Lo viejo se queda como está ─────────────────────────────
--
-- Las compras anteriores a esta migración quedan con card_id NULL. No se
-- rellenan adivinando: en una cuenta con dos tarjetas, elegir una sería
-- inventarse un dato que nadie registró. Marcan dónde empieza la trazabilidad
-- por tarjeta, igual que el asiento de apertura marca dónde empieza la del
-- fondeo.
--
-- Aplicar sobre una base ya creada. En una base nueva ya viene en schema.sql /
-- init-schema.sql.
-- ============================================================

ALTER TABLE card ADD CONSTRAINT uq_card_id_account UNIQUE (id, account_id);

ALTER TABLE account_movement ADD (card_id NUMBER);

ALTER TABLE account_movement ADD CONSTRAINT fk_mov_card
    FOREIGN KEY (card_id, account_id) REFERENCES card (id, account_id);

-- Sólo un consumo con tarjeta puede traer tarjeta. Un depósito, una
-- transferencia o una reintegración no pasan por ningún plástico.
ALTER TABLE account_movement ADD CONSTRAINT chk_mov_card_only_withdrawal
    CHECK (card_id IS NULL OR movement_type = 'WITHDRAWAL');

COMMENT ON COLUMN account_movement.card_id IS
    'Tarjeta con la que se hizo el consumo. NULL en el resto de movimientos y en las compras anteriores a V13.';

-- ── La vista global aprende a enseñar la tarjeta.
--
--    Misma razón que en V12: si el dato existe en la base pero no en
--    /admin/movimientos, la trazabilidad no se puede demostrar, que es para lo
--    que se guarda.
CREATE OR REPLACE VIEW v_movement AS
SELECT 'CUENTA'                             AS scope,
       m.id                                 AS source_id,
       m.movement_type                      AS movement_type,
       m.amount                             AS amount,
       CASE WHEN m.movement_type IN ('DEPOSIT', 'TRANSFER_IN')
            THEN 'IN' ELSE 'OUT' END        AS direction,
       m.description                        AS description,
       a.id                                 AS account_id,
       a.account_number                     AS account_number,
       ch.first_name || ' ' || ch.last_name AS holder,
       ch.employee_code                     AS employee_code,
       cat.id                               AS category_id,
       cat.name                             AS category_name,
       cat.color_index                      AS color_index,
       r.account_number                     AS related_number,
       CAST(NULL AS VARCHAR2(120))          AS actor,
       CAST(NULL AS NUMBER(16, 2))          AS balance_after,
       CAST(NULL AS VARCHAR2(12))           AS canal,
       CAST(NULL AS VARCHAR2(40))           AS referencia,
       CAST(NULL AS VARCHAR2(160))          AS ordenante,
       k.masked_pan                         AS card_pan,
       k.card_type                          AS card_type,
       m.created_at                         AS created_at
  FROM account_movement m
  JOIN account    a   ON a.id   = m.account_id
  JOIN cardholder ch  ON ch.id  = a.cardholder_id
  JOIN category   cat ON cat.id = a.category_id
  LEFT JOIN account r ON r.id   = m.related_account_id
  LEFT JOIN card    k ON k.id   = m.card_id
UNION ALL
SELECT 'CONCENTRADORA',
       cm.id,
       cm.movement_type,
       cm.amount,
       CASE WHEN cm.movement_type IN ('FUNDING', 'REINTEGRATION')
            THEN 'IN' ELSE 'OUT' END,
       CAST(NULL AS VARCHAR2(200)),
       CAST(NULL AS NUMBER),
       CAST(NULL AS VARCHAR2(20)),
       CAST(NULL AS VARCHAR2(121)),
       CAST(NULL AS VARCHAR2(12)),
       CAST(NULL AS NUMBER),
       CAST(NULL AS VARCHAR2(40)),
       CAST(NULL AS NUMBER),
       CAST(NULL AS VARCHAR2(20)),
       cm.actor,
       cm.balance_after,
       fd.canal,
       fd.referencia,
       fd.ordenante_nombre,
       CAST(NULL AS VARCHAR2(19)),
       CAST(NULL AS VARCHAR2(10)),
       cm.created_at
  FROM concentrator_movement cm
  LEFT JOIN funding_deposit fd ON fd.id = cm.funding_deposit_id;
