package it.gov.pagopa.register.controller.operation;

import it.gov.pagopa.register.dto.operation.RegisterUploadReqeustDTO;
import it.gov.pagopa.register.service.operation.ProductService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;

@RestController
@RequestMapping("/idpay/register")
public class ProductController {


    private final ProductService productService;


    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping("upload")
    public ResponseEntity<Void> uploadCsv(@RequestParam(required = false) String idProduttore,
                                          @RequestParam(required = false) String orgName,
                                          @RequestParam(required = false) String role,
                                          @RequestBody()RegisterUploadReqeustDTO registerUploadReqeustDTO) {
        // CONTROLLO AUTORIZZAZIONI TRAMITE ORGNAME E OPERATION
        productService.saveCsv(registerUploadReqeustDTO);
        return ResponseEntity.ok().build();
    }

  @GetMapping("/download/report/{idUpload}")
  public ResponseEntity<Resource> downloadCsv(
    @RequestParam(required = false) String idProduttore,
    @RequestParam(required = false) String orgName,
    @PathVariable("idUpload") String idUpload) throws IOException {

    Path file = productService.downloadReport(idUpload); // ora ritorna `Path`
    Resource resource = new UrlResource(file.toUri());

    if (!resource.exists()) {
      throw new FileNotFoundException("Report non trovato per l'id: " + idUpload);
    }

    return ResponseEntity.ok()
      .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFileName().toString() + "\"")
      .contentType(MediaType.APPLICATION_OCTET_STREAM)
      .body(resource);
  }



}
