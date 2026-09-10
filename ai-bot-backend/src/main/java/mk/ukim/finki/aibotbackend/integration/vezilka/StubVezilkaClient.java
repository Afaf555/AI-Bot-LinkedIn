package mk.ukim.finki.aibotbackend.integration.vezilka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import mk.ukim.finki.aibotbackend.model.enums.DonationStatus;
import mk.ukim.finki.aibotbackend.model.exception.VezilkaIntegrationException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Real client for the doniraj.vezilka.ai Public Donation API v1
 * (X-Donation-Api-Key auth, POST /api/public/v1/donations/text/).
 */
@Component
public class StubVezilkaClient implements VezilkaClient {
    private static final String TEXT_DONATIONS_PATH = "/api/public/v1/donations/text/";
    private static final String READ_BACK_PATH = "/api/public/v1/donations/{id}/";

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public StubVezilkaClient(VezilkaProperties vezilkaProperties) {
        this.restClient = RestClient.builder()
                .baseUrl(vezilkaProperties.baseUrl())
                .defaultHeader("X-Donation-Api-Key", vezilkaProperties.apiKey())
                .build();
    }

    @Override
    public DonationReceipt submitTextDonation(TextDonationRequest request) {
        if (request.items().isEmpty()) {
            throw new VezilkaIntegrationException("Cannot submit a donation with no text items.", null);
        }

        try {
            String body = objectMapper.writeValueAsString(
                    Map.of("items", request.items().stream().map(this::toApiItem).toList())
            );

            String rawResponse = restClient.post()
                    .uri(TEXT_DONATIONS_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            return toReceipt(rawResponse);
        } catch (RestClientException | JsonProcessingException exception) {
            throw new VezilkaIntegrationException(
                    "Failed to submit donation to doniraj.vezilka.ai", exception);
        }
    }

    @Override
    public DonationStatus checkStatus(String vezilkaReference) {
        if (vezilkaReference == null || vezilkaReference.isBlank()) {
            return DonationStatus.SUBMITTED;
        }

        boolean anyAccepted = false;
        boolean anyPending = false;

        for (String id : vezilkaReference.split(",")) {
            try {
                String rawResponse = restClient.get()
                        .uri(READ_BACK_PATH, id)
                        .retrieve()
                        .body(String.class);

                String status = objectMapper.readTree(rawResponse).path("status").asText("");
                if ("accepted".equalsIgnoreCase(status)) {
                    anyAccepted = true;
                } else if (!"rejected".equalsIgnoreCase(status)) {
                    anyPending = true;
                }
            } catch (RestClientException | JsonProcessingException exception) {
                throw new VezilkaIntegrationException(
                        "Failed to check donation status on doniraj.vezilka.ai", exception);
            }
        }

        if (anyPending) {
            return DonationStatus.SUBMITTED;
        }
        return anyAccepted ? DonationStatus.ACCEPTED : DonationStatus.REJECTED;
    }

    private Map<String, Object> toApiItem(TextDonationItem item) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("source_url", item.sourceUrl());
        map.put("text", item.text());
        if (item.pageTitle() != null) {
            map.put("page_title", item.pageTitle());
        }
        if (item.retrievedAt() != null) {
            map.put("retrieved_at",
                    item.retrievedAt().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        }
        return map;
    }

    private DonationReceipt toReceipt(String rawResponse) throws JsonProcessingException {
        JsonNode root = objectMapper.readTree(rawResponse);
        JsonNode results = root.path("results");

        int accepted = root.path("accepted").asInt(0);
        int rejected = root.path("rejected").asInt(0);
        int duplicates = root.path("duplicates").asInt(0);

        List<String> acceptedIds = new ArrayList<>();
        for (JsonNode result : results) {
            if ("accepted".equalsIgnoreCase(result.path("status").asText())) {
                acceptedIds.add(result.path("id").asText());
            }
        }

        if (accepted == 0) {
            String rejectionSummary = StreamSupport.stream(results.spliterator(), false)
                    .map(r -> r.path("rejectionReason").asText("unknown"))
                    .collect(Collectors.joining(", "));
            throw new VezilkaIntegrationException(
                    "Donation rejected by doniraj.vezilka.ai (reasons: " + rejectionSummary + ")", null);
        }

        String reference = String.join(",", acceptedIds);
        String message = "%d accepted, %d rejected, %d duplicates".formatted(accepted, rejected, duplicates);
        return new DonationReceipt(reference, message);
    }
}