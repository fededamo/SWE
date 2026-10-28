package it.unifi.ing.drivehub.domain.sales;

/**
 * Tipologia di richiesta nel flusso compravendite (Request):
 * - CLIENT_PURCHASE: Richiesta di acquisto da parte del cliente su un'auto in vendita.
 * - CLIENT_PROPOSAL: Proposta del cliente di vendere la propria auto usata alla concessionaria.
 */
public enum RequestType {
    CLIENT_PURCHASE,
    CLIENT_PROPOSAL
}
