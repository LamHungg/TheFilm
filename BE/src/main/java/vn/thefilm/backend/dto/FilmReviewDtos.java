package vn.thefilm.backend.dto;

import java.time.OffsetDateTime;
import java.util.List;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class FilmReviewDtos {
    private FilmReviewDtos() { }
    public record CreateRequest(@Min(1) @Max(10) int score,
            @Size(max = 120) String title,
            @NotBlank @Size(max = 2000) String content,
            @Size(max = 8) List<@Size(max = 40) String> tags) { }
    public record Item(long id, String displayName, int score, String title, String content,
            List<String> tags, OffsetDateTime createdAt, boolean verifiedPurchase) { }
    public record Summary(List<Item> items, double averageScore, long reviewCount) { }
}
