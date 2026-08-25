package it.unifi.ing.drivehub.dao.postgres;

import it.unifi.ing.drivehub.dao.interfaces.UserDao;
import it.unifi.ing.drivehub.domain.users.Role;
import it.unifi.ing.drivehub.domain.users.User;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;

final class PostgresUserDao extends AbstractPostgresDao implements UserDao {

    PostgresUserDao(Connection connection, JdbcEntityLoader loader) {
        super(connection, loader);
    }

    @Override
    public User save(User user) {
        if (user.id() != null) {
            throw new IllegalArgumentException("user is already persistent");
        }
        long id = execute("Could not save user", () -> JdbcSupport.insert(connection, """
                INSERT INTO users(
                    fiscal_code, first_name, last_name, email, phone, password_hash, role, manager_id, active
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, statement -> {
            statement.setString(1, user.fiscalCode());
            statement.setString(2, user.firstName());
            statement.setString(3, user.lastName());
            statement.setString(4, user.email());
            statement.setString(5, user.phone());
            statement.setString(6, user.passwordHash());
            statement.setString(7, user.role().name());
            JdbcSupport.setNullableLong(statement, 8, user.managerId());
            statement.setBoolean(9, user.active());
        }));
        user.assignId(id);
        return user;
    }

    @Override
    public void update(User user) {
        execute("Could not update user", () -> JdbcSupport.updateExactlyOne(connection, """
                UPDATE users
                SET fiscal_code = ?, first_name = ?, last_name = ?, email = ?, phone = ?,
                    password_hash = ?, role = ?, manager_id = ?, active = ?
                WHERE id = ?
                """, statement -> {
            statement.setString(1, user.fiscalCode());
            statement.setString(2, user.firstName());
            statement.setString(3, user.lastName());
            statement.setString(4, user.email());
            statement.setString(5, user.phone());
            statement.setString(6, user.passwordHash());
            statement.setString(7, user.role().name());
            JdbcSupport.setNullableLong(statement, 8, user.managerId());
            statement.setBoolean(9, user.active());
            statement.setLong(10, user.requireId());
        }));
    }

    @Override
    public Optional<User> findById(long id) {
        return loader.user(id);
    }

    @Override
    public Optional<User> findByEmail(String normalizedEmail) {
        return findOne("SELECT id FROM users WHERE email = ?", statement ->
                statement.setString(1, User.normalizeEmail(normalizedEmail)), loader::user,
                "Could not find user by email");
    }

    @Override
    public Optional<User> findByFiscalCode(String normalizedFiscalCode) {
        return findOne("SELECT id FROM users WHERE fiscal_code = ?", statement ->
                statement.setString(1, User.normalizeFiscalCode(normalizedFiscalCode)), loader::user,
                "Could not find user by fiscal code");
    }

    @Override
    public List<User> findByRole(Role role) {
        return findMany("SELECT id FROM users WHERE role = ? ORDER BY id", statement ->
                statement.setString(1, role.name()), loader::user, "Could not list users by role");
    }
}
