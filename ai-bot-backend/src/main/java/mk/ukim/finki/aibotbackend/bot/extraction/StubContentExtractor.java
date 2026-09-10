package mk.ukim.finki.aibotbackend.bot.extraction;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import mk.ukim.finki.aibotbackend.bot.browser.PageSnapshot;
import mk.ukim.finki.aibotbackend.model.dto.CreateExtractedPostDto;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;
import mk.ukim.finki.aibotbackend.model.dto.CreateMediaItemDto;
import mk.ukim.finki.aibotbackend.model.enums.MediaType;
/**
 * Parses LinkedIn feed/search-result HTML into structured posts using Jsoup.
 *
 * <p>LinkedIn's CSS class names are randomized per build (e.g. "_227243f3"),
 * so class-based selectors are unreliable. Instead, this anchors on the
 * accessibility label of each post's "control menu" button — e.g.
 * {@code aria-label="Open control menu for post by Jane Doe"} — which stays
 * semantically stable because it must remain readable by screen readers.
 * From that anchor, it climbs up the DOM until the enclosing element would
 * span more than one post (detected by more than one such button appearing),
 * then treats the previous level as that post's container.</p>
 */
@Component
public class StubContentExtractor implements ContentExtractor {

    private static final String CONTROL_MENU_SELECTOR =
            "button[aria-label*='Open control menu for post by']";

    private static final Pattern AUTHOR_NAME_PATTERN =
            Pattern.compile("Open control menu for post by (.+)$");

    private static final int MAX_CLIMB_LEVELS = 10;

    @Override
    public List<CreateExtractedPostDto> extract(PageSnapshot snapshot) {
        List<CreateExtractedPostDto> posts = new ArrayList<>();
        if (snapshot.domContent() == null || snapshot.domContent().isBlank()) {
            System.out.println("[DEBUG] EXTRACT: domContent is null or blank!");
            return posts;
        }

        System.out.println("[DEBUG] EXTRACT: domContent length = " + snapshot.domContent().length());
        System.out.println("[DEBUG] EXTRACT: page URL = " + snapshot.url());

        Document document = Jsoup.parse(snapshot.domContent());
        Elements controlButtons = document.select(CONTROL_MENU_SELECTOR);

        System.out.println("[DEBUG] EXTRACT: found " + controlButtons.size() + " control-menu buttons");

        boolean phrasePresent = snapshot.domContent().contains("Open control menu for post by");
        System.out.println("[DEBUG] EXTRACT: raw text contains phrase = " + phrasePresent);

        Set<String> seenExternalIds = new LinkedHashSet<>();

        for (Element controlButton : controlButtons) {
            String authorName = extractAuthorName(controlButton.attr("aria-label"));
            if (authorName == null) {
                continue;
            }

            Element postContainer = findPostContainer(controlButton);
            if (postContainer == null) {
                continue;
            }

            String content = postContainer.text();
            if (content == null || content.isBlank()) {
                continue;
            }

            String sourceUrl = extractFirstProfileOrArticleLink(postContainer, snapshot.url());
            String externalId = sourceUrl + "#" + Integer.toHexString(content.hashCode());

            if (!seenExternalIds.add(externalId)) {
                continue;
            }

            posts.add(new CreateExtractedPostDto(
                    externalId,
                    authorName,
                    content.trim(),
                    sourceUrl,
                    null,
                    null,
                    extractMediaItems(postContainer)
            ));
        }

        System.out.println("[DEBUG] EXTRACT: returning " + posts.size() + " posts");
        return posts;
    }
    private List<CreateMediaItemDto> extractMediaItems(Element postContainer) {
        List<CreateMediaItemDto> mediaItems = new ArrayList<>();
        Set<String> seenUrls = new LinkedHashSet<>();

        for (Element video : postContainer.select("video[src], video source[src]")) {
            String url = video.absUrl("src");
            if (!url.isBlank() && seenUrls.add(url)) {
                mediaItems.add(new CreateMediaItemDto(MediaType.VIDEO, url, null));
            }
        }

        for (Element img : postContainer.select("img[src*='media.licdn.com']")) {
            if (isLikelyIconOrAvatar(img)) {
                continue;
            }
            String url = img.absUrl("src");
            if (!url.isBlank() && seenUrls.add(url)) {
                mediaItems.add(new CreateMediaItemDto(MediaType.IMAGE, url, null));
            }
        }

        return mediaItems;
    }

    /**
     * Filters out reaction icons, emoji and small avatar/profile-picture
     * images so only actual post-content media is captured.
     */
    private boolean isLikelyIconOrAvatar(Element img) {
        String alt = img.attr("alt").toLowerCase();
        if (alt.contains("like") || alt.contains("celebrate") || alt.contains("support")
                || alt.contains("love") || alt.contains("insightful") || alt.contains("funny")
                || alt.contains("reaction") || alt.contains("curious")) {
            return true;
        }

        try {
            String width = img.attr("width");
            String height = img.attr("height");
            if (!width.isBlank() && Integer.parseInt(width) <= 48) {
                return true;
            }
            if (!height.isBlank() && Integer.parseInt(height) <= 48) {
                return true;
            }
        } catch (NumberFormatException ignored) {
        }

        return false;
    }
    private String extractAuthorName(String ariaLabel) {
        if (ariaLabel == null) {
            return null;
        }
        Matcher matcher = AUTHOR_NAME_PATTERN.matcher(ariaLabel);
        return matcher.find() ? matcher.group(1).trim() : null;
    }

    /**
     * Climbs up from the control-menu button until including the next
     * ancestor would span more than one post (i.e. contain more than one
     * such button), then returns the last ancestor that still scoped to
     * exactly this one post.
     */
    private Element findPostContainer(Element controlButton) {
        Element current = controlButton;
        Element lastSinglePostAncestor = controlButton.parent();

        for (int level = 0; level < MAX_CLIMB_LEVELS; level++) {
            Element parent = current.parent();
            if (parent == null) {
                break;
            }
            int buttonsInParent = parent.select(CONTROL_MENU_SELECTOR).size();
            if (buttonsInParent > 1) {
                break;
            }
            lastSinglePostAncestor = parent;
            current = parent;
        }

        return lastSinglePostAncestor;
    }

    private String extractFirstProfileOrArticleLink(Element postContainer, String fallbackUrl) {
        Element link = postContainer.selectFirst("a[href*='linkedin.com/in/'], a[href*='/pulse/']");
        if (link != null) {
            String href = link.absUrl("href");
            if (!href.isBlank()) {
                return href;
            }
        }
        return fallbackUrl;
    }
}