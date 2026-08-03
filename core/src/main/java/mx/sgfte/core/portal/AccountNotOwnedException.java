package mx.sgfte.core.portal;

/**
 * Raised when a logged-in employee refers to an account that is not theirs.
 *
 * Normally this means someone edited an id in the URL or in a form field, so the
 * servlet answers with a plain 404 rather than an explanatory message: confirming
 * that "this account exists but is not yours" would leak the existence of other
 * people's accounts.
 */
public class AccountNotOwnedException extends RuntimeException {

    public AccountNotOwnedException(long cardholderId, long accountId) {
        super("Cardholder " + cardholderId + " does not own account " + accountId);
    }
}
