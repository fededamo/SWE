package it.unifi.ing.drivehub.domain.users;

import it.unifi.ing.drivehub.domain.BaseEntity;
import it.unifi.ing.drivehub.domain.DomainRuleViolationException;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

public final class User extends BaseEntity {
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern FISCAL_CODE = Pattern.compile("^[A-Z0-9]{11,16}$");
    private static final Pattern PHONE = Pattern.compile("^\\+?[0-9][0-9 .-]{5,19}$");

    private final String fiscalCode;
    private final String firstName;
    private final String lastName;
    private final String email;
    private final String phone;
    private final String passwordHash;
    private final Role role;
    private Long managerId;
    private boolean active;

    public User(Long id, String fiscalCode, String firstName, String lastName, String email, String phone,
                String passwordHash, Role role, Long managerId, boolean active) {
        super(id);
        this.fiscalCode = normalizeFiscalCode(fiscalCode);
        this.firstName = requireText(firstName, "firstName");
        this.lastName = requireText(lastName, "lastName");
        this.email = normalizeEmail(email);
        this.phone = normalizePhone(phone);
        this.passwordHash = requireText(passwordHash, "passwordHash");
        this.role = Objects.requireNonNull(role, "role");
        if (managerId != null && managerId <= 0) {
            throw new IllegalArgumentException("managerId must be positive");
        }
        if (role != Role.SALESMAN && managerId != null) {
            throw new IllegalArgumentException("only a salesman may have an assigned manager");
        }
        this.managerId = managerId;
        this.active = active;
    }

    public static User register(String fiscalCode, String firstName, String lastName, String email,
                                String phone, String passwordHash, Role role, Long managerId) {
        return new User(null, fiscalCode, firstName, lastName, email, phone, passwordHash,
                role, managerId, true);
    }

    public String fiscalCode() {
        return fiscalCode;
    }

    public String firstName() {
        return firstName;
    }

    public String lastName() {
        return lastName;
    }

    public String displayName() {
        return firstName + " " + lastName;
    }

    public String email() {
        return email;
    }

    public String phone() {
        return phone;
    }

    public String passwordHash() {
        return passwordHash;
    }

    public Role role() {
        return role;
    }

    public Long managerId() {
        return managerId;
    }

    public boolean active() {
        return active;
    }

    public boolean hasRole(Role expected) {
        return role == expected;
    }

    public void requireRole(Role expected) {
        if (role != expected) {
            throw new DomainRuleViolationException("operation requires role " + expected);
        }
        if (!active) {
            throw new DomainRuleViolationException("inactive user cannot perform operations");
        }
    }

    public void deactivate() {
        active = false;
    }

    public void assignManager(User manager) {
        requireRole(Role.SALESMAN);
        Objects.requireNonNull(manager, "manager").requireRole(Role.MANAGER);
        managerId = manager.requireId();
    }

    public void removeManager() {
        requireRole(Role.SALESMAN);
        managerId = null;
    }

    public static String normalizeEmail(String email) {
        String normalized = requireText(email, "email").toLowerCase(Locale.ROOT);
        if (!EMAIL.matcher(normalized).matches()) {
            throw new IllegalArgumentException("invalid email address");
        }
        return normalized;
    }

    public static String normalizeFiscalCode(String fiscalCode) {
        String normalized = requireText(fiscalCode, "fiscalCode").toUpperCase(Locale.ROOT);
        if (!FISCAL_CODE.matcher(normalized).matches()) {
            throw new IllegalArgumentException("invalid fiscal code");
        }
        return normalized;
    }

    private static String normalizePhone(String phone) {
        String normalized = requireText(phone, "phone");
        if (!PHONE.matcher(normalized).matches()) {
            throw new IllegalArgumentException("invalid phone number");
        }
        return normalized;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }
}
