package mx.sgfte.core.funding;

/**
 * La referencia ya estaba registrada: este depósito ya se contabilizó.
 *
 * Va aparte de ValidationException porque no es lo mismo "capturaste mal" que
 * "esto ya entró". Lo primero se corrige y se reintenta; lo segundo significa
 * que el sistema hizo bien su trabajo y no hay nada que corregir. El endpoint
 * lo traduce a 409 y no a 400 por la misma razón.
 */
public class DuplicateDepositException extends RuntimeException {

    private final String referencia;

    public DuplicateDepositException(String referencia) {
        super("La referencia " + referencia + " ya fue registrada");
        this.referencia = referencia;
    }

    public String getReferencia() { return referencia; }
}
