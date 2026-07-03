package io.github.sihyuuun.youthmoa.program;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProgramStatus {
  UPCOMING("진행예정", "status--upcoming"),
  ACTIVE("진행중", "status--active"),
  CLOSED("마감", "status--closed");

  private final String label;
  private final String cssClass;
}
