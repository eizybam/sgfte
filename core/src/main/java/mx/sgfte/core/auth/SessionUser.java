package mx.sgfte.core.auth;

import java.io.Serializable;

/**
 * Minimal identity kept in the session after login — never the password/hash.
 * Plain getters so JSP EL can read ${sessionScope.user.fullName}.
 */
public class SessionUser implements Serializable {

    private final long id;               // app_user.id — who logged in
    private final String fullName;
    private final String email;      // identifica de forma única a quien actúa
    private final String role;
    private final Long cardholderId;     // cardholder.id — NULL for admins

    public SessionUser(long id, String fullName, String email, String role, Long cardholderId) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
        this.cardholderId = cardholderId;
    }

    public long getId() { return id; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getRole() { return role; }

    /**
     * Which cardholder file this login belongs to, or null for an ADMIN.
     *
     * This is the value every /app query is scoped by, and it comes from the
     * session — never from a request parameter. That is what stops an employee
     * from reading someone else's accounts by editing the URL.
     */
    public Long getCardholderId() { return cardholderId; }

    /**
     * True when this session belongs to an administrator.
     * Exposed as a getter-style method so JSP EL can read ${sessionScope.user.admin}
     * to show or hide admin-only links.
     */
    public boolean isAdmin() { return Role.ADMIN.equals(role); }

    /** True when this login is tied to a cardholder file, i.e. it can use /app. */
    public boolean isCardholder() { return cardholderId != null; }
}
