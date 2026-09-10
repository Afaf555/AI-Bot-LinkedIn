package mk.ukim.finki.aibotbackend.service.domain.impl;

import java.util.*;
import java.util.stream.Collectors;

import mk.ukim.finki.aibotbackend.model.domain.ExtractedPost;
import mk.ukim.finki.aibotbackend.model.dto.PostFilterDto;
import mk.ukim.finki.aibotbackend.repository.ExtractedPostRepository;
import mk.ukim.finki.aibotbackend.repository.ExtractedPostSpecifications;
import mk.ukim.finki.aibotbackend.service.domain.ExtractedPostService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class ExtractedPostServiceImpl implements ExtractedPostService {
    private final ExtractedPostRepository extractedPostRepository;

    public ExtractedPostServiceImpl(ExtractedPostRepository extractedPostRepository) {
        this.extractedPostRepository = extractedPostRepository;
    }

    @Override
    public Page<ExtractedPost> findAll(PostFilterDto filter, int page, int size) {
        return extractedPostRepository.findAll(
                ExtractedPostSpecifications.fromFilter(filter),
                PageRequest.of(page, size)
        );
    }

    @Override
    public Optional<ExtractedPost> findById(Long id) {
        return extractedPostRepository.findById(id);
    }

    @Override
    public List<ExtractedPost> findAllById(List<Long> ids) {
        return extractedPostRepository.findAllById(ids);
    }

    @Override
    public List<ExtractedPost> saveAll(List<ExtractedPost> posts) {
        if (posts.isEmpty()) {
            return List.of();
        }

        // De-duplicate within this batch itself (keep first occurrence).
        Map<String, ExtractedPost> distinctByExternalId = new LinkedHashMap<>();
        for (ExtractedPost post : posts) {
            distinctByExternalId.putIfAbsent(post.getExternalId(), post);
        }

        Long sessionId = posts.get(0).getSession().getId();
        Set<String> alreadySaved = extractedPostRepository
                .findBySession_IdAndExternalIdIn(sessionId, distinctByExternalId.keySet())
                .stream()
                .map(ExtractedPost::getExternalId)
                .collect(Collectors.toSet());

        List<ExtractedPost> newPosts = distinctByExternalId.values().stream()
                .filter(post -> !alreadySaved.contains(post.getExternalId()))
                .toList();

        return extractedPostRepository.saveAll(newPosts);
    }
    @Override
    public Optional<ExtractedPost> deleteById(Long id) {
        Optional<ExtractedPost> post = extractedPostRepository.findById(id);
        post.ifPresent(extractedPostRepository::delete);
        return post;
    }
}