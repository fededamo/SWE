package it.unifi.ing.drivehub.domain.sales;

/**
 * Tipologia di operazione registrata nel registro compravendite (Sale):
 * - CLIENT_SALE: Vendita di un'auto a un cliente.
 * - DEALERSHIP_ACQUISITION: Ritiro/acquisto di un veicolo usato da un cliente.
 * - CANCELLATION_PENALTY: Trattenuta della caparra per recesso/inadempimento del cliente.
 */
public enum SaleType {
    CLIENT_SALE,
    DEALERSHIP_ACQUISITION,
    CANCELLATION_PENALTY
}
