package com.arcpay.policy.policyengine.test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
public abstract class RestControllerAbstractTest extends FullContextIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;
}
