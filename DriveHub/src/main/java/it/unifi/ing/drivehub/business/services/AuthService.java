package it.unifi.ing.drivehub.business.services;

import it.unifi.ing.drivehub.business.core.TransactionRunner;
import it.unifi.ing.drivehub.business.exceptions.AuthenticationException;
import it.unifi.ing.drivehub.business.exceptions.ConflictException;
import it.unifi.ing.drivehub.business.exceptions.EntityNotFoundException;
import it.unifi.ing.drivehub.business.security.PasswordHasher;
import it.unifi.ing.drivehub.dao.interfaces.DaoFactory;
import it.unifi.ing.drivehub.domain.users.Role;
import it.unifi.ing.drivehub.domain.users.User;

import java.util.Objects;

public final class AuthService {
    private final TransactionRunner transactions;
    private final PasswordHasher passwordHasher;

    public AuthService(DaoFactory daoFactory, PasswordHasher passwordHasher) {
        this.transactions = new TransactionRunner(daoFactory);
        this.passwordHasher = Objects.requireNonNull(passwordHasher, "passwordHasher");
    }

    /** All UML roles can register; a salesman may optionally be linked to an existing manager. */
    public User register(RegistrationRequest request, char[] rawPassword) {
        Objects.requireNonNull(request, "request");
        String normalizedEmail = User.normalizeEmail(request.email());
        String normalizedFiscalCode = User.normalizeFiscalCode(request.fiscalCode());
        return transactions.execute(unit -> {
            if (unit.users().findByEmail(normalizedEmail).isPresent()) {
                throw new ConflictException("email is already registered");
            }
            if (unit.users().findByFiscalCode(normalizedFiscalCode).isPresent()) {
                throw new ConflictException("fiscal code is already registered");
            }
            if (request.managerId() != null) {
                if (request.role() != Role.SALESMAN) {
                    throw new IllegalArgumentException("only a salesman may specify a manager");
                }
                User manager = unit.users().findById(request.managerId())
                        .orElseThrow(() -> new EntityNotFoundException("manager", request.managerId()));
                manager.requireRole(Role.MANAGER);
            }
            String encodedPassword = passwordHasher.hash(rawPassword);
            User user = User.register(normalizedFiscalCode, request.firstName(), request.lastName(),
                    normalizedEmail, request.phone(), encodedPassword, request.role(), request.managerId());
            return unit.users().save(user);
        });
    }

    public User login(String email, char[] rawPassword) {
        String normalizedEmail = User.normalizeEmail(email);
        return transactions.execute(unit -> {
            User user = unit.users().findByEmail(normalizedEmail)
                    .orElseThrow(() -> new AuthenticationException("invalid email or password"));
            if (!user.active() || !passwordHasher.verify(rawPassword, user.passwordHash())) {
                throw new AuthenticationException("invalid email or password");
            }
            return user;
        });
    }
}
