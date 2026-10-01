package io.github.sihyuuun.youthmoa.admin;

import java.util.List;

/**
 * A8-search (2026-10-01) — 관리자 헤더 글로벌 검색 드롭다운 결과 DTO.
 *
 * <p>프로그램 최대 4건 + 사용자 최대 3건. HTMX fragment 렌더에만 사용 (JSON 노출 X).
 *
 * <p>record 의 불변성 + 명시적 필드 접근자로 Thymeleaf 표현식 안정성 확보.
 *
 * <p>Q4 결정 (사용자 /search 와 분리): 공지 미포함. 센터 미포함.
 */
public record AdminSearchResult(List<ProgramHit> programs, List<UserHit> users) {

  public boolean isEmpty() {
    return (programs == null || programs.isEmpty()) && (users == null || users.isEmpty());
  }

  /**
   * 프로그램 히트. centerName 은 소속 센터 라벨 (서브텍스트 표시용).
   *
   * <p>Program.center 는 NOT NULL (A9-b) 이지만 fetch 시 LAZY 로딩 사고를 피하기 위해 서비스 layer 에서 명시 추출.
   */
  public record ProgramHit(Long id, String title, String centerName) {}

  /** 사용자 히트. email 은 서브텍스트 표시용 — PII 노출 범위는 admin 세션 안쪽으로 제한된다. */
  public record UserHit(Long id, String name, String email) {}
}
