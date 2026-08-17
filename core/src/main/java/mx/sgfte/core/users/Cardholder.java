package mx.sgfte.core.users;

/**
 * A cardholder (Tarjetahabiente). Core domain entity.
 * Money never lives here — it lives in the Account.
 */
public class Cardholder {

    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String status; // ACTIVE / INACTIVE
    private String employeeCode;  // "DJE0077" — se asigna al registrar y no cambia

    /*
      El área a la que pertenece. Desde V10 es una FK al catálogo department:
      departmentId es lo que se guarda, y department el nombre que se lee para
      pintarlo. Escribir el nombre no cambia nada en la base — es sólo lectura.
     */
    private Long departmentId;
    private String department;

    public Cardholder() {}

    public Cardholder(String firstName, String lastName, String email, String phone) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.status = "ACTIVE";
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getEmployeeCode() { return employeeCode; }
    public void setEmployeeCode(String employeeCode) { this.employeeCode = employeeCode; }

    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
}
