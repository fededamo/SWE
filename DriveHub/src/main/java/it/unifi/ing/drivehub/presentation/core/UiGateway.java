package it.unifi.ing.drivehub.presentation.core;

import it.unifi.ing.drivehub.domain.users.Role;
import it.unifi.ing.drivehub.domain.vehicles.VehiclePurpose;
import it.unifi.ing.drivehub.domain.vehicles.VehicleStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Narrow application boundary used by the JavaFX layer. Implementations delegate
 * to business services; controllers never access JDBC or DAOs directly.
 */
public interface UiGateway extends AutoCloseable {
    UiModels.Session login(String email, char[] password);

    UiModels.Session register(String fiscalCode, String firstName, String lastName,
                              String email, String phone, char[] password, Role role);

    List<UiModels.VehicleItem> searchCatalog(String query, VehiclePurpose purpose, BigDecimal maxPrice);

    List<UiModels.VehicleItem> inventory(long actorId);

    List<UiModels.TestDriveItem> customerTestDrives(long customerId);

    List<UiModels.TestDriveItem> manageableTestDrives(long salesmanId);

    void bookTestDrive(long customerId, long vehicleId, LocalDateTime scheduledAt);

    void confirmTestDrive(long salesmanId, long bookingId);

    void startTestDrive(long salesmanId, long bookingId);

    void completeTestDrive(long salesmanId, long bookingId);

    void cancelTestDrive(long actorId, Role actorRole, long bookingId);

    BigDecimal quoteRental(long vehicleId, LocalDate startDate, LocalDate endDate);

    void rentAndPay(long customerId, long vehicleId, LocalDate startDate, LocalDate endDate,
                    String paymentMethod, BigDecimal expectedAmount);

    List<UiModels.RentalItem> customerRentals(long customerId);

    List<UiModels.RentalItem> manageableRentals(long salesmanId);

    void claimRental(long salesmanId, long rentalId);

    void confirmRental(long salesmanId, long rentalId);

    void startRental(long salesmanId, long rentalId);

    void completeRental(long salesmanId, long rentalId);

    void cancelRental(long actorId, Role actorRole, long rentalId);

    BigDecimal quotePurchase(long vehicleId, boolean fullPurchase);

    void reserveOrPurchase(long customerId, long vehicleId, boolean fullPurchase,
                           String paymentMethod, BigDecimal expectedAmount);

    void submitVehicleSale(long customerId, UiModels.VehicleSaleRequest request);

    List<UiModels.ProposalItem> proposalsForSalesman(long salesmanId);

    void submitPurchaseProposal(long salesmanId, long proposalId, BigDecimal offeredAmount);

    List<UiModels.ProposalItem> proposalsAwaitingManager(long managerId);

    void decidePurchaseProposal(long managerId, long proposalId, boolean approve);

    void updateVehicleStatus(long salesmanId, long vehicleId, VehicleStatus status);

    void updateSalePrice(long managerId, long vehicleId, BigDecimal price);

    void applyDiscount(long managerId, long vehicleId, BigDecimal percentage);

    void removeDiscount(long managerId, long vehicleId);

    void createStockOrder(long managerId, UiModels.StockOrderRequest request);

    List<UiModels.StockOrderItem> stockOrders(long managerId);

    UiModels.Dashboard dashboard(long managerId);

    List<UiModels.ActivityItem> recentActivity(long managerId);

    @Override
    default void close() {
    }
}
