package mk.ukim.finki.aibotbackend.bot.core;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "linkedin")
public record LinkedInProperties(
        String username,
        String password
) {
}