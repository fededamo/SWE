package it.unifi.ing.drivehub.domain.users;

public final class Salesman extends User {

    public Salesman(Long id, String name, String surname, String email, String passwordHash) {
        super(id, name, surname, email, passwordHash);
    }

    public static Salesman create(String name, String surname, String email, String passwordHash) {
        return new Salesman(null, name, surname, email, passwordHash);
    }

    @Override
    public Role role() {
        return Role.SALESMAN;
    }
}
