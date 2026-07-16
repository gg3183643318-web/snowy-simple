package com.example.snowydemo.webapp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthFlowIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void loginWithAdminReturnsToken() throws Exception {
        ResponseEntity<String> response = login("admin");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.path("data").path("token").asText()).isNotBlank();
    }

    @Test
    void tokenCanAccessCurrentAdminUser() throws Exception {
        String token = loginToken("admin");

        ResponseEntity<String> response = getCurrentUser(token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("admin", "演示管理员");
    }

    @Test
    void loginWithMissingAccountReturnsBusinessError() throws Exception {
        ResponseEntity<String> response = login("missing");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.path("code").asInt()).isEqualTo(400);
        assertThat(body.path("message").asText()).contains("只支持 admin 或 viewer");
    }

    @Test
    void loginWithBlankUsernameReturnsValidationError() throws Exception {
        ResponseEntity<String> response = login("   ");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.path("message").asText()).contains("账号不能为空");
    }

    @Test
    void currentUserWithoutTokenReturnsUnauthorized() throws Exception {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/auth/me", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.path("code").asInt()).isEqualTo(401);
        assertThat(body.path("message").asText()).contains("请先登录");
    }

    @Test
    void logoutInvalidatesCurrentToken() throws Exception {
        String token = loginToken("viewer");
        HttpEntity<Void> request = new HttpEntity<>(tokenHeaders(token));

        ResponseEntity<String> logoutResponse = restTemplate.exchange(
                "/api/auth/logout", HttpMethod.POST, request, String.class);
        ResponseEntity<String> currentUserResponse = getCurrentUser(token);

        assertThat(logoutResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(currentUserResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        JsonNode body = objectMapper.readTree(currentUserResponse.getBody());
        assertThat(body.path("code").asInt()).isEqualTo(401);
    }

    private ResponseEntity<String> login(String username) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> request = new HttpEntity<>(Map.of("username", username), headers);
        return restTemplate.postForEntity("/api/auth/login", request, String.class);
    }

    private String loginToken(String username) throws Exception {
        ResponseEntity<String> response = login(username);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return objectMapper.readTree(response.getBody()).path("data").path("token").asText();
    }

    private ResponseEntity<String> getCurrentUser(String token) {
        return restTemplate.exchange(
                "/api/auth/me",
                HttpMethod.GET,
                new HttpEntity<>(tokenHeaders(token)),
                String.class);
    }

    private HttpHeaders tokenHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("token", token);
        return headers;
    }
}
