package mk.ukim.finki.aibotbackend.service;

import jakarta.transaction.Transactional;
import java.util.List;
import mk.ukim.finki.aibotbackend.integration.vezilka.DonationReceipt;
import mk.ukim.finki.aibotbackend.integration.vezilka.VezilkaClient;
import mk.ukim.finki.aibotbackend.model.domain.DonationBatch;
import mk.ukim.finki.aibotbackend.model.domain.ExtractedPost;
import mk.ukim.finki.aibotbackend.model.domain.ExtractionSession;
import mk.ukim.finki.aibotbackend.model.enums.DonationStatus;
import mk.ukim.finki.aibotbackend.model.enums.SocialNetwork;
import mk.ukim.finki.aibotbackend.model.exception.InvalidDonationStateException;
import mk.ukim.finki.aibotbackend.repository.ExtractedPostRepository;
import mk.ukim.finki.aibotbackend.repository.ExtractionSessionRepository;
import mk.ukim.finki.aibotbackend.service.domain.DonationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Integration-tests the donation workflow (createBatch -> approve -> submit)
 * with the full Spring context and a mocked {@link VezilkaClient}.
 */
@SpringBootTest
@Testcontainers
@Transactional
public class DonationServiceIntegrationTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("aibot_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private DonationService donationService;

    @Autowired
    private ExtractionSessionRepository extractionSessionRepository;

    @Autowired
    private ExtractedPostRepository extractedPostRepository;

    @MockitoBean
    private VezilkaClient vezilkaClient;

    private Long firstPostId;
    private Long secondPostId;

    @BeforeEach
    void setUp() {
        ExtractionSession session = new ExtractionSession(SocialNetwork.LINKEDIN, "test session");
        session = extractionSessionRepository.saveAndFlush(session);

        ExtractedPost firstPost = new ExtractedPost(
                session, "ext-1", "Author One", "Прв пост на македонски.",
                "https://example.com/1", null, 0.9
        );
        ExtractedPost secondPost = new ExtractedPost(
                session, "ext-2", "Author Two", "Втор пост на македонски.",
                "https://example.com/2", null, 0.85
        );

        firstPost = extractedPostRepository.saveAndFlush(firstPost);
        secondPost = extractedPostRepository.saveAndFlush(secondPost);

        firstPostId = firstPost.getId();
        secondPostId = secondPost.getId();
    }

    @Test
    void testDonationWorkflow() {
        when(vezilkaClient.submitTextDonation(any()))
                .thenReturn(new DonationReceipt("vezilka-ref-123", "accepted"));

        DonationBatch created = donationService.createBatch(List.of(firstPostId, secondPostId));
        assertThat(created.getStatus()).isEqualTo(DonationStatus.DRAFT);
        assertThat(created.getPosts()).hasSize(2);

        DonationBatch approved = donationService.approve(created.getId());
        assertThat(approved.getStatus()).isEqualTo(DonationStatus.APPROVED);

        DonationBatch submitted = donationService.submit(approved.getId());
        assertThat(submitted.getStatus()).isEqualTo(DonationStatus.SUBMITTED);
        assertThat(submitted.getVezilkaReference()).isEqualTo("vezilka-ref-123");
        assertThat(submitted.getSubmittedAt()).isNotNull();
    }

    @Test
    void testCannotSubmitBeforeApproval() {
        DonationBatch created = donationService.createBatch(List.of(firstPostId, secondPostId));

        assertThatThrownBy(() -> donationService.submit(created.getId()))
                .isInstanceOf(InvalidDonationStateException.class);
    }

    @Test
    void testCannotApproveTwice() {
        DonationBatch created = donationService.createBatch(List.of(firstPostId, secondPostId));
        donationService.approve(created.getId());

        assertThatThrownBy(() -> donationService.approve(created.getId()))
                .isInstanceOf(InvalidDonationStateException.class);
    }
}