/**
 * F0f 프로그램 캘린더 뷰 client-side 상호작용 (2026-08-27, R3-4 2026-08-31).
 *
 * 담당 영역:
 *  - 셀 클릭 → 우측 패널 slide-in + 해당 날짜 카드 그룹만 표시
 *  - × 클릭 → 우측 패널 닫힘
 *  - [오늘] 클릭 → 오늘 셀 선택
 *  - pill 클릭 → 셀 클릭 이벤트 전파 차단 (버블 방지). 링크 이동은 <a href> 기본 동작
 *
 * 서버 왕복 없음 (Q4 결정). 셀별 카드 데이터는 초기 렌더에 hidden 으로 포함.
 *
 * ym-verify R3-4 대응: 이벤트 리스너를 `document.body` 로 위임하고, 핸들러 내부에서
 * 매번 DOM 을 재조회한다. `applyFiltersFromPopovers()` 가 HTMX innerHTML swap 으로
 * `.program-calendar-layout` 을 새 DOM 으로 교체해도 body 는 유지되므로 리스너 유실 없음.
 */
(function () {
  'use strict';

  function ready(fn) {
    if (document.readyState !== 'loading') fn();
    else document.addEventListener('DOMContentLoaded', fn);
  }

  function getLayout() {
    return document.querySelector('.program-calendar-layout');
  }

  function clearSelected(layout) {
    layout.querySelectorAll('.program-calendar-cell--selected')
          .forEach(function (el) { el.classList.remove('program-calendar-cell--selected'); });
  }

  // F0f PR-2 (2026-09-01): 모바일 뷰포트 감지 — 하단 시트 사용 여부 판단
  function isMobile() {
    return window.matchMedia && window.matchMedia('(max-width: 767px)').matches;
  }

  function selectDay(layout, day) {
    if (!day) return;
    var panel = document.getElementById('program-calendar-panel');
    var panelDate = document.getElementById('program-calendar-panel-date');
    var panelCount = document.getElementById('program-calendar-panel-count');
    var panelEmpty = document.getElementById('program-calendar-panel-empty');
    var groups = panel ? panel.querySelectorAll('[data-panel-day]') : [];
    var viewMonth = parseInt(layout.getAttribute('data-view-month'), 10);

    clearSelected(layout);
    var cell = layout.querySelector('.program-calendar-cell[data-day="' + day + '"][data-in-month="true"]');
    if (cell) cell.classList.add('program-calendar-cell--selected');

    // 그룹 표시 (데스크톱 패널 + 모바일 시트 공통 데이터 소스)
    var selectedGroup = null;
    var visible = 0;
    groups.forEach(function (g) {
      var gd = parseInt(g.getAttribute('data-panel-day'), 10);
      if (gd === day) {
        g.hidden = false;
        selectedGroup = g;
        visible = g.querySelectorAll('.program-calendar-panel-card').length;
      } else {
        g.hidden = true;
      }
    });

    // 데스크톱 우측 패널 헤더 갱신
    if (panelDate) panelDate.textContent = viewMonth + '월 ' + day + '일';
    if (panelCount) panelCount.textContent = '시작 ' + visible + '건';
    if (panelEmpty) panelEmpty.hidden = visible !== 0;
    if (panel) panel.hidden = false;

    // 모바일 하단 시트 open + 카드 클론
    if (isMobile()) {
      openMobileSheet(viewMonth, day, visible, selectedGroup);
    }
  }

  function openMobileSheet(viewMonth, day, visible, selectedGroup) {
    var sheet = document.getElementById('program-calendar-mobile-sheet');
    var title = document.getElementById('program-calendar-mobile-sheet-title');
    var count = document.getElementById('program-calendar-mobile-sheet-count');
    var body = document.getElementById('program-calendar-mobile-sheet-body');
    if (!sheet || !body) return;

    if (title) title.textContent = viewMonth + '월 ' + day + '일';
    if (count) count.textContent = '시작 ' + visible + '건';

    // 시트 body 초기화 후 선택 그룹의 카드 노드를 클론해서 삽입
    body.innerHTML = '';
    if (selectedGroup && visible > 0) {
      selectedGroup.querySelectorAll('.program-calendar-panel-card').forEach(function (card) {
        body.appendChild(card.cloneNode(true));
      });
    } else {
      var empty = document.createElement('div');
      empty.className = 'program-calendar-mobile-sheet-empty';
      empty.textContent = '이 날에는 프로그램이 없어요.';
      body.appendChild(empty);
    }
    sheet.hidden = false;
    document.body.style.overflow = 'hidden';
  }

  function closeMobileSheet() {
    var sheet = document.getElementById('program-calendar-mobile-sheet');
    if (sheet) sheet.hidden = true;
    document.body.style.overflow = '';
    var layout = getLayout();
    if (layout) clearSelected(layout);
  }

  function closePanel(layout) {
    var panel = document.getElementById('program-calendar-panel');
    clearSelected(layout);
    if (panel) panel.hidden = true;
    closeMobileSheet();
  }

  ready(function () {
    // body 레벨 이벤트 위임. HTMX innerHTML swap 후에도 재바인딩 불필요.
    document.body.addEventListener('click', function (e) {
      var layout = getLayout();
      if (!layout) return;
      // 클릭이 캘린더 레이아웃 밖이면 무시
      if (!layout.contains(e.target)) return;

      // pill 클릭은 링크 이동 우선 + 셀 선택 차단
      var pill = e.target.closest('[data-calendar-pill]');
      if (pill) {
        e.stopPropagation();
        return;
      }
      // 닫기 (데스크톱 패널 · 모바일 시트 backdrop/× 통합)
      if (e.target.closest('[data-calendar-close]')) {
        closePanel(layout);
        return;
      }
      if (e.target.closest('[data-calendar-sheet-close]')) {
        closeMobileSheet();
        return;
      }
      // 오늘 버튼
      if (e.target.closest('[data-calendar-today]')) {
        var viewYear = parseInt(layout.getAttribute('data-view-year'), 10);
        var viewMonth = parseInt(layout.getAttribute('data-view-month'), 10);
        var todayYear = parseInt(layout.getAttribute('data-today-year'), 10);
        var todayMonth = parseInt(layout.getAttribute('data-today-month'), 10);
        var todayDay = parseInt(layout.getAttribute('data-today-day'), 10);
        // 뷰가 오늘 월이 아니면 이동, 같으면 오늘 셀 선택
        if (viewYear === todayYear && viewMonth === todayMonth) {
          selectDay(layout, todayDay);
        } else {
          var params = new URLSearchParams(window.location.search);
          params.set('view', 'calendar');
          params.set('year', String(todayYear));
          params.set('month', String(todayMonth));
          window.location.href = '/programs?' + params.toString();
        }
        return;
      }
      var cell = e.target.closest('.program-calendar-cell[data-in-month="true"]');
      if (cell) {
        var day = parseInt(cell.getAttribute('data-day'), 10);
        if (day) selectDay(layout, day);
      }
    });
  });
})();
