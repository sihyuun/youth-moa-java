package io.github.sihyuuun.youthmoa.region;

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
@Table(name = "region")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Region {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(nullable = false)
    private boolean isFeatured;

    @Builder
    private Region(String name, Boolean isFeatured) {
        this.name = name;
        this.isFeatured = isFeatured != null && isFeatured;
    }

    public void markFeatured() { this.isFeatured = true; }
    public void unmarkFeatured() { this.isFeatured = false; }
}
