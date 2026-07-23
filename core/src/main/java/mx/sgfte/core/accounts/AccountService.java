package mx.sgfte.core.accounts;

import mx.sgfte.core.users.ValidationException;

import java.security.SecureRandom;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

/**
 * Business logic for accounts.
 * validate() is pure Java (no DB, no Tomcat) so it can be unit-tested directly.
 * create() adds the DB-backed rules: duplicate-purpose check + account_number generation.
 */
public class AccountService {

    // Excludes ambiguous chars (O/0, I/1) so codes are easy to read/type.
    // Exactly 32 symbols => 5 bits/char => 32^5 ≈ 33.5M codes per prefix.
    private static final String SUFFIX_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int SUFFIX_LENGTH = 5;
    private static final int MAX_TRIES = 10;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final AccountDao dao;

    public AccountService() {
        this(new AccountDao());
    }

    // Constructor for tests (inject a fake DAO).
    public AccountService(AccountDao dao) {
        this.dao = dao;
    }

    /**
     * Validates and creates an account. Generates the public account_number from
     * the category name (e.g. Gasolina -> "GAS-48HSY") and lets the DB's UNIQUE
     * constraint be the source of truth: if it collides, we regenerate and retry.
     *
     * @param account  the account to create (balance starts at 0)
     * @param categoryName name of the chosen category, used only for the code prefix
     * @return the new numeric id
     * @throws ValidationException if any field is missing or the purpose is duplicated
     */
    public long create(Account account, String categoryName) {
        List<String> errors = validate(account);
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        if (dao.existForPurpose(account.getCardholderId(), account.getCategoryId())) {
            throw new ValidationException(
                    List.of("El tarjetahabiente ya tiene una cuenta con ese propósito"));
        }

        String prefix = prefixFrom(categoryName);
        for (int i = 0; i < MAX_TRIES; i++) {
            account.setAccountNumber(prefix + "-" + randomSuffix());
            try {
                return dao.insert(account);
            } catch (DuplicateAccountNumberException e) {
                // Astronomically rare collision on account_number: regenerate and retry.
                // (Purpose duplicates surface as a RuntimeException, not this one.)
            }
        }
        throw new IllegalStateException(
                "No se pudo generar un número de cuenta único tras " + MAX_TRIES + " intentos");
    }

    /** Field validation, independent of the database. */
    public List<String> validate(Account account) {
        List<String> errors = new ArrayList<>();
        if (account.getCardholderId() == null) {
            errors.add("Debes elegir un tarjetahabiente");
        }
        if (account.getCategoryId() == null) {
            errors.add("Debes elegir un propósito");
        }
        return errors;
    }

    /** First 3 letters of the category, accents stripped, upper-cased (Gasolina -> GAS). */
    private String prefixFrom(String categoryName) {
        String base = categoryName == null ? "" : categoryName;
        String ascii = Normalizer.normalize(base, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String letters = ascii.replaceAll("[^A-Za-z]", "").toUpperCase();
        if (letters.length() < 3) {
            letters = letters + "XXX";
        }
        return letters.substring(0, 3);
    }

    private String randomSuffix() {
        StringBuilder sb = new StringBuilder(SUFFIX_LENGTH);
        for (int i = 0; i < SUFFIX_LENGTH; i++) {
            sb.append(SUFFIX_ALPHABET.charAt(RANDOM.nextInt(SUFFIX_ALPHABET.length())));
        }
        return sb.toString();
    }
}
