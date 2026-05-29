package com.arcpay.compliance.application.controller;

import com.arcpay.compliance.api.model.WatchlistEntryResponse;
import com.arcpay.compliance.domain.port.WatchlistStore;
import com.arcpay.compliance.test.RestControllerAbstractTest;
import com.arcpay.platform.api.ApiError;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Set;

import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_WATCHLIST_ADDRESS;
import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_WATCHLIST_ADDRESS_MIXED_CASE;
import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_WATCHLIST_LABEL;
import static com.arcpay.compliance.fixtures.IdentityFixtures.SOME_OFFICER_EMAIL;
import static com.arcpay.compliance.fixtures.SecurityContextFixtures.officerAuth;
import static com.arcpay.compliance.fixtures.SecurityContextFixtures.ownerAuth;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WatchlistControllerIntegrationTest extends RestControllerAbstractTest {

    @MockitoBean
    private WatchlistStore watchlistStore;

    @Autowired
    private JsonMapper jsonMapper;

    @Test
    void shouldAddValidAddressAndReturn201() throws Exception {
        // given
        var body = jsonMapper.writeValueAsString(
                new java.util.HashMap<>(java.util.Map.of(
                        "address", SOME_WATCHLIST_ADDRESS_MIXED_CASE,
                        "label", SOME_WATCHLIST_LABEL)));

        // when
        var response = mockMvc.perform(post("/compliance/watchlist")
                        .with(authentication(officerAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        // then
        var actual = jsonMapper.readValue(response, WatchlistEntryResponse.class);
        assertThat(actual).usingRecursiveComparison().isEqualTo(WatchlistEntryResponse.builder()
                .address(SOME_WATCHLIST_ADDRESS)
                .label(SOME_WATCHLIST_LABEL)
                .build());
        then(watchlistStore).should().addAddress(SOME_WATCHLIST_ADDRESS, SOME_WATCHLIST_LABEL, SOME_OFFICER_EMAIL);
    }

    @Test
    void shouldRejectMalformedAddressWith422() throws Exception {
        // given
        var body = "{\"address\": \"not-an-address\"}";

        // when
        var response = mockMvc.perform(post("/compliance/watchlist")
                        .with(authentication(officerAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnprocessableEntity())
                .andReturn().getResponse().getContentAsString();

        // then
        var actual = jsonMapper.readValue(response, ApiError.class);
        assertThat(actual.code()).isEqualTo("ARCPAY-COMPLIANCE-0007");
        then(watchlistStore).shouldHaveNoInteractions();
    }

    @Test
    void shouldRejectBlankAddressWith422() throws Exception {
        // given
        var body = "{\"address\": \"   \"}";

        // when
        var response = mockMvc.perform(post("/compliance/watchlist")
                        .with(authentication(officerAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnprocessableEntity())
                .andReturn().getResponse().getContentAsString();

        // then
        var actual = jsonMapper.readValue(response, ApiError.class);
        assertThat(actual.code()).isEqualTo("ARCPAY-COMPLIANCE-0007");
    }

    @Test
    void shouldRejectNonOfficerWith403() throws Exception {
        // given
        var body = jsonMapper.writeValueAsString(
                java.util.Map.of("address", SOME_WATCHLIST_ADDRESS, "label", SOME_WATCHLIST_LABEL));

        // when
        var response = mockMvc.perform(post("/compliance/watchlist")
                        .with(authentication(ownerAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andReturn().getResponse().getContentAsString();

        // then
        var actual = jsonMapper.readValue(response, ApiError.class);
        assertThat(actual.code()).isEqualTo("ARCPAY-COMPLIANCE-0003");
        then(watchlistStore).shouldHaveNoInteractions();
    }

    @Test
    void shouldDeleteAddressAndReturn204() throws Exception {
        // when
        mockMvc.perform(delete("/compliance/watchlist/{address}", SOME_WATCHLIST_ADDRESS_MIXED_CASE)
                        .with(authentication(officerAuth())))
                .andExpect(status().isNoContent());

        // then
        then(watchlistStore).should().removeAddress(SOME_WATCHLIST_ADDRESS);
    }

    @Test
    void shouldDeleteIdempotentlyEvenWhenAbsent() throws Exception {
        // when
        mockMvc.perform(delete("/compliance/watchlist/{address}", SOME_WATCHLIST_ADDRESS)
                        .with(authentication(officerAuth())))
                .andExpect(status().isNoContent());

        // then
        then(watchlistStore).should().removeAddress(SOME_WATCHLIST_ADDRESS);
    }

    @Test
    void shouldListAddresses() throws Exception {
        // given
        given(watchlistStore.getAllAddresses()).willReturn(Set.of(SOME_WATCHLIST_ADDRESS));

        // when
        var response = mockMvc.perform(get("/compliance/watchlist")
                        .with(authentication(officerAuth())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // then
        var actual = jsonMapper.readValue(response, WatchlistEntryResponse[].class);
        assertThat(List.of(actual)).usingRecursiveComparison().isEqualTo(List.of(
                WatchlistEntryResponse.builder().address(SOME_WATCHLIST_ADDRESS).build()));
    }

    @Test
    void shouldRejectListForNonOfficerWith403() throws Exception {
        // when
        var response = mockMvc.perform(get("/compliance/watchlist")
                        .with(authentication(ownerAuth())))
                .andExpect(status().isForbidden())
                .andReturn().getResponse().getContentAsString();

        // then
        var actual = jsonMapper.readValue(response, ApiError.class);
        assertThat(actual.code()).isEqualTo("ARCPAY-COMPLIANCE-0003");
    }
}
