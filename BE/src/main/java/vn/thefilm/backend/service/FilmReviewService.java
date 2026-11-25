package vn.thefilm.backend.service;

import vn.thefilm.backend.dto.FilmReviewDtos.CreateRequest;
import vn.thefilm.backend.dto.FilmReviewDtos.Summary;

public interface FilmReviewService {
    Summary getReviews(long filmId);
    Summary createReview(long filmId, Long appUserId, CreateRequest request);
}
