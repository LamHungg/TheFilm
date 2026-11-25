package vn.thefilm.backend.controller;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.thefilm.backend.dto.ApiResponse;
import vn.thefilm.backend.dto.FilmReviewDtos.CreateRequest;
import vn.thefilm.backend.dto.FilmReviewDtos.Summary;
import vn.thefilm.backend.security.TheFilmUserPrincipal;
import vn.thefilm.backend.service.CustomerAccountService;
import vn.thefilm.backend.service.FilmReviewService;

@RestController
@RequestMapping("/api/v1/public/films/{filmId}/reviews")
public class FilmReviewController {
    private final FilmReviewService reviewService;
    private final CustomerAccountService customerAccountService;
    public FilmReviewController(FilmReviewService reviewService, CustomerAccountService customerAccountService) {
        this.reviewService = reviewService; this.customerAccountService = customerAccountService;
    }
    @GetMapping public ApiResponse<Summary> get(@PathVariable long filmId) { return ApiResponse.success(reviewService.getReviews(filmId)); }
    @PostMapping public ApiResponse<Summary> create(@PathVariable long filmId, @Valid @RequestBody CreateRequest request, Authentication authentication) {
        Long userId = null;
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof TheFilmUserPrincipal principal) {
            userId = customerAccountService.syncOrGetCustomer(principal.oidcIssuer(), principal.oidcSubject(),
                    principal.email(), principal.displayName()).appUserId();
        }
        return ApiResponse.success(reviewService.createReview(filmId, userId, request));
    }
}
