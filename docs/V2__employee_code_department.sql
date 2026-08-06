-- SGFTE · V2 · Código de empleado y departamento
-- Oracle 23 (contenedor gvenzl/oracle-free). Ejecutar conectado al esquema SGFTE.
--
-- Por qué: el prototipo enseña un código de empleado ("DJE0077") y un
-- departamento en tres pantallas —alta, listado y expedición de tarjetas— y
-- ninguno de los dos existía en `cardholder`.
--
-- Regla del código: iniciales del nombre completo + cuatro dígitos de una
-- SECUENCIA. Las iniciales no bastan para ser únicas (dos "Juan Pérez López"
-- dan JPL), así que la unicidad la garantiza el número, no las letras. Se
-- genera UNA vez al insertar y no se vuelve a calcular: si luego se corrige una
-- errata en el nombre, el código no cambia — es un identificador, no un
-- resumen del nombre.
--
-- El departamento se deja como columna de texto a propósito. Hoy la única
-- opción es 'IT'; cuando exista un catálogo se cambia por una FK y estos
-- valores se migran.

ALTER TABLE cardholder ADD (
    employee_code VARCHAR2(12),
    department    VARCHAR2(60)
);

-- Arranca en 1: los cuatro dígitos aguantan 9,999 altas antes de necesitar
-- ensancharse. NOCACHE para que no se pierdan tramos al reiniciar la base;
-- el volumen de altas no justifica el caché.
CREATE SEQUENCE seq_employee_code
    START WITH 1
    INCREMENT BY 1
    NOCACHE
    NOCYCLE;

-- ---------------------------------------------------------------------------
-- Relleno de las filas que ya existen.
--
-- Las iniciales salen de la primera letra del nombre y de la primera del
-- apellido; TRANSLATE quita los acentos para que "Ángel Ñuño" dé "AN" y no
-- caracteres raros en un código que se lee en voz alta.
-- ---------------------------------------------------------------------------
UPDATE cardholder
   SET employee_code =
           TRANSLATE(UPPER(SUBSTR(TRIM(first_name), 1, 1) || SUBSTR(TRIM(last_name), 1, 1)),
                     'ÁÉÍÓÚÜÑÀÈÌÒÙÂÊÎÔÛ',
                     'AEIOUUNAEIOUAEIOU')
           || LPAD(seq_employee_code.NEXTVAL, 4, '0'),
       department = 'IT'
 WHERE employee_code IS NULL;

COMMIT;

-- Ya con todas las filas rellenas se pueden endurecer las restricciones.
ALTER TABLE cardholder MODIFY employee_code NOT NULL;
ALTER TABLE cardholder ADD CONSTRAINT uq_cardholder_employee_code UNIQUE (employee_code);

-- Comprobación:
-- SELECT id, first_name, last_name, employee_code, department FROM cardholder ORDER BY id;
