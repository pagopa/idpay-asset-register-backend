package it.gov.pagopa.register.dto.operation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductDTO {
    private String codiceGTIN;
    private String categoria;
    private String marca;
    private String modello;
    private String codiceProdotto;

    // Campi opzionali per EPREL
    private String codiceEprel;
    private String classeEnergetica;
    private String linkEprel;

}
