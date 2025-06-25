package it.gov.pagopa.register.controller.operation;


import it.gov.pagopa.register.dto.mapper.operation.AssetProductDTO;
import it.gov.pagopa.register.service.operation.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;


@RestController
@RequestMapping("/idpay/register")
public class ProductController {


    private final ProductService productService;



    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping(value = "upload", consumes = "multipart/form-data")
    public ResponseEntity<AssetProductDTO> uploadCsv(@RequestParam(required = false) String idUser,
                                                     @RequestParam(required = false) String idOrg,
                                                     @RequestParam(required = false) String role,
                                                     @RequestPart("category") String category,
                                                     @RequestPart("csv") MultipartFile csv) {

      AssetProductDTO assetProductDTO = productService.saveCsv(csv, category, idOrg, idUser);
        return ResponseEntity.ok().body(assetProductDTO);
    }

  @GetMapping("/download/report/{idUpload}")
  public ResponseEntity<byte[]> downloadCsv(
    @RequestParam(required = false) String idProducer,
    @RequestParam(required = false) String orgName,
    @PathVariable("idUpload") String idUpload) {

    ByteArrayOutputStream file = productService.downloadReport(idUpload);

    byte[] zipBytes = file.toByteArray();

    return ResponseEntity.ok()
      .header("Content-Disposition", "attachment; filename=test.csv")
      .contentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM)
      .body(zipBytes);
  }


}
