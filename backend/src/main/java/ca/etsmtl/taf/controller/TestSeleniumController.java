package ca.etsmtl.taf.controller;

import ca.etsmtl.taf.selenium.payload.SeleniumTestService;
import ca.etsmtl.taf.selenium.payload.requests.SeleniumCase;
import ca.etsmtl.taf.selenium.payload.requests.SeleniumResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api")
public class TestSeleniumController {
    private final SeleniumTestService seleniumTestService;

    public TestSeleniumController(SeleniumTestService seleniumTestService) {
        this.seleniumTestService = seleniumTestService;
    }

    @PostMapping("/testselenium")
    public ResponseEntity<List<SeleniumResponse>> runTests(@RequestBody List<SeleniumCase> seleniumCases) {
        // Exécute chaque cas de test avec SeleniumTestService
        List<SeleniumResponse> responses = seleniumCases.stream()
                .map(seleniumTestService::executeTestCase)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }
}
