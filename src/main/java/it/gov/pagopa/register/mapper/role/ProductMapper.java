package it.gov.pagopa.register.dto.mapper.role;

import it.gov.pagopa.register.dto.ProductDTO;
import it.gov.pagopa.register.model.role.Product;

import static it.gov.pagopa.register.utils.Utils.generateEprelUrl;


public class ProductMapper {

  public static ProductDTO toDTO(Product entity){
    ProductDTO dto = new ProductDTO();

    if(entity!=null){
      String linkEprel = generateEprelUrl(entity.getProductGroup(), entity.getEprelCode());
      dto.builder()
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
        .linkEprel(linkEprel)
        .build();
    }


    return dto;
  }

  public static Product toEntity(ProductDTO dto){
    Product entity = new Product();

    if(dto!=null){
      entity.builder()
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
        .build();
    }


    return entity;
  }


}
