package it.unifi.ing.drivehub.business.core;

import it.unifi.ing.drivehub.domain.sales.PaymentMethod;

import java.math.BigDecimal;

@FunctionalInterface
public interface PaymentGateway {
    PaymentGatewayResult charge(BigDecimal amount, PaymentMethod method);
}
