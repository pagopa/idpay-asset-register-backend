package it.gov.pagopa.register.model.operation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "product")
public class Product {

    private String eprelRegistrationNumber;
    private String gtinCode;
    private String productCode;
    private String linkEprel;
    private boolean eprelControl;
    private String supplierOrTrademark;
    private String modelIdentifier;
    private String energyClass;
    private String productGroup;
    private String registranNature;

    /*

        ###PIANO COTTURA###

        Codice EAN/GTIN
        Categoria
        Marca
        Modello
        Codice Prodotto

        ###################

        ###EPREL####

        Codice EPREL
        Codice GTIN/EAN
        Categoria
        Codice Prodotto

        Da recuperare tramite API
        Link di redirect su EPREL = https://eprel.ec.europa.eu/screen/product/{productGroup}/{Codice Prodotto}
        Marca = supplierOrTrademark
        Modello = modelIdentifier
        Classe Energetica  = energyClass
        productGroup = categoria per creare l'url
        registranNaturare =
        Esito del controllo di esistenza su EPRAL
        ###################


     */
}
