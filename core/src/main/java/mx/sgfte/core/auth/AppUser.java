package mx.sgfte.core.auth;

public class AppUser {
    private Long id;
    private String email;
    private String passwordHash;
    private String fullName;
    private String role;
    private Long cardholderId;   // nullable; set for TARJETAHABIENTE logins
    private String status;       // ACTIVE | INACTIVE

    public AppUser() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Long getCardholderId() {
        return cardholderId;
    }

    public void setCardholderId(Long cardholderId) {
        this.cardholderId = cardholderId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}

