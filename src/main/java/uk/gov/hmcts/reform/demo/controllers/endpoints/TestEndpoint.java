package uk.gov.hmcts.reform.demo.controllers.endpoints;


import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;




@RestController
@RequestMapping(path = "/test/",
    consumes = MediaType.APPLICATION_JSON_VALUE,
    produces = MediaType.APPLICATION_JSON_VALUE)
public class TestEndpoint {

    @RequestMapping(value="/helloworld", method= RequestMethod.GET)
    public ResponseEntity<String> testS2SAutorization(){
        return  ResponseEntity.ok("Hello World !!");
    }
}
