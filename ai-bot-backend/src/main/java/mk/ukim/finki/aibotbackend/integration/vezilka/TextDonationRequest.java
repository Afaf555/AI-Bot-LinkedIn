package mk.ukim.finki.aibotbackend.integration.vezilka;

import java.util.List;

/**
 * The payload of a text donation towards doniraj.vezilka.ai — an envelope
 * of one or more {@link TextDonationItem}s, mirroring the Public Donation
 * API's {@code {"items": [...]}} request body (up to 100 items/request).
 *
 * @param items the donated text items, each with its own provenance
 */
public record TextDonationRequest(
        List<TextDonationItem> items
) {
}