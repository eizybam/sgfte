-- ============================================================
-- Depósitos de demostración (V12) · RESPALDO, no el camino normal
--
-- Lo normal para llenar esto es el simulador: /admin/simulador-banco publica
-- contra /api/banco/deposito firmado, que es el camino de verdad y el que
-- conviene enseñar en la defensa. Este script existe para cuando hace falta
-- tener datos ya puestos sin ir capturando uno por uno.
--
-- OJO con lo que hace: escribe el depósito, el asiento del ledger y el saldo a
-- mano, saltándose FundingService. Es dato semilla, igual que el resto de
-- demo-data.sql, y por eso puede permitírselo — pero NO es un ejemplo de cómo
-- se mueve dinero en este sistema. En la aplicación, el dinero sólo se mueve
-- dentro de un Service y en una transacción.
--
-- Las CLABE ordenantes son válidas de verdad: su dígito de control está
-- calculado, así que si algún día se vuelven a meter por el simulador, pasan.
--
-- Las fechas se derivan del ÚLTIMO asiento que ya existe y no de SYSTIMESTAMP
-- menos unos días. La primera versión de este archivo las fechaba semanas atrás
-- y dejaba el ledger incoherente: balance_after se calcula acumulando sobre el
-- saldo ACTUAL, así que un asiento insertado "en el pasado" hereda un saldo que
-- en ese momento no era el suyo, y la cadena deja de cuadrar. La comprobación
-- del final de este archivo es la que lo detectó.
--
-- Y aviso por si hay que deshacerlo: los dos ledgers son inmutables por
-- disparador, así que borrar un seed mal puesto obliga a DISABLE del trigger,
-- borrar, y ENABLE otra vez. Esa fricción es intencional.
--
-- Aplicar con:
--   docker exec -i sgfte-db-1 sqlplus -S sgfte/<clave>@//localhost:1521/FREEPDB1 \
--     < docs/db/demo-depositos.sql
-- ============================================================

SET DEFINE OFF

-- Tres depósitos: dos SPEI de distintos ordenantes y uno en ventanilla, para
-- que la pantalla enseñe los dos canales y la asimetría entre ellos.
INSERT INTO funding_deposit
    (canal, referencia, ordenante_nombre, ordenante_rfc, ordenante_clabe,
     institucion, sucursal, beneficiario_clabe, monto, concepto,
     referencia_numerica, fecha_operacion)
SELECT 'SPEI', 'MBAN01002607150000045678', 'CORPORATIVO TEXTIL DEL BAJIO SA DE CV',
       'CTB050818HQ2', '002180000456789123', 'BANAMEX', NULL,
       c.clabe, 480000.00, 'Fondeo operativo julio', 7150001,
       (SELECT MAX(created_at) + INTERVAL '1' MINUTE FROM concentrator_movement)
  FROM concentrator_account c WHERE c.singleton = 'Y';

INSERT INTO funding_deposit
    (canal, referencia, ordenante_nombre, ordenante_rfc, ordenante_clabe,
     institucion, sucursal, beneficiario_clabe, monto, concepto,
     referencia_numerica, fecha_operacion)
SELECT 'SPEI', 'BBVA00012607280000112233', 'CORPORATIVO TEXTIL DEL BAJIO SA DE CV',
       'CTB050818HQ2', '012180000789456126', 'BBVA MEXICO', NULL,
       c.clabe, 265000.00, 'Fondeo nomina agosto', 7280002,
       (SELECT MAX(created_at) + INTERVAL '2' MINUTE FROM concentrator_movement)
  FROM concentrator_account c WHERE c.singleton = 'Y';

-- En ventanilla no hay CLABE ordenante y el RFC deja de ser opcional: es la
-- compensación por usar el canal con menos rastro. Los CHECK de V12 rechazan
-- este INSERT si se le quita cualquiera de las dos cosas.
INSERT INTO funding_deposit
    (canal, referencia, ordenante_nombre, ordenante_rfc, ordenante_clabe,
     institucion, sucursal, beneficiario_clabe, monto, concepto,
     referencia_numerica, fecha_operacion)
SELECT 'VENTANILLA', 'FICHA-2026-0805-4417', 'DISTRIBUIDORA MORELOS SA DE CV',
       'DMO110322TT8', NULL, 'BANORTE', 'Cuernavaca Plaza Cuernavaca',
       c.clabe, 92500.00, 'Deposito en efectivo', NULL,
       (SELECT MAX(created_at) + INTERVAL '3' MINUTE FROM concentrator_movement)
  FROM concentrator_account c WHERE c.singleton = 'Y';

-- El asiento de cada depósito, en el orden en que ocurrieron, con el saldo
-- acumulado. balance_after se calcula sumando lo anterior porque aquí no hay un
-- Service que lo lea de vuelta dentro de la transacción — y por eso los tres
-- asientos tienen que ir DESPUÉS del último que ya existía: es sobre ese saldo
-- sobre el que se acumula.
INSERT INTO concentrator_movement
    (movement_type, amount, balance_after, actor, funding_deposit_id, created_at)
SELECT 'FUNDING', d.monto,
       (SELECT balance FROM concentrator_account WHERE singleton = 'Y')
         + SUM(d.monto) OVER (ORDER BY d.fecha_operacion
                              ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW),
       d.ordenante_nombre, d.id, d.fecha_operacion
  FROM funding_deposit d
 WHERE d.referencia IN ('MBAN01002607150000045678',
                        'BBVA00012607280000112233',
                        'FICHA-2026-0805-4417');

-- Y el saldo sube lo que entró.
UPDATE concentrator_account
   SET balance = balance + (SELECT SUM(monto) FROM funding_deposit
                             WHERE referencia IN ('MBAN01002607150000045678',
                                                  'BBVA00012607280000112233',
                                                  'FICHA-2026-0805-4417'))
 WHERE singleton = 'Y';

COMMIT;

-- Comprobación, y no es decorativa: el saldo de la cuenta tiene que coincidir
-- con el balance_after del último asiento. Si las dos cifras no salen iguales,
-- el seed quedó incoherente — hay que deshacerlo (DISABLE de los disparadores)
-- en vez de dejarlo así, porque Analíticas lee balance_after para responder
-- "cuánto había hace un periodo".
SELECT (SELECT balance FROM concentrator_account WHERE singleton = 'Y') AS saldo_cuenta,
       (SELECT balance_after FROM concentrator_movement
         ORDER BY created_at DESC, id DESC FETCH FIRST 1 ROWS ONLY) AS ultimo_asiento
  FROM dual;
