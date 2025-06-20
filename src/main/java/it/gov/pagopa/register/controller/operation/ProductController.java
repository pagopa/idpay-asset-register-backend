package it.gov.pagopa.register.controller.operation;

import it.gov.pagopa.register.dto.operation.RegisterUploadReqeustDTO;
import it.gov.pagopa.register.service.operation.AuthorizationService;
import it.gov.pagopa.register.service.operation.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@RestController
@RequestMapping("/idpay/register")
public class ProductController {


    private final ProductService productService;

    private final AuthorizationService authorizationService;


    public ProductController(ProductService productService, AuthorizationService authorizationService) {
        this.productService = productService;
        this.authorizationService = authorizationService;
    }

    @PostMapping("upload")
    public ResponseEntity<Void> uploadCsv(@RequestParam(required = false) String idUser,
                                          @RequestParam(required = false) String idOrg,
                                          @RequestParam(required = false) String role,
                                          @RequestBody()RegisterUploadReqeustDTO registerUploadReqeustDTO) {
        // CONTROLLO AUTORIZZAZIONI TRAMITE ORGNAME E OPERATION
      //enum con operazioni (
        authorizationService.validateAction(role, "operation");
        productService.saveCsv(registerUploadReqeustDTO, idOrg, idUser, role);
        return ResponseEntity.ok().build();
    }

  @GetMapping("/download/report/{idUpload}")
  public ResponseEntity<byte[]> downloadCsv(
    @RequestParam(required = false) String idProduttore,
    @RequestParam(required = false) String orgName,
    @PathVariable("idUpload") String idUpload) throws IOException {

    ByteArrayOutputStream file = productService.downloadReport(idUpload); // ora ritorna `Path`

    byte[] zipBytes = file.toByteArray();

    return ResponseEntity.ok()
      .header("Content-Disposition", "attachment; filename=expenseFiles.zip")
      .contentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM)
      .body(zipBytes);
  }


}
