package mk.ukim.finki.aibotbackend.bot.core;

import java.util.List;

import jakarta.transaction.Transactional;
import mk.ukim.finki.aibotbackend.model.domain.ExtractedPost;
import mk.ukim.finki.aibotbackend.model.domain.ExtractionSession;
import mk.ukim.finki.aibotbackend.model.domain.ExtractionTarget;
import mk.ukim.finki.aibotbackend.model.dto.CreateExtractedPostDto;
import mk.ukim.finki.aibotbackend.model.exception.SessionNotFoundException;
import mk.ukim.finki.aibotbackend.service.domain.BotActionLogService;
import mk.ukim.finki.aibotbackend.service.domain.ExtractedPostService;
import mk.ukim.finki.aibotbackend.service.domain.ExtractionSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class BotOrchestratorImpl implements BotOrchestrator {
    private final SocialNetworkBot socialNetworkBot;
    private final ExtractionSessionService extractionSessionService;
    private final ExtractedPostService extractedPostService;
    private final BotActionLogService botActionLogService;
    private static final Logger log = LoggerFactory.getLogger(BotOrchestratorImpl.class);

    public BotOrchestratorImpl(
        SocialNetworkBot socialNetworkBot,
        ExtractionSessionService extractionSessionService,
        ExtractedPostService extractedPostService,
        BotActionLogService botActionLogService
    ) {
        this.socialNetworkBot = socialNetworkBot;
        this.extractionSessionService = extractionSessionService;
        this.extractedPostService = extractedPostService;
        this.botActionLogService = botActionLogService;
    }


    @Override
    @Transactional
    public void runSession(Long sessionId) {
        ExtractionSession session = extractionSessionService
                .findById(sessionId)
                .orElseThrow(() -> new SessionNotFoundException(sessionId));

        try {
            socialNetworkBot.login();

            for (ExtractionTarget target : session.getTargets()) {
                BotStepListener stepListener =
                        (action, successful) -> botActionLogService.log(session, action, successful);

                List<CreateExtractedPostDto> extractedDtos =
                        socialNetworkBot.execute(target, stepListener);

                List<ExtractedPost> posts = extractedDtos.stream()
                        .map(dto -> dto.toExtractedPost(session))
                        .toList();

                List<ExtractedPost> macedonianOnly = posts.stream()
                        .filter(post -> post.getMacedonianConfidence() != null && post.getMacedonianConfidence() >= 0.4)
                        .toList();

                extractedPostService.saveAll(macedonianOnly);
            }

            extractionSessionService.complete(sessionId);
        } catch (Exception exception) {
            log.error("Session {} failed", sessionId, exception);
            extractionSessionService.fail(sessionId);
        } finally {
            socialNetworkBot.shutdown();
        }
    }

}