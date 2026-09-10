package mk.ukim.finki.aibotbackend.bot.core;

import mk.ukim.finki.aibotbackend.bot.browser.BrowserAgent;
import mk.ukim.finki.aibotbackend.bot.extraction.ContentExtractor;
import mk.ukim.finki.aibotbackend.bot.extraction.LanguageDetector;
import mk.ukim.finki.aibotbackend.bot.llm.LlmClient;
import mk.ukim.finki.aibotbackend.config.BotProperties;
import mk.ukim.finki.aibotbackend.model.domain.ExtractionTarget;
import mk.ukim.finki.aibotbackend.model.enums.SocialNetwork;
import org.springframework.stereotype.Component;

/**
 * Placeholder so the application boots before the assignment is implemented.
 *
 * <p>TODO(student): Replace this bean with a bot for YOUR assigned social
 * network, e.g. {@code InstagramBot extends AbstractSocialNetworkBot}, and
 * implement {@link #network()}, {@link #login()} and
 * {@link #buildGoal(ExtractionTarget)}. Do not override
 * {@code execute(...)} — the loop is shared.</p>
 */
@Component
public class StubSocialNetworkBot extends AbstractSocialNetworkBot {
    private static final String LOGIN_URL = "https://www.linkedin.com/login";

    private final LinkedInProperties linkedInProperties;

    public StubSocialNetworkBot(
            BrowserAgent browserAgent,
            LlmClient llmClient,
            ContentExtractor contentExtractor,
            LanguageDetector languageDetector,
            BotProperties botProperties,
            LinkedInProperties linkedInProperties
    ) {
        super(browserAgent, llmClient, contentExtractor, languageDetector, botProperties);
        this.linkedInProperties = linkedInProperties;
    }

    @Override
    public SocialNetwork network() {
        return SocialNetwork.LINKEDIN;
    }

    @Override
    public void login() {
        browserAgent.start();
        browserAgent.navigateTo(LOGIN_URL);

        if (browserAgent instanceof mk.ukim.finki.aibotbackend.bot.browser.StubBrowserAgent playwrightAgent) {
            playwrightAgent.tryClick("button:has-text('Accept')", 5000);
        }

        try {
            browserAgent.type("input[autocomplete*='username']:visible", linkedInProperties.username());
            browserAgent.type("input[autocomplete*='current-password']:visible", linkedInProperties.password());
            if (browserAgent instanceof mk.ukim.finki.aibotbackend.bot.browser.StubBrowserAgent playwrightAgent) {
                playwrightAgent.pressEnter("input[autocomplete*='current-password']:visible");
            }
        } catch (Exception exception) {
            try {
                byte[] screenshot = browserAgent.takeScreenshot();
                java.nio.file.Files.write(
                        java.nio.file.Path.of("login-failure-debug.png"), screenshot);
            } catch (Exception ignored) {
            }
            throw exception;
        }
    }

    @Override
    protected String buildGoal(ExtractionTarget target) {
        return switch (target.getType()) {
            case PROFILE -> """
                    Navigate directly to https://www.linkedin.com/in/%s/recent-activity/all/
                    — this page lists all posts and shares by this profile, not just the
                    limited preview on the main profile page. Extract every public post
                    written in Macedonian from this feed. Scroll down repeatedly to load
                    the full post history before extracting — LinkedIn loads posts in
                    batches as you scroll, so keep scrolling until no new content
                    appears before running EXTRACT.
                    """.formatted(target.getValue());
            case HASHTAG -> """
                    Search LinkedIn for the hashtag #%s and extract public posts
                    written in Macedonian from the results. Scroll down if needed
                    to load more posts before extracting.
                    """.formatted(target.getValue());
            case KEYWORD -> """
                    Search LinkedIn for posts matching the keyword "%s" and extract
                    public posts written in Macedonian from the results. Scroll down
                    if needed to load more posts before extracting.
                    """.formatted(target.getValue());
            case FEED_URL -> """
                    Navigate to %s and extract public posts written in Macedonian
                    from this page. Scroll down if needed to load more posts before
                    extracting.
                    """.formatted(target.getValue());
        };
    }
}
