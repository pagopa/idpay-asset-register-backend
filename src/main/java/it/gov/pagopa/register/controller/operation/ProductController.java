package it.gov.pagopa.register.controller.operation;

import it.gov.pagopa.register.dto.operation.RegisterUploadReqeustDTO;
import it.gov.pagopa.register.service.operation.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/download/report/{idUpload}")
    public ResponseEntity<Void> uploadCsv( @RequestParam(required = false) String idProduttore,
                                           @RequestParam(required = false) String orgName,
                                           @PathVariable("idUpload") String idUpload) {
      // CONTROLLO AUTORIZZAZIONI TRAMITE ORGNAME E OPERATION
      productService.downloadReport(idUpload);
      return ResponseEntity.ok().build();
    }



}
