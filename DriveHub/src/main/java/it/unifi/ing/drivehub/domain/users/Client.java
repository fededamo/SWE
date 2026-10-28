package it.unifi.ing.drivehub.domain.users;

public final class Client extends User {
    private final Role role;

    public Client(Long id, String name, String surname, String email, String passwordHash) {
        this(id, name, surname, email, passwordHash, Role.CLIENT);
    }

    public Client(Long id, String name, String surname, String email, String passwordHash, Role role) {
        super(id, name, surname, email, passwordHash);
        this.role = (role == Role.CUSTOMER) ? Role.CUSTOMER : Role.CLIENT;
    }

    public static Client create(String name, String surname, String email, String passwordHash) {
        return new Client(null, name, surname, email, passwordHash, Role.CLIENT);
    }

    public static Client create(String name, String surname, String email, String passwordHash, Role role) {
        return new Client(null, name, surname, email, passwordHash, role);
    }

    @Override
    public Role role() {
        return role;
    }
}
