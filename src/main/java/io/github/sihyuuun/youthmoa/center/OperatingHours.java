package io.github.sihyuuun.youthmoa.center;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * F0h-operating-hours-badge (spec §9-1, §9-6): 청년센터 요일별 운영시간 값 객체.
 *
 * <p>Center 에 {@code @Embedded} 로 flatten 되며, {@link #isOpenAt(LocalDateTime, boolean)} 도메인
 * 메서드가 현재 시각의 운영 여부를 계산한다.
 *
 * <p>제약 (spec §9-3): 자정 넘김 미지원. {@code close > open} 위반 시 builder 에서 fail-fast.
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OperatingHours {

  /** 평일(월~금) open. null = 평일 미운영. */
  @Column(name = "weekday_open")
  private LocalTime weekdayOpen;

  /** 평일(월~금) close. null = 평일 미운영. */
  @Column(name = "weekday_close")
  private LocalTime weekdayClose;

  /** 토요일 open. null = 토요일 미운영. */
  @Column(name = "saturday_open")
  private LocalTime saturdayOpen;

  /** 토요일 close. null = 토요일 미운영. */
  @Column(name = "saturday_close")
  private LocalTime saturdayClose;

  /** 일요일 open. null = 일요일 미운영. */
  @Column(name = "sunday_open")
  private LocalTime sundayOpen;

  /** 일요일 close. null = 일요일 미운영. */
  @Column(name = "sunday_close")
  private LocalTime sundayClose;

  /**
   * 공휴일 휴관 여부. spec §9-7: default true.
   *
   * <p>@Embedded 상 부모 Center.schedule 이 null 일 수 있어 (파싱 불가 3행) 임베드 컬럼은 nullable=true 로 둠.
   * 실제 배지 판정은 Center.hasSchedule() gate 이후에만 이 값을 읽으므로 nullable 이라도 안전.
   */
  @Column(name = "holiday_closed")
  private boolean holidayClosed;

  /**
   * spec §9-3: 자정 넘김 미지원. close &gt; open 위반 시 fail-fast. Builder 를 lombok 없이 직접 작성한 이유는
   * 검증 로직을 강제하기 위함.
   */
  private OperatingHours(
      LocalTime weekdayOpen,
      LocalTime weekdayClose,
      LocalTime saturdayOpen,
      LocalTime saturdayClose,
      LocalTime sundayOpen,
      LocalTime sundayClose,
      boolean holidayClosed) {
    validatePair("weekday", weekdayOpen, weekdayClose);
    validatePair("saturday", saturdayOpen, saturdayClose);
    validatePair("sunday", sundayOpen, sundayClose);
    this.weekdayOpen = weekdayOpen;
    this.weekdayClose = weekdayClose;
    this.saturdayOpen = saturdayOpen;
    this.saturdayClose = saturdayClose;
    this.sundayOpen = sundayOpen;
    this.sundayClose = sundayClose;
    this.holidayClosed = holidayClosed;
  }

  private static void validatePair(String label, LocalTime open, LocalTime close) {
    if (open == null && close == null) return;
    if (open == null || close == null) {
      throw new IllegalArgumentException(
          label + " open/close 는 함께 지정하거나 함께 비워야 합니다 (open=" + open + ", close=" + close + ")");
    }
    if (!close.isAfter(open)) {
      throw new IllegalArgumentException(
          label
              + " close("
              + close
              + ") 는 open("
              + open
              + ") 보다 뒤여야 합니다. 자정 넘김은 미지원 (spec §9-3).");
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private LocalTime weekdayOpen;
    private LocalTime weekdayClose;
    private LocalTime saturdayOpen;
    private LocalTime saturdayClose;
    private LocalTime sundayOpen;
    private LocalTime sundayClose;
    private boolean holidayClosed = true; // spec §9-7 default

    public Builder weekdayOpen(LocalTime v) { this.weekdayOpen = v; return this; }
    public Builder weekdayClose(LocalTime v) { this.weekdayClose = v; return this; }
    public Builder saturdayOpen(LocalTime v) { this.saturdayOpen = v; return this; }
    public Builder saturdayClose(LocalTime v) { this.saturdayClose = v; return this; }
    public Builder sundayOpen(LocalTime v) { this.sundayOpen = v; return this; }
    public Builder sundayClose(LocalTime v) { this.sundayClose = v; return this; }
    public Builder holidayClosed(boolean v) { this.holidayClosed = v; return this; }

    public OperatingHours build() {
      return new OperatingHours(
          weekdayOpen, weekdayClose,
          saturdayOpen, saturdayClose,
          sundayOpen, sundayClose,
          holidayClosed);
    }
  }

  /**
   * 주어진 시각의 운영 여부.
   *
   * <ul>
   *   <li>{@code isHoliday=true && holidayClosed=true} → false
   *   <li>해당 요일 open/close 가 null → false (미운영)
   *   <li>open inclusive, close exclusive
   * </ul>
   */
  public boolean isOpenAt(LocalDateTime now, boolean isHoliday) {
    if (now == null) return false;
    if (isHoliday && holidayClosed) return false;
    LocalDate date = now.toLocalDate();
    LocalTime time = now.toLocalTime();
    DayOfWeek day = date.getDayOfWeek();
    LocalTime open;
    LocalTime close;
    if (day == DayOfWeek.SATURDAY) {
      open = saturdayOpen;
      close = saturdayClose;
    } else if (day == DayOfWeek.SUNDAY) {
      open = sundayOpen;
      close = sundayClose;
    } else {
      open = weekdayOpen;
      close = weekdayClose;
    }
    if (open == null || close == null) return false;
    return !time.isBefore(open) && time.isBefore(close);
  }
}
