package mk.ukim.finki.aibotbackend.integration.vezilka;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import java.nio.file.Path;
import java.util.Scanner;

/**
 * One-time manual helper: opens a real (non-headless) browser, lets you log
 * into doniraj.vezilka.ai by hand (including solving the reCAPTCHA), and
 * saves the authenticated session to vezilka-session.json so
 * {@link VezilkaClient} can reuse it without logging in again.
 *
 * <p>Run this manually (e.g. right-click this file → Run) whenever the saved
 * session expires. Not part of the Spring application context.</p>
 */
public final class VezilkaSessionBootstrap {
    private static final String SESSION_FILE = "vezilka-session.json";

    private VezilkaSessionBootstrap() {
    }

    public static void main(String[] args) {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions().setHeadless(false));
            BrowserContext context = browser.newContext();
            Page page = context.newPage();
            page.navigate("https://doniraj.vezilka.ai/login");

            System.out.println("Логирај се рачно во отворениот прозорец (реши го и reCAPTCHA-то).");
            System.out.println("Кога ќе бидеш успешно логирана, притисни ENTER тука...");
            new Scanner(System.in).nextLine();

            context.storageState(new BrowserContext.StorageStateOptions().setPath(Path.of(SESSION_FILE)));
            System.out.println("Сесијата е зачувана во " + SESSION_FILE);

            browser.close();
        }
    }
}