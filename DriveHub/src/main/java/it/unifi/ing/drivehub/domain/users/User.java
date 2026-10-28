package it.unifi.ing.drivehub.domain.users;

import it.unifi.ing.drivehub.domain.DomainRuleViolationException;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

public abstract class User {
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private Long id;
    private final String name;
    private final String surname;
    private final String email;
    private final String passwordHash;

    protected User(Long id, String name, String surname, String email, String passwordHash) {
        if (id != null && id <= 0) {
            throw new IllegalArgumentException("id must be positive");
        }
        this.id = id;
        this.name = requireText(name, "name");
        this.surname = requireText(surname, "surname");
        this.email = normalizeEmail(email);
        this.passwordHash = requireText(passwordHash, "passwordHash");
    }

    public static User create(String name, String surname, String email, String passwordHash, Role role) {
        Objects.requireNonNull(role, "role");
        return switch (role) {
            case CLIENT, CUSTOMER -> new Client(null, name, surname, email, passwordHash, role);
            case SALESMAN -> new Salesman(null, name, surname, email, passwordHash);
            case MANAGER -> new Manager(null, name, surname, email, passwordHash);
        };
    }

    public Long id() {
        return id;
    }

    public void assignId(long id) {
        if (id <= 0) {
            throw new IllegalArgumentException("id must be positive");
        }
        if (this.id != null && this.id != id) {
            throw new DomainRuleViolationException("entity id is already assigned");
        }
        this.id = id;
    }

    public long requireId() {
        if (id == null) {
            throw new DomainRuleViolationException("entity has not been persisted yet");
        }
        return id;
    }

    public String name() {
        return name;
    }

    public String surname() {
        return surname;
    }

    public String firstName() {
        return name;
    }

    public String lastName() {
        return surname;
    }

    public String email() {
        return email;
    }

    public String passwordHash() {
        return passwordHash;
    }

    public abstract Role role();

    public String fullName() {
        return name + " " + surname;
    }

    public String displayName() {
        return fullName();
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }

    private static String normalizeEmail(String email) {
        String trimmed = requireText(email, "email").toLowerCase(Locale.ROOT);
        if (!EMAIL.matcher(trimmed).matches()) {
            throw new IllegalArgumentException("invalid email: " + email);
        }
        return trimmed;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User user)) return false;
        return Objects.equals(email, user.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(email);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", surname='" + surname + '\'' +
                ", email='" + email + '\'' +
                ", role=" + role() +
                '}';
    }
}
