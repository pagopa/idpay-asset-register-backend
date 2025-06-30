package it.gov.pagopa.register.dto.mapper.operation;


import it.gov.pagopa.register.dto.operation.ProductDTO;
import it.gov.pagopa.register.model.operation.Product;

public class ProductMapper {

    private ProductMapper(){}

    public static ProductDTO toDTO(Product prodotto){
        return ProductDTO.builder()
                .codiceGTIN(prodotto.getGtinCode())
                .categoria(prodotto.getProductGroup())
                .marca(prodotto.getSupplierOrTrademark())
                .modello(prodotto.getModelIdentifier())
                .codiceProdotto(prodotto.getProductCode())
                .codiceEprel(prodotto.getEprelRegistrationNumber())
                .classeEnergetica(prodotto.getEnergyClass())
                .linkEprel(prodotto.getLinkEprel())
                .build();
    }

    public static Product toEntity(ProductDTO prodottoDTO){
        return Product.builder()
                .gtinCode(prodottoDTO.getCodiceGTIN())
                .productCode(prodottoDTO.getCategoria())
                .supplierOrTrademark(prodottoDTO.getMarca())
                .modelIdentifier(prodottoDTO.getModello())
                .productCode(prodottoDTO.getCodiceProdotto())
                .eprelRegistrationNumber(prodottoDTO.getCodiceEprel())
                .energyClass(prodottoDTO.getClasseEnergetica())
                .linkEprel(prodottoDTO.getLinkEprel())
                .build();
    }

}
