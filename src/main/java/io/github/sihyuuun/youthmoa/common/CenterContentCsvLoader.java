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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * F0h-center-desc-image: {@code classpath:/data/centers-content.csv} 로더.
 *
 * <p>파생 시드(imagePool·descByKeyword·featuredDesc) 제거의 근본 대체. {@link CenterCsvLoader} 와 동일 파싱
 * 패턴(opencsv, BOM 스킵, fail-fast) 을 재사용한다.
 */
@Slf4j
@Component
public class CenterContentCsvLoader {

  private static final String RESOURCE_PATH = "data/centers-content.csv";
  private static final String[] EXPECTED_HEADERS = {"name", "description", "imageUrl"};

  public List<CenterContentCsvRow> load() {
    ClassPathResource resource = new ClassPathResource(RESOURCE_PATH);
    if (!resource.exists()) {
      throw new IllegalStateException("centers-content.csv not found in classpath:/data/");
    }

    List<CenterContentCsvRow> rows = new ArrayList<>();
    try (InputStream in = resource.getInputStream();
        Reader raw = new InputStreamReader(in, StandardCharsets.UTF_8);
        Reader reader = stripBom(raw);
        CSVReader csv = new CSVReaderBuilder(reader).build()) {

      String[] header = csv.readNext();
      if (header == null) {
        throw new IllegalStateException("centers-content.csv 는 헤더가 비어 있습니다.");
      }
      validateHeader(header);

      String[] cols;
      int lineNumber = 1;
      while ((cols = csv.readNext()) != null) {
        lineNumber++;
        if (cols.length == 1 && cols[0].isBlank()) {
          continue;
        }
        if (cols.length < EXPECTED_HEADERS.length) {
          throw new IllegalStateException(
              "centers-content.csv line "
                  + lineNumber
                  + ": expected "
                  + EXPECTED_HEADERS.length
                  + " columns, got "
                  + cols.length
                  + " — "
                  + Arrays.toString(cols));
        }
        rows.add(new CenterContentCsvRow(cols[0].trim(), cols[1].trim(), cols[2].trim()));
      }
    } catch (IOException | CsvValidationException e) {
      throw new IllegalStateException("centers-content.csv 읽기 실패", e);
    }

    log.info("Loaded {} center contents from {}", rows.size(), RESOURCE_PATH);
    return rows;
  }

  private void validateHeader(String[] header) {
    String[] trimmed = new String[header.length];
    for (int i = 0; i < header.length; i++) trimmed[i] = header[i].trim();
    if (!Arrays.equals(trimmed, EXPECTED_HEADERS)) {
      throw new IllegalStateException(
          "centers-content.csv 헤더 불일치. 기대="
              + Arrays.toString(EXPECTED_HEADERS)
              + ", 실제="
              + Arrays.toString(trimmed));
    }
  }

  private Reader stripBom(Reader raw) throws IOException {
    PushbackReader pb = new PushbackReader(new BufferedReader(raw), 1);
    int first = pb.read();
    if (first != -1 && first != 0xFEFF) {
      pb.unread(first);
    }
    return pb;
  }
}
