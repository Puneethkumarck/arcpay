package com.arcpay.settlement.test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.client.RestClient;

@DirtiesContext
public abstract class BusinessTest extends FullContextIntegrationTest {

    @LocalServerPort
    private int port;

    private RestClient cachedRestClient;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    protected RestClient restClient() {
        if (cachedRestClient == null) {
            cachedRestClient = RestClient.builder()
                    .baseUrl("http://localhost:" + port)
                    .build();
        }
        return cachedRestClient;
    }

    protected void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM settlement_outbox_record");
        jdbcTemplate.update("DELETE FROM settlement_transaction");
    }
}
