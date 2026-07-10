package io.github.sihyuuun.youthmoa.common;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;
import io.github.sihyuuun.youthmoa.center.OperatingHours;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * F0h-real-coords: {@code classpath:/data/centers.csv} 로더.
 *
 * <p>파생 시드(regionCoords + offset) 제거의 근본 대체. RFC 4180 파싱은 opencsv 5.9 에 위임한다.
 *
 * <p>정책:
 *
 * <ul>
 *   <li>파일 부재 · 헤더 불일치 · 컬럼 개수 미달 · 좌표 파싱 실패 · isActive 파싱 실패 → fail-fast
 *       ({@link IllegalStateException}) — spec §9-1, §9-7
 *   <li>좌표 범위(lat 33~39 / lng 124~132) 벗어남 → warn 로그만 (spec §9-2 유연성 원칙)
 * </ul>
 */
@Slf4j
@Component
public class CenterCsvLoader {

  private static final String RESOURCE_PATH = "data/centers.csv";
  private static final String[] EXPECTED_HEADERS = {
    "name",
    "region",
    "address",
    "latitude",
    "longitude",
    "phone",
    "operatingHours",
    "isActive",
    "weekdayOpen",
    "weekdayClose",
    "saturdayOpen",
    "saturdayClose",
    "sundayOpen",
    "sundayClose",
    "holidayClosed"
  };
  private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
  private static final BigDecimal LAT_MIN = new BigDecimal("33");
  private static final BigDecimal LAT_MAX = new BigDecimal("39");
  private static final BigDecimal LNG_MIN = new BigDecimal("124");
  private static final BigDecimal LNG_MAX = new BigDecimal("132");

  public List<CenterCsvRow> load() {
    ClassPathResource resource = new ClassPathResource(RESOURCE_PATH);
    if (!resource.exists()) {
      throw new IllegalStateException("centers.csv not found in classpath:/data/");
    }

    List<CenterCsvRow> rows = new ArrayList<>();
    try (InputStream in = resource.getInputStream();
        Reader raw = new InputStreamReader(in, StandardCharsets.UTF_8);
        Reader reader = stripBom(raw);
        CSVReader csv = new CSVReaderBuilder(reader).build()) {

      String[] header = csv.readNext();
      if (header == null) {
        throw new IllegalStateException("centers.csv 는 헤더가 비어 있습니다.");
      }
      validateHeader(header);

      String[] cols;
      int lineNumber = 1; // header 는 1행
      while ((cols = csv.readNext()) != null) {
        lineNumber++;
        if (cols.length == 1 && cols[0].isBlank()) {
          continue; // 빈 줄 skip
        }
        if (cols.length < EXPECTED_HEADERS.length) {
          throw new IllegalStateException(
              "centers.csv line "
                  + lineNumber
                  + ": expected "
                  + EXPECTED_HEADERS.length
                  + " columns, got "
                  + cols.length
                  + " — "
                  + Arrays.toString(cols));
        }
        rows.add(parseRow(cols, lineNumber));
      }
    } catch (IOException | CsvValidationException e) {
      throw new IllegalStateException("centers.csv 읽기 실패", e);
    }

    log.info("Loaded {} centers from {}", rows.size(), RESOURCE_PATH);
    return rows;
  }

  private void validateHeader(String[] header) {
    String[] trimmed = new String[header.length];
    for (int i = 0; i < header.length; i++) trimmed[i] = header[i].trim();
    if (!Arrays.equals(trimmed, EXPECTED_HEADERS)) {
      throw new IllegalStateException(
          "centers.csv 헤더 불일치. 기대="
              + Arrays.toString(EXPECTED_HEADERS)
              + ", 실제="
              + Arrays.toString(trimmed));
    }
  }

  private CenterCsvRow parseRow(String[] cols, int lineNumber) {
    String name = cols[0].trim();
    String region = cols[1].trim();
    String address = cols[2].trim();
    BigDecimal lat = parseCoordinate(cols[3], "latitude", lineNumber);
    BigDecimal lng = parseCoordinate(cols[4], "longitude", lineNumber);
    warnIfOutOfRange(lat, lng, name, lineNumber);
    String phone = cols[5].trim();
    String operatingHours = cols[6].trim();
    boolean isActive = parseIsActive(cols[7], lineNumber);
    OperatingHours schedule = parseSchedule(cols, lineNumber);
    return new CenterCsvRow(
        name, region, address, lat, lng, phone, operatingHours, isActive, schedule);
  }

  /**
   * F0h-operating-hours-badge (spec §9-4): CSV 9~15컬럼 → {@link OperatingHours} 파싱.
   *
   * <p>파싱 불가 케이스 (weekday open/close 둘 다 빈값) → {@code null} 반환. spec §9-2 안전 default 로 배지 미표시.
   *
   * <p>hour 필드 fail-fast: 잘못된 시간 포맷 (예: "10.00") 은 IllegalStateException.
   */
  private OperatingHours parseSchedule(String[] cols, int lineNumber) {
    LocalTime weekdayOpen = parseTime(cols[8], "weekdayOpen", lineNumber);
    LocalTime weekdayClose = parseTime(cols[9], "weekdayClose", lineNumber);
    // spec §9-2: 파싱 불가 3행 (weekday 컬럼 모두 빈 값) → schedule 자체를 null.
    if (weekdayOpen == null && weekdayClose == null) {
      return null;
    }
    LocalTime saturdayOpen = parseTime(cols[10], "saturdayOpen", lineNumber);
    LocalTime saturdayClose = parseTime(cols[11], "saturdayClose", lineNumber);
    LocalTime sundayOpen = parseTime(cols[12], "sundayOpen", lineNumber);
    LocalTime sundayClose = parseTime(cols[13], "sundayClose", lineNumber);
    boolean holidayClosed = parseHolidayClosed(cols[14], lineNumber);
    try {
      return OperatingHours.builder()
          .weekdayOpen(weekdayOpen)
          .weekdayClose(weekdayClose)
          .saturdayOpen(saturdayOpen)
          .saturdayClose(saturdayClose)
          .sundayOpen(sundayOpen)
          .sundayClose(sundayClose)
          .holidayClosed(holidayClosed)
          .build();
    } catch (IllegalArgumentException e) {
      throw new IllegalStateException(
          "centers.csv line " + lineNumber + ": OperatingHours 검증 실패 — " + e.getMessage(), e);
    }
  }

  private LocalTime parseTime(String raw, String field, int lineNumber) {
    if (raw == null) return null;
    String v = raw.trim();
    if (v.isEmpty()) return null;
    try {
      return LocalTime.parse(v, TIME_FMT);
    } catch (DateTimeParseException e) {
      throw new IllegalStateException(
          "centers.csv line "
              + lineNumber
              + ": "
              + field
              + " 파싱 실패 — '"
              + raw
              + "' (HH:mm 포맷 필요)",
          e);
    }
  }

  /**
   * holidayClosed 파싱. 빈 값 → spec §9-7 default {@code true}. true/false 외 값은 fail-fast.
   */
  private boolean parseHolidayClosed(String raw, int lineNumber) {
    String v = raw == null ? "" : raw.trim();
    if (v.isEmpty()) return true; // spec §9-7 default
    if (v.equalsIgnoreCase("true")) return true;
    if (v.equalsIgnoreCase("false")) return false;
    throw new IllegalStateException(
        "centers.csv line "
            + lineNumber
            + ": holidayClosed 값은 true/false/빈값 만 허용 — '"
            + raw
            + "'");
  }

  private BigDecimal parseCoordinate(String raw, String field, int lineNumber) {
    try {
      return new BigDecimal(raw.trim());
    } catch (NumberFormatException e) {
      throw new IllegalStateException(
          "centers.csv line " + lineNumber + ": " + field + " 파싱 실패 — '" + raw + "'", e);
    }
  }

  private void warnIfOutOfRange(BigDecimal lat, BigDecimal lng, String name, int lineNumber) {
    if (lat.compareTo(LAT_MIN) < 0 || lat.compareTo(LAT_MAX) > 0) {
      log.warn(
          "centers.csv line {} ({}): latitude {} 가 한반도 범위(33~39) 밖", lineNumber, name, lat);
    }
    if (lng.compareTo(LNG_MIN) < 0 || lng.compareTo(LNG_MAX) > 0) {
      log.warn(
          "centers.csv line {} ({}): longitude {} 가 한반도 범위(124~132) 밖", lineNumber, name, lng);
    }
  }

  private boolean parseIsActive(String raw, int lineNumber) {
    String v = raw == null ? "" : raw.trim();
    if (v.equalsIgnoreCase("true")) return true;
    if (v.equalsIgnoreCase("false")) return false;
    throw new IllegalStateException(
        "centers.csv line " + lineNumber + ": isActive 값은 true/false 만 허용 — '" + raw + "'");
  }

  /** UTF-8 BOM(U+FEFF) 방어. 첫 문자만 검사 후 소진, 아니면 pushback. */
  private Reader stripBom(Reader raw) throws IOException {
    PushbackReader pb = new PushbackReader(new BufferedReader(raw), 1);
    int first = pb.read();
    if (first != -1 && first != 0xFEFF) {
      pb.unread(first);
    }
    return pb;
  }
}
