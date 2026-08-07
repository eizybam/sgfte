package mx.sgfte.core.accounts;

/**
 * Thrown by AccountDao when the UNIQUE constraint on account_number is violated.
 * Signals AccountService to regenerate the code and retry — it is NOT a user error.
 */
public class DuplicateAccountNumberException extends RuntimeException {
    public DuplicateAccountNumberException(String accountNumber, Throwable cause) {
        super("Duplicate account_number: " + accountNumber, cause);
    }
}
