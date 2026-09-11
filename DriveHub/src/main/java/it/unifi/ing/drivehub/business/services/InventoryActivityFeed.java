package it.unifi.ing.drivehub.business.services;

import it.unifi.ing.drivehub.domain.observer.InventoryEvent;
import it.unifi.ing.drivehub.domain.observer.InventoryObserver;
import java.util.ArrayDeque;
import java.util.List;

/** Last ten committed manual inventory changes in this application process; not a durable audit. */
public final class InventoryActivityFeed implements InventoryObserver {
    private final ArrayDeque<DashboardActivity> recent = new ArrayDeque<>();

    @Override
    public synchronized void onInventoryEvent(InventoryEvent event) {
        recent.addFirst(new DashboardActivity(event.occurredAt(), "Inventario " + event.plate()
                + ": " + event.previousStatus() + " → " + event.currentStatus()));
        while (recent.size() > 10) recent.removeLast();
    }

    public synchronized List<DashboardActivity> recentActivity() { return List.copyOf(recent); }
}
