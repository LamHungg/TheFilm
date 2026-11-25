package vn.thefilm.backend.service.impl;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.thefilm.backend.dto.FilmReviewDtos.CreateRequest;
import vn.thefilm.backend.dto.FilmReviewDtos.Item;
import vn.thefilm.backend.dto.FilmReviewDtos.Summary;
import vn.thefilm.backend.entity.AppUserEntity;
import vn.thefilm.backend.entity.FilmEntity;
import vn.thefilm.backend.entity.FilmReviewEntity;
import vn.thefilm.backend.exception.ResourceNotFoundException;
import vn.thefilm.backend.repository.AppUserRepository;
import vn.thefilm.backend.repository.FilmRepository;
import vn.thefilm.backend.repository.FilmReviewRepository;
import vn.thefilm.backend.service.CatalogService;
import vn.thefilm.backend.service.FilmReviewService;

@Service
@Transactional(readOnly = true)
public class FilmReviewServiceImpl implements FilmReviewService {
    private final FilmReviewRepository reviewRepository;
    private final FilmRepository filmRepository;
    private final AppUserRepository userRepository;
    private final CatalogService catalogService;
    public FilmReviewServiceImpl(FilmReviewRepository reviewRepository, FilmRepository filmRepository, AppUserRepository userRepository, CatalogService catalogService) {
        this.reviewRepository = reviewRepository; this.filmRepository = filmRepository; this.userRepository = userRepository; this.catalogService = catalogService;
    }
    @Override public Summary getReviews(long filmId) {
        catalogService.getFilm(filmId);
        return summary(filmId);
    }
    @Override @Transactional public Summary createReview(long filmId, Long appUserId, CreateRequest request) {
        if (appUserId == null) throw new IllegalArgumentException("Bạn cần đăng nhập để gửi đánh giá.");
        catalogService.getFilm(filmId);
        FilmEntity film = filmRepository.findById(filmId).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phim."));
        AppUserEntity user = userRepository.findById(appUserId).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản."));
        if (!reviewRepository.hasValidTicket(appUserId, filmId)) throw new IllegalArgumentException("Chỉ tài khoản đã mua vé và được phát hành vé cho phim này mới có thể đánh giá.");
        if (reviewRepository.existsByFilm_FilmIdAndAppUser_AppUserId(filmId, appUserId)) throw new IllegalArgumentException("Tài khoản đã gửi đánh giá cho phim này.");
        String tags = request.tags() == null ? null : request.tags().stream().filter(tag -> tag != null && !tag.isBlank())
                .map(String::trim).map(tag -> tag.replace(",", " ")).limit(8).reduce((a, b) -> a + "," + b).orElse(null);
        reviewRepository.save(new FilmReviewEntity(film, user, request.score(), request.title() == null ? "" : request.title().trim(), request.content().trim(), tags));
        return summary(filmId);
    }
    private Summary summary(long filmId) {
        List<Item> items = reviewRepository.findTop50ByFilm_FilmIdOrderByCreatedAtDesc(filmId).stream().map(review -> new Item(
                review.getId(), review.getAppUser().getDisplayName() == null ? "Khán giả THEFILM" : review.getAppUser().getDisplayName(),
                review.getScore(), review.getTitle(), review.getContent(), review.getTags() == null || review.getTags().isBlank()
                        ? List.of() : Arrays.stream(review.getTags().split(",")).map(String::trim).filter(s -> !s.isBlank()).toList(),
                review.getCreatedAt(), true)).toList();
        Object[] aggregate = reviewRepository.summarize(filmId);
        double average = aggregate == null || aggregate[0] == null ? 0 : ((Number) aggregate[0]).doubleValue();
        long count = aggregate == null || aggregate[1] == null ? 0 : ((Number) aggregate[1]).longValue();
        return new Summary(items, Math.round(average * 10.0) / 10.0, count);
    }
}
