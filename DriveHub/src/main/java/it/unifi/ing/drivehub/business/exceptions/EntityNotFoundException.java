package it.unifi.ing.drivehub.business.exceptions;

public final class EntityNotFoundException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public EntityNotFoundException(String entity, long id) {
        super(entity + " " + id + " was not found");
    }

    public EntityNotFoundException(String entity, Object key) {
        super(entity + " " + key + " was not found");
    }
}
