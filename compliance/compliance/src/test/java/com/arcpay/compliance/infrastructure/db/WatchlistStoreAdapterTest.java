package com.arcpay.compliance.infrastructure.db;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.arcpay.platform.test.TestUtils.eqIgnoring;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class WatchlistStoreAdapterTest {

    private static final String MIXED_CASE_ADDRESS = "0xABCDEF1234567890ABCDEF1234567890ABCDEF12";
    private static final String NORMALIZED_ADDRESS = "0xabcdef1234567890abcdef1234567890abcdef12";
    private static final String SOME_LABEL = "operator-flagged";
    private static final String SOME_ADDED_BY = "officer@arcpay.io";

    @Mock
    private WatchlistAddressRepository watchlistAddressRepository;

    private WatchlistStoreAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new WatchlistStoreAdapter(watchlistAddressRepository);
    }

    @Test
    void shouldNormalizeAddressLowercaseOnAdd() {
        // given
        var expectedEntity = WatchlistAddressEntity.builder()
                .address(NORMALIZED_ADDRESS)
                .label(SOME_LABEL)
                .addedBy(SOME_ADDED_BY)
                .build();
        given(watchlistAddressRepository.existsByAddress(NORMALIZED_ADDRESS)).willReturn(false);

        // when
        adapter.addAddress(MIXED_CASE_ADDRESS, SOME_LABEL, SOME_ADDED_BY);

        // then
        then(watchlistAddressRepository).should().save(eqIgnoring(expectedEntity, "id", "addedAt"));
    }

    @Test
    void shouldIdempotentlyIgnoreDuplicateAddress() {
        // given
        given(watchlistAddressRepository.existsByAddress(NORMALIZED_ADDRESS)).willReturn(true);

        // when
        adapter.addAddress(MIXED_CASE_ADDRESS, SOME_LABEL, SOME_ADDED_BY);

        // then
        then(watchlistAddressRepository).should().existsByAddress(NORMALIZED_ADDRESS);
        then(watchlistAddressRepository).shouldHaveNoMoreInteractions();
    }

    @Test
    void shouldRemoveNormalizedAddress() {
        // when
        adapter.removeAddress(MIXED_CASE_ADDRESS);

        // then
        then(watchlistAddressRepository).should().deleteByAddress(NORMALIZED_ADDRESS);
    }

    @Test
    void shouldReportContainsForNormalizedAddress() {
        // given
        given(watchlistAddressRepository.existsByAddress(NORMALIZED_ADDRESS)).willReturn(true);

        // when
        var result = adapter.contains(MIXED_CASE_ADDRESS);

        // then
        assertThat(result).isTrue();
    }

    @Test
    void shouldReturnAllAddresses() {
        // given
        var entity = WatchlistAddressEntity.builder().address(NORMALIZED_ADDRESS).build();
        given(watchlistAddressRepository.findAll()).willReturn(java.util.List.of(entity));

        // when
        var result = adapter.getAllAddresses();

        // then
        assertThat(result).usingRecursiveComparison().isEqualTo(java.util.Set.of(NORMALIZED_ADDRESS));
    }
}
