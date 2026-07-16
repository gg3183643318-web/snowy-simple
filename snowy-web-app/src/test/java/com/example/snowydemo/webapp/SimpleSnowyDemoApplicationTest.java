package com.example.snowydemo.webapp;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SimpleSnowyDemoApplicationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void webAppExposesPluginEndpointsFromOneSpringBootApplication() throws Exception {
        HttpHeaders loginHeaders = new HttpHeaders();
        loginHeaders.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> loginResponse = restTemplate.postForEntity(
                "/api/auth/login",
                new HttpEntity<>(Map.of("username", "admin"), loginHeaders),
                String.class);
        String token = objectMapper.readTree(loginResponse.getBody()).path("data").path("token").asText();

        HttpHeaders tokenHeaders = new HttpHeaders();
        tokenHeaders.set("token", token);
        String currentUser = restTemplate.exchange(
                "/api/auth/me",
                HttpMethod.GET,
                new HttpEntity<>(tokenHeaders),
                String.class).getBody();
        String menus = restTemplate.getForObject("/api/sys/menus", String.class);
        String customers = restTemplate.exchange(
                "/api/crm/customers",
                HttpMethod.GET,
                new HttpEntity<>(tokenHeaders),
                String.class).getBody();

        assertThat(currentUser).contains("admin");
        assertThat(menus).contains("客户管理");
        assertThat(customers).contains("上海星河科技");
    }
}
