package it.unifi.ing.drivehub.presentation.core;

import it.unifi.ing.drivehub.domain.users.Role;
import it.unifi.ing.drivehub.domain.vehicles.VehiclePurpose;
import it.unifi.ing.drivehub.domain.vehicles.VehicleStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/** Immutable projections consumed by JavaFX controllers. */
public final class UiModels {
    private UiModels() {
    }

    public record Session(long userId, String displayName, String email, Role role) {
        public Session {
            Objects.requireNonNull(displayName);
            Objects.requireNonNull(email);
            Objects.requireNonNull(role);
        }
    }

    public record VehicleItem(long id, String plate, String displayName, VehiclePurpose purpose,
                              long mileage, BigDecimal price, VehicleStatus status,
                              BigDecimal discountPercentage) {
    }

    public record TestDriveItem(long id, String code, String customer, String vehicle,
                                LocalDateTime scheduledAt, String status) {
    }

    public record RentalItem(long id, String code, String customer, String vehicle,
                             LocalDate startDate, LocalDate endDate, BigDecimal amount,
                             String status, Long salesmanId) {
    }

    public record ProposalItem(long id, String code, String customer, String salesman,
                               String vehicle, BigDecimal requestedAmount, BigDecimal offeredAmount,
                               String status) {
    }

    public record StockOrderItem(long id, String code, String model, int quantity,
                                 BigDecimal unitCost, String status) {
    }

    public record Dashboard(long totalVehicles, long activeRentals, BigDecimal recordedRevenue,
                            long pendingProposals) {
    }

    public record ActivityItem(String type, String description, BigDecimal amount,
                               LocalDateTime occurredAt) {
    }

    public record VehicleSaleRequest(String plate, String brand, String model, int year,
                                     long mileage, BigDecimal requestedAmount) {
    }

    public record StockOrderRequest(String brand, String model, int year, int quantity,
                                    BigDecimal unitCost) {
    }
}
