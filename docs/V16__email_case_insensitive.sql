-- SGFTE · V16 · El correo identifica a UNA persona, escríbase como se escriba
-- Oracle 23. Ejecutar conectado al esquema SGFTE.
--
-- El fallo: se podían dar de alta dos empleados con "mail@empresa.com" y
-- "mAIL@empresa.com". Son el mismo buzón y la misma persona, pero el UNIQUE de
-- Oracle compara carácter a carácter, así que para la base eran dos filas
-- distintas — y el chequeo previo del servicio (emailExists) tampoco miraba.
--
-- Es más grave de lo que parece porque el correo NO es un dato de contacto: es
-- el usuario con el que se inicia sesión. Dos filas para la misma persona
-- significan dos logins, dos tokens de activación y dos destinos posibles para
-- el mismo aviso.
--
-- Y tenía un segundo síntoma, del lado contrario: quien escribía su correo con
-- la primera letra en mayúscula —el teclado del móvil la pone solo— no
-- encontraba su propio usuario y recibía "Credenciales inválidas" con la
-- contraseña correcta.
--
-- El servicio ya guarda en minúsculas y las consultas ya comparan sin
-- distinguir. Esto lo pone en la base, que es donde la regla no depende de que
-- alguien se acuerde: dos pestañas dando de alta a la vez pasan las dos por el
-- SELECT del servicio antes de que ninguna inserte.
--
-- ── ORDEN DE EJECUCIÓN ──────────────────────────────────────
--
-- PASO 0. Busca los duplicados que YA existan. Si devuelve filas, el paso 1
--         falla con ORA-00001: hay que decidir a mano cuál se queda y borrar
--         la otra ANTES de seguir. No se puede automatizar — sólo tú sabes
--         cuál de las dos es la buena.

-- SELECT LOWER(email) AS correo, COUNT(*), LISTAGG(id, ', ') AS ids
--   FROM cardholder GROUP BY LOWER(email) HAVING COUNT(*) > 1;

-- SELECT LOWER(email) AS correo, COUNT(*), LISTAGG(id, ', ') AS ids
--   FROM app_user  GROUP BY LOWER(email) HAVING COUNT(*) > 1;

-- PASO 1. Deja lo ya guardado en la forma canónica, la misma que escribirá la
--         aplicación a partir de ahora. Sin esto convivirían dos maneras de
--         escribir a la misma persona, y la bitácora, el correo de activación
--         y la pantalla de ajustes enseñarían cada uno la suya.
--
--         Las dos tablas juntas y en la misma transacción: cardholder.email y
--         app_user.email son el mismo dato en dos sitios, y el servicio los
--         actualiza siempre a la vez.

UPDATE cardholder SET email = LOWER(TRIM(email)) WHERE email <> LOWER(TRIM(email));
UPDATE app_user   SET email = LOWER(TRIM(email)) WHERE email <> LOWER(TRIM(email));
COMMIT;

-- PASO 2. Que no vuelva a pasar.
--
--         Índice ÚNICO sobre una expresión, no una constraint: lo que tiene que
--         ser único no es la columna, es su versión en minúsculas. Con los
--         datos ya normalizados por el paso 1 el índice es hoy equivalente al
--         UNIQUE que ya existe; lo que aporta es que lo siga siendo el día que
--         alguien inserte saltándose la aplicación.

CREATE UNIQUE INDEX uq_cardholder_email_lower ON cardholder (LOWER(email));
CREATE UNIQUE INDEX uq_app_user_email_lower   ON app_user   (LOWER(email));

-- Los UNIQUE originales (uq_cardholder_email, uq_app_user_email) se quedan: no
-- estorban —con los datos en minúsculas comprueban lo mismo— y quitarlos sería
-- soltar una red mientras se tiende otra.
