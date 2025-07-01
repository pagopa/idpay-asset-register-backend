package it.gov.pagopa.register.mapper.operation;

import it.gov.pagopa.register.dto.ProductDTO;
import it.gov.pagopa.register.model.operation.Product;


public class ProductMapper {

  public static ProductDTO toDTO(Product entity){

    if(entity==null){
      return null;
    }


    return ProductDTO.builder()
      .productFileId(entity.getProductFileId())
      .organizationId(entity.getOrganizationId())
      .registrationDate(entity.getRegistrationDate())
      .status(entity.getStatus())
      .model(entity.getModel())
      .productGroup(entity.getProductGroup())
      .category(entity.getCategory())
      .brand(entity.getBrand())
      .eprelCode(entity.getEprelCode())
      .gtinCode(entity.getGtinCode())
      .productCode(entity.getProductCode())
      .countryOfProduction(entity.getCountryOfProduction())
      .energyClass(entity.getEnergyClass())
      .linkEprel(entity.getLinkEprel())
      .build();
  }

  public static Product toEntity(ProductDTO dto){

    if(dto==null){
      return null;
    }

    return Product.builder()
      .productFileId(dto.getProductFileId())
      .organizationId(dto.getOrganizationId())
      .registrationDate(dto.getRegistrationDate())
      .status(dto.getStatus())
      .model(dto.getModel())
      .productGroup(dto.getProductGroup())
      .category(dto.getCategory())
      .brand(dto.getBrand())
      .eprelCode(dto.getEprelCode())
      .gtinCode(dto.getGtinCode())
      .productCode(dto.getProductCode())
      .countryOfProduction(dto.getCountryOfProduction())
      .energyClass(dto.getEnergyClass())
      .linkEprel(dto.getLinkEprel())
      .build();
  }


}
