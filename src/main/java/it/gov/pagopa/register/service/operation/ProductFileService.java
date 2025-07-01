package it.gov.pagopa.register.service.operation;
import it.gov.pagopa.register.mapper.operation.ProductFileMapper;
import it.gov.pagopa.register.dto.operation.ProductFileDTO;
import it.gov.pagopa.register.dto.operation.ProductFileResponseDTO;
import it.gov.pagopa.register.model.operation.ProductFile;
import it.gov.pagopa.register.repository.operation.ProductFileRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;



@Service
@Slf4j
public class ProductFileService {
  private final ProductFileRepository productFileRepository;

  public ProductFileService(ProductFileRepository productFileRepository) {
    this.productFileRepository = productFileRepository;
  }

  public ProductFileResponseDTO getFilesByPage(String organizationId, Pageable pageable) {

    if (organizationId == null) {
      throw new IllegalArgumentException("OrganizationId must not be null");
    }

    Page<ProductFile> filesPage = productFileRepository.findByOrganizationIdAndUploadStatusNot(
      organizationId, "FORMAL_ERROR", pageable);

    Page<ProductFileDTO> filesPageDTO = filesPage.map(ProductFileMapper::toDTO);

    return ProductFileResponseDTO.builder()
      .content(filesPageDTO.getContent())
      .pageNo(filesPageDTO.getNumber())
      .pageSize(filesPageDTO.getSize())
      .totalElements(filesPageDTO.getTotalElements())
      .totalPages(filesPageDTO.getTotalPages())
      .build();
  }

}
