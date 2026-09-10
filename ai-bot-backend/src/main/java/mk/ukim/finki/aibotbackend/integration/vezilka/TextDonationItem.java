package mk.ukim.finki.aibotbackend.integration.vezilka;

import java.time.LocalDateTime;

/**
 * One item of a text donation request — mirrors one entry in the
 * doniraj.vezilka.ai Public Donation API's {@code items} envelope.
 *
 * @param sourceUrl   where the text was found, for provenance. Required.
 * @param text        the donated Macedonian text. Required.
 * @param pageTitle   optional title of the source page.
 * @param retrievedAt optional timestamp of when the text was captured.
 */
public record TextDonationItem(
        String sourceUrl,
        String text,
        String pageTitle,
        LocalDateTime retrievedAt
) {
}