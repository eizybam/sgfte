-- Pegar TAL CUAL en Database Actions -> SQL (F5 = Run Script).
-- La primera linea es obligatoria: Database Actions entra como ADMIN, no como SGFTE.
ALTER SESSION SET CURRENT_SCHEMA = SGFTE;

-- SGFTE · V15 · Una cuenta ACTIVA de cada propósito por tarjetahabiente
-- Oracle 23. Ejecutar conectado al esquema SGFTE.
--
-- Por qué: la regla "un tarjetahabiente no tiene dos cuentas del mismo
-- propósito" vivía sólo en AccountService, y encima mal contada — miraba TODAS
-- las cuentas, cerradas incluidas. Cerrar la cuenta de Alimentos y volver a
-- crearla fallaba con "ya tiene una cuenta con ese propósito", apuntando a una
-- cuenta que para el sistema ya no existe. El aviso de cierre promete lo
-- contrario: "si vuelve a hacer falta ese propósito, se crea una cuenta nueva".
--
-- El servicio ya está corregido. Esto pone la regla también en la base, que es
-- donde no se puede olvidar: dos pestañas abiertas creando la misma cuenta a la
-- vez pasan las dos por el SELECT del servicio antes de que ninguna inserte.
--
-- Índice ÚNICO y no CHECK, y con el mismo CASE que V6 hizo para las tarjetas:
-- un CHECK sólo ve la fila que se inserta, y aquí hay que mirar las demás filas
-- del mismo tarjetahabiente. En Oracle una entrada de índice con TODAS sus
-- columnas en NULL no se guarda, así que las cuentas cerradas quedan fuera del
-- índice y no estorban — que es justo la corrección.

CREATE UNIQUE INDEX uq_account_active_purpose ON account (
    CASE WHEN status = 'ACTIVE' THEN cardholder_id END,
    CASE WHEN status = 'ACTIVE' THEN category_id  END
);

-- Comprobación previa: si esto devuelve filas, el índice no se puede crear y
-- hay duplicados que resolver a mano primero. No debería devolver ninguna: el
-- servicio era MÁS estricto de la cuenta, nunca menos.
-- SELECT cardholder_id, category_id, COUNT(*)
--   FROM account WHERE status = 'ACTIVE'
--  GROUP BY cardholder_id, category_id HAVING COUNT(*) > 1;
