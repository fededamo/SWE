package it.unifi.ing.drivehub.business.core;

public record PaymentGatewayResult(boolean approved, String processorReference, String rejectionReason) {
    public PaymentGatewayResult {
        if (approved && (processorReference == null || processorReference.isBlank())) {
            throw new IllegalArgumentException("approved payment requires a processor reference");
        }
        if (!approved && (rejectionReason == null || rejectionReason.isBlank())) {
            throw new IllegalArgumentException("rejected payment requires a reason");
        }
    }

    public static PaymentGatewayResult approved(String reference) {
        return new PaymentGatewayResult(true, reference, null);
    }

    public static PaymentGatewayResult rejected(String reason) {
        return new PaymentGatewayResult(false, null, reason);
    }
}
