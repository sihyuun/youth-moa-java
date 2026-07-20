package io.github.sihyuuun.youthmoa.program;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 프로그램 명시 상태 (2026-07-20 F0f-fix-3 정책 확정).
 *
 * <ul>
 *   <li>{@link #UPCOMING} — 진행예정 (모집 시작 전)
 *   <li>{@link #OPEN} — 모집중 (모집 기간 내). "마감(isFull)" 은 파생값이며 enum 이 아님.
 *   <li>{@link #ENDED} — 종료 (기간 만료, 자연 종료)
 *   <li>{@link #SUSPENDED} — 운영중단 (관리자 강제, 복구 가능)
 * </ul>
 *
 * <p>파생값: applied ≥ capacity → isFull ("마감" UI). 90% 이상 → isClosing.
 */
@Getter
@RequiredArgsConstructor
public enum ProgramStatus {
  UPCOMING("진행예정", "status--upcoming"),
  OPEN("모집중", "status--open"),
  ENDED("종료", "status--ended"),
  SUSPENDED("운영중단", "status--suspended");

  private final String label;
  private final String cssClass;
}
