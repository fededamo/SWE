package it.unifi.ing.drivehub.business.services;

import it.unifi.ing.drivehub.domain.users.Role;

public record RegistrationRequest(
        String fiscalCode,
        String firstName,
        String lastName,
        String email,
        String phone,
        Role role,
        Long managerId
) { }
