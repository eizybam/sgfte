# Plan de pruebas QA — Control de acceso por roles (RF-13, RNF-05, RF-01)

Requisito previo: dos usuarios en `app_user`.

```sql
-- Admin del seed (password: Sgfte2026$)
SELECT email, role FROM app_user WHERE email = 'admin@empresa.com';

-- Alta de un tarjetahabiente con login, ligado a su ficha.
-- El hash es el mismo del seed, así que la contraseña también es Sgfte2026$
INSERT INTO app_user (email, password_hash, full_name, role, cardholder_id)
VALUES ('juanlopez@empresa.com',
        '$2a$12$P6s7lL4fFj3Qcy0Szxi1g.mKC1LD6DlVRn6xKyiNTD7yK4am6T2ky',
        'Juan Lopez', 'TARJETAHABIENTE',
        (SELECT id FROM cardholder WHERE email = 'juanlopez@empresa.com'));
COMMIT;
```

## Matriz de acceso esperada

| # | URL | Anónimo | TARJETAHABIENTE | ADMIN |
|---|-----|---------|-----------------|-------|
| A1 | `/login` (GET) | Formulario | → `/app/home` | → `/admin/home` |
| A2 | `/admin/home` | → `/login` | → `/app/home` | 200 |
| A3 | `/admin/concentradora` | → `/login` | → `/app/home` | 200 |
| A4 | `/admin/dispersion` | → `/login` | → `/app/home` | 200 |
| A5 | `/admin/cards` | → `/login` | → `/app/home` | 200 |
| A6 | `/admin/transferencia` | → `/login` | → `/app/home` | 200 |
| A7 | `/admin/movimientos` | → `/login` | → `/app/home` | 200 |
| A8 | `/admin/logs` | → `/login` | → `/app/home` | 200 |
| A9 | `/admin/dashboard` | → `/login` | → `/app/home` | 200 |
| A10 | `/admin/analytics.json` | → `/login` | → `/app/home` | 200 (JSON) |
| A11 | **`/accounts`** | → `/login` | → `/app/home` | 200 |
| A12 | **`/cardholders`** | → `/login` | → `/app/home` | 200 |
| A13 | `/app/*` | → `/login` | 200 | → `/admin/home` |
| A14 | `/logout` (POST) | — | Cierra sesión → `/login` | Cierra sesión → `/login` |

> Las filas **A11 y A12** son la regresión importante: antes de este cambio
> `/accounts` y `/cardholders` respondían **200 sin sesión alguna**, es decir,
> cualquier persona podía dar de alta cuentas y tarjetahabientes.

## Casos de prueba

| # | Caso | Pasos | Resultado esperado |
|---|------|-------|--------------------|
| S1 | Acceso anónimo al área admin | Sin iniciar sesión, abrir `/admin/home` | Redirige a `/login`; no se filtra ningún dato |
| S2 | Alta anónima de cuenta (regresión) | Sin sesión, `POST /accounts` con datos válidos | Redirige a `/login`; **no** se inserta fila en `account` |
| S3 | Alta anónima de tarjetahabiente (regresión) | Sin sesión, `POST /cardholders` con datos válidos | Redirige a `/login`; **no** se inserta fila en `cardholder` |
| S4 | Escalada de privilegios | Login como `juanlopez@empresa.com`, escribir a mano `/admin/dispersion` | Redirige a `/app/home`; nunca se muestra el formulario |
| S5 | Escalada vía POST | Con sesión de tarjetahabiente, `POST /admin/dispersion` con monto | Redirige a `/app/home`; el saldo de la Concentradora **no** cambia |
| S6 | Admin sin restricción | Login como `admin@empresa.com`, recorrer A2–A12 | Todas responden 200 |
| S7 | Login ya iniciado (tarjetahabiente) | Con sesión activa de empleado, abrir `/login` | Redirige a `/app/home`, **no** a `/admin/home` |
| S8 | Login ya iniciado (admin) | Con sesión activa de admin, abrir `/login` | Redirige a `/admin/home` |
| S9 | Sin bucle de redirección | Repetir S4 y observar la traza de red | Máximo un redirect; el navegador no reporta "too many redirects" |
| S10 | Fijación de sesión | Anotar `JSESSIONID` antes del login, comparar después | El identificador cambia (la sesión previa se invalida) |
| S11 | Sesión cerrada | `POST /logout`, luego abrir `/admin/home` | Redirige a `/login` |
| S12 | Expiración | Iniciar sesión, esperar >30 min inactivo, abrir `/admin/home` | Redirige a `/login` |

## Verificación en BD (tras S2 y S3)

```sql
-- Ninguna de las dos debe haber crecido durante las pruebas anónimas.
SELECT COUNT(*) FROM account;
SELECT COUNT(*) FROM cardholder;
```

## Notas de implementación

- `AuthFilter` mapea `{"/admin/*", "/accounts", "/cardholders"}` y aplica dos
  comprobaciones en orden: sesión válida y después rol `ADMIN`.
- `AppAuthFilter` mapea `/app/*` y exige sesión válida **y** que el login esté
  ligado a una ficha de tarjetahabiente. Un `ADMIN` tiene `cardholder_id = NULL`,
  así que no tiene cuentas que mostrar y se le redirige a `/admin/home`.
- Los literales de rol viven en `mx.sgfte.core.auth.Role` y deben coincidir con
  el `CHECK chk_app_user_role` de `docs/schema.sql`.
- **Pendiente:** `/accounts` y `/cardholders` deberían moverse bajo `/admin/`
  por coherencia. No se hizo aquí para no romper las URLs que otras ramas en
  curso ya están usando.
