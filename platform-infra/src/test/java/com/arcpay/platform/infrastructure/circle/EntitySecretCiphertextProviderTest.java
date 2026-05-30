package com.arcpay.platform.infrastructure.circle;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.spec.MGF1ParameterSpec;
import java.util.Base64;
import java.util.HexFormat;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EntitySecretCiphertextProviderTest {

    private static final String SOME_ENTITY_SECRET_HEX =
            "0102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f20";

    private static final KeyPair KEY_PAIR = generateKeyPair();

    private WireMockServer circleServer;
    private EntitySecretCiphertextProvider provider;

    @BeforeEach
    void setUp() {
        circleServer = new WireMockServer(options().dynamicPort());
        circleServer.start();
        stubEntityPublicKey(publicKeyPem());

        var restClient = RestClient.builder().baseUrl(circleServer.baseUrl()).build();
        provider = new EntitySecretCiphertextProvider(SOME_ENTITY_SECRET_HEX, restClient);
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

    @Test
    void shouldRejectEntitySecretThatIsNotThirtyTwoBytes() {
        // given
        var shortSecretProvider = new EntitySecretCiphertextProvider(
                "0102030405", RestClient.builder().baseUrl(circleServer.baseUrl()).build());

        // when / then
        assertThatThrownBy(shortSecretProvider::generate)
                .isInstanceOf(CircleEntitySecretException.class)
                .hasMessageContaining("32 bytes");
    }

    @Test
    void shouldRejectEntitySecretThatIsNotValidHex() {
        // given
        var badHexProvider = new EntitySecretCiphertextProvider(
                "zzzz", RestClient.builder().baseUrl(circleServer.baseUrl()).build());

        // when / then
        assertThatThrownBy(badHexProvider::generate)
                .isInstanceOf(CircleEntitySecretException.class)
                .hasMessageContaining("hex");
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

    private void stubEntityPublicKey(String pemPublicKey) {
        circleServer.stubFor(get(urlPathEqualTo("/v1/w3s/config/entity/publicKey"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "data": {
                                    "publicKey": "%s"
                                  }
                                }
                                """.formatted(pemPublicKey.replace("\n", "\\n")))));
    }

    private PrivateKey privateKey() {
        return KEY_PAIR.getPrivate();
    }

    private String publicKeyPem() {
        var base64 = Base64.getMimeEncoder(64, "\n".getBytes())
                .encodeToString(KEY_PAIR.getPublic().getEncoded());
        return "-----BEGIN PUBLIC KEY-----\n" + base64 + "\n-----END PUBLIC KEY-----";
    }

    private static KeyPair generateKeyPair() {
        try {
            var generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate RSA key pair for tests", e);
        }
    }
}
