package it.unifi.ing.drivehub.presentation.core;

import it.unifi.ing.drivehub.business.services.DashboardActivity;
import it.unifi.ing.drivehub.domain.rentals.Rental;
import it.unifi.ing.drivehub.domain.rentals.TestDrive;
import it.unifi.ing.drivehub.domain.sales.PurchaseProposal;
import it.unifi.ing.drivehub.domain.users.User;
import it.unifi.ing.drivehub.domain.vehicles.StockOrder;
import it.unifi.ing.drivehub.domain.vehicles.Vehicle;
import it.unifi.ing.drivehub.domain.vehicles.VehicleModel;

import java.time.LocalDateTime;
import java.time.ZoneId;

/** Pure screen projections; no persistence access, transaction or pricing decisions. */
final class UiModelMapper {
    private UiModelMapper() { }

    static UiModels.Session session(User user) {
        return new UiModels.Session(user.requireId(), user.displayName(), user.email(), user.role());
    }

    static UiModels.TestDriveItem testDriveItem(TestDrive booking) {
        return new UiModels.TestDriveItem(booking.requireId(), code("TD", booking.requireId()),
                booking.customer().displayName(), vehicleName(booking.vehicle()),
                booking.scheduledAt(), booking.status().name());
    }

    static UiModels.RentalItem rentalItem(Rental rental) {
        Long salesmanId = rental.salesman() == null ? null : rental.salesman().id();
        return new UiModels.RentalItem(rental.requireId(), code("NL", rental.requireId()),
                rental.customer().displayName(), vehicleName(rental.vehicle()), rental.startsOn(),
                rental.endsOn(), rental.totalPrice(), rental.status().name(), salesmanId);
    }

    static UiModels.ProposalItem proposalItem(PurchaseProposal proposal) {
        String salesman = proposal.salesman() == null ? "—" : proposal.salesman().displayName();
        return new UiModels.ProposalItem(proposal.requireId(), code("PR", proposal.requireId()),
                proposal.customer().displayName(), salesman, vehicleName(proposal.vehicle()),
                proposal.requestedAmount(), proposal.offeredAmount(), proposal.status().name());
    }

    static UiModels.StockOrderItem stockOrderItem(StockOrder order) {
        return new UiModels.StockOrderItem(order.requireId(), code("OR", order.requireId()),
                modelName(order.model()), order.quantity(), order.unitCost(), order.status().name());
    }

    static UiModels.ActivityItem activityItem(DashboardActivity activity) {
        String type = activity.amount() != null ? "PAGAMENTO"
                : activity.description().startsWith("Inventario") ? "INVENTARIO" : "PROPOSTA";
        LocalDateTime date = LocalDateTime.ofInstant(activity.occurredAt(), ZoneId.systemDefault());
        return new UiModels.ActivityItem(type, activity.description(), activity.amount(), date);
    }

    static String vehicleName(Vehicle vehicle) {
        return modelName(vehicle.model()) + " · " + vehicle.plate();
    }

    static String modelName(VehicleModel model) {
        return model.brand().name() + " " + model.name() + " (" + model.modelYear() + ")";
    }

    static String code(String prefix, long id) {
        return "%s-%05d".formatted(prefix, id);
    }
}
