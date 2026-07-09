package com.example.snowydemo.webapp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SimpleSnowyDemoApplicationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void webAppExposesPluginEndpointsFromOneSpringBootApplication() {
        String currentUser = restTemplate.getForObject("/api/auth/me", String.class);
        String menus = restTemplate.getForObject("/api/sys/menus", String.class);
        String customers = restTemplate.getForObject("/api/crm/customers", String.class);

        assertThat(currentUser).contains("demo_admin");
        assertThat(menus).contains("客户管理");
        assertThat(customers).contains("上海星河科技");
    }
}
