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
import org.springframework.test.annotation.DirtiesContext;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class CrmPermissionIntegrationTest {

    private static final String CUSTOMERS_PATH = "/api/crm/customers";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void adminCanReadCreateUpdateAndDeleteGeneratedCustomer() throws Exception {
        String token = loginToken("admin");

        JsonNode initialCustomers = getCustomerData(token);
        assertThat(initialCustomers.size()).isEqualTo(3);

        Map<String, String> createBody = Map.of(
                "name", "苏州新客户",
                "owner", "陈经理",
                "level", "B");
        ResponseEntity<String> createResponse = request(HttpMethod.POST, CUSTOMERS_PATH, token, createBody);

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode createdBody = readBody(createResponse);
        assertThat(createdBody.path("code").asInt()).isEqualTo(201);
        long generatedId = createdBody.path("data").path("id").asLong();
        assertThat(generatedId).isEqualTo(1004L);
        assertThat(createdBody.path("data").path("name").asText()).isEqualTo("苏州新客户");

        JsonNode customersAfterCreate = getCustomerData(token);
        assertThat(customersAfterCreate.size()).isEqualTo(4);
        assertCustomer(customersAfterCreate, 1004L, "苏州新客户", "陈经理", "B");

        Map<String, String> updateBody = Map.of(
                "name", "苏州重点客户",
                "owner", "周经理",
                "level", "A");
        ResponseEntity<String> updateResponse = request(
                HttpMethod.PUT, CUSTOMERS_PATH + "/" + generatedId, token, updateBody);

        assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode updatedCustomer = readBody(updateResponse).path("data");
        assertThat(updatedCustomer.path("id").asLong()).isEqualTo(generatedId);
        assertThat(updatedCustomer.path("name").asText()).isEqualTo("苏州重点客户");
        assertThat(updatedCustomer.path("owner").asText()).isEqualTo("周经理");
        assertThat(updatedCustomer.path("level").asText()).isEqualTo("A");

        JsonNode customersAfterUpdate = getCustomerData(token);
        assertThat(customersAfterUpdate.size()).isEqualTo(4);
        assertCustomer(customersAfterUpdate, 1004L, "苏州重点客户", "周经理", "A");

        ResponseEntity<String> deleteResponse = request(
                HttpMethod.DELETE, CUSTOMERS_PATH + "/" + generatedId, token, null);
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(readBody(deleteResponse).path("code").asInt()).isEqualTo(200);

        JsonNode customersAfterDelete = getCustomerData(token);
        assertThat(customersAfterDelete.size()).isEqualTo(3);
        assertThat(findCustomer(customersAfterDelete, 1004L)).isNull();
    }

    @Test
    void viewerCanReadButCannotCreateUpdateOrDelete() throws Exception {
        String token = loginToken("viewer");

        JsonNode customersBeforeDeniedChanges = getCustomerData(token);

        Map<String, String> customerBody = Map.of(
                "name", "只读用户不能保存",
                "owner", "测试负责人",
                "level", "C");
        assertForbidden(request(HttpMethod.POST, CUSTOMERS_PATH, token, customerBody));
        assertForbidden(request(HttpMethod.PUT, CUSTOMERS_PATH + "/1001", token, customerBody));
        assertForbidden(request(HttpMethod.DELETE, CUSTOMERS_PATH + "/1001", token, null));

        JsonNode customersAfterDeniedChanges = getCustomerData(token);
        assertThat(customersAfterDeniedChanges).isEqualTo(customersBeforeDeniedChanges);
    }

    @Test
    void unauthenticatedUserCannotReadCustomers() throws Exception {
        ResponseEntity<String> response = request(HttpMethod.GET, CUSTOMERS_PATH, null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(readBody(response).path("code").asInt()).isEqualTo(401);
    }

    @Test
    void adminChangingUnknownCustomerReturnsNotFoundWithoutChangingState() throws Exception {
        String token = loginToken("admin");
        JsonNode customersBeforeChanges = getCustomerData(token);
        Map<String, String> updateBody = Map.of(
                "name", "不存在的客户",
                "owner", "陈经理",
                "level", "B");

        ResponseEntity<String> updateResponse = request(
                HttpMethod.PUT, CUSTOMERS_PATH + "/999999", token, updateBody);
        assertNotFound(updateResponse);
        assertThat(getCustomerData(token)).isEqualTo(customersBeforeChanges);

        ResponseEntity<String> deleteResponse = request(
                HttpMethod.DELETE, CUSTOMERS_PATH + "/999999", token, null);
        assertNotFound(deleteResponse);
        assertThat(getCustomerData(token)).isEqualTo(customersBeforeChanges);
    }

    private String loginToken(String username) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/auth/login",
                new HttpEntity<>(Map.of("username", username), headers),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return readBody(response).path("data").path("token").asText();
    }

    private ResponseEntity<String> request(
            HttpMethod method, String path, String token, Map<String, String> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            headers.set("token", token);
        }
        return restTemplate.exchange(path, method, new HttpEntity<>(body, headers), String.class);
    }

    private JsonNode readBody(ResponseEntity<String> response) throws Exception {
        assertThat(response.getBody()).isNotNull();
        return objectMapper.readTree(response.getBody());
    }

    private JsonNode getCustomerData(String token) throws Exception {
        ResponseEntity<String> response = request(HttpMethod.GET, CUSTOMERS_PATH, token, null);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = readBody(response);
        assertThat(body.path("code").asInt()).isEqualTo(200);
        return body.path("data");
    }

    private JsonNode findCustomer(JsonNode customers, long id) {
        for (JsonNode customer : customers) {
            if (customer.path("id").asLong() == id) {
                return customer;
            }
        }
        return null;
    }

    private void assertCustomer(
            JsonNode customers, long id, String name, String owner, String level) {
        JsonNode customer = findCustomer(customers, id);
        assertThat(customer).isNotNull();
        assertThat(customer.path("name").asText()).isEqualTo(name);
        assertThat(customer.path("owner").asText()).isEqualTo(owner);
        assertThat(customer.path("level").asText()).isEqualTo(level);
    }

    private void assertForbidden(ResponseEntity<String> response) throws Exception {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        JsonNode body = readBody(response);
        assertThat(body.path("code").asInt()).isEqualTo(403);
        assertThat(body.path("message").asText()).contains("没有操作权限");
    }

    private void assertNotFound(ResponseEntity<String> response) throws Exception {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        JsonNode body = readBody(response);
        assertThat(body.path("code").asInt()).isEqualTo(404);
        assertThat(body.path("message").asText()).contains("客户不存在");
    }
}
