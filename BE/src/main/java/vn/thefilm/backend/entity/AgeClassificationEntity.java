package vn.thefilm.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import vn.thefilm.backend.enums.ActiveStatus;

@Entity
@Table(name = "age_classification")
public class AgeClassificationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "age_classification_id")
    private Long ageClassificationId;

    @Column(name = "classification_code")
    private String classificationCode;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "minimum_age")
    private Integer minimumAge;

    @Column(name = "description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ActiveStatus status;

    protected AgeClassificationEntity() {
    }

    public Long getAgeClassificationId() {
        return ageClassificationId;
    }

    public String getClassificationCode() {
        return classificationCode;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Integer getMinimumAge() {
        return minimumAge;
    }

    public String getDescription() {
        return description;
    }

    public ActiveStatus getStatus() {
        return status;
    }
}