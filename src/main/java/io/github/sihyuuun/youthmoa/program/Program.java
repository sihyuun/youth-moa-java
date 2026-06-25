package io.github.sihyuuun.youthmoa.program;

import io.github.sihyuuun.youthmoa.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Getter
@Entity
@Table(name = "program")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Program extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, length = 100)
    private String organization;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(length = 50)
    private String region;

    @Column(length = 500)
    private String imageUrl;

    @Lob
    @Column(nullable = false)
    private String content;

    @Lob
    @Column(nullable = false)
    private String requirements;

    private LocalDate startDate;

    private LocalDate endDate;

    @Column(length = 500)
    private String applyUrl;

    @Column(nullable = false)
    private boolean isActive;

    @Column
    private Integer capacity;

    @Builder
    private Program(String title, String organization, String category, String region,
                    String imageUrl, String content, String requirements,
                    LocalDate startDate, LocalDate endDate, String applyUrl,
                    Boolean isActive, Integer capacity) {
        this.title = title;
        this.organization = organization;
        this.category = category;
        this.region = region;
        this.imageUrl = imageUrl;
        this.content = content;
        this.requirements = requirements;
        this.startDate = startDate;
        this.endDate = endDate;
        this.applyUrl = applyUrl;
        this.isActive = isActive != null ? isActive : true;
        this.capacity = capacity;
    }

    public void update(String title, String organization, String category, String region,
                       String imageUrl, String content, String requirements,
                       LocalDate startDate, LocalDate endDate, String applyUrl, Integer capacity) {
        this.title = title;
        this.organization = organization;
        this.category = category;
        this.region = region;
        this.imageUrl = imageUrl;
        this.content = content;
        this.requirements = requirements;
        this.startDate = startDate;
        this.endDate = endDate;
        this.applyUrl = applyUrl;
        this.capacity = capacity;
    }

    public void activate() { this.isActive = true; }
    public void deactivate() { this.isActive = false; }

    public boolean hasCapacityLimit() {
        return capacity != null;
    }

    public ProgramStatus getStatus() {
        if (!isActive) return ProgramStatus.CLOSED;
        LocalDate today = LocalDate.now();
        if (startDate != null && today.isBefore(startDate)) return ProgramStatus.UPCOMING;
        if (endDate != null && today.isAfter(endDate)) return ProgramStatus.CLOSED;
        return ProgramStatus.ACTIVE;
    }

    /** endDate까지 남은 일수. endDate가 없으면 -1, 이미 지났으면 음수. */
    public long getDaysUntilDeadline() {
        if (endDate == null) return -1;
        return ChronoUnit.DAYS.between(LocalDate.now(), endDate);
    }

    /** D-day 표시 레이블 (예: "D-3", "D-DAY", "마감") */
    public String getDdayLabel() {
        long days = getDaysUntilDeadline();
        if (days == -1) return "";
        if (days < 0) return "마감";
        if (days == 0) return "D-DAY";
        return "D-" + days;
    }
}
