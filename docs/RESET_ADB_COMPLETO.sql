-- ============================================================================
-- SGFTE · RESET COMPLETO de la Autonomous Database (SGFTE1)
--
-- QUÉ HACE, EN ESTE ORDEN:
--   1) Borra TODOS los objetos del esquema SGFTE (tablas, vistas, secuencias).
--   2) Recrea el esquema canónico desde docs/schema.sql (ya incluye V1..V16).
--   3) Siembra los datos de demo desde docker/demo-data.sql.
--
-- BORRA TODO LO QUE HAY HOY. No hay vuelta atrás sin un backup.
--
-- CÓMO CORRERLO: consola OCI -> SGFTE1 -> Database actions -> SQL.
--                Pegar TODO y pulsar F5 (Run Script), NO Ctrl+Enter.
--
-- POR QUÉ LA PRIMERA LÍNEA: Database Actions entra como ADMIN, no como SGFTE.
-- Sin el CURRENT_SCHEMA los objetos se crearían en el esquema equivocado.
-- ============================================================================

ALTER SESSION SET CURRENT_SCHEMA = SGFTE;

-- ── PASO 1 · Tabla rasa ─────────────────────────────────────────────────────
-- Dinámico y no una lista de DROPs: así no depende de que la lista esté al día,
-- y no falla por un objeto que ya no exista. CASCADE CONSTRAINTS se lleva las
-- FK que apunten a la tabla; PURGE evita que quede en la papelera ocupando
-- espacio (la Always Free tiene 20 GB). Los índices y triggers caen con su
-- tabla, por eso no hace falta borrarlos aparte.
-- Se excluye DBTOOLS$%: son tablas del propio Database Actions, no tuyas.
BEGIN
    FOR r IN (SELECT table_name FROM all_tables
               WHERE owner = 'SGFTE' AND table_name NOT LIKE 'DBTOOLS$%') LOOP
        EXECUTE IMMEDIATE 'DROP TABLE SGFTE."' || r.table_name || '" CASCADE CONSTRAINTS PURGE';
    END LOOP;
    FOR r IN (SELECT view_name FROM all_views WHERE owner = 'SGFTE') LOOP
        EXECUTE IMMEDIATE 'DROP VIEW SGFTE."' || r.view_name || '"';
    END LOOP;
    FOR r IN (SELECT sequence_name FROM all_sequences WHERE sequence_owner = 'SGFTE') LOOP
        EXECUTE IMMEDIATE 'DROP SEQUENCE SGFTE."' || r.sequence_name || '"';
    END LOOP;
END;
/

-- ── PASO 2 · Esquema canónico (docs/schema.sql, V1..V16) ────────────────────
-- ============================================================
-- SGFTE · Esquema completo (Oracle)
-- Sistema de Gestión de Fondos, Tarjetas y Empleados
-- ------------------------------------------------------------
-- Ejecutar conectado al esquema de la app (usuario SGFTE en FREEPDB1).
-- Convención: identificadores en inglés. Moneda única: MXN.
-- Jerarquía: cardholder 1─N account 1─N card. Concentradora = fuente de fondos.
--
-- Este archivo es el esquema CANÓNICO: crea la base desde cero, ya con todo lo
-- que fueron añadiendo las migraciones. Si ya tienes una base creada, NO uses
-- este archivo: aplica las V en orden, que es para lo que están.
--   · V2  → employee_code y department en cardholder, y su secuencia.
--   · V3  → ledger de la concentradora (+ V3a corrige su fila de apertura).
--   · V4  → severidad, módulo y origen en la bitácora (+ V4a repara bases
--           creadas con la versión de este archivo que se quedó sin ellas).
--   · V5  → descripción y color propio en las categorías.
--   · V6  → una tarjeta activa de cada tipo por cuenta.
--   · V7  → vencimiento de la tarjeta.
--   · V8  → bitácora de notificaciones del tarjetahabiente.
--   · V9  → PENDING en app_user y enlaces de activación/restablecimiento.
--   · V10 → catálogo de departamentos.
--   · V11 → vista v_movement: los dos ledgers unidos, para /admin/movimientos.
--   · V12 → fondeo con respaldo bancario: CLABE, funding_deposit y su enlace.
--   · V13 → con qué tarjeta se hizo cada consumo (account_movement.card_id).
-- ============================================================

-- ── Limpieza para desarrollo (re-ejecutar). Descomenta si necesitas recrear.
--    OJO: borra datos. Respeta el orden inverso por las llaves foráneas.
-- DROP TABLE password_token CASCADE CONSTRAINTS;
-- DROP TABLE notification CASCADE CONSTRAINTS;
-- DROP TABLE account_movement CASCADE CONSTRAINTS;
-- DROP TABLE concentrator_movement CASCADE CONSTRAINTS;
-- DROP TABLE card CASCADE CONSTRAINTS;
-- DROP TABLE account CASCADE CONSTRAINTS;
-- DROP TABLE concentrator_account CASCADE CONSTRAINTS;
-- DROP TABLE app_user CASCADE CONSTRAINTS;
-- DROP TABLE cardholder CASCADE CONSTRAINTS;
-- DROP TABLE category CASCADE CONSTRAINTS;
-- DROP TABLE audit_log CASCADE CONSTRAINTS;
-- DROP SEQUENCE seq_employee_code;

-- ============================================================
-- 1) category · catálogo global de propósitos (gestión de categorías = core)
-- ============================================================
--    color_index es el color con el que la categoría se pinta en TODA la app
--    (badge de la tabla de cuentas, pastel de la Vista Global, píldora del
--    detalle). Se guarda en vez de derivarse de la posición en el catálogo:
--    así lo elige quien crea la categoría, y dar de alta una nueva no repinta
--    las que ya existían. Los siete valores son los siete --sgfte-purpose-N de
--    la hoja de estilos, en el mismo orden que la paleta del modal (2041:155).
--
--    No se borran categorías: account.category_id es NOT NULL y apunta aquí,
--    así que una categoría en uso no se puede borrar sin arrastrar cuentas.
--    Retirarla es status = 'INACTIVE', que la saca de los desplegables y deja
--    intacto el histórico de las cuentas que la usaron.
CREATE TABLE category (
                          id      NUMBER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
                          name    VARCHAR2(40) NOT NULL,
                          status      VARCHAR2(10) DEFAULT 'ACTIVE' NOT NULL,
                          description VARCHAR2(120),
                          color_index NUMBER(1) DEFAULT 1 NOT NULL,
                          CONSTRAINT uq_category_name   UNIQUE (name),
                          CONSTRAINT chk_category_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
                          CONSTRAINT chk_category_color  CHECK (color_index BETWEEN 1 AND 7)
);

-- ============================================================
-- 1b) department · Catálogo de áreas de la empresa (V10).
--
--     Misma forma que category, y a propósito: es el mismo tipo de cosa, un
--     catálogo que alimenta un desplegable, con nombre único y baja lógica.
--     Lo único que no lleva es color_index — el color de una categoría
--     distingue propósitos en tablas y gráficas; un departamento no se pinta
--     en ninguna parte.
--
--     Tampoco se borra: cardholder.department_id apunta aquí, así que un área
--     con gente dentro no se puede borrar. Retirarla es status = 'INACTIVE':
--     deja de ofrecerse al dar de alta y los empleados que ya la tenían la
--     conservan.
-- ============================================================
CREATE TABLE department (
                            id          NUMBER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
                            name        VARCHAR2(60)  NOT NULL,
                            description VARCHAR2(120),
                            status      VARCHAR2(10)  DEFAULT 'ACTIVE' NOT NULL,
                            created_at  TIMESTAMP     DEFAULT SYSTIMESTAMP NOT NULL,
                            CONSTRAINT uq_department_name    UNIQUE (name),
                            CONSTRAINT chk_department_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

-- ============================================================
-- 2) cardholder · Tarjetahabiente (empleado). El dinero NUNCA vive aquí.
--
--    employee_code es el código que se ve en pantalla ("DJE0077"): iniciales
--    del nombre completo + cuatro dígitos de seq_employee_code. Las iniciales
--    NO bastan para ser únicas —dos "Juan Pérez López" dan JPL—, así que la
--    unicidad la garantiza el número, no las letras. Se asigna una vez al dar
--    de alta y no se recalcula: corregir después una errata en el nombre no
--    debe cambiar un identificador ya emitido.
--
--    department_id apunta al catálogo de áreas (V10). Antes era texto libre
--    con 'IT' escrito a mano en el JSP; ahora el desplegable del alta se llena
--    desde department y renombrar un área no obliga a un UPDATE masivo aquí.
--    Es NULL-able: un empleado puede existir sin área asignada.
-- ============================================================
CREATE TABLE cardholder (
                            id            NUMBER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
                            first_name    VARCHAR2(60)  NOT NULL,
                            last_name     VARCHAR2(60)  NOT NULL,
                            email         VARCHAR2(120) NOT NULL,
                            phone         VARCHAR2(20),
                            employee_code VARCHAR2(12)  NOT NULL,
                            department_id NUMBER,
                            status        VARCHAR2(10) DEFAULT 'ACTIVE' NOT NULL,
                            created_at    TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
                            CONSTRAINT uq_cardholder_email  UNIQUE (email),
                            CONSTRAINT uq_cardholder_employee_code UNIQUE (employee_code),
                            CONSTRAINT chk_cardholder_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
                            CONSTRAINT fk_cardholder_department FOREIGN KEY (department_id)
                                REFERENCES department (id)
);

-- Correo insensible a mayúsculas (V16). Lo que tiene que ser único no es la
-- columna sino su versión en minúsculas: "mail@x.com" y "mAIL@x.com" son la
-- misma persona, y aquí el correo es además el usuario con el que se entra.
CREATE UNIQUE INDEX uq_cardholder_email_lower ON cardholder (LOWER(email));

CREATE INDEX idx_cardholder_department ON cardholder (department_id);

-- Parte numérica del código de empleado. Cuatro dígitos aguantan 9,999 altas
-- antes de tener que ensanchar la columna. NOCACHE para no perder tramos al
-- reiniciar la base; el volumen de altas no justifica el caché.
CREATE SEQUENCE seq_employee_code
    START WITH 1
    INCREMENT BY 1
    NOCACHE
    NOCYCLE;

-- ============================================================
-- 3) concentrator_account · Cuenta Concentradora (fuente única de fondos).
--    Singleton: solo puede existir UNA fila (truco: columna 'singleton' única con CHECK).
-- ============================================================
CREATE TABLE concentrator_account (
                                      id         NUMBER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
                                      name       VARCHAR2(60) DEFAULT 'Cuenta Concentradora' NOT NULL,
                                      balance    NUMBER(16, 2) DEFAULT 0 NOT NULL,
                                      currency   CHAR(3) DEFAULT 'MXN' NOT NULL,
                                      clabe      CHAR(18),        -- destino de todo fondeo (V12)
                                      singleton  CHAR(1) DEFAULT 'Y' NOT NULL,
                                      CONSTRAINT chk_concentrator_balance   CHECK (balance >= 0),
                                      CONSTRAINT chk_concentrator_currency  CHECK (currency = 'MXN'),
                                      CONSTRAINT chk_concentrator_singleton CHECK (singleton = 'Y'),
                                      CONSTRAINT uq_concentrator_singleton  UNIQUE (singleton)
);

-- ============================================================
-- 4) account · Cuenta con propósito. EL DINERO VIVE AQUÍ.
--    FK a cardholder (dueño) y a category (propósito).
--    Sin ON DELETE CASCADE a propósito: al borrar un cardholder, la lógica Java
--    debe reintegrar el saldo a la Concentradora ANTES de borrar (regla de negocio 4).
-- ============================================================
CREATE TABLE account (
                         id             NUMBER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
                         cardholder_id  NUMBER NOT NULL,
                         category_id    NUMBER NOT NULL,
                         account_number  VARCHAR2(20) NOT NULL,
                         balance        NUMBER(14, 2) DEFAULT 0 NOT NULL,
                         currency       CHAR(3) DEFAULT 'MXN' NOT NULL,
                         status         VARCHAR2(10) DEFAULT 'ACTIVE' NOT NULL,
                         created_at     TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
                         CONSTRAINT fk_account_cardholder FOREIGN KEY (cardholder_id) REFERENCES cardholder(id),
                         CONSTRAINT fk_account_category   FOREIGN KEY (category_id)   REFERENCES category(id),
                         CONSTRAINT uq_account_number UNIQUE (account_number),
                         CONSTRAINT chk_account_balance   CHECK (balance >= 0),
                         CONSTRAINT chk_account_currency  CHECK (currency = 'MXN'),
                         CONSTRAINT chk_account_status    CHECK (status IN ('ACTIVE', 'INACTIVE'))
);
-- Una cuenta ACTIVA de cada propósito por tarjetahabiente (V15). Índice y no
-- CHECK: hay que mirar las OTRAS filas del mismo tarjetahabiente. El CASE lo
-- limita a las activas — cerrar una cuenta libera su propósito, que es lo que
-- promete el aviso de cierre.
CREATE UNIQUE INDEX uq_account_active_purpose ON account (
    CASE WHEN status = 'ACTIVE' THEN cardholder_id END,
    CASE WHEN status = 'ACTIVE' THEN category_id  END
);


-- ============================================================
-- 5) card · Tarjeta (punto de acceso a la cuenta). NO guarda dinero.
--    Borrar una tarjeta NO mueve dinero (regla de negocio 4).
-- ============================================================
CREATE TABLE card (
                      id          NUMBER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
                      account_id  NUMBER NOT NULL,
                      card_type   VARCHAR2(10) NOT NULL,
                      masked_pan  VARCHAR2(19),
                      status      VARCHAR2(10) DEFAULT 'ACTIVE' NOT NULL,
                      created_at  TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
                      -- Vigencia: cuatro años desde la emisión. La regla vive en
                      -- CardService.VALIDITY_YEARS; aquí sólo se guarda el resultado.
                      expires_at  DATE NOT NULL,
                      CONSTRAINT fk_card_account FOREIGN KEY (account_id) REFERENCES account(id),
                      -- Redundante como clave —id ya es única— pero es lo que permite que
                      -- account_movement exija el PAR (tarjeta, cuenta) y no sólo la tarjeta.
                      CONSTRAINT uq_card_id_account UNIQUE (id, account_id),
                      CONSTRAINT chk_card_type   CHECK (card_type IN ('PHYSICAL', 'DIGITAL')),
                      CONSTRAINT chk_card_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'BLOCKED')),
                      CONSTRAINT chk_card_expiry CHECK (expires_at > created_at)
);

-- Como mucho UNA tarjeta activa de cada tipo por cuenta: una física y una
-- digital. Índice único y no CHECK, porque un CHECK sólo ve la fila que se
-- inserta y aquí hay que mirar las hermanas.
--
-- El CASE lo limita a las ACTIVAS: en Oracle una entrada con todas sus columnas
-- en NULL no se indexa, así que las canceladas quedan fuera y reponer una
-- tarjeta perdida —invalidar y expedir otra— sigue siendo posible.
CREATE UNIQUE INDEX uq_card_active_type ON card (
    CASE WHEN status = 'ACTIVE' THEN account_id END,
    CASE WHEN status = 'ACTIVE' THEN card_type  END
);

-- ============================================================
-- 6) account_movement · Ledger inmutable de dinero (trazabilidad).
--    Cada depósito, retiro, transferencia P2P y reintegración deja registro.
-- ============================================================
CREATE TABLE account_movement (
                                  id                 NUMBER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
                                  account_id         NUMBER NOT NULL,
                                  movement_type      VARCHAR2(15) NOT NULL,
                                  amount             NUMBER(14, 2) NOT NULL,
                                  related_account_id NUMBER,          -- contraparte en transferencias P2P
                                  description        VARCHAR2(200),
                                  created_at         TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
                                  -- Con qué tarjeta se gastó (V13). NULL en todo lo que no es
                                  -- consumo, y en las compras anteriores a esa migración.
                                  card_id            NUMBER,
                                  CONSTRAINT fk_mov_account FOREIGN KEY (account_id)         REFERENCES account(id),
                                  CONSTRAINT fk_mov_related FOREIGN KEY (related_account_id) REFERENCES account(id),
                                  -- La llave va sobre el PAR: obliga a que la tarjeta sea de la
                                  -- cuenta del movimiento. Registrar un consumo con la tarjeta de
                                  -- otra cuenta es imposible, no "algo que el servicio revisa".
                                  CONSTRAINT fk_mov_card    FOREIGN KEY (card_id, account_id)
                                      REFERENCES card (id, account_id),
                                  CONSTRAINT chk_mov_card_only_withdrawal CHECK (
                                      card_id IS NULL OR movement_type = 'WITHDRAWAL'),
                                  CONSTRAINT chk_mov_type   CHECK (movement_type IN
                                                                   ('DEPOSIT', 'WITHDRAWAL', 'TRANSFER_IN', 'TRANSFER_OUT', 'REINTEGRATION')),
                                  CONSTRAINT chk_mov_amount CHECK (amount > 0)
);

-- Trigger: el ledger es inmutable (no se puede modificar ni borrar un movimiento).
CREATE OR REPLACE TRIGGER trg_account_movement_immutable
    BEFORE UPDATE OR DELETE ON account_movement
BEGIN
    RAISE_APPLICATION_ERROR(-20001,
                            'account_movement es inmutable: no se permite modificar ni borrar movimientos');
END;
/

-- ============================================================
-- 6b) funding_deposit · el respaldo bancario de cada fondeo (V12).
--
--     Antes, fondear era teclear un monto: el saldo subía y no quedaba nada que
--     dijera de dónde salió el dinero. El ledger era inmutable, sí, pero la
--     inmutabilidad de un asiento inventado sólo garantiza que la mentira no se
--     puede borrar. Faltaba la PROCEDENCIA, que es lo que pide la trazabilidad
--     en materia de prevención de lavado.
--
--     Ahora el saldo sólo sube cuando se registra un depósito con referencia
--     bancaria verificable, y el administrador pasa de CREAR dinero a CONCILIAR
--     el que el banco reporta.
--
--     Dos canales, y la diferencia entre ellos es la regla que hay que poder
--     defender: EL CANAL CON MENOS TRAZABILIDAD CARGA CON MÁS IDENTIFICACIÓN.
--       · SPEI       → trae clave de rastreo y cuenta ordenante; se identifica
--                      solo, así que el RFC es opcional.
--       · VENTANILLA → depósito en efectivo en sucursal. No hay cuenta que lo
--                      ordene ni clave de rastreo, sólo el folio de la ficha,
--                      así que el RFC del depositante es OBLIGATORIO.
--     Las dos reglas son CHECK, no un if de Java.
--
--     `referencia` UNIQUE es la restricción clave del módulo: contabilizar dos
--     veces el mismo depósito no es "desaconsejable", es imposible.
--
--     Sólo se guardan los depósitos APLICADOS; un intento rechazado deja ALERTA
--     en la bitácora, que es donde ya viven los intentos fallidos.
-- ============================================================
CREATE TABLE funding_deposit (
    id                  NUMBER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    canal               VARCHAR2(12)  NOT NULL,   -- SPEI | VENTANILLA
    referencia          VARCHAR2(40)  NOT NULL,   -- clave de rastreo o folio de ficha
    ordenante_nombre    VARCHAR2(160) NOT NULL,   -- razón social de quien depositó
    ordenante_rfc       VARCHAR2(13),             -- obligatorio en ventanilla
    ordenante_clabe     CHAR(18),                 -- sólo SPEI: no existe en efectivo
    institucion         VARCHAR2(60)  NOT NULL,
    sucursal            VARCHAR2(60),             -- sólo ventanilla
    beneficiario_clabe  CHAR(18)      NOT NULL,   -- tiene que ser la nuestra
    monto               NUMBER(16, 2) NOT NULL,
    concepto            VARCHAR2(40),
    referencia_numerica NUMBER(7),                -- referencia numérica del SPEI
    fecha_operacion     TIMESTAMP     NOT NULL,   -- cuándo lo hizo el banco
    recibido_en         TIMESTAMP     DEFAULT SYSTIMESTAMP NOT NULL,  -- cuándo lo supimos
    CONSTRAINT uq_funding_deposit_ref UNIQUE (referencia),
    CONSTRAINT chk_funding_canal CHECK (canal IN ('SPEI', 'VENTANILLA')),
    CONSTRAINT chk_funding_monto CHECK (monto > 0),
    CONSTRAINT chk_funding_ordenante_clabe CHECK (
        (canal = 'SPEI'       AND ordenante_clabe IS NOT NULL) OR
        (canal = 'VENTANILLA' AND ordenante_clabe IS NULL)),
    CONSTRAINT chk_funding_ventanilla_rfc CHECK (
        canal <> 'VENTANILLA' OR ordenante_rfc IS NOT NULL),
    CONSTRAINT chk_funding_sucursal CHECK (
        canal = 'VENTANILLA' OR sucursal IS NULL)
);

CREATE INDEX idx_funding_deposit_fecha ON funding_deposit (fecha_operacion);

-- El respaldo es tan inmutable como el asiento que respalda: si se pudiera
-- editar, la inmutabilidad del ledger no valdría nada.
CREATE OR REPLACE TRIGGER trg_funding_deposit_immutable
    BEFORE UPDATE OR DELETE ON funding_deposit
BEGIN
    RAISE_APPLICATION_ERROR(-20004,
        'funding_deposit es inmutable: un depósito registrado no se modifica ni se borra');
END;
/

-- ============================================================
-- 7) concentrator_movement · Ledger inmutable de la Concentradora.
--
--    concentrator_account es UNA fila con saldo mutable y sin historia. Las
--    dispersiones y reintegraciones dejaban rastro del lado de la cuenta, en
--    account_movement, pero FONDEAR la concentradora no dejaba rastro en ningún
--    sitio: era la única operación del sistema que movía dinero sin registro.
--
--    Sirve para dos cosas: auditar quién mete dinero, y responder "cuánto había
--    hace un periodo" en Analíticas.
--
--    balance_after se guarda explícitamente en vez de recalcularse sumando: el
--    saldo histórico se lee de una fila y no depende de que la suma de todos
--    los movimientos anteriores cuadre.
-- ============================================================
CREATE TABLE concentrator_movement (
                                       id            NUMBER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
                                       movement_type VARCHAR2(15)  NOT NULL,
                                       amount        NUMBER(16, 2) NOT NULL,
                                       balance_after NUMBER(16, 2) NOT NULL,
                                       actor         VARCHAR2(120),   -- quién fondeó; NULL cuando lo mueve el sistema
                                       created_at    TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
                                       -- El respaldo del fondeo (V12). Nullable a propósito: el asiento de
                                       -- apertura del seed es anterior a la trazabilidad y no tiene depósito
                                       -- que enseñar. Marca dónde empieza, que es más honesto que inventarle uno.
                                       funding_deposit_id NUMBER,
                                       CONSTRAINT chk_conc_mov_type CHECK (movement_type IN
                                                                           ('FUNDING', 'DISPERSION', 'REINTEGRATION')),
                                       CONSTRAINT chk_conc_mov_amount  CHECK (amount > 0),
                                       CONSTRAINT chk_conc_mov_balance CHECK (balance_after >= 0),
                                       CONSTRAINT fk_conc_mov_deposit FOREIGN KEY (funding_deposit_id)
                                           REFERENCES funding_deposit (id),
                                       -- Sólo un FUNDING respalda algo de afuera; dispersar y reintegrar
                                       -- son movimientos internos.
                                       CONSTRAINT chk_conc_mov_deposit_only_funding CHECK (
                                           funding_deposit_id IS NULL OR movement_type = 'FUNDING')
);

CREATE INDEX idx_conc_mov_created ON concentrator_movement (created_at);

-- El ledger es inmutable, igual que account_movement.
CREATE OR REPLACE TRIGGER trg_concentrator_movement_immutable
    BEFORE UPDATE OR DELETE ON concentrator_movement
BEGIN
    RAISE_APPLICATION_ERROR(-20003,
                            'concentrator_movement es inmutable: no se permite modificar ni borrar movimientos');
END;
/

-- ============================================================
-- 8) app_user · Usuarios que inician sesión (login). Rol: ADMIN | TARJETAHABIENTE.
--    Guarda SOLO el hash bcrypt de la contraseña, nunca texto plano.
--    cardholder_id enlaza el login de un TARJETAHABIENTE con su ficha; NULL en admins.
-- ============================================================
-- PENDING (V9): un login de tarjetahabiente nace así, con una contraseña que
-- nadie pudo escribir, hasta que su dueño la crea por el enlace de activación
-- que se le manda por correo. AuthService ya rechaza cualquier status que no
-- sea 'ACTIVE', así que un login PENDING no puede entrar sin tocar ese
-- archivo — esta columna sólo tenía que aceptar guardar el valor.
CREATE TABLE app_user (
    id            NUMBER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    email         VARCHAR2(120) NOT NULL,
    password_hash VARCHAR2(72)  NOT NULL,
    full_name     VARCHAR2(120) NOT NULL,
    role          VARCHAR2(20)  DEFAULT 'ADMIN' NOT NULL,
    cardholder_id NUMBER,
    status        VARCHAR2(10)  DEFAULT 'ACTIVE' NOT NULL,
    created_at    TIMESTAMP     DEFAULT SYSTIMESTAMP NOT NULL,
    -- Foto de perfil (V14). Sólo la pinta la pantalla de Ajustes; ninguna otra
    -- vista la consulta. photo_type es el Content-Type con el que se sirve.
    photo         BLOB,
    photo_type    VARCHAR2(40),
    CONSTRAINT uq_app_user_email      UNIQUE (email),
    CONSTRAINT chk_app_user_role      CHECK (role IN ('ADMIN', 'TARJETAHABIENTE')),
    CONSTRAINT chk_app_user_status    CHECK (status IN ('ACTIVE', 'INACTIVE', 'PENDING')),
    CONSTRAINT chk_app_user_photo_type CHECK (photo_type IS NULL OR photo_type IN ('image/png', 'image/jpeg', 'image/webp')),
    CONSTRAINT fk_app_user_cardholder FOREIGN KEY (cardholder_id) REFERENCES cardholder (id)
);

-- Correo insensible a mayúsculas (V16). Lo que tiene que ser único no es la
-- columna sino su versión en minúsculas: "mail@x.com" y "mAIL@x.com" son la
-- misma persona, y aquí el correo es además el usuario con el que se entra.
CREATE UNIQUE INDEX uq_app_user_email_lower ON app_user (LOWER(email));

-- 9) Microservicio de Auditoría: bitácora inmutable de eventos del sistema.
--
--    La pantalla "Logs y Auditoría" (Figma 253:43) muestra seis columnas
--    —fecha, NIVEL, acción, usuario, MÓDULO, ORIGEN—, de ahí las tres últimas.
--
--    `severity` y no `level` a propósito: LEVEL es una pseudocolumna de Oracle
--    (la de CONNECT BY) y usarla como nombre obliga a entrecomillar en cada
--    consulta.
--
--    ip_address llega a 45 caracteres para que quepa una IPv6 completa; el
--    contenedor y el proxy pueden entregar ::1 o una dirección mapeada.
--
--    Van DESPUÉS de created_at, aunque leído así quede raro, porque es donde
--    las deja el ALTER de V4: una base creada desde este archivo y otra migrada
--    quedan idénticas, columna por columna. Que diverjan es justo lo que
--    produce un "en mi máquina sí funciona".
CREATE TABLE audit_log (
    id          NUMBER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    event_type  VARCHAR2(40)  NOT NULL,   -- LOGIN, DEPOSIT, TRANSFER, DELETE, NOTIFICATION, ...
    detail      VARCHAR2(400),
    actor       VARCHAR2(120),            -- quién lo hizo (email admin, sistema, etc.)
    created_at  TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    severity    VARCHAR2(10) DEFAULT 'INFO' NOT NULL,
    module      VARCHAR2(30),
    ip_address  VARCHAR2(45),
    CONSTRAINT chk_audit_severity CHECK (severity IN ('INFO', 'ALERTA', 'CRIT'))
);

-- Se consulta casi siempre por fecha descendente y filtrando por severidad.
CREATE INDEX idx_audit_created  ON audit_log (created_at);
CREATE INDEX idx_audit_severity ON audit_log (severity);

-- El log es inmutable, igual que account_movement.
CREATE OR REPLACE TRIGGER trg_audit_log_immutable
BEFORE UPDATE OR DELETE ON audit_log
BEGIN
    RAISE_APPLICATION_ERROR(-20002, 'audit_log es inmutable: no se puede modificar ni borrar');
END;
/

-- 10) notification · bitácora de avisos por correo, scopeada al tarjetahabiente.
--
--     Pantalla "Notificaciones" (Figma) del portal del tarjetahabiente: TODO lo
--     que se le ha avisado por correo, sólo lo suyo. audit_log no sirve para
--     esto porque guarda quién HIZO la acción, no a quién iba dirigida — el
--     destinatario del aviso de dinero recibido vive enterrado en el texto de
--     detail. Aquí cardholder_id es una columna real e indexada.
--
--     event_type reutiliza el vocabulario de AuditEvent: el título de cada fila
--     sale del mismo enum que ya alimenta audit_log, así que no hay dos
--     catálogos de texto que puedan desalinearse. category se guarda
--     materializada (SEGURIDAD / ADMINISTRATIVA), igual que audit_log guarda
--     severity y module aparte del event_type: es la columna por la que se
--     filtra todo el tiempo.
CREATE TABLE notification (
    id            NUMBER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    cardholder_id NUMBER NOT NULL,
    event_type    VARCHAR2(40) NOT NULL,
    category      VARCHAR2(20) NOT NULL,
    detail        VARCHAR2(400),
    created_at    TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT fk_notification_cardholder FOREIGN KEY (cardholder_id) REFERENCES cardholder(id),
    CONSTRAINT chk_notification_category CHECK (category IN ('SEGURIDAD', 'ADMINISTRATIVA'))
);

CREATE INDEX idx_notification_cardholder ON notification (cardholder_id, created_at);
CREATE INDEX idx_notification_category   ON notification (category);

-- Inmutable, igual que audit_log: es un registro de lo que se avisó.
CREATE OR REPLACE TRIGGER trg_notification_immutable
    BEFORE UPDATE OR DELETE ON notification
BEGIN
    RAISE_APPLICATION_ERROR(-20004, 'notification es inmutable: no se permite modificar ni borrar');
END;
/

-- 11) password_token · activación de cuenta y "olvidé mi contraseña" (V9).
--
--     Un enlace de un solo uso vale por una fila. Se emite al dar de alta a
--     un tarjetahabiente (CardholderService.register() crea su app_user en
--     PENDING y manda el enlace) y se reutiliza tal cual para un
--     restablecimiento posterior — el token es lo único que dice de qué
--     login se trata, así que no hace falta distinguir los dos casos aquí.
--
--     A diferencia de audit_log/notification, esta tabla SÍ se modifica —
--     used_at se pone al redimir el token—, así que no lleva el trigger de
--     inmutabilidad de esas dos: no es un ledger histórico, es estado
--     operativo, más parecido a una sesión que a un registro de auditoría.
CREATE TABLE password_token (
    id           NUMBER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    app_user_id  NUMBER NOT NULL,
    token        VARCHAR2(64) NOT NULL,
    expires_at   TIMESTAMP NOT NULL,
    used_at      TIMESTAMP,
    created_at   TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT fk_password_token_user FOREIGN KEY (app_user_id) REFERENCES app_user(id),
    CONSTRAINT uq_password_token_value UNIQUE (token)
);

-- Se busca casi siempre por token exacto (la URL que llega en el correo); el
-- índice por app_user_id es para poder auditar/depurar "qué enlaces se le han
-- mandado a este login".
CREATE INDEX idx_password_token_user ON password_token (app_user_id);

-- ============================================================
-- 12) v_movement · los dos ledgers, una sola forma (V11).
--
--     account_movement y concentrator_movement son los dos libros del sistema,
--     y una dispersión escribe en los dos: DISPERSION de un lado, DEPOSIT del
--     otro. Es partida doble, no duplicación. Un fondeo, en cambio, sólo existe
--     en concentrator_movement — es la única operación que no toca ninguna
--     cuenta—, así que "todos los movimientos" no se responde con una tabla.
--
--     La unión vive en la base y no en Java porque es una decisión sobre la
--     FORMA de los datos: el DAO de /admin/movimientos consulta una vista en
--     vez de rehacer el UNION en cada método.
--
--     Las columnas de cuenta van NULL del lado de la Concentradora, con CAST
--     explícito: Oracle no siempre infiere el tipo de un NULL pelado en un
--     UNION.
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

-- Por el filtro de periodo de /admin/movimientos, que es un rango sobre
-- created_at: concentrator_movement ya tenía el suyo y account_movement no,
-- así que media consulta iba por índice y la otra media a barrido completo.
-- El ORDER BY no se beneficia: cae sobre el resultado del UNION.
CREATE INDEX idx_acct_mov_created ON account_movement (created_at);

-- ============================================================
-- Notas de reglas de negocio que se aplican en la CAPA JAVA (no en SQL):
--   · Reintegración automática: al borrar account/cardholder, el saldo regresa
--     a concentrator_account antes de borrar; las tarjetas se invalidan.
--   · Transferencia P2P: solo entre cuentas con el MISMO category_id (mismo propósito).
--   · Toda operación de dinero es transaccional y escribe en account_movement.
--   · Todo cambio de saldo de la Concentradora escribe en concentrator_movement.
--     El asiento se escribe dentro de ConcentratorDao, junto a los tres métodos
--     que tocan el saldo, para que ninguna ruta pueda moverlo sin dejar rastro.
--   · El código de empleado se genera al dar de alta (ver EmployeeCode) y no se
--     vuelve a calcular al leer.
-- ============================================================

-- ============================================================
-- Seed mínimo para poder trabajar
-- ============================================================
INSERT INTO concentrator_account (name, balance, clabe)
VALUES ('Cuenta Concentradora', 1000000.00, '012180000000000002');

-- Los códigos consumen la secuencia en vez de ir escritos a mano: si se fijaran
-- como 'JL0001', la secuencia volvería a entregar el 1 en la primera alta y
-- chocaría contra uq_cardholder_employee_code.
-- El catálogo de áreas va antes que la gente: cardholder.department_id apunta
-- aquí, así que estas filas tienen que existir primero.
INSERT INTO department (name, description) VALUES ('IT',               'Tecnologías de la información');
INSERT INTO department (name, description) VALUES ('Finanzas',         'Contabilidad y control de gastos');
INSERT INTO department (name, description) VALUES ('Operaciones',      'Logística y operación diaria');
INSERT INTO department (name, description) VALUES ('Ventas',           'Fuerza comercial y viáticos');
INSERT INTO department (name, description) VALUES ('Recursos Humanos', 'Personal y prestaciones');

INSERT INTO cardholder (first_name, last_name, email, phone, employee_code, department_id)
VALUES ('Juan', 'Lopez', 'juanlopez@empresa.com', '7771234567',
        'JL' || LPAD(seq_employee_code.NEXTVAL, 4, '0'),
        (SELECT id FROM department WHERE name = 'IT'));
INSERT INTO cardholder (first_name, last_name, email, phone, employee_code, department_id)
VALUES ('Raul', 'Torres', 'raultorres@empresa.com', '7773216548',
        'RT' || LPAD(seq_employee_code.NEXTVAL, 4, '0'),
        (SELECT id FROM department WHERE name = 'Ventas'));

INSERT INTO category (name, color_index, description) VALUES ('Gasolina',  1, 'Combustible vehicular asignado');
INSERT INTO category (name, color_index, description) VALUES ('Viajes',    2, 'Viáticos y viajes corporativos');
INSERT INTO category (name, color_index, description) VALUES ('Alimentos', 3, 'Alimentos y restaurantes');

-- Admin inicial. Password: sgfte
INSERT INTO app_user (email, password_hash, full_name, role) VALUES ('admin@empresa.com', '$2a$12$EtVTq2PPQ8dp/XE0GHMomeJKZuU6bT.KRpGK91l3ji6rm5mBHFjFi', 'Administrador General', 'ADMIN');

-- Fila de apertura del ledger de la Concentradora.
--
-- Va FECHADA EN EL PASADO, no con SYSTIMESTAMP, y eso es lo importante: el saldo
-- "de hace un periodo" se busca como el último movimiento ANTERIOR al inicio de
-- la ventana. Una apertura fechada hoy no es anterior a ninguna ventana, así que
-- la comparación de Analíticas saldría en guion para siempre. 400 días cubren de
-- sobra la ventana más larga del selector (12 meses).
INSERT INTO concentrator_movement (movement_type, amount, balance_after, actor, created_at)
SELECT 'FUNDING', GREATEST(balance, 0.01), balance, 'saldo inicial', SYSDATE - 400
  FROM concentrator_account
 WHERE singleton = 'Y';

COMMIT;

-- ── PASO 3 · Datos de demo (docker/demo-data.sql) ───────────────────────────
-- Se omite el "ALTER SESSION SET CONTAINER = FREEPDB1" del original: era para
-- el contenedor gvenzl/oracle-free, donde se entraba como SYS y había que
-- bajar al PDB. En Autonomous ese privilegio no existe y la sentencia truena.
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

-- ── PASO 4 · Verificación ───────────────────────────────────────────────────
SELECT 'cardholder' t, COUNT(*) n FROM sgfte.cardholder
UNION ALL SELECT 'app_user',              COUNT(*) FROM sgfte.app_user
UNION ALL SELECT 'account',               COUNT(*) FROM sgfte.account
UNION ALL SELECT 'card',                  COUNT(*) FROM sgfte.card
UNION ALL SELECT 'category',              COUNT(*) FROM sgfte.category
UNION ALL SELECT 'department',            COUNT(*) FROM sgfte.department
UNION ALL SELECT 'account_movement',      COUNT(*) FROM sgfte.account_movement
UNION ALL SELECT 'concentrator_movement', COUNT(*) FROM sgfte.concentrator_movement
UNION ALL SELECT 'funding_deposit',       COUNT(*) FROM sgfte.funding_deposit
UNION ALL SELECT 'audit_log',             COUNT(*) FROM sgfte.audit_log;

-- Los índices que ponen las reglas de V6, V15 y V16 en la base:
SELECT index_name FROM all_indexes WHERE owner = 'SGFTE'
   AND index_name IN ('UQ_CARD_ACTIVE_TYPE', 'UQ_ACCOUNT_ACTIVE_PURPOSE',
                      'UQ_CARDHOLDER_EMAIL_LOWER', 'UQ_APP_USER_EMAIL_LOWER')
 ORDER BY 1;

-- El saldo de la Concentradora tiene que cuadrar con la suma de su ledger:
SELECT c.balance AS saldo_tabla,
       (SELECT NVL(SUM(CASE WHEN movement_type IN ('FUNDING','REINTEGRATION')
                            THEN amount ELSE -amount END), 0)
          FROM sgfte.concentrator_movement) AS suma_ledger
  FROM sgfte.concentrator_account c WHERE c.singleton = 'Y';
