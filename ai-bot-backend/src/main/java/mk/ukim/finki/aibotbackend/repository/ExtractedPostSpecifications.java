package mk.ukim.finki.aibotbackend.repository;

import mk.ukim.finki.aibotbackend.model.domain.ExtractedPost;
import mk.ukim.finki.aibotbackend.model.dto.PostFilterDto;
import org.springframework.data.jpa.domain.Specification;

public final class ExtractedPostSpecifications {

    private ExtractedPostSpecifications() {
    }

    public static Specification<ExtractedPost> fromFilter(PostFilterDto filter) {
        Specification<ExtractedPost> specification = Specification.where(null);

        if (filter.sessionId() != null) {
            specification = specification.and((root, query, cb) ->
                cb.equal(root.get("session").get("id"), filter.sessionId()));
        }
        if (filter.socialNetwork() != null) {
            specification = specification.and((root, query, cb) ->
                cb.equal(root.get("session").get("socialNetwork"), filter.socialNetwork()));
        }
        if (filter.minMacedonianConfidence() != null) {
            specification = specification.and((root, query, cb) ->
                cb.greaterThanOrEqualTo(root.get("macedonianConfidence"), filter.minMacedonianConfidence()));
        }
        if (filter.donated() != null) {
            specification = specification.and(filter.donated()
                ? (root, query, cb) -> cb.isNotNull(root.get("donationBatch"))
                : (root, query, cb) -> cb.isNull(root.get("donationBatch")));
        }
        if (filter.search() != null && !filter.search().isBlank()) {
            String pattern = "%" + filter.search().toLowerCase() + "%";
            specification = specification.and((root, query, cb) ->
                cb.like(cb.lower(root.get("content")), pattern));
        }

        return specification;
    }
}