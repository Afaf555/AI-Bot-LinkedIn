package mk.ukim.finki.aibotbackend.service.domain.impl;

import java.util.List;
import java.util.Optional;

import mk.ukim.finki.aibotbackend.integration.vezilka.DonationReceipt;
import mk.ukim.finki.aibotbackend.integration.vezilka.TextDonationItem;
import mk.ukim.finki.aibotbackend.integration.vezilka.TextDonationRequest;
import mk.ukim.finki.aibotbackend.integration.vezilka.VezilkaClient;
import mk.ukim.finki.aibotbackend.model.domain.DonationBatch;
import mk.ukim.finki.aibotbackend.model.domain.ExtractedPost;
import mk.ukim.finki.aibotbackend.model.enums.DonationStatus;
import mk.ukim.finki.aibotbackend.model.exception.InvalidDonationStateException;
import mk.ukim.finki.aibotbackend.repository.DonationBatchRepository;
import mk.ukim.finki.aibotbackend.service.domain.DonationService;
import mk.ukim.finki.aibotbackend.service.domain.ExtractedPostService;
import org.springframework.stereotype.Service;

@Service
public class DonationServiceImpl implements DonationService {
    private final DonationBatchRepository donationBatchRepository;
    private final ExtractedPostService extractedPostService;
    private final VezilkaClient vezilkaClient;

    public DonationServiceImpl(
            DonationBatchRepository donationBatchRepository,
            ExtractedPostService extractedPostService,
            VezilkaClient vezilkaClient
    ) {
        this.donationBatchRepository = donationBatchRepository;
        this.extractedPostService = extractedPostService;
        this.vezilkaClient = vezilkaClient;
    }

    @Override
    public List<DonationBatch> findAll() {
        return donationBatchRepository.findAll();
    }

    @Override
    public Optional<DonationBatch> findById(Long id) {
        return donationBatchRepository.findById(id);
    }

    @Override
    public DonationBatch createBatch(List<Long> postIds) {
        List<ExtractedPost> posts = extractedPostService.findAllById(postIds);

        boolean alreadyDonated = posts.stream().anyMatch(post -> post.getDonationBatch() != null);
        if (alreadyDonated) {
            throw new IllegalStateException("One or more posts are already part of a donation batch.");
        }

        DonationBatch batch = new DonationBatch(DonationStatus.DRAFT);
        donationBatchRepository.save(batch);
        posts.forEach(post -> post.setDonationBatch(batch));
        batch.getPosts().addAll(posts);
        return donationBatchRepository.save(batch);
    }

    @Override
    public DonationBatch approve(Long id) {
        DonationBatch batch = getOrThrow(id);
        if (batch.getStatus() != DonationStatus.DRAFT) {
            throw new InvalidDonationStateException(id, batch.getStatus());
        }
        batch.setStatus(DonationStatus.APPROVED);
        return donationBatchRepository.save(batch);
    }

    @Override
    public DonationBatch submit(Long id) {
        DonationBatch batch = getOrThrow(id);
        if (batch.getStatus() != DonationStatus.APPROVED) {
            throw new InvalidDonationStateException(id, batch.getStatus());
        }

        List<TextDonationItem> items = batch.getPosts().stream()
                .filter(post -> post.getContent() != null && !post.getContent().isBlank())
                .map(post -> new TextDonationItem(
                        post.getSourceUrl(),
                        post.getContent(),
                        null,
                        post.getCreatedAt()
                ))
                .toList();

        if (items.isEmpty()) {
            throw new IllegalStateException("Donation batch " + id + " has no donatable text content.");
        }

        DonationReceipt receipt = vezilkaClient.submitTextDonation(new TextDonationRequest(items));

        batch.setVezilkaReference(receipt.reference());
        batch.setSubmittedAt(java.time.LocalDateTime.now());
        batch.setStatus(DonationStatus.SUBMITTED);
        return donationBatchRepository.save(batch);
    }

    @Override
    public void refreshSubmittedStatuses() {
        List<DonationBatch> submittedBatches = donationBatchRepository.findAllByStatus(DonationStatus.SUBMITTED);
        for (DonationBatch batch : submittedBatches) {
            DonationStatus updatedStatus = vezilkaClient.checkStatus(batch.getVezilkaReference());
            if (updatedStatus != DonationStatus.SUBMITTED) {
                batch.setStatus(updatedStatus);
                donationBatchRepository.save(batch);
            }
        }
    }

    private DonationBatch getOrThrow(Long id) {
        return donationBatchRepository.findById(id)
                .orElseThrow(() -> new mk.ukim.finki.aibotbackend.model.exception.DonationBatchNotFoundException(id));
    }
}