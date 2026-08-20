package mx.sgfte.core.shared.time;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * La hora que se guarda y la hora que se enseña, que no son la misma.
 *
 * ── El problema ─────────────────────────────────────────────
 *
 * Las marcas de tiempo las pone la base con DEFAULT SYSTIMESTAMP, y el servidor
 * de Oracle Cloud corre en UTC. La columna es TIMESTAMP a secas, sin zona, así
 * que lo que queda guardado es la hora de pared de UTC: un movimiento hecho a
 * las 22:42 en CDMX se lee "04:42" del día siguiente. Las pantallas lo pintaban
 * tal cual, y de ahí que todo se viera seis horas adelantado.
 *
 * ── Por qué se convierte al ENSEÑAR y no al guardar ─────────
 *
 * Podría pensarse que lo correcto es guardar ya en hora local. No lo es, y
 * además aquí es imposible:
 *
 *   · account_movement, audit_log, concentrator_movement y funding_deposit
 *     tienen triggers de inmutabilidad que rechazan cualquier UPDATE. Las filas
 *     que ya existen NO se pueden reescribir — es a propósito, es un ledger—,
 *     así que cambiar sólo el DEFAULT dejaría media tabla en UTC y media en
 *     CDMX. Mezclar es peor que estar corrido de forma uniforme.
 *
 *   · AnalyticsDao construye los extremos de sus rangos en Java y los compara
 *     contra lo guardado. Hoy cuadra justamente porque los dos relojes son UTC.
 *     Mover uno solo descuadraría los reportes en silencio, que es la peor
 *     forma de romper algo.
 *
 * Así que dentro del sistema todo sigue en UTC —una sola vara de medir— y la
 * conversión ocurre en el último metro, cuando la fecha deja de ser un dato y
 * pasa a ser texto en pantalla. Es también lo que se defiende solo: se almacena
 * en UTC y se muestra en la zona en la que opera la empresa.
 *
 * ── Por qué una zona y no un desfase fijo ───────────────────
 *
 * ZoneId y no "menos seis horas": México suprimió el horario de verano en 2022,
 * pero las fechas anteriores a ese cambio siguen en la base, y la zona sabe cuál
 * era la regla en cada momento. Un -6 a secas las movería mal.
 */
public final class AppTime {

    /** Cómo guarda la base: hora de pared de UTC en una columna sin zona. */
    public static final ZoneId STORAGE = ZoneOffset.UTC;

    /** La zona en la que opera la empresa, y en la que se lee todo en pantalla. */
    public static final ZoneId LOCAL = ZoneId.of("America/Mexico_City");

    private AppTime() {}

    /**
     * Una marca de tiempo recién salida de la base, lista para enseñarse.
     *
     * Null entra y null sale: hay columnas opcionales —la última recarga de una
     * cuenta que nunca recibió una, por ejemplo— y quien llama ya sabe pintar el
     * guion.
     */
    public static LocalDateTime display(LocalDateTime stored) {
        if (stored == null) return null;
        return stored.atZone(STORAGE).withZoneSameInstant(LOCAL).toLocalDateTime();
    }

    /**
     * Hoy, aquí.
     *
     * La necesitan las pantallas que dicen "Hoy" y "Ayer" en vez de la fecha: si
     * compararan contra LocalDate.now() —que en el contenedor es UTC— entre las
     * 18:00 y la medianoche de CDMX llamarían "mañana" a lo que acaba de pasar.
     */
    public static LocalDate today() {
        return LocalDate.now(LOCAL);
    }

    /** Ahora, aquí. Para el sello de las tarjetas de resultado. */
    public static LocalDateTime now() {
        return LocalDateTime.now(LOCAL);
    }

    /**
     * Un instante que produjo LocalDateTime.now(), listo para enseñarse.
     *
     * Se distingue de display() a propósito. display() da por hecho que el valor
     * viene de la base, que guarda en UTC. Esto otro viene del reloj de la JVM,
     * y lo que hay que suponer es la zona del sistema — hoy UTC en el contenedor,
     * pero se lee de ZoneId.systemDefault() en vez de darlo por sentado: si
     * mañana alguien pone TZ en el docker-compose, esto sigue estando bien en
     * vez de restar seis horas de más.
     */
    public static LocalDateTime displaySystem(LocalDateTime systemNow) {
        if (systemNow == null) return null;
        return systemNow.atZone(ZoneId.systemDefault())
                        .withZoneSameInstant(LOCAL)
                        .toLocalDateTime();
    }
}
