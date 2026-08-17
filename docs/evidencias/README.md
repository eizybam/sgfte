# Evidencias de la entrega

Una captura por caso, con el nombre que aquí se indica, para que la matriz de
trazabilidad (`../MATRIZ_QA.md`) pueda citarlas sin ambigüedad.

Antes de empezar, deja la base como la ve un evaluador:

```bash
docker compose down -v && docker compose up --build
```

Y **apunta el saldo de la Concentradora**: es el testigo de la mitad de los
casos.

| Archivo | Caso | Qué tiene que verse |
|---|---|---|
| `01-login-admin.png` | RF-01 | Sesión iniciada como `admin@empresa.com` |
| `02-dispersion-confirmada.png` | CP-02 | El modal "DISPERSIÓN CONFIRMADA" con el importe |
| `03-expedicion-tarjeta.png` | CP-03 | "EXPEDICIÓN CONFIRMADA" y el tipo de tarjeta |
| `04-cierre-cuenta.png` | **CP-04** | "REINTEGRACIÓN CONFIRMADA" al cerrar una cuenta con saldo |
| `04b-concentradora-antes-despues.png` | CP-04 · RNF-03 | El saldo de la Concentradora antes y después, uno al lado del otro |
| `05-baja-empleado.png` | **CP-05** | La baja, y sus cuentas cerradas y tarjetas invalidadas |
| `06-transferencia-p2p.png` | CP-06 | Transferencia entre dos cuentas del mismo propósito |
| `07-tarjeta-bloqueada.png` | RF-04 | El portal con la tarjeta marcada BLOQUEADA |
| `07b-gasto-rechazado.png` | RF-04 | "Gasto rechazado" al intentar pagar con ella |
| `08-notificacion-correo.png` | CP-07 | El correo recibido por el empleado |
| `09-logs-inmutables.png` | **CP-08** | La consola de Oracle **rechazando** el `UPDATE` (ver abajo) |
| `10-analiticas-export.png` | CP-09 | Dashboard con periodo elegido + el CSV descargado |
| `11-ajustes-contrasena.png` | RF-01 | "Contraseña actualizada" en `/admin/ajustes` |
| `12-aislamiento-portal.png` | RNF-05 | 404 al pedir la cuenta de otro empleado desde `/app` |
| `invariante-dinero.md` | RNF-03 | La tabla de saldos antes/después de cada operación |

## CP-08 — la captura buena no es la pantalla de logs

Es la base rechazando el cambio:

```sql
UPDATE audit_log SET severity = 'INFO' WHERE id = 1;
-- ORA-20003: audit_log es inmutable: no se permite modificar ni borrar
```

Lo mismo vale para `account_movement` y `notification`, que llevan el mismo
trigger. Que la bitácora sea inmutable **por la base** y no por convención es
justo lo que RNF-04 pide demostrar.

## Invariante financiera (RNF-03)

En `invariante-dinero.md`, una fila por operación:

| Operación | Concentradora + Σ cuentas (antes) | (después) | ¿Cambia? |
|---|---|---|---|
| Dispersión | | | no |
| Transferencia P2P | | | no |
| Gasto rechazado | | | no |
| Cierre de cuenta | | | no |
| Gasto aceptado | | | sí, sale del sistema |
| Fondeo | | | sí, entra al sistema |

```sql
SELECT (SELECT balance FROM concentrator_account WHERE ROWNUM = 1)
     + (SELECT NVL(SUM(balance), 0) FROM account WHERE status = 'ACTIVE') AS total
  FROM dual;
```
