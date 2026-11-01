package vn.thefilm.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import vn.thefilm.backend.enums.CinemaStatus;

@Entity
@Table(name = "cinema")
public class CinemaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cinema_id")
    private Long cinemaId;

    @Column(name = "cinema_code")
    private String cinemaCode;

    @Column(name = "cinema_name")
    private String cinemaName;

    @Column(name = "address_line")
    private String addressLine;

    @Column(name = "province_name")
    private String provinceName;

    @Column(name = "district_name")
    private String districtName;

    @Column(name = "ward_name")
    private String wardName;

    @Column(name = "phone")
    private String phone;

    @Column(name = "email")
    private String email;

    @Column(name = "time_zone")
    private String timeZone;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private CinemaStatus status;

    protected CinemaEntity() {
    }

    public Long getCinemaId() {
        return cinemaId;
    }

    public String getCinemaCode() {
        return cinemaCode;
    }

    public String getCinemaName() {
        return cinemaName;
    }

    public String getAddressLine() {
        return addressLine;
    }

    public String getProvinceName() {
        return provinceName;
    }

    public String getDistrictName() {
        return districtName;
    }

    public String getWardName() {
        return wardName;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public CinemaStatus getStatus() {
        return status;
    }
}