-- SGFTE · V5 · Descripción y color propio en el catálogo de categorías
-- Oracle 23. Ejecutar conectado al esquema SGFTE.
--
-- Por qué: la pantalla "Categorías - Admin" (Figma 2036:6) lista una columna
-- DESCRIPCIÓN, y su modal (2041:155) deja ELEGIR el color entre siete. Hasta
-- ahora la tabla sólo tenía id, name y status, y el color no se guardaba: se
-- derivaba en cada consulta con
--
--     (SELECT MOD(COUNT(*), 4) + 1 FROM category c_rank WHERE c_rank.id < cat.id)
--
-- es decir, la posición de la categoría en el catálogo. Eso da un color estable
-- pero AJENO a quien la crea, y con siete colores en el prototipo ya no basta.
-- Al pasar a columna, el color se elige una vez y deja de moverse: insertar una
-- categoría en medio ya no repinta las siguientes, que es justo lo que hacía la
-- fórmula anterior.
--
-- Los cuatro primeros colores de la paleta del modal son exactamente los cuatro
-- --sgfte-purpose-N que ya usa la hoja de estilos, así que el relleno de abajo
-- reproduce la fórmula vieja y NADA cambia de aspecto al aplicar esta migración.

ALTER TABLE category ADD (
    description VARCHAR2(120),
    color_index NUMBER(1)
);

-- Relleno: el mismo color que la fórmula daba hasta ahora, congelado.
UPDATE category c
   SET c.color_index = (SELECT MOD(COUNT(*), 4) + 1
                          FROM category c_rank
                         WHERE c_rank.id < c.id)
 WHERE c.color_index IS NULL;

-- Descripciones del prototipo para las categorías sembradas. Sólo toca las que
-- siguen vacías, para no pisar nada escrito a mano.
--
-- El marco lista cinco categorías (Gasolina, Viajes, Comida, Viáticos...) y el
-- esquema siembra tres (Gasolina, Viajes, Alimentos): son datos de maqueta, no
-- un catálogo acordado. Se rellenan las que existen de verdad; el resto se dan
-- de alta desde la pantalla, que para eso está.
UPDATE category SET description = 'Combustible vehicular asignado' WHERE name = 'Gasolina'  AND description IS NULL;
UPDATE category SET description = 'Viáticos y viajes corporativos' WHERE name = 'Viajes'    AND description IS NULL;
UPDATE category SET description = 'Alimentos y restaurantes'       WHERE name = 'Alimentos' AND description IS NULL;

-- NOT NULL va DESPUÉS del relleno: antes, las filas existentes lo violarían.
ALTER TABLE category MODIFY (color_index DEFAULT 1 NOT NULL);

ALTER TABLE category ADD CONSTRAINT chk_category_color
    CHECK (color_index BETWEEN 1 AND 7);

COMMIT;

-- Comprobación:
-- SELECT id, name, color_index, description FROM category ORDER BY id;
