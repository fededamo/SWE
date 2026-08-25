package it.unifi.ing.drivehub.business.exceptions;

public final class EntityNotFoundException extends RuntimeException {
    public EntityNotFoundException(String entity, long id) {
        super(entity + " " + id + " was not found");
    }
}
