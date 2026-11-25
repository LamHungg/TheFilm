package vn.thefilm.backend.entity;

import java.time.OffsetDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "film_review", uniqueConstraints = @UniqueConstraint(name = "uq_film_review_user", columnNames = {"film_id", "app_user_id"}))
public class FilmReviewEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "film_review_id") private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "film_id", nullable = false) private FilmEntity film;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "app_user_id", nullable = false) private AppUserEntity appUser;
    @Column(name = "score", nullable = false) private Integer score;
    @Column(name = "review_title", length = 120) private String title;
    @Column(name = "review_text", nullable = false, length = 2000) private String content;
    @Column(name = "tags", length = 500) private String tags;
    @Column(name = "created_at", nullable = false) private OffsetDateTime createdAt;

    protected FilmReviewEntity() { }
    public FilmReviewEntity(FilmEntity film, AppUserEntity appUser, int score, String title, String content, String tags) {
        this.film = film; this.appUser = appUser; this.score = score; this.title = title;
        this.content = content; this.tags = tags;
    }
    @PrePersist void onCreate() { if (createdAt == null) createdAt = OffsetDateTime.now(); }
    public Long getId() { return id; }
    public FilmEntity getFilm() { return film; }
    public AppUserEntity getAppUser() { return appUser; }
    public Integer getScore() { return score; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getTags() { return tags; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
