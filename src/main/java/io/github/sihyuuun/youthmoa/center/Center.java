package io.github.sihyuuun.youthmoa.center;

import io.github.sihyuuun.youthmoa.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "center")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Center extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 50)
    private String region;

    @Column(length = 255)
    private String address;

    @Column(length = 20)
    private String phone;

    @Column(nullable = false)
    private boolean isActive;

    @Column(nullable = false)
    private boolean isFeatured;

    @Builder
    private Center(String name, String region, String address, String phone, Boolean isActive, Boolean isFeatured) {
        this.name = name;
        this.region = region;
        this.address = address;
        this.phone = phone;
        this.isActive = isActive != null ? isActive : true;
        this.isFeatured = isFeatured != null && isFeatured;
    }

    public void markFeatured() { this.isFeatured = true; }
    public void unmarkFeatured() { this.isFeatured = false; }

    public void updateInfo(String name, String region, String address, String phone) {
        this.name = name;
        this.region = region;
        this.address = address;
        this.phone = phone;
    }

    public void activate() { this.isActive = true; }
    public void deactivate() { this.isActive = false; }
}
