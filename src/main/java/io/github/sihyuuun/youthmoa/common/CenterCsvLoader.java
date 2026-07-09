package io.github.sihyuuun.youthmoa.common;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
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
    "name", "region", "address", "latitude", "longitude", "phone", "operatingHours", "isActive"
  };
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
    return new CenterCsvRow(name, region, address, lat, lng, phone, operatingHours, isActive);
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
