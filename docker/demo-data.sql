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
    /*
      Catorce personas. Nombre y apellido van emparejados por posición, y los
      pares tienen que ser distintos entre sí: el correo se arma como
      nombre.apellido@empresa.com y desde V16 la base lo exige único sin
      distinguir mayúsculas.
    */
    v_first_names t_str_arr := t_str_arr(
        'Ana', 'Diego', 'Marta', 'Luis',
        'Sofia', 'Carlos', 'Fernanda', 'Ricardo', 'Paulina',
        'Andres', 'Gabriela', 'Hector', 'Valeria', 'Emilio');
    v_last_names  t_str_arr := t_str_arr(
        'Ramirez', 'Vega', 'Solis', 'Ortega',
        'Herrera', 'Mendoza', 'Rios', 'Nava', 'Cortes',
        'Beltran', 'Fuentes', 'Zamora', 'Cordova', 'Salgado');
    /*
      Los cinco que siembra schema.sql, y se reparten en rueda con MOD: con
      catorce personas ya no hay un área por cabeza, y lo que le hace falta al
      filtro de /admin/empleados es justo eso — varias personas por área.
    */
    v_departments t_str_arr := t_str_arr('IT', 'Finanzas', 'Operaciones', 'Ventas', 'Recursos Humanos');

    v_merchants_gas t_str_arr := t_str_arr('Pemex Reforma', 'Gasolinera BP Insurgentes', 'Pemex Autopista km 54', 'Circle K Combustible');
    v_merchants_ali t_str_arr := t_str_arr('OXXO Centro', 'Walmart Supercenter', 'Starbucks Polanco', 'Restaurante La Terraza', 'Superama');
    v_merchants_via t_str_arr := t_str_arr('Aeromexico', 'Hotel Marriott CDMX', 'Uber', 'Avianca', 'Booking.com');

    TYPE t_num_arr IS TABLE OF NUMBER;
    v_cardholder_ids t_num_arr := t_num_arr();
    v_cat_ids        t_num_arr := t_num_arr();
    v_cat_names      t_str_arr := t_str_arr();
    v_account_ids    t_num_arr := t_num_arr();
    v_account_cat    t_num_arr := t_num_arr();   -- índice en v_cat_ids/v_cat_names

    v_dept_idx  PLS_INTEGER;
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

    -- 2) Los dos cardholders de schema.sql + los de las listas de arriba,
    --    con altas escalonadas hacia atrás para que "nuevos tarjetahabientes"
    --    del período tenga algo que enseñar en cada ventana del selector.
    FOR r IN (SELECT id FROM cardholder ORDER BY id) LOOP
        v_cardholder_ids.EXTEND; v_cardholder_ids(v_cardholder_ids.COUNT) := r.id;
    END LOOP;

    -- Repartidos entre las áreas: con todos en la misma, el filtro por
    -- departamento de /admin/empleados no tendría nada que demostrar.
    FOR i IN 1..v_first_names.COUNT LOOP
        /*
          El índice se calcula ANTES y aparte. Dentro de la sentencia SQL no
          cabe: v_departments.COUNT es un método de una colección de PL/SQL, y
          el motor de SQL no sabe llamarlo — PLS-00425. Indexar la colección sí
          se puede; invocarle un método, no.
        */
        v_dept_idx := MOD(i - 1, v_departments.COUNT) + 1;

        INSERT INTO cardholder (first_name, last_name, email, phone, employee_code, department_id, created_at)
        VALUES (v_first_names(i), v_last_names(i),
                LOWER(v_first_names(i) || '.' || v_last_names(i) || '@empresa.com'),
                '555000' || LPAD(i, 4, '0'),
                UPPER(SUBSTR(v_first_names(i), 1, 1) || SUBSTR(v_last_names(i), 1, 1))
                    || LPAD(seq_employee_code.NEXTVAL, 4, '0'),
                (SELECT id FROM department WHERE name = v_departments(v_dept_idx)),
                -- Escalonadas: la primera hace unos 10 meses y la última hace
                -- pocas semanas. Con GREATEST no se van al futuro por muchas
                -- que se añadan a las listas.
                v_now - GREATEST(300 - i * 20, 15))
        RETURNING id INTO v_new_ch_id;
        v_cardholder_ids.EXTEND; v_cardholder_ids(v_cardholder_ids.COUNT) := v_new_ch_id;
    END LOOP;

    -- 3) Login para TODOS los cardholders — misma contraseña que el admin
    --    ('sgfte'), para poder entrar al portal como cualquiera de ellos
    --    durante la demo sin pasar por el correo de activación.
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

    -- 5) Concentradora y dispersiones, MES A MES y en orden cronológico.
    --
    --    Antes esto recorría cuenta por cuenta y, dentro, sus once meses. Daba
    --    igual con seis personas; con dieciséis ya no, por dos razones:
    --
    --      · La apertura de 1,000,000 no alcanza. Cuarenta y ocho cuentas por
    --        once meses son cerca de dos millones dispersados, y la
    --        Concentradora se habría ido a números rojos — que es justo lo que
    --        prohíbe chk_concentrator_balance. Así que ahora la empresa la
    --        fondea cada mes, que además es lo que pasa de verdad y le da a la
    --        pantalla de Concentradora un ledger con entradas y salidas en vez
    --        de una sola línea de apertura.
    --
    --      · balance_after tiene que ser cierto. Se guarda en la fila, el ledger
    --        es inmutable (no hay UPDATE que valga después) y sólo cuadra si los
    --        movimientos se escriben en el mismo orden en que ocurrieron. Con el
    --        bucle por cuenta, la columna era una cifra sin sentido: saltaba
    --        hacia atrás y hacia adelante al leer la tabla por fecha.
    SELECT balance INTO v_conc_bal FROM concentrator_account WHERE singleton = 'Y';

    DECLARE
        TYPE t_bal_arr IS TABLE OF NUMBER INDEX BY PLS_INTEGER;
        v_acc_bal  t_bal_arr;                  -- saldo corrido de cada cuenta
        v_fund     NUMBER;
        v_fund_ts  TIMESTAMP;
        v_dep      NUMBER;
        v_wd       NUMBER;
        v_dep_ts   TIMESTAMP;
        v_cat_name VARCHAR2(60);
        v_merchant VARCHAR2(60);
    BEGIN
        FOR i IN 1..v_account_ids.COUNT LOOP
            v_acc_bal(i) := 0;
        END LOOP;

        FOR m IN REVERSE 0..10 LOOP
            -- El fondeo del mes entra ANTES que las dispersiones que paga, y
            -- cubre de sobra lo que se va a repartir (el tope por cuenta es
            -- 6,000, así que 7,000 por cuenta no se queda corto ningún mes).
            v_fund    := v_account_ids.COUNT * 7000;
            v_fund_ts := v_now - (m * 30) - 6;

            INSERT INTO concentrator_movement (movement_type, amount, balance_after, actor, created_at)
            VALUES ('FUNDING', v_fund, v_conc_bal + v_fund, 'Tesorería', v_fund_ts);
            v_conc_bal := v_conc_bal + v_fund;

            FOR i IN 1..v_account_ids.COUNT LOOP
                v_cat_name := v_cat_names(v_account_cat(i));
                v_dep := ROUND(DBMS_RANDOM.VALUE(1500, 6000), 2);
                /*
                  La fecha NO es al azar, y esa es la mitad del asunto.

                  balance_after se calcula con el saldo corrido, o sea en el
                  orden en que se insertan las filas. Si dentro del mes cada
                  dispersión cayera en un día cualquiera, leer el ledger por
                  fecha daría saltos absurdos: una salida de 3,867 seguida de un
                  saldo 10,050 más bajo. Se comprobó, y pasaba.

                  Así que se reparten en orden por los cuatro días siguientes al
                  fondeo que las paga. Orden de inserción = orden cronológico, y
                  la columna vuelve a querer decir lo que dice.
                */
                v_dep_ts := v_fund_ts
                          + NUMTODSINTERVAL(i * (4 * 24 * 60 / v_account_ids.COUNT), 'MINUTE');

                INSERT INTO account_movement (account_id, movement_type, amount, description, created_at)
                VALUES (v_account_ids(i), 'DEPOSIT', v_dep, 'Asignación de fondos', v_dep_ts);
                v_acc_bal(i) := v_acc_bal(i) + v_dep;

                INSERT INTO concentrator_movement (movement_type, amount, balance_after, actor, created_at)
                VALUES ('DISPERSION', v_dep, v_conc_bal - v_dep, 'sistema', v_dep_ts);
                v_conc_bal := v_conc_bal - v_dep;

                -- Dos compras dentro del mes, nunca por encima de lo que la
                -- cuenta lleva acumulado hasta ese punto.
                FOR p IN 1..2 LOOP
                    v_wd := ROUND(LEAST(v_acc_bal(i) * 0.3, DBMS_RANDOM.VALUE(150, 1200)), 2);
                    IF v_wd > 5 THEN
                        v_merchant := merchant_for(v_cat_name);
                        INSERT INTO account_movement (account_id, movement_type, amount, description, created_at)
                        VALUES (v_account_ids(i), 'WITHDRAWAL', v_wd, v_merchant,
                                v_now - (m * 30) - TRUNC(DBMS_RANDOM.VALUE(6, 25)));
                        v_acc_bal(i) := v_acc_bal(i) - v_wd;
                    END IF;
                END LOOP;
            END LOOP;
        END LOOP;
    END;

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
