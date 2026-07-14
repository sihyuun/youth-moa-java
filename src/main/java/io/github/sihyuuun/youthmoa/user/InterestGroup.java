package io.github.sihyuuun.youthmoa.user;

import java.util.List;

/**
 * MyPage 프로필 요약 카드의 관심 정보 그룹 뷰 DTO (prototype.tsx L1236~1252 매칭).
 *
 * <p>지역·분야 각각을 그룹으로 묶어 최대 {@code MAX} 개 값칩 + `+N` 축약칩 형태로 노출. 값이 비면 "미설정" 표시 (그룹 자체는 항상 노출).
 *
 * @param icon fragments/icons.html 의 fragment 이름 (pin / star)
 * @param label 라벨 텍스트 (예: "관심 지역", "관심 분야")
 * @param shown 최대 MAX 개 표시 칩
 * @param restCount `+N` 축약칩에 표시할 수 (0 이면 축약칩 미노출)
 * @param restJoin 축약된 나머지 값들의 `, ` join (title 툴팁용)
 * @param empty 값 0개 여부 (true 시 "미설정" 표시)
 */
public record InterestGroup(
    String icon, String label, List<String> shown, int restCount, String restJoin, boolean empty) {

  public static final int MAX = 3;

  /** 그룹 생성 헬퍼 — 원본 리스트를 MAX 로 분할. */
  public static InterestGroup of(String icon, String label, List<String> values) {
    if (values == null || values.isEmpty()) {
      return new InterestGroup(icon, label, List.of(), 0, "", true);
    }
    if (values.size() <= MAX) {
      return new InterestGroup(icon, label, List.copyOf(values), 0, "", false);
    }
    List<String> shown = List.copyOf(values.subList(0, MAX));
    List<String> rest = values.subList(MAX, values.size());
    return new InterestGroup(icon, label, shown, rest.size(), String.join(", ", rest), false);
  }
}
