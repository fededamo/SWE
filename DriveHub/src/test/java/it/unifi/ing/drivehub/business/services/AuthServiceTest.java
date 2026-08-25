package it.unifi.ing.drivehub.business.services;

import it.unifi.ing.drivehub.business.exceptions.ConflictException;
import it.unifi.ing.drivehub.business.security.PasswordHasher;
import it.unifi.ing.drivehub.dao.interfaces.DaoFactory;
import it.unifi.ing.drivehub.dao.interfaces.UnitOfWork;
import it.unifi.ing.drivehub.dao.interfaces.UserDao;
import it.unifi.ing.drivehub.domain.users.Role;
import it.unifi.ing.drivehub.domain.users.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthServiceTest {
    private UnitOfWork unit;
    private UserDao users;
    private PasswordHasher hasher;
    private AuthService service;

    @BeforeEach
    void setUp() {
        DaoFactory factory = mock(DaoFactory.class);
        unit = mock(UnitOfWork.class);
        users = mock(UserDao.class);
        hasher = mock(PasswordHasher.class);
        when(factory.begin()).thenReturn(unit);
        when(unit.users()).thenReturn(users);
        when(users.findByEmail(any())).thenReturn(Optional.empty());
        when(users.findByFiscalCode(any())).thenReturn(Optional.empty());
        when(hasher.hash(any())).thenReturn("pbkdf2-encoded");
        when(users.save(any())).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.assignId(99L);
            return user;
        });
        service = new AuthService(factory, hasher);
    }

    @ParameterizedTest(name = "registers {0}")
    @EnumSource(Role.class)
    @DisplayName("UC-REG-ALL: customer, salesman and manager registration use one invariant path")
    void registersEveryRole(Role role) {
        User user = service.register(new RegistrationRequest("RSSMRA80A01H501U", "Maria", "Rossi",
                "maria@example.it", "+39055123456", role, null), "password-123".toCharArray());

        assertEquals(role, user.role());
        assertEquals(99L, user.id());
        verify(unit).commit();
    }

    @Test
    @DisplayName("UC-REG-DUP: duplicate fiscal code aborts without hashing or saving")
    void rejectsDuplicateFiscalCode() {
        when(users.findByFiscalCode("RSSMRA80A01H501U"))
                .thenReturn(Optional.of(mock(User.class)));

        assertThrows(ConflictException.class, () -> service.register(
                new RegistrationRequest("RSSMRA80A01H501U", "Maria", "Rossi",
                        "maria@example.it", "+39055123456", Role.CUSTOMER, null),
                "password-123".toCharArray()));

        verify(hasher, never()).hash(any());
        verify(users, never()).save(any());
        verify(unit).rollback();
    }
}
