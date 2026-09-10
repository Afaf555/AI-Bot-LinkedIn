package mk.ukim.finki.aibotbackend.bot.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import mk.ukim.finki.aibotbackend.bot.browser.PageSnapshot;
import mk.ukim.finki.aibotbackend.model.enums.BotActionType;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/**
 * LlmClient implementation backed by OpenRouter (https://openrouter.ai),
 * which exposes an OpenAI-compatible chat completions API in front of many
 * models, including several free-tier ones.
 *
 * <p>Free-tier models are frequently rate-limited or discontinued without
 * notice, so this client tries the configured primary model first, then
 * falls back through {@code openrouter.fallback-models} in order until one
 * succeeds.</p>
 */
@Component
public class StubLlmClient implements LlmClient {

    private static final int MAX_DOM_CHARS = 15_000;
    private static final int MAX_HISTORY_ENTRIES = 8;

    private final OpenRouterProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();


    private final ThreadLocal<String> lastDomHash = new ThreadLocal<>();
    private final ThreadLocal<Integer> unchangedCount = new ThreadLocal<>();
    public StubLlmClient(OpenRouterProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .defaultHeader("Authorization", "Bearer " + properties.apiKey())
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        return completeWithFallback(systemPrompt, userPrompt);
    }

    @Override
    public BotDecision decideNextAction(PageSnapshot snapshot, String goal, List<BotAction> history) {
        String systemPrompt = """
            You are the decision-making component of a web-browsing agent.
            You are shown the current state of a web page and a goal, and you must
            reply with ONLY a JSON object (no prose, no markdown fences) describing
            the single next action to take.

            IMPORTANT ACTION SEMANTICS:
            - TYPE only fills text into an input field. It does NOT submit forms or
              press Enter. If you need to submit a search, either NAVIGATE directly
              to the constructed search results URL (preferred, more reliable), or
              CLICK an explicit search/submit button/icon.
            - Do not choose TYPE expecting it to also press Enter — it will not.
            - Prefer NAVIGATE with a fully-formed URL over typing into a search box
              whenever the target URL pattern is predictable (e.g. LinkedIn search
              URLs follow /search/results/{type}/?keywords={query}).

            The JSON object must have exactly this shape:
            {
              "goalReached": boolean,
              "rationale": "short explanation of your overall reasoning",
              "action": {
                "type": one of NAVIGATE, CLICK, TYPE, SCROLL, WAIT, EXTRACT, LOGIN, FINISH,
                "target": "a URL for NAVIGATE, or a CSS/Playwright selector for CLICK/TYPE, otherwise null",
                "value": "text to type for TYPE, otherwise null",
                "reasoning": "short explanation of why you chose this specific action"
              }
            }

            When goalReached is true, the action object may be minimal but must still be present.
            Prefer EXTRACT when the page already shows content relevant to the goal.
            Prefer FINISH when there is nothing further useful to do.

            Respond with ONLY the JSON object. Do not think out loud, do not explain
            your reasoning in prose before the JSON, do not write "Let me think..." or
            similar. Output the JSON object immediately as your entire response.
            """;

        String noProgressNotice = trackAndDetectNoProgress(snapshot);
        String userPrompt = buildUserPrompt(snapshot, goal, history)+noProgressNotice;
        String rawContent = completeWithFallback(systemPrompt, userPrompt);
        return parseDecision(rawContent);
    }

    /**
     * Tries the configured primary model first, then each configured
     * fallback model in order, until one returns a non-empty response.
     */
    private String completeWithFallback(String systemPrompt, String userPrompt) {
        List<String> candidateModels = new ArrayList<>();
        candidateModels.add(properties.model());
        if (properties.fallbackModels() != null) {
            candidateModels.addAll(properties.fallbackModels());
        }

        RuntimeException lastError = null;
        for (String model : candidateModels) {
            try {
                return callModel(model, systemPrompt, userPrompt);
            } catch (HttpClientErrorException.TooManyRequests rateLimitException) {
                lastError = rateLimitException;
                // rate-limited on this model — try the next candidate immediately
            } catch (RuntimeException exception) {
                lastError = exception;
            }
        }
        throw new IllegalStateException("All configured LLM models failed or were rate-limited", lastError);
    }

    private String callModel(String model, String systemPrompt, String userPrompt) {
        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "max_tokens", 4000
        );

        JsonNode response = callChatCompletions(requestBody);
        String rawContent = response.path("choices").path(0).path("message").path("content").asText();

        if (rawContent == null || rawContent.isBlank()) {
            throw new IllegalStateException(
                    "Model " + model + " returned empty content. Full response: " + response);
        }
        return rawContent;
    }
    /**
     * Detects when the page content hasn't changed since the last decision,
     * which usually means scrolling/extraction has reached the end of
     * available content. Returns an extra instruction block to append to the
     * prompt once the same content has been seen repeatedly, nudging the model
     * toward FINISH instead of looping SCROLL/EXTRACT indefinitely.
     */
    private String trackAndDetectNoProgress(PageSnapshot snapshot) {
        String currentHash = snapshot.domContent() == null
                ? ""
                : Integer.toHexString(snapshot.domContent().hashCode());

        String previousHash = lastDomHash.get();
        int count = unchangedCount.get() == null ? 0 : unchangedCount.get();

        if (currentHash.equals(previousHash)) {
            count++;
        } else {
            count = 0;
        }

        lastDomHash.set(currentHash);
        unchangedCount.set(count);

        if (count >= 2) {
            return """


            NOTE: The page content has not changed for the last %d decisions in a
            row, even after scrolling. This means no new content is loading —
            you have likely reached the end of available results. Do not choose
            SCROLL or EXTRACT again with the same expectation of new content.
            Choose EXTRACT one final time only if you have not yet extracted this
            exact content, otherwise choose FINISH now.
            """.formatted(count);
        }
        return "";
    }
    private String buildUserPrompt(PageSnapshot snapshot, String goal, List<BotAction> history) {
        String domExcerpt = snapshot.domContent() == null
                ? ""
                : snapshot.domContent().substring(0, Math.min(snapshot.domContent().length(), MAX_DOM_CHARS));

        List<BotAction> recentHistory = history.size() > MAX_HISTORY_ENTRIES
                ? history.subList(history.size() - MAX_HISTORY_ENTRIES, history.size())
                : history;

        StringBuilder historyText = new StringBuilder();
        for (BotAction pastAction : recentHistory) {
            historyText.append("- ").append(pastAction.type())
                    .append(" target=").append(pastAction.target())
                    .append(" value=").append(pastAction.value())
                    .append('\n');
        }

        return """
            GOAL: %s

            CURRENT PAGE URL: %s
            CURRENT PAGE TITLE: %s

            ACTIONS TAKEN SO FAR:
            %s

            CURRENT PAGE CONTENT (truncated HTML):
            %s
            """.formatted(
                goal,
                snapshot.url(),
                snapshot.title(),
                historyText.isEmpty() ? "(none yet)" : historyText.toString(),
                domExcerpt
        );
    }

    private BotDecision parseDecision(String rawContent) {
        try {
            String json = extractJsonObject(rawContent);
            JsonNode node = objectMapper.readTree(json);
            JsonNode actionNode = node.path("action");

            BotAction action = new BotAction(
                    BotActionType.valueOf(actionNode.path("type").asText()),
                    actionNode.path("target").isNull() ? null : actionNode.path("target").asText(null),
                    actionNode.path("value").isNull() ? null : actionNode.path("value").asText(null),
                    actionNode.path("reasoning").asText(null)
            );

            return new BotDecision(
                    action,
                    node.path("goalReached").asBoolean(false),
                    node.path("rationale").asText(null)
            );
        } catch (Exception exception) {
            throw new IllegalStateException("Could not parse LLM decision from response: " + rawContent, exception);
        }
    }

    /**
     * Best-effort extraction of the first JSON object in the model's raw text
     * response, in case the model wraps it in markdown fences despite instructions.
     */
    private String extractJsonObject(String text) {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start == -1 || end == -1 || end < start) {
            throw new IllegalStateException("No JSON object found in LLM response: " + text);
        }
        return text.substring(start, end + 1);
    }

    private JsonNode callChatCompletions(Map<String, Object> requestBody) {
        String rawResponse = restClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(String.class);
        try {
            return objectMapper.readTree(rawResponse);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not parse OpenRouter response: " + rawResponse, exception);
        }
    }
}