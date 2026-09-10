package mk.ukim.finki.aibotbackend.bot.llm;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "openrouter")
public record OpenRouterProperties(
        String baseUrl,
        String apiKey,
        String model,
        List<String> fallbackModels
) {
}