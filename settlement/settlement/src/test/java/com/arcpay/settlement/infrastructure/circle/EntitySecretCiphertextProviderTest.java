package com.arcpay.settlement.infrastructure.circle;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import java.security.spec.MGF1ParameterSpec;
import javax.crypto.spec.PSource;
import java.util.Base64;
import java.util.HexFormat;

import static com.arcpay.settlement.fixtures.CircleKeyFixtures.SOME_ENTITY_SECRET_HEX;
import static com.arcpay.settlement.fixtures.CircleKeyFixtures.privateKey;
import static com.arcpay.settlement.fixtures.CircleKeyFixtures.publicKeyPem;
import static com.arcpay.settlement.test.stubs.CircleStubs.stubEntityPublicKey;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EntitySecretCiphertextProviderTest {

    private WireMockServer circleServer;
    private EntitySecretCiphertextProvider provider;

    @BeforeEach
    void setUp() {
        circleServer = new WireMockServer(options().dynamicPort());
        circleServer.start();
        stubEntityPublicKey(circleServer, publicKeyPem());

        var properties = new CircleApiProperties(
                circleServer.baseUrl(),
                "test-key",
                "wallet-set",
                "ARC-TESTNET",
                "0x3600000000000000000000000000000000000000",
                SOME_ENTITY_SECRET_HEX,
                new CircleApiProperties.Timeout(5000, 15000));
        var restClient = RestClient.builder().baseUrl(circleServer.baseUrl()).build();
        provider = new EntitySecretCiphertextProvider(properties, restClient);
    }

    @AfterEach
    void tearDown() {
        circleServer.stop();
    }

    @Test
    void shouldProduceBase64CiphertextDecryptableWithMgf1Sha256() throws Exception {
        // given
        var expectedSecret = HexFormat.of().parseHex(SOME_ENTITY_SECRET_HEX);

        // when
        var ciphertext = provider.generate();
        var decrypted = decrypt(ciphertext, mgf1("SHA-256"));

        // then
        assertThat(Base64.getDecoder().decode(ciphertext)).hasSize(256);
        assertThat(decrypted).isEqualTo(expectedSecret);
    }

    @Test
    void shouldRejectDecryptionWhenMgf1IsSha1() {
        // given
        var ciphertext = provider.generate();

        // when / then
        assertThatThrownBy(() -> decrypt(ciphertext, mgf1("SHA-1")))
                .isInstanceOf(Exception.class);
    }

    @Test
    void shouldProduceFreshCiphertextPerCall() {
        // given / when
        var first = provider.generate();
        var second = provider.generate();

        // then
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void shouldEncodeBase64AroundExpectedLength() {
        // given / when
        var ciphertext = provider.generate();

        // then
        assertThat(ciphertext).hasSize(344);
    }

    private byte[] decrypt(String ciphertext, OAEPParameterSpec spec) throws Exception {
        var cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        cipher.init(Cipher.DECRYPT_MODE, privateKey(), spec);
        return cipher.doFinal(Base64.getDecoder().decode(ciphertext));
    }

    private OAEPParameterSpec mgf1(String mgfHash) {
        return new OAEPParameterSpec("SHA-256", "MGF1",
                new MGF1ParameterSpec(mgfHash), PSource.PSpecified.DEFAULT);
    }
}
