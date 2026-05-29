package com.arcpay.compliance.infrastructure.temporal;

import com.arcpay.compliance.infrastructure.sanctions.SanctionsSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
class HttpSanctionsFeedDownloader implements SanctionsFeedDownloader {

    private final SanctionsIngestionProperties properties;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    @Override
    public byte[] download(SanctionsSource source) {
        var url = properties.sourceUrls().get(source);
        if (url == null) {
            throw new IllegalStateException("No download URL configured for sanctions source " + source);
        }
        var request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(properties.downloadTimeoutSeconds()))
                .GET()
                .build();
        try {
            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() >= 400) {
                throw new SanctionsDownloadException(
                        "Download of " + source + " returned HTTP " + response.statusCode());
            }
            log.info("Downloaded sanctions source {} ({} bytes)", source, response.body().length);
            return response.body();
        } catch (IOException e) {
            throw new SanctionsDownloadException("Failed to download sanctions source " + source, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SanctionsDownloadException("Interrupted downloading sanctions source " + source, e);
        }
    }
}
