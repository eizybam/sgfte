-- 02_demo_data.sql (mounted under this name by docker-compose.yml)
--
-- Datos de demostración: ~11 meses de actividad simulada —altas escalonadas,
-- dispersiones, compras y transferencias P2P— para que Analíticas, Logs y
-- Notificaciones se vean como un sistema en uso real, no una base recién
-- sembrada. Corre automáticamente junto con 01_schema.sql en el primer
-- arranque (mismo mecanismo de gvenzl, orden alfabético: 01 antes que 02).
--
-- Todos los balances se recalculan al final a partir de la suma real de sus
-- movimientos (misma idea que AccountDao/ConcentratorDao ya aplican en la
-- app), así que no hay forma de que queden inconsistentes con el ledger.

ALTER SESSION SET CONTAINER = FREEPDB1;
ALTER SESSION SET CURRENT_SCHEMA = SGFTE;

SET SERVEROUTPUT ON

DECLARE
    v_now        TIMESTAMP := SYSTIMESTAMP;
    v_admin_hash VARCHAR2(72) := '$2a$12$EtVTq2PPQ8dp/XE0GHMomeJKZuU6bT.KRpGK91l3ji6rm5mBHFjFi'; -- 'sgfte'

    TYPE t_str_arr IS TABLE OF VARCHAR2(60);
    v_first_names t_str_arr := t_str_arr('Ana', 'Diego', 'Marta', 'Luis');
    v_last_names  t_str_arr := t_str_arr('Ramirez', 'Vega', 'Solis', 'Ortega');

    v_merchants_gas t_str_arr := t_str_arr('Pemex Reforma', 'Gasolinera BP Insurgentes', 'Pemex Autopista km 54', 'Circle K Combustible');
    v_merchants_ali t_str_arr := t_str_arr('OXXO Centro', 'Walmart Supercenter', 'Starbucks Polanco', 'Restaurante La Terraza', 'Superama');
    v_merchants_via t_str_arr := t_str_arr('Aeromexico', 'Hotel Marriott CDMX', 'Uber', 'Avianca', 'Booking.com');

    TYPE t_num_arr IS TABLE OF NUMBER;
    v_cardholder_ids t_num_arr := t_num_arr();
    v_cat_ids        t_num_arr := t_num_arr();
    v_cat_names      t_str_arr := t_str_arr();
    v_account_ids    t_num_arr := t_num_arr();
    v_account_cat    t_num_arr := t_num_arr();   -- índice en v_cat_ids/v_cat_names

    v_new_ch_id NUMBER;
    v_acc_id    NUMBER;
    v_conc_bal  NUMBER;

    FUNCTION merchant_for(p_cat VARCHAR2) RETURN VARCHAR2 IS
        v_idx PLS_INTEGER;
    BEGIN
        IF p_cat = 'Gasolina' THEN
            v_idx := TRUNC(DBMS_RANDOM.VALUE(1, v_merchants_gas.COUNT + 1));
            RETURN v_merchants_gas(v_idx);
        ELSIF p_cat = 'Viajes' THEN
            v_idx := TRUNC(DBMS_RANDOM.VALUE(1, v_merchants_via.COUNT + 1));
            RETURN v_merchants_via(v_idx);
        ELSE
            v_idx := TRUNC(DBMS_RANDOM.VALUE(1, v_merchants_ali.COUNT + 1));
            RETURN v_merchants_ali(v_idx);
        END IF;
    END;
BEGIN
    -- 1) Categorías que ya sembró schema.sql (Gasolina, Viajes, Alimentos).
    FOR r IN (SELECT id, name FROM category ORDER BY id) LOOP
        v_cat_ids.EXTEND;   v_cat_ids(v_cat_ids.COUNT) := r.id;
        v_cat_names.EXTEND; v_cat_names(v_cat_names.COUNT) := r.name;
    END LOOP;

    -- 2) Los dos cardholders de schema.sql + 4 nuevos, altas escalonadas en
    --    los últimos 6-9 meses (para que "nuevos tarjetahabientes" del
    --    período tenga algo que enseñar).
    FOR r IN (SELECT id FROM cardholder ORDER BY id) LOOP
        v_cardholder_ids.EXTEND; v_cardholder_ids(v_cardholder_ids.COUNT) := r.id;
    END LOOP;

    FOR i IN 1..v_first_names.COUNT LOOP
        INSERT INTO cardholder (first_name, last_name, email, phone, employee_code, department, created_at)
        VALUES (v_first_names(i), v_last_names(i),
                LOWER(v_first_names(i) || '.' || v_last_names(i) || '@empresa.com'),
                '555000' || LPAD(i, 4, '0'),
                UPPER(SUBSTR(v_first_names(i), 1, 1) || SUBSTR(v_last_names(i), 1, 1))
                    || LPAD(seq_employee_code.NEXTVAL, 4, '0'),
                'IT',
                v_now - (300 - i * 30))
        RETURNING id INTO v_new_ch_id;
        v_cardholder_ids.EXTEND; v_cardholder_ids(v_cardholder_ids.COUNT) := v_new_ch_id;
    END LOOP;

    -- 3) Login para LOS SEIS cardholders (los 2 originales + los 4 nuevos) —
    --    misma contraseña que el admin ('sgfte'), para poder entrar al
    --    portal como cualquiera de ellos en la demo sin activar por correo.
    INSERT INTO app_user (email, password_hash, full_name, role, cardholder_id, status)
    SELECT email, v_admin_hash, first_name || ' ' || last_name, 'TARJETAHABIENTE', id, 'ACTIVE'
      FROM cardholder;

    -- 4) Una cuenta por cardholder × categoría, con su par de tarjetas
    --    (física + digital) — no hay cuentas todavía en una base recién
    --    sembrada, así que no hace falta comprobar duplicados.
    FOR ch_idx IN 1..v_cardholder_ids.COUNT LOOP
        FOR cat_idx IN 1..v_cat_ids.COUNT LOOP
            INSERT INTO account (cardholder_id, category_id, account_number, balance, created_at)
            VALUES (v_cardholder_ids(ch_idx), v_cat_ids(cat_idx),
                    UPPER(SUBSTR(v_cat_names(cat_idx), 1, 3)) || '-' || TRUNC(DBMS_RANDOM.VALUE(10000, 99999)),
                    0, v_now - 300)
            RETURNING id INTO v_acc_id;

            v_account_ids.EXTEND; v_account_ids(v_account_ids.COUNT) := v_acc_id;
            v_account_cat.EXTEND; v_account_cat(v_account_cat.COUNT) := cat_idx;

            INSERT INTO card (account_id, card_type, masked_pan, status, created_at, expires_at)
            VALUES (v_acc_id, 'PHYSICAL',
                    '**** **** **** ' || LPAD(TRUNC(DBMS_RANDOM.VALUE(0, 9999)), 4, '0'),
                    'ACTIVE', v_now - 300, v_now - 300 + NUMTOYMINTERVAL(4, 'YEAR'));
            INSERT INTO card (account_id, card_type, masked_pan, status, created_at, expires_at)
            VALUES (v_acc_id, 'DIGITAL',
                    '**** **** **** ' || LPAD(TRUNC(DBMS_RANDOM.VALUE(0, 9999)), 4, '0'),
                    'ACTIVE', v_now - 300, v_now - 300 + NUMTOYMINTERVAL(4, 'YEAR'));
        END LOOP;
    END LOOP;

    -- 5) Concentradora: arranca en 1,000,000 (fila de apertura de
    --    schema.sql), de sobra para 18 cuentas × 11 meses de dispersión.
    SELECT balance INTO v_conc_bal FROM concentrator_account WHERE singleton = 'Y';

    -- 6) Por cada cuenta: una dispersión (DEPOSIT) al mes durante 11 meses,
    --    con 2 compras (WITHDRAWAL) dentro de ese mes, sin gastar nunca más
    --    del saldo acumulado hasta ese punto.
    FOR i IN 1..v_account_ids.COUNT LOOP
        DECLARE
            v_cat_name VARCHAR2(60) := v_cat_names(v_account_cat(i));
            v_bal      NUMBER := 0;
            v_dep      NUMBER;
            v_wd       NUMBER;
            v_dep_ts   TIMESTAMP;
            v_merchant VARCHAR2(60);
        BEGIN
            FOR m IN REVERSE 0..10 LOOP
                v_dep    := ROUND(DBMS_RANDOM.VALUE(1500, 6000), 2);
                v_dep_ts := v_now - (m * 30) - TRUNC(DBMS_RANDOM.VALUE(1, 5));

                INSERT INTO account_movement (account_id, movement_type, amount, description, created_at)
                VALUES (v_account_ids(i), 'DEPOSIT', v_dep, 'Asignación de fondos', v_dep_ts);
                v_bal := v_bal + v_dep;

                INSERT INTO concentrator_movement (movement_type, amount, balance_after, actor, created_at)
                VALUES ('DISPERSION', v_dep, GREATEST(v_conc_bal - v_dep, 0), 'sistema', v_dep_ts);
                v_conc_bal := v_conc_bal - v_dep;

                FOR p IN 1..2 LOOP
                    v_wd := ROUND(LEAST(v_bal * 0.3, DBMS_RANDOM.VALUE(150, 1200)), 2);
                    IF v_wd > 5 THEN
                        v_merchant := merchant_for(v_cat_name);
                        INSERT INTO account_movement (account_id, movement_type, amount, description, created_at)
                        VALUES (v_account_ids(i), 'WITHDRAWAL', v_wd, v_merchant,
                                v_now - (m * 30) - TRUNC(DBMS_RANDOM.VALUE(6, 25)));
                        v_bal := v_bal - v_wd;
                    END IF;
                END LOOP;
            END LOOP;
        END;
    END LOOP;

    -- 7) Un puñado de transferencias P2P: cuentas del mismo propósito
    --    (mismo índice de categoría) pertenecientes a tarjetahabientes
    --    distintos quedan siempre 3 posiciones aparte en v_account_ids,
    --    porque se insertaron en bloques de "3 categorías por persona".
    FOR i IN 1..v_account_ids.COUNT LOOP
        IF i + 3 <= v_account_ids.COUNT AND DBMS_RANDOM.VALUE < 0.5 THEN
            DECLARE
                v_amt NUMBER    := ROUND(DBMS_RANDOM.VALUE(200, 900), 2);
                v_ts  TIMESTAMP := v_now - TRUNC(DBMS_RANDOM.VALUE(10, 250));
            BEGIN
                INSERT INTO account_movement (account_id, movement_type, amount, related_account_id, description, created_at)
                VALUES (v_account_ids(i), 'TRANSFER_OUT', v_amt, v_account_ids(i + 3), 'Transferencia P2P', v_ts);
                INSERT INTO account_movement (account_id, movement_type, amount, related_account_id, description, created_at)
                VALUES (v_account_ids(i + 3), 'TRANSFER_IN', v_amt, v_account_ids(i), 'Transferencia P2P', v_ts);
            END;
        END IF;
    END LOOP;

    -- 8) Notificaciones: las 8 dispersiones/transferencias más recientes DE
    --    CADA cardholder (no las 25 más recientes del sistema completo) —
    --    con seis personas y sólo dos o tres cuentas cada una, un top global
    --    dejaba a la mitad de la gente sin nada que ver en su propio portal.
    INSERT INTO notification (cardholder_id, event_type, category, detail, created_at)
    SELECT cardholder_id, event_type, category, detail, created_at FROM (
        SELECT a.cardholder_id,
               CASE WHEN m.movement_type = 'DEPOSIT' THEN 'DISPERSION' ELSE 'TRANSFER' END AS event_type,
               'ADMINISTRATIVA' AS category,
               'Recibiste $' || TO_CHAR(m.amount, 'FM999G999D00') || ' MXN' AS detail,
               m.created_at,
               ROW_NUMBER() OVER (PARTITION BY a.cardholder_id ORDER BY m.created_at DESC) AS rn
          FROM account_movement m JOIN account a ON a.id = m.account_id
         WHERE m.movement_type IN ('DEPOSIT', 'TRANSFER_IN')
    ) WHERE rn <= 8;

    -- Inicios de sesión, unos cuantos por cada uno de los seis.
    FOR ch_idx IN 1..v_cardholder_ids.COUNT LOOP
        FOR d IN 1..4 LOOP
            INSERT INTO notification (cardholder_id, event_type, category, detail, created_at)
            VALUES (v_cardholder_ids(ch_idx), 'LOGIN_OK', 'SEGURIDAD', NULL,
                    v_now - TRUNC(DBMS_RANDOM.VALUE(1, 270)));
        END LOOP;
    END LOOP;

    -- 9) Bitácora de administración: altas y accesos repartidos en los
    --    últimos meses, para que Logs no sólo tenga la fila del arranque.
    FOR d IN 1..15 LOOP
        INSERT INTO audit_log (event_type, detail, actor, created_at, severity, module, ip_address)
        VALUES ('LOGIN_OK', 'Rol: ADMIN', 'admin@empresa.com',
                v_now - TRUNC(DBMS_RANDOM.VALUE(1, 270)), 'INFO', 'Seguridad', '192.168.1.10');
    END LOOP;

    FOR ch_idx IN 1..v_cardholder_ids.COUNT LOOP
        DECLARE
            v_email cardholder.email%TYPE;
            v_name  VARCHAR2(120);
        BEGIN
            SELECT email, first_name || ' ' || last_name INTO v_email, v_name
              FROM cardholder WHERE id = v_cardholder_ids(ch_idx);

            FOR d IN 1..5 LOOP
                INSERT INTO audit_log (event_type, detail, actor, created_at, severity, module, ip_address)
                VALUES ('LOGIN_OK', 'Rol: TARJETAHABIENTE', v_email,
                        v_now - TRUNC(DBMS_RANDOM.VALUE(1, 270)), 'INFO', 'Seguridad',
                        '192.168.1.' || TRUNC(DBMS_RANDOM.VALUE(20, 250)));
            END LOOP;

            -- El alta del cardholder mismo, para los cuatro nuevos.
            IF ch_idx > 2 THEN
                INSERT INTO audit_log (event_type, detail, actor, created_at, severity, module)
                SELECT 'CARDHOLDER_CREATED', v_name || ' · ' || v_email, 'admin@empresa.com',
                       created_at, 'INFO', 'Empleados'
                  FROM cardholder WHERE id = v_cardholder_ids(ch_idx);
            END IF;
        END;
    END LOOP;

    -- Emisión de tarjetas y alta de cuentas, una fila por cada una de las
    -- que se acaban de crear arriba.
    INSERT INTO audit_log (event_type, detail, actor, created_at, severity, module)
    SELECT 'ACCOUNT_CREATED',
           a.account_number || ' · ' || cat.name, 'admin@empresa.com', a.created_at, 'INFO', 'Cuentas'
      FROM account a JOIN category cat ON cat.id = a.category_id;

    INSERT INTO audit_log (event_type, detail, actor, created_at, severity, module)
    SELECT 'CARD_ISSUED',
           a.account_number || ' · ' || k.card_type, 'admin@empresa.com', k.created_at, 'INFO', 'Tarjetas'
      FROM card k JOIN account a ON a.id = k.account_id;

    -- 10) Balances reales, recalculados desde el ledger — la única fuente
    --     de verdad, para que no puedan quedar desincronizados de los
    --     movimientos que sí se generaron con cuidado arriba.
    UPDATE account a
       SET balance = NVL((SELECT SUM(CASE WHEN movement_type IN ('DEPOSIT', 'TRANSFER_IN') THEN amount
                                           WHEN movement_type IN ('WITHDRAWAL', 'TRANSFER_OUT', 'REINTEGRATION') THEN -amount
                                      END)
                            FROM account_movement m WHERE m.account_id = a.id), 0);

    UPDATE concentrator_account
       SET balance = (SELECT SUM(CASE WHEN movement_type IN ('FUNDING', 'REINTEGRATION') THEN amount
                                       WHEN movement_type = 'DISPERSION' THEN -amount
                                  END)
                        FROM concentrator_movement)
     WHERE singleton = 'Y';

    COMMIT;
END;
/
