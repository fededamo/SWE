package it.unifi.ing.drivehub.presentation.core;

import it.unifi.ing.drivehub.business.exceptions.ConflictException;
import it.unifi.ing.drivehub.business.services.AuthService;
import it.unifi.ing.drivehub.business.services.CatalogService;
import it.unifi.ing.drivehub.business.services.DashboardActivity;
import it.unifi.ing.drivehub.business.services.DashboardService;
import it.unifi.ing.drivehub.business.services.DashboardSnapshot;
import it.unifi.ing.drivehub.business.services.InventoryService;
import it.unifi.ing.drivehub.business.services.PaymentService;
import it.unifi.ing.drivehub.business.services.PricingService;
import it.unifi.ing.drivehub.business.services.PurchaseProposalService;
import it.unifi.ing.drivehub.business.services.RegistrationRequest;
import it.unifi.ing.drivehub.business.services.RentalService;
import it.unifi.ing.drivehub.business.services.SalesService;
import it.unifi.ing.drivehub.business.services.TestDriveService;
import it.unifi.ing.drivehub.domain.rentals.Rental;
import it.unifi.ing.drivehub.domain.rentals.TestDrive;
import it.unifi.ing.drivehub.domain.sales.Discount;
import it.unifi.ing.drivehub.domain.sales.Payment;
import it.unifi.ing.drivehub.domain.sales.PaymentMethod;
import it.unifi.ing.drivehub.domain.sales.PaymentPurpose;
import it.unifi.ing.drivehub.domain.sales.PaymentStatus;
import it.unifi.ing.drivehub.domain.sales.PurchaseProposal;
import it.unifi.ing.drivehub.domain.sales.SaleOrder;
import it.unifi.ing.drivehub.domain.users.Role;
import it.unifi.ing.drivehub.domain.users.User;
import it.unifi.ing.drivehub.domain.vehicles.StockOrder;
import it.unifi.ing.drivehub.domain.vehicles.Vehicle;
import it.unifi.ing.drivehub.domain.vehicles.VehicleModel;
import it.unifi.ing.drivehub.domain.vehicles.VehiclePurpose;
import it.unifi.ing.drivehub.domain.vehicles.VehicleStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Application adapter that keeps JavaFX independent from persistence details.
 * It also performs the small projection and orchestration steps required by a
 * desktop screen (for example: create a rental and immediately pay it).
 */
public final class ServiceUiGateway implements UiGateway {
    private static final BigDecimal DEPOSIT_RATE = new BigDecimal("0.10");
    private static final Duration DEFAULT_TEST_DRIVE_DURATION = Duration.ofMinutes(45);

    private final AuthService auth;
    private final CatalogService catalog;
    private final RentalService rentals;
    private final TestDriveService testDrives;
    private final SalesService sales;
    private final PurchaseProposalService proposals;
    private final PricingService pricing;
    private final InventoryService inventory;
    private final PaymentService payments;
    private final DashboardService dashboards;

    public ServiceUiGateway(AuthService auth, CatalogService catalog, RentalService rentals,
                            TestDriveService testDrives, SalesService sales,
                            PurchaseProposalService proposals, PricingService pricing,
                            InventoryService inventory, PaymentService payments,
                            DashboardService dashboards) {
        this.auth = Objects.requireNonNull(auth);
        this.catalog = Objects.requireNonNull(catalog);
        this.rentals = Objects.requireNonNull(rentals);
        this.testDrives = Objects.requireNonNull(testDrives);
        this.sales = Objects.requireNonNull(sales);
        this.proposals = Objects.requireNonNull(proposals);
        this.pricing = Objects.requireNonNull(pricing);
        this.inventory = Objects.requireNonNull(inventory);
        this.payments = Objects.requireNonNull(payments);
        this.dashboards = Objects.requireNonNull(dashboards);
    }

    @Override
    public UiModels.Session login(String email, char[] password) {
        return session(auth.login(email, password));
    }

    @Override
    public UiModels.Session register(String fiscalCode, String firstName, String lastName,
                                     String email, String phone, char[] password, Role role) {
        RegistrationRequest request = new RegistrationRequest(
                fiscalCode, firstName, lastName, email, phone, role, null);
        return session(auth.register(request, password));
    }

    @Override
    public List<UiModels.VehicleItem> searchCatalog(String query, VehiclePurpose purpose,
                                                    BigDecimal maxPrice) {
        if (maxPrice != null && maxPrice.signum() < 0) {
            throw new IllegalArgumentException("Il prezzo massimo non può essere negativo");
        }
        Stream<Vehicle> source;
        if (purpose == VehiclePurpose.FOR_SALE) {
            source = catalog.availableForSale().stream();
        } else if (purpose == VehiclePurpose.RENTAL) {
            source = catalog.availableForRental().stream();
        } else if (purpose == null) {
            LinkedHashMap<Long, Vehicle> unique = new LinkedHashMap<>();
            Stream.concat(catalog.availableForSale().stream(), catalog.availableForRental().stream())
                    .forEach(vehicle -> unique.put(vehicle.requireId(), vehicle));
            source = unique.values().stream();
        } else {
            return List.of();
        }

        String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        LocalDate today = LocalDate.now();
        List<Discount> discounts = pricing.discounts();
        return source
                .filter(vehicle -> matches(vehicle, needle))
                .map(vehicle -> vehicleItem(vehicle, today, discounts, true))
                .filter(item -> maxPrice == null || item.price().compareTo(maxPrice) <= 0)
                .sorted(Comparator.comparing(UiModels.VehicleItem::displayName)
                        .thenComparing(UiModels.VehicleItem::plate))
                .toList();
    }

    @Override
    public List<UiModels.VehicleItem> inventory() {
        LocalDate today = LocalDate.now();
        List<Discount> discounts = pricing.discounts();
        return inventory.inventory().stream()
                .map(vehicle -> vehicleItem(vehicle, today, discounts, false))
                .sorted(Comparator.comparing(UiModels.VehicleItem::plate))
                .toList();
    }

    @Override
    public List<UiModels.TestDriveItem> customerTestDrives(long customerId) {
        return testDrives.bookingsForCustomer(customerId).stream().map(ServiceUiGateway::testDriveItem).toList();
    }

    @Override
    public List<UiModels.TestDriveItem> manageableTestDrives(long salesmanId) {
        return testDrives.manageableBookings(salesmanId).stream()
                .map(ServiceUiGateway::testDriveItem)
                .toList();
    }

    @Override
    public void bookTestDrive(long customerId, long vehicleId, LocalDateTime scheduledAt) {
        testDrives.book(customerId, vehicleId, scheduledAt, DEFAULT_TEST_DRIVE_DURATION);
    }

    @Override
    public void confirmTestDrive(long salesmanId, long bookingId) {
        testDrives.confirm(bookingId, salesmanId);
    }

    @Override
    public void startTestDrive(long salesmanId, long bookingId) {
        testDrives.start(bookingId, salesmanId);
    }

    @Override
    public void completeTestDrive(long salesmanId, long bookingId) {
        testDrives.complete(bookingId, salesmanId);
    }

    @Override
    public void cancelTestDrive(long actorId, Role actorRole, long bookingId) {
        Objects.requireNonNull(actorRole, "actorRole");
        testDrives.cancel(bookingId, actorId);
    }

    @Override
    public BigDecimal quoteRental(long vehicleId, LocalDate startDate, LocalDate endDate) {
        return pricing.quoteRental(vehicleId, rentalDays(startDate, endDate), startDate);
    }

    @Override
    public void rentAndPay(long customerId, long vehicleId, LocalDate startDate, LocalDate endDate,
                           String paymentMethod) {
        Rental rental = rentals.requestRental(customerId, vehicleId, startDate, endDate);
        Payment payment = payments.payRental(customerId, rental.requireId(), paymentMethod(paymentMethod));
        if (payment.status() != PaymentStatus.COMPLETED) {
            rentals.cancelRental(rental.requireId(), customerId);
            throw new ConflictException("Pagamento rifiutato: " + payment.failureReason());
        }
    }

    @Override
    public List<UiModels.RentalItem> customerRentals(long customerId) {
        return rentals.rentalsForCustomer(customerId).stream().map(ServiceUiGateway::rentalItem).toList();
    }

    @Override
    public List<UiModels.RentalItem> manageableRentals(long salesmanId) {
        LinkedHashMap<Long, Rental> unique = new LinkedHashMap<>();
        Stream.concat(rentals.unassignedRentals().stream(), rentals.rentalsForSalesman(salesmanId).stream())
                .forEach(rental -> unique.put(rental.requireId(), rental));
        return unique.values().stream().map(ServiceUiGateway::rentalItem).toList();
    }

    @Override
    public void claimRental(long salesmanId, long rentalId) {
        rentals.claimRental(rentalId, salesmanId);
    }

    @Override
    public void confirmRental(long salesmanId, long rentalId) {
        rentals.confirmRental(rentalId, salesmanId);
    }

    @Override
    public void startRental(long salesmanId, long rentalId) {
        rentals.startRental(rentalId, salesmanId);
    }

    @Override
    public void completeRental(long salesmanId, long rentalId) {
        rentals.completeRental(rentalId, salesmanId);
    }

    @Override
    public void cancelRental(long actorId, Role actorRole, long rentalId) {
        Objects.requireNonNull(actorRole, "actorRole");
        rentals.cancelRental(rentalId, actorId);
    }

    @Override
    public void reserveOrPurchase(long customerId, long vehicleId, boolean fullPurchase,
                                  String paymentMethod) {
        LocalDate today = LocalDate.now();
        BigDecimal total = pricing.quoteSale(vehicleId, today);
        BigDecimal deposit = total.multiply(DEPOSIT_RATE).setScale(2, RoundingMode.HALF_UP);
        SaleOrder order = sales.reserveVehicle(customerId, vehicleId, deposit, today);
        BigDecimal amount = fullPurchase ? total : deposit;
        Payment payment = payments.paySaleOrder(customerId, order.requireId(),
                PaymentPurpose.SALE_DEPOSIT, paymentMethod(paymentMethod), amount);
        if (payment.status() != PaymentStatus.COMPLETED) {
            sales.cancelOrder(order.requireId(), customerId);
            throw new ConflictException("Pagamento rifiutato: " + payment.failureReason());
        }
    }

    @Override
    public void submitVehicleSale(long customerId, UiModels.VehicleSaleRequest request) {
        Objects.requireNonNull(request, "request");
        proposals.request(customerId, request.brand(), request.model(), request.year(), request.plate(),
                request.mileage(), request.requestedAmount(), "Richiesta inserita dal cliente");
    }

    @Override
    public List<UiModels.ProposalItem> proposalsForSalesman(long salesmanId) {
        return proposals.proposalsForSalesman(salesmanId).stream()
                .map(ServiceUiGateway::proposalItem)
                .toList();
    }

    @Override
    public void submitPurchaseProposal(long salesmanId, long proposalId, BigDecimal offeredAmount) {
        proposals.submitOffer(proposalId, salesmanId, offeredAmount,
                "Offerta economica soggetta ad approvazione del manager");
    }

    @Override
    public List<UiModels.ProposalItem> proposalsAwaitingManager() {
        return proposals.awaitingManagerDecision().stream().map(ServiceUiGateway::proposalItem).toList();
    }

    @Override
    public void decidePurchaseProposal(long managerId, long proposalId, boolean approve) {
        if (approve) {
            proposals.approve(proposalId, managerId, "Approvata dal manager");
        } else {
            proposals.reject(proposalId, managerId, "Rifiutata dal manager");
        }
    }

    @Override
    public void updateVehicleStatus(long salesmanId, long vehicleId, VehicleStatus status) {
        if (status == VehicleStatus.MAINTENANCE) {
            inventory.sendToMaintenance(salesmanId, vehicleId);
        } else if (status == VehicleStatus.AVAILABLE) {
            inventory.returnFromMaintenance(salesmanId, vehicleId);
        } else {
            throw new IllegalArgumentException("Lo stato è gestito dal relativo flusso operativo");
        }
    }

    @Override
    public void updateSalePrice(long managerId, long vehicleId, BigDecimal price) {
        Vehicle vehicle = catalog.vehicle(vehicleId);
        if (vehicle.purpose().supportsSale()) {
            pricing.updateSalePrice(managerId, vehicleId, price);
        } else if (vehicle.purpose().supportsRental()) {
            pricing.updateDailyRentalRate(managerId, vehicleId, price);
        } else {
            throw new IllegalArgumentException("Il veicolo non ha un prezzo di listino modificabile");
        }
    }

    @Override
    public void applyDiscount(long managerId, long vehicleId, BigDecimal percentage) {
        LocalDate today = LocalDate.now();
        pricing.applyDiscount(managerId, "Promozione veicolo " + vehicleId, vehicleId,
                percentage, today, today.plusMonths(1));
    }

    @Override
    public void removeDiscount(long managerId, long vehicleId) {
        List<Discount> matching = pricing.discounts().stream()
                .filter(Discount::enabled)
                .filter(discount -> discount.vehicle().requireId() == vehicleId)
                .toList();
        if (matching.isEmpty()) {
            throw new IllegalArgumentException("Nessuno sconto attivo per il veicolo selezionato");
        }
        matching.forEach(discount -> pricing.removeDiscount(managerId, discount.requireId()));
    }

    @Override
    public void createStockOrder(long managerId, UiModels.StockOrderRequest request) {
        Objects.requireNonNull(request, "request");
        VehicleModel model = catalog.models().stream()
                .filter(candidate -> candidate.brand().name().equalsIgnoreCase(request.brand().trim()))
                .filter(candidate -> candidate.name().equalsIgnoreCase(request.model().trim()))
                .filter(candidate -> candidate.modelYear() == request.year())
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Modello non presente in catalogo: inserirlo prima dall'inventario"));
        inventory.placeStockOrder(managerId, model.requireId(), request.quantity(), request.unitCost());
    }

    @Override
    public List<UiModels.StockOrderItem> stockOrders() {
        return inventory.stockOrders().stream().map(ServiceUiGateway::stockOrderItem).toList();
    }

    @Override
    public UiModels.Dashboard dashboard(long managerId) {
        DashboardSnapshot value = dashboards.snapshot(managerId);
        return new UiModels.Dashboard(value.totalVehicles(), value.openRentals(),
                value.recordedRevenue(), value.awaitingProposalDecisions());
    }

    @Override
    public List<UiModels.ActivityItem> recentActivity(long managerId) {
        return dashboards.snapshot(managerId).recentActivity().stream()
                .map(ServiceUiGateway::activityItem)
                .toList();
    }

    private UiModels.VehicleItem vehicleItem(Vehicle vehicle, LocalDate date,
                                             List<Discount> discounts, boolean discountedPrice) {
        BigDecimal price;
        if (vehicle.purpose().supportsSale()) {
            price = discountedPrice ? pricing.quoteSale(vehicle.requireId(), date) : vehicle.salePrice();
        } else if (vehicle.purpose().supportsRental()) {
            price = discountedPrice ? pricing.quoteRental(vehicle.requireId(), 1, date)
                    : vehicle.dailyRentalRate();
        } else {
            price = null;
        }
        BigDecimal percentage = discounts.stream()
                .filter(discount -> discount.appliesTo(vehicle, date))
                .map(Discount::percentage)
                .max(Comparator.naturalOrder())
                .orElse(null);
        return new UiModels.VehicleItem(vehicle.requireId(), vehicle.plate(), vehicleName(vehicle),
                vehicle.purpose(), vehicle.mileage(), price, vehicle.status(), percentage);
    }

    private static boolean matches(Vehicle vehicle, String needle) {
        if (needle.isEmpty()) {
            return true;
        }
        String searchable = (vehicle.plate() + " " + vehicle.model().brand().name() + " "
                + vehicle.model().name() + " " + vehicle.model().code()).toLowerCase(Locale.ROOT);
        return searchable.contains(needle);
    }

    private static UiModels.Session session(User user) {
        return new UiModels.Session(user.requireId(), user.displayName(), user.email(), user.role());
    }

    private static UiModels.TestDriveItem testDriveItem(TestDrive booking) {
        return new UiModels.TestDriveItem(booking.requireId(), code("TD", booking.requireId()),
                booking.customer().displayName(), vehicleName(booking.vehicle()),
                booking.scheduledAt(), booking.status().name());
    }

    private static UiModels.RentalItem rentalItem(Rental rental) {
        Long salesmanId = rental.salesman() == null ? null : rental.salesman().id();
        return new UiModels.RentalItem(rental.requireId(), code("NL", rental.requireId()),
                rental.customer().displayName(), vehicleName(rental.vehicle()), rental.startsOn(),
                rental.endsOn(), rental.totalPrice(), rental.status().name(), salesmanId);
    }

    private static UiModels.ProposalItem proposalItem(PurchaseProposal proposal) {
        String salesman = proposal.salesman() == null ? "—" : proposal.salesman().displayName();
        return new UiModels.ProposalItem(proposal.requireId(), code("PR", proposal.requireId()),
                proposal.customer().displayName(), salesman, vehicleName(proposal.vehicle()),
                proposal.requestedAmount(), proposal.offeredAmount(), proposal.status().name());
    }

    private static UiModels.StockOrderItem stockOrderItem(StockOrder order) {
        return new UiModels.StockOrderItem(order.requireId(), code("OR", order.requireId()),
                modelName(order.model()), order.quantity(), order.unitCost(), order.status().name());
    }

    private static UiModels.ActivityItem activityItem(DashboardActivity activity) {
        String type = activity.description().startsWith("Payment") ? "PAGAMENTO" : "PROPOSTA";
        LocalDateTime date = LocalDateTime.ofInstant(activity.occurredAt(), ZoneId.systemDefault());
        return new UiModels.ActivityItem(type, activity.description(), null, date);
    }

    private static String vehicleName(Vehicle vehicle) {
        return modelName(vehicle.model()) + " · " + vehicle.plate();
    }

    private static String modelName(VehicleModel model) {
        return model.brand().name() + " " + model.name() + " (" + model.modelYear() + ")";
    }

    private static long rentalDays(LocalDate startDate, LocalDate endDate) {
        Objects.requireNonNull(startDate, "Data iniziale obbligatoria");
        Objects.requireNonNull(endDate, "Data finale obbligatoria");
        long days = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
        if (days <= 0) {
            throw new IllegalArgumentException("La data finale deve seguire quella iniziale");
        }
        return days;
    }

    private static PaymentMethod paymentMethod(String label) {
        if (label == null) {
            throw new IllegalArgumentException("Metodo di pagamento obbligatorio");
        }
        String normalized = label.toLowerCase(Locale.ROOT);
        if (normalized.contains("carta")) {
            return PaymentMethod.CARD;
        }
        if (normalized.contains("bonifico")) {
            return PaymentMethod.BANK_TRANSFER;
        }
        if (normalized.contains("contanti")) {
            return PaymentMethod.CASH;
        }
        throw new IllegalArgumentException("Metodo di pagamento non riconosciuto");
    }

    private static String code(String prefix, long id) {
        return "%s-%05d".formatted(prefix, id);
    }
}
