package io.github.sihyuuun.youthmoa.admin.chart;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class SvgChartRendererTest {

  private final SvgChartRenderer renderer = new SvgChartRenderer();

  @Test
  void barChart_producesSvgWithRects() {
    SvgChartRenderer.SeriesData data =
        new SvgChartRenderer.SeriesData(List.of("A", "B", "C"), List.of(10L, 20L, 30L), 30L);
    String svg = renderer.createBarChart(data);
    assertThat(svg).startsWith("<svg");
    assertThat(svg).endsWith("</svg>");
    // 3개의 rect
    int rects = svg.split("<rect", -1).length - 1;
    assertThat(rects).isEqualTo(3);
  }

  @Test
  void lineChart_containsGradientAndPath() {
    SvgChartRenderer.SeriesData data =
        new SvgChartRenderer.SeriesData(
            List.of("2024", "2025", "2026"), List.of(100L, 200L, 300L), 300L);
    String svg = renderer.createLineChart(data);
    assertThat(svg).contains("linearGradient");
    assertThat(svg).contains("id=\"areaGrad\"");
    assertThat(svg).contains("<path");
  }

  @Test
  void donutChart_producesPathPerSlice() {
    List<SvgChartRenderer.DonutSlice> data =
        List.of(
            new SvgChartRenderer.DonutSlice("A", 30, "#3F30E9"),
            new SvgChartRenderer.DonutSlice("B", 60, "#8B7FF0"),
            new SvgChartRenderer.DonutSlice("C", 10, "#D6CFFA"));
    String svg = renderer.createDonutChart(data);
    int paths = svg.split("<path", -1).length - 1;
    assertThat(paths).isEqualTo(3);
    assertThat(svg).contains("#3F30E9");
    assertThat(svg).contains("#8B7FF0");
  }

  @Test
  void donutChart_emptyData_returnsPlaceholder() {
    String svg = renderer.createDonutChart(List.of());
    assertThat(svg).contains("데이터 없음");
  }

  @Test
  void barChart_emptyData_returnsPlaceholder() {
    SvgChartRenderer.SeriesData empty = new SvgChartRenderer.SeriesData(List.of(), List.of(), 0L);
    assertThat(renderer.createBarChart(empty)).contains("데이터 없음");
  }
}
