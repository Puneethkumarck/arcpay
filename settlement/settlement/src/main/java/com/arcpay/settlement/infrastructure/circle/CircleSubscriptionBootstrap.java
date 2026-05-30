package com.arcpay.settlement.infrastructure.circle;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "circle.api.webhook", name = "subscription-endpoint")
class CircleSubscriptionBootstrap implements ApplicationRunner {

    private static final String SUBSCRIPTIONS_PATH = "/v2/notifications/subscriptions";
    private static final List<String> NOTIFICATION_TYPES = List.of("transactions.*");

    private final RestClient restClient;
    private final CircleApiProperties properties;

    @Override
    public void run(ApplicationArguments args) {
        var endpoint = properties.webhook().subscriptionEndpoint();
        if (existsFor(endpoint)) {
            log.info("Circle notification subscription already present for endpoint={}", endpoint);
            return;
        }
        create(endpoint);
    }

    private boolean existsFor(String endpoint) {
        try {
            var response = restClient.get()
                    .uri(SUBSCRIPTIONS_PATH)
                    .retrieve()
                    .body(SubscriptionsResponse.class);
            return subscriptions(response).stream()
                    .anyMatch(subscription -> endpoint.equals(subscription.endpoint()));
        } catch (Exception e) {
            log.warn("Failed to list Circle subscriptions: {}", e.getMessage());
            return false;
        }
    }

    private void create(String endpoint) {
        try {
            restClient.post()
                    .uri(SUBSCRIPTIONS_PATH)
                    .body(Map.of("endpoint", endpoint, "notificationTypes", NOTIFICATION_TYPES))
                    .retrieve()
                    .toBodilessEntity();
            log.info("Created Circle notification subscription for endpoint={}", endpoint);
        } catch (Exception e) {
            log.error("Failed to create Circle subscription for endpoint={}: {}", endpoint, e.getMessage());
        }
    }

    private List<Subscription> subscriptions(SubscriptionsResponse response) {
        if (response == null || response.data() == null || response.data().subscriptions() == null) {
            return emptyList();
        }
        return response.data().subscriptions();
    }

    record SubscriptionsResponse(Data data) {
        record Data(List<Subscription> subscriptions) {}
    }

    record Subscription(String id, String endpoint) {}
}
