package it.unifi.ing.drivehub.domain;

/** Raised when an operation would violate an aggregate invariant. */
public final class DomainRuleViolationException extends IllegalStateException {
    private static final long serialVersionUID = 1L;

    public DomainRuleViolationException(String message) {
        super(message);
    }
}
