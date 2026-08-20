-- ============================================================
-- V14 · app_user.photo · foto de perfil, sólo para la pantalla de Ajustes.
--
-- En la base y no en disco. Un <img src="/uploads/12.png"> obliga a que exista
-- una carpeta que sobreviva, y no sobrevive: `mvnw clean` borra target/, que es
-- donde se despliega el WAR; el contenedor de Tomcat no tiene volumen para
-- subidas, así que un `docker compose down` se las lleva; y en la VM habría que
-- crearla, darle permisos al usuario de Tomcat y respaldarla aparte del backup
-- de Oracle. Dentro del BLOB, la foto viaja con el respaldo que ya se hace del
-- esquema. Para una foto por usuario y 2 MB de tope, el costo es cero.
--
-- photo_type se guarda porque es lo que el servlet devuelve como Content-Type
-- al servir la imagen: sin él habría que adivinar el formato al leerla. El
-- CHECK lo cierra a tres valores para que ese header nunca pueda salir de la
-- base con algo que el navegador no deba interpretar — la validación del
-- servlet da el mensaje en español, ésta hace que no dependa de aquélla.
--
-- Aplicar sobre una base ya creada. En una base nueva ya viene en schema.sql /
-- init-schema.sql.
-- ============================================================

ALTER TABLE app_user ADD (
    photo      BLOB,
    photo_type VARCHAR2(40)
);

ALTER TABLE app_user ADD CONSTRAINT chk_app_user_photo_type
    CHECK (photo_type IS NULL OR photo_type IN ('image/png', 'image/jpeg', 'image/webp'));

COMMENT ON COLUMN app_user.photo IS
    'Foto de perfil. NULL = sin foto; sólo se muestra en la pantalla de Ajustes.';
COMMENT ON COLUMN app_user.photo_type IS
    'Content-Type con el que se sirve la foto. NULL cuando photo es NULL.';
