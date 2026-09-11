package it.unifi.ing.drivehub.business.core;

import it.unifi.ing.drivehub.domain.sales.PaymentMethod;

import java.math.BigDecimal;

@FunctionalInterface
public interface PaymentGateway {
    /**
     * Returns a simulated authorization outcome without moving real money.
     * A real processor would require idempotency and recovery beyond the local JDBC transaction.
     */
    PaymentGatewayResult charge(BigDecimal amount, PaymentMethod method);
}
