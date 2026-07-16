package com.example.snowydemo.webapp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RequestPipelineIntegrationTest {

    private static final String CUSTOMERS_PATH = "/api/crm/customers";
    private static final String FRONTEND_ORIGIN = "http://localhost:8091";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void authenticatedRequestExposesTraceAndHandlerAndLogsPipelineInOrder(CapturedOutput output)
            throws Exception {
        String token = loginToken("admin");
        HttpHeaders headers = new HttpHeaders();
        headers.set("token", token);
        headers.setOrigin(FRONTEND_ORIGIN);

        ResponseEntity<String> response = restTemplate.exchange(
                CUSTOMERS_PATH,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String traceId = response.getHeaders().getFirst("X-Trace-Id");
        assertThat(traceId).isNotNull().matches("\\S{8}");
        assertThat(response.getHeaders().getFirst("X-Handler"))
                .isEqualTo("CrmCustomerController#customers");
        assertThat(response.getHeaders().getAccessControlExposeHeaders())
                .contains("X-Trace-Id", "X-Handler");

        String logs = output.getOut();
        assertThat(logs).contains("[" + traceId + "] 3 AOP before: 查询客户");
        assertInOrder(
                logs,
                "[" + traceId + "] 1 Filter before",
                "[" + traceId + "] 2 Interceptor before",
                "[" + traceId + "] 3 AOP before",
                "[" + traceId + "] 4 AOP after",
                "[" + traceId + "] 5 Interceptor after",
                "[" + traceId + "] 6 Filter after");
    }

    @Test
    void unauthenticatedProtectedRequestStillReturnsTraceHeader() {
        ResponseEntity<String> response = restTemplate.getForEntity(CUSTOMERS_PATH, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getHeaders().getFirst("X-Trace-Id"))
                .isNotNull()
                .matches("\\S{8}");
    }

    private String loginToken(String username) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/auth/login",
                new HttpEntity<>(Map.of("username", username), headers),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = objectMapper.readTree(response.getBody());
        return body.path("data").path("token").asText();
    }

    private void assertInOrder(String output, String... entries) {
        int previousIndex = -1;
        for (String entry : entries) {
            int currentIndex = output.indexOf(entry);
            assertThat(currentIndex)
                    .as("日志中应包含 %s", entry)
                    .isGreaterThan(previousIndex);
            previousIndex = currentIndex;
        }
    }
}
