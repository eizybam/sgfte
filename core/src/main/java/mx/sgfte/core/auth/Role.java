package mx.sgfte.core.auth;

/**
 * The two roles the system knows about. These strings must match the values
 * allowed by chk_app_user_role in the database (see docs/schema.sql), so keep
 * them here instead of scattering literals across filters and servlets.
 */
public final class Role {

    /** Administrator / Manager: full access to the /admin area. */
    public static final String ADMIN = "ADMIN";

    /**
     * Cardholder (empleado). Access limited to the /app area.
     * Kept in Spanish because that is the value stored in app_user.role.
     */
    public static final String CARDHOLDER = "TARJETAHABIENTE";

    private Role() {}

    /** Landing page for a role right after login. */
    public static String homeFor(String role) {
        return ADMIN.equals(role) ? "/admin/home" : "/app/home";
    }
}
