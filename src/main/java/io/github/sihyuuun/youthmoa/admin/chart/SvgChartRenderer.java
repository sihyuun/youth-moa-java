package io.github.sihyuuun.youthmoa.admin.chart;

import java.util.List;
import org.springframework.stereotype.Component;

/**
 * A6 (2026-09-16 신설) — 서버 렌더 SVG 차트 생성기.
 *
 * <p>prototype {@code createBarChart} (L3406~) / {@code createDonutChart} (L3494~) 를 Java 로 이식. 외부
 * CDN·라이브러리 없음. Thymeleaf {@code th:utext} 로 문자열 그대로 삽입한다.
 *
 * <p>학습 목적상 브라우저 hover 툴팁 등 클라 상태는 제외 (deferred: A8-polish).
 */
@Component
public class SvgChartRenderer {

  // ── 색상 팔레트 (design-token 정합 · Qn-5) ───────────────────────
  public static final String COLOR_PRIMARY = "#3F30E9";
  public static final String COLOR_PRIMARY_MUTE = "#C7BFF5";
  public static final String COLOR_SECONDARY = "#8B7FF0";
  public static final String COLOR_LIGHT = "#D6CFFA";
  public static final String COLOR_MUTED = "#E3E1E8";
  public static final String COLOR_GRID = "#F0EFF3";
  public static final String COLOR_TICK = "#C9C6D3";
  public static final String COLOR_LABEL = "#A6A3B3";

  // ── 도넛 팔레트 (성별 3 · 연령 5+1) ─────────────────────────────
  public static final List<String> DONUT_PALETTE =
      List.of(
          COLOR_PRIMARY, COLOR_SECONDARY, COLOR_LIGHT, COLOR_PRIMARY_MUTE, "#B4A7F5", COLOR_MUTED);

  /** 도넛 세그먼트 (라벨 · 값 · 색상). */
  public record DonutSlice(String label, long value, String color) {}

  /** 라인·바 차트 데이터 (labels 와 values 길이 동일). */
  public record SeriesData(List<String> labels, List<Long> values, long maxValue) {}

  // ─────────────────────────────────────────────────────────────
  //   Bar chart (월별)
  // ─────────────────────────────────────────────────────────────

  /** prototype L3457~3491 이식. viewBox 900x220. */
  public String createBarChart(SeriesData data) {
    final int W = 900, H = 220, padL = 42, padB = 28, padT = 12, padR = 8;
    final int cW = W - padL - padR;
    final int cH = H - padT - padB;
    int n = data.labels().size();
    if (n == 0) {
      return emptyChart(W, H, "데이터 없음");
    }
    long maxVal = Math.max(1, data.maxValue());
    double slot = cW / (double) n;
    double barW = Math.floor(slot * 0.58);

    StringBuilder sb = new StringBuilder(2048);
    sb.append("<svg width=\"100%\" height=\"100%\" viewBox=\"0 0 ")
        .append(W)
        .append(' ')
        .append(H)
        .append(
            "\" preserveAspectRatio=\"xMidYMid meet\" style=\"display:block\" xmlns=\"http://www.w3.org/2000/svg\">");

    // y-축 tick 4개 (0, 1/3, 2/3, max)
    long[] yTicks = niceTicks(maxVal);
    for (long v : yTicks) {
      double y = padT + cH - (v / (double) maxVal) * cH;
      sb.append("<g><line x1=\"")
          .append(padL)
          .append("\" y1=\"")
          .append(fmt(y))
          .append("\" x2=\"")
          .append(W - padR)
          .append("\" y2=\"")
          .append(fmt(y))
          .append("\" stroke=\"")
          .append(COLOR_GRID)
          .append("\" stroke-width=\"1\"/>");
      sb.append("<text x=\"")
          .append(padL - 4)
          .append("\" y=\"")
          .append(fmt(y + 4))
          .append("\" text-anchor=\"end\" font-size=\"10\" fill=\"")
          .append(COLOR_TICK)
          .append("\" font-family=\"Inter,sans-serif\">")
          .append(formatCompact(v))
          .append("</text></g>");
    }
    // bars + x-label
    for (int i = 0; i < n; i++) {
      long v = data.values().get(i);
      double bH = (v / (double) maxVal) * cH;
      double x = padL + i * slot + (slot - barW) / 2.0;
      double y = padT + cH - bH;
      // 최근 3개는 primary, 나머지는 mute
      String fill = i >= n - 3 ? COLOR_PRIMARY : COLOR_PRIMARY_MUTE;
      sb.append("<rect x=\"")
          .append(fmt(x))
          .append("\" y=\"")
          .append(fmt(y))
          .append("\" width=\"")
          .append(fmt(barW))
          .append("\" height=\"")
          .append(fmt(bH))
          .append("\" fill=\"")
          .append(fill)
          .append("\" rx=\"3\"/>");
      sb.append("<text x=\"")
          .append(fmt(x + barW / 2.0))
          .append("\" y=\"")
          .append(H - 4)
          .append("\" text-anchor=\"middle\" font-size=\"9\" fill=\"")
          .append(COLOR_LABEL)
          .append("\" font-family=\"Inter,sans-serif\">")
          .append(escape(data.labels().get(i)))
          .append("</text>");
    }
    sb.append("</svg>");
    return sb.toString();
  }

  // ─────────────────────────────────────────────────────────────
  //   Line chart (연도별 · 30일 방문자 등)
  // ─────────────────────────────────────────────────────────────

  /** prototype L3410~3453 이식. area gradient 포함. */
  public String createLineChart(SeriesData data) {
    final int W = 900, H = 220, padL = 42, padB = 28, padT = 12, padR = 8;
    final int cW = W - padL - padR;
    final int cH = H - padT - padB;
    int n = data.labels().size();
    if (n == 0) {
      return emptyChart(W, H, "데이터 없음");
    }
    long maxVal = Math.max(1, data.maxValue());
    double slot = cW / (double) n;

    StringBuilder sb = new StringBuilder(2048);
    sb.append("<svg width=\"100%\" height=\"100%\" viewBox=\"0 0 ")
        .append(W)
        .append(' ')
        .append(H)
        .append(
            "\" preserveAspectRatio=\"xMidYMid meet\" style=\"display:block\" xmlns=\"http://www.w3.org/2000/svg\">");

    // defs: linearGradient
    sb.append("<defs><linearGradient id=\"areaGrad\" x1=\"0\" y1=\"0\" x2=\"0\" y2=\"1\">")
        .append("<stop offset=\"0%\" stop-color=\"")
        .append(COLOR_PRIMARY)
        .append("\" stop-opacity=\"0.4\"/>")
        .append("<stop offset=\"100%\" stop-color=\"")
        .append(COLOR_PRIMARY)
        .append("\" stop-opacity=\"0\"/>")
        .append("</linearGradient></defs>");

    long[] yTicks = niceTicks(maxVal);
    for (long v : yTicks) {
      double y = padT + cH - (v / (double) maxVal) * cH;
      sb.append("<line x1=\"")
          .append(padL)
          .append("\" y1=\"")
          .append(fmt(y))
          .append("\" x2=\"")
          .append(W - padR)
          .append("\" y2=\"")
          .append(fmt(y))
          .append("\" stroke=\"")
          .append(COLOR_GRID)
          .append("\" stroke-width=\"1\"/>");
      sb.append("<text x=\"")
          .append(padL - 4)
          .append("\" y=\"")
          .append(fmt(y + 4))
          .append("\" text-anchor=\"end\" font-size=\"10\" fill=\"")
          .append(COLOR_TICK)
          .append("\" font-family=\"Inter,sans-serif\">")
          .append(formatCompact(v))
          .append("</text>");
    }

    // area + line path
    StringBuilder areaD = new StringBuilder();
    StringBuilder lineD = new StringBuilder();
    for (int i = 0; i < n; i++) {
      double cx = padL + i * slot + slot / 2.0;
      double cy = padT + cH - (data.values().get(i) / (double) maxVal) * cH;
      if (i == 0) {
        lineD.append('M').append(fmt(cx)).append(',').append(fmt(cy));
        areaD.append('M').append(fmt(cx)).append(',').append(fmt(cy));
      } else {
        lineD.append(" L").append(fmt(cx)).append(',').append(fmt(cy));
        areaD.append(" L").append(fmt(cx)).append(',').append(fmt(cy));
      }
    }
    double lastCx = padL + (n - 1) * slot + slot / 2.0;
    double firstCx = padL + slot / 2.0;
    areaD.append(" L").append(fmt(lastCx)).append(',').append(padT + cH);
    areaD.append(" L").append(fmt(firstCx)).append(',').append(padT + cH).append(" Z");

    sb.append("<path d=\"").append(areaD).append("\" fill=\"url(#areaGrad)\" opacity=\"0.3\"/>");
    sb.append("<path d=\"")
        .append(lineD)
        .append("\" fill=\"none\" stroke=\"")
        .append(COLOR_PRIMARY)
        .append("\" stroke-width=\"2.5\" stroke-linecap=\"round\" stroke-linejoin=\"round\"/>");

    // dots + x-label
    for (int i = 0; i < n; i++) {
      double cx = padL + i * slot + slot / 2.0;
      double cy = padT + cH - (data.values().get(i) / (double) maxVal) * cH;
      int r = (i == n - 1) ? 4 : 3;
      String fill = (i == n - 1) ? COLOR_PRIMARY : "white";
      sb.append("<circle cx=\"")
          .append(fmt(cx))
          .append("\" cy=\"")
          .append(fmt(cy))
          .append("\" r=\"")
          .append(r)
          .append("\" fill=\"")
          .append(fill)
          .append("\" stroke=\"")
          .append(COLOR_PRIMARY)
          .append("\" stroke-width=\"2\"/>");
      sb.append("<text x=\"")
          .append(fmt(cx))
          .append("\" y=\"")
          .append(H - 4)
          .append("\" text-anchor=\"middle\" font-size=\"9\" fill=\"")
          .append(COLOR_LABEL)
          .append("\" font-family=\"Inter,sans-serif\">")
          .append(escape(data.labels().get(i)))
          .append("</text>");
    }
    sb.append("</svg>");
    return sb.toString();
  }

  // ─────────────────────────────────────────────────────────────
  //   Donut chart
  // ─────────────────────────────────────────────────────────────

  /** prototype L3494~3511 이식. */
  public String createDonutChart(List<DonutSlice> data) {
    return createDonutChart(data, 130, 54, 36);
  }

  public String createDonutChart(List<DonutSlice> data, int size, int outerR, int innerR) {
    long total = data.stream().mapToLong(DonutSlice::value).sum();
    if (total <= 0) {
      return emptyChart(size, size, "데이터 없음");
    }
    double cx = size / 2.0;
    double cy = size / 2.0;
    double start = -Math.PI / 2.0;

    StringBuilder sb = new StringBuilder(1024);
    sb.append("<svg width=\"")
        .append(size)
        .append("\" height=\"")
        .append(size)
        .append("\" viewBox=\"0 0 ")
        .append(size)
        .append(' ')
        .append(size)
        .append("\" style=\"display:block;flex-shrink:0\" xmlns=\"http://www.w3.org/2000/svg\">");
    for (DonutSlice s : data) {
      if (s.value() <= 0) continue;
      double angle = (s.value() / (double) total) * 2 * Math.PI;
      double end = start + angle - 0.02;
      double x1 = cx + outerR * Math.cos(start), y1 = cy + outerR * Math.sin(start);
      double x2 = cx + outerR * Math.cos(end), y2 = cy + outerR * Math.sin(end);
      double ix1 = cx + innerR * Math.cos(start), iy1 = cy + innerR * Math.sin(start);
      double ix2 = cx + innerR * Math.cos(end), iy2 = cy + innerR * Math.sin(end);
      int large = angle > Math.PI ? 1 : 0;
      String path =
          "M" + fmt(x1) + "," + fmt(y1) + " A" + outerR + "," + outerR + ",0," + large + ",1,"
              + fmt(x2) + "," + fmt(y2) + " L" + fmt(ix2) + "," + fmt(iy2) + " A" + innerR + ","
              + innerR + ",0," + large + ",0," + fmt(ix1) + "," + fmt(iy1) + " Z";
      sb.append("<path d=\"").append(path).append("\" fill=\"").append(s.color()).append("\"/>");
      start = end + 0.02;
    }
    sb.append("</svg>");
    return sb.toString();
  }

  // ── helper ──────────────────────────────────────────────────
  private String emptyChart(int w, int h, String message) {
    return "<svg width=\"100%\" height=\"100%\" viewBox=\"0 0 "
        + w
        + " "
        + h
        + "\" preserveAspectRatio=\"xMidYMid meet\" style=\"display:block\" xmlns=\"http://www.w3.org/2000/svg\">"
        + "<text x=\""
        + (w / 2)
        + "\" y=\""
        + (h / 2)
        + "\" text-anchor=\"middle\" font-size=\"12\" fill=\""
        + COLOR_LABEL
        + "\">"
        + escape(message)
        + "</text></svg>";
  }

  private static long[] niceTicks(long maxVal) {
    long step = Math.max(1, maxVal / 3);
    return new long[] {0, step, step * 2, step * 3};
  }

  private static String formatCompact(long v) {
    if (v >= 1000) {
      double k = v / 1000.0;
      return (k == Math.floor(k)) ? ((long) k) + "k" : String.format("%.1fk", k);
    }
    return String.valueOf(v);
  }

  private static String fmt(double v) {
    return String.format("%.2f", v);
  }

  private static String escape(String s) {
    if (s == null) return "";
    return s.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;");
  }
}
