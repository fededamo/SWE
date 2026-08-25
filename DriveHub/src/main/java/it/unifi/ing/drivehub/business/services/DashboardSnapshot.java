package it.unifi.ing.drivehub.business.services;

import java.math.BigDecimal;
import java.util.List;

public record DashboardSnapshot(
        long totalVehicles,
        long availableForSale,
        long availableForRental,
        long rentedVehicles,
        long reservedVehicles,
        long soldVehicles,
        long maintenanceVehicles,
        long openRentals,
        long awaitingProposalDecisions,
        long completedPayments,
        BigDecimal recordedRevenue,
        List<DashboardActivity> recentActivity
) { }
