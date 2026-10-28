package it.unifi.ing.drivehub.business.exceptions;

public final class ConflictException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public ConflictException(String message) {
        super(message);
    }
}
