package mx.sgfte.core.auth;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Password hashing with bcrypt. bcrypt salts automatically and embeds the
 * cost + salt inside the hash string, so verification needs only the hash.
 */
public class PasswordHasher {

    private static final int COST = 12;  // work factor; higher = slower = safer

    private PasswordHasher() {}

    public static String hash(String rawPassword) {
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt(COST));
    }

    public static boolean matches(String rawPassword, String storedHash) {
        return BCrypt.checkpw(rawPassword, storedHash);
    }

    /** Utility: generate a hash to seed a user. Usage: PasswordHasher "myPassword" */
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("usage: PasswordHasher <password>");
            return;
        }
        System.out.println(hash(args[0]));
    }

}
