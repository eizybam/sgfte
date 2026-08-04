-- SGFTE · V4 · Severidad, módulo y origen en la bitácora
-- Oracle 23. Ejecutar conectado al esquema SGFTE.
--
-- Por qué: la pantalla "Logs y Auditoría" (Figma 253:43) muestra seis columnas
-- —fecha, NIVEL, acción, usuario, MÓDULO, ORIGEN— y audit_log sólo guardaba
-- event_type, detail, actor y created_at. Faltaban las tres en mayúsculas.
--
-- La columna se llama `severity` y no `level` a propósito: LEVEL es una
-- pseudocolumna de Oracle (la de CONNECT BY) y usarla como nombre obliga a ir
-- entrecomillando en cada consulta.
--
-- ip_address llega a 45 caracteres para que quepa una IPv6 completa; el
-- contenedor y el proxy pueden entregar ::1 o una dirección mapeada.

ALTER TABLE audit_log ADD (
    severity   VARCHAR2(10) DEFAULT 'INFO' NOT NULL,
    module     VARCHAR2(30),
    ip_address VARCHAR2(45)
);

ALTER TABLE audit_log ADD CONSTRAINT chk_audit_severity
    CHECK (severity IN ('INFO', 'ALERTA', 'CRIT'));

-- Se consulta casi siempre por fecha descendente y filtrando por severidad.
CREATE INDEX idx_audit_created  ON audit_log (created_at);
CREATE INDEX idx_audit_severity ON audit_log (severity);

COMMIT;

-- Comprobación:
-- SELECT event_type, severity, module, actor, ip_address, created_at
--   FROM audit_log ORDER BY created_at DESC FETCH FIRST 10 ROWS ONLY;
