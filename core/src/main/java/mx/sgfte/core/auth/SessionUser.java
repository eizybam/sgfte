package mx.sgfte.core.auth;

import java.io.Serializable;

/**
 * Minimal identity kept in the session after login — never the password/hash.
 * Plain getters so JSP EL can read ${sessionScope.user.fullName}.
 */
public class SessionUser implements Serializable {

    private final long id;
    private final String fullName;
    private final String role;

    public SessionUser(long id, String fullName, String role) {
        this.id = id;
        this.fullName = fullName;
        this.role = role;
    }

    public long getId() { return id; }
    public String getFullName() { return fullName; }
    public String getRole() { return role; }
}
