package it.unifi.ing.drivehub.domain.users;

public final class Manager extends User {

    public Manager(Long id, String name, String surname, String email, String passwordHash) {
        super(id, name, surname, email, passwordHash);
    }

    public static Manager create(String name, String surname, String email, String passwordHash) {
        return new Manager(null, name, surname, email, passwordHash);
    }

    @Override
    public Role role() {
        return Role.MANAGER;
    }
}
