package mk.ukim.finki.aibotbackend.bot.browser;

import com.microsoft.playwright.*;
import mk.ukim.finki.aibotbackend.config.BotProperties;
import org.springframework.stereotype.Component;

import java.util.Base64;


@Component
public class StubBrowserAgent implements BrowserAgent {

    private final BotProperties botProperties;

    private Playwright playwright;
    private Browser browser;
    private BrowserContext context;
    private Page page;

    public StubBrowserAgent(BotProperties botProperties) {
        this.botProperties = botProperties;
    }

    @Override
    public void start() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions()
                        .setHeadless(Boolean.TRUE.equals(botProperties.headless()))
                        .setSlowMo(50)
        );
        context = browser.newContext(
                new Browser.NewContextOptions()
                        .setViewportSize(1366, 900)
                        .setUserAgent(
                                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                                        + "(KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
                        )
        );
        page = context.newPage();
        page.setDefaultTimeout(60000);
    }

    @Override
    public void navigateTo(String url) {
        page.navigate(url);
    }

    @Override
    public void click(String elementDescription) {
        page.locator(elementDescription).first().click();
    }

    @Override
    public void type(String elementDescription, String text) {
        page.locator(elementDescription).first().fill(text);
    }

    @Override
    public void scrollDown() {
        page.mouse().wheel(0, 1200);
    }

    @Override
    public byte[] takeScreenshot() {
        return page.screenshot();
    }

    @Override
    public PageSnapshot snapshot() {
        String screenshotBase64;
        try {
            screenshotBase64 = Base64.getEncoder().encodeToString(takeScreenshot());
        } catch (Exception exception) {
            // Screenshot is best-effort only — the LLM decision logic relies on
            // domContent/url/title, not the image, so a failed screenshot
            // (e.g. a slow-rendering page) must not abort the whole target.
            screenshotBase64 = null;
        }

        return new PageSnapshot(
                page.url(),
                page.title(),
                page.content(),
                screenshotBase64
        );
    }

    @Override
    public void close() {
        if (page != null) {
            page.close();
        }
        if (context != null) {
            context.close();
        }
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
    }
    public boolean tryClick(String elementDescription, int timeoutMillis) {
        try {
            page.locator(elementDescription).first().click(
                    new com.microsoft.playwright.Locator.ClickOptions().setTimeout(timeoutMillis));
            return true;
        } catch (Exception exception) {
            return false;
        }
    }
    public void pressEnter(String elementDescription) {
        page.locator(elementDescription).first().press("Enter");
    }
}