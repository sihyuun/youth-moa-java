package io.github.sihyuuun.youthmoa.notice;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 공지사항 카테고리. prototype.tsx §5.13 의 5개 pill (전체/행사/공지/운영/기타) 중 "전체"는 필터 미적용을 의미하므로 enum 값에서 제외. */
@Getter
@RequiredArgsConstructor
public enum NoticeCategory {
  EVENT("행사", "cat-event"),
  NOTICE("공지", "cat-notice"),
  OPERATION("운영", "cat-operation"),
  ETC("기타", "cat-etc");

  private final String label;
  private final String cssClass;

  /** 라벨 → enum 변환. 시드/legacy tag 문자열 매핑용. */
  public static NoticeCategory fromLabel(String label) {
    for (NoticeCategory c : values()) {
      if (c.label.equals(label)) return c;
    }
    return ETC;
  }
}
