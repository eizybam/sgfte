package mx.sgfte.core.concentrator;

/** The Concentrator does not have enough balance to disperse. */
public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(String message) {
        super(message);
    }
}
