package it.gov.pagopa.register.service.operation;
import it.gov.pagopa.register.mapper.operation.ProductFileMapper;
import it.gov.pagopa.register.dto.operation.ProductFileDTO;
import it.gov.pagopa.register.dto.operation.ProductFileResponseDTO;
import it.gov.pagopa.register.model.operation.ProductFile;
import it.gov.pagopa.register.repository.operation.ProductFileRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;


@Service
@Slf4j
public class ProductFileService {
  private final ProductFileRepository uploadRepository;

  public ProductFileService(ProductFileRepository uploadRepository) {
    this.uploadRepository = uploadRepository;
  }

  public ProductFileResponseDTO downloadFilesByPage(String organizationId, Pageable pageable) {
    Page<ProductFile> filesPage = uploadRepository.findByIdOrgAndStatusNot(organizationId, "FORMAL_ERROR", pageable);

    Page<ProductFileDTO> filesPageDTO = filesPage.map(ProductFileMapper::toDTO);

    return ProductFileResponseDTO.builder()
      .content(filesPageDTO.getContent())
      .totalElements(filesPageDTO.getTotalElements())
      .build();
  }

}
