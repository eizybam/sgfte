-- SGFTE · V4a · Reparación: completar las columnas de bitácora que falten
-- Oracle 23. Ejecutar conectado al esquema SGFTE.
--
-- Por qué existe: docs/schema.sql se consolidó en c5cc6c0 y V4 entró en el
-- commit siguiente (3ba71ef) sin volver a plegarse en él. Durante ese hueco,
-- quien creó su base desde schema.sql se quedó sin severity, module ni
-- ip_address, y /admin/logs revienta con ORA-00904 al seleccionarlas.
-- schema.sql ya está corregido; esto arregla las bases que nacieron torcidas.
--
-- V4 no sirve para eso: su ALTER añade las tres columnas de golpe y falla con
-- ORA-01430 si alguna ya existe. Este script mira antes de tocar, así que se
-- puede correr en cualquier estado —completo, a medias o vacío— y las veces
-- que haga falta. Si ya está todo, no hace nada.

SET SERVEROUTPUT ON

DECLARE
    PROCEDURE add_column_if_missing(p_column VARCHAR2, p_definition VARCHAR2) IS
        v_count NUMBER;
    BEGIN
        SELECT COUNT(*) INTO v_count FROM user_tab_columns
         WHERE table_name = 'AUDIT_LOG' AND column_name = p_column;
        IF v_count = 0 THEN
            EXECUTE IMMEDIATE 'ALTER TABLE audit_log ADD (' || p_definition || ')';
            DBMS_OUTPUT.PUT_LINE('  + columna ' || p_column || ' añadida');
        ELSE
            DBMS_OUTPUT.PUT_LINE('  = columna ' || p_column || ' ya existía');
        END IF;
    END;

    PROCEDURE add_constraint_if_missing(p_name VARCHAR2, p_definition VARCHAR2) IS
        v_count NUMBER;
    BEGIN
        SELECT COUNT(*) INTO v_count FROM user_constraints
         WHERE constraint_name = UPPER(p_name);
        IF v_count = 0 THEN
            EXECUTE IMMEDIATE 'ALTER TABLE audit_log ADD CONSTRAINT '
                              || p_name || ' ' || p_definition;
            DBMS_OUTPUT.PUT_LINE('  + restricción ' || p_name || ' añadida');
        ELSE
            DBMS_OUTPUT.PUT_LINE('  = restricción ' || p_name || ' ya existía');
        END IF;
    END;

    PROCEDURE add_index_if_missing(p_name VARCHAR2, p_columns VARCHAR2) IS
        v_count NUMBER;
    BEGIN
        SELECT COUNT(*) INTO v_count FROM user_indexes
         WHERE index_name = UPPER(p_name);
        IF v_count = 0 THEN
            EXECUTE IMMEDIATE 'CREATE INDEX ' || p_name
                              || ' ON audit_log (' || p_columns || ')';
            DBMS_OUTPUT.PUT_LINE('  + índice ' || p_name || ' añadido');
        ELSE
            DBMS_OUTPUT.PUT_LINE('  = índice ' || p_name || ' ya existía');
        END IF;
    END;
BEGIN
    -- DEFAULT 'INFO' rellena las filas ya escritas, que es lo que permite que
    -- la columna sea NOT NULL en una tabla con historia. Y hay que poner la
    -- columna antes que su CHECK, o la restricción no tendría qué validar.
    add_column_if_missing('SEVERITY',   'severity VARCHAR2(10) DEFAULT ''INFO'' NOT NULL');
    add_column_if_missing('MODULE',     'module VARCHAR2(30)');
    add_column_if_missing('IP_ADDRESS', 'ip_address VARCHAR2(45)');

    add_constraint_if_missing('chk_audit_severity',
                              'CHECK (severity IN (''INFO'', ''ALERTA'', ''CRIT''))');

    -- Se consulta casi siempre por fecha descendente y filtrando por severidad.
    add_index_if_missing('idx_audit_created',  'created_at');
    add_index_if_missing('idx_audit_severity', 'severity');
END;
/

COMMIT;

-- Comprobación: deben salir las ocho columnas, en este orden.
SELECT column_id, column_name FROM user_tab_columns
 WHERE table_name = 'AUDIT_LOG' ORDER BY column_id;
