-- ============================================================
-- V11 · v_movement · los dos ledgers, una sola forma.
--
-- El sistema lleva DOS libros y hasta ahora nadie los había mirado juntos:
--
--   · account_movement      → el lado de la cuenta del empleado
--                             (DEPOSIT, WITHDRAWAL, TRANSFER_IN, TRANSFER_OUT,
--                              REINTEGRATION)
--   · concentrator_movement → el lado de la Concentradora
--                             (FUNDING, DISPERSION, REINTEGRATION)
--
-- Una dispersión escribe en los dos: DISPERSION aquí, DEPOSIT allá. Eso NO es
-- duplicación, es partida doble — cada peso queda registrado desde los dos
-- lados, y por eso el sistema puede cuadrar. Un fondeo, en cambio, sólo existe
-- en concentrator_movement: es la única operación que no toca ninguna cuenta.
-- Por eso "todos los movimientos" no se puede responder leyendo una sola tabla.
--
-- La unión vive aquí y no en Java porque es una decisión sobre la FORMA de los
-- datos, no sobre el negocio: así el DAO consulta una tabla en vez de rehacer
-- la unión en cada método, y la consulta se puede probar sola antes de que
-- exista la pantalla.
--
-- `scope` dice de qué libro viene cada fila, y es lo que permite que
-- "Ver historial completo" de la Concentradora sea ?ambito=CONCENTRADORA.
--
-- Las columnas de cuenta van NULL del lado de la Concentradora: esa tabla no
-- guarda a qué cuenta fue el dinero (ver el comentario de
-- ConcentratorMovement.getConcept()). Van con CAST explícito a propósito —
-- Oracle no siempre infiere el tipo de un NULL pelado dentro de un UNION, y
-- ORA-01790 a media pantalla no es una forma agradable de enterarse.
--
-- Aplicar sobre una base ya creada. En una base nueva esto ya viene en
-- schema.sql / init-schema.sql. Es aditivo: no hace falta recrear el volumen.
-- ============================================================

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
       m.created_at                         AS created_at
  FROM account_movement m
  JOIN account    a   ON a.id   = m.account_id
  JOIN cardholder ch  ON ch.id  = a.cardholder_id
  JOIN category   cat ON cat.id = a.category_id
  LEFT JOIN account r ON r.id   = m.related_account_id
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
       cm.created_at
  FROM concentrator_movement cm;

-- concentrator_movement ya tenía idx_conc_mov_created; account_movement no
-- tenía ninguno sobre la fecha y la vista ordena SIEMPRE por created_at.
CREATE INDEX idx_acct_mov_created ON account_movement (created_at);
