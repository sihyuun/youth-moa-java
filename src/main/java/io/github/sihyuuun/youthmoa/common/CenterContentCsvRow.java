package io.github.sihyuuun.youthmoa.common;

/**
 * F0h-center-desc-image: {@code classpath:/data/centers-content.csv} 한 행 스냅샷.
 *
 * <p>name 은 {@code centers.csv} 와 정확히 일치해야 Center 엔티티에 매핑된다. 매칭 실패는 fail 아닌 warn 로그 (spec §9).
 */
public record CenterContentCsvRow(String name, String description, String imageUrl) {}
