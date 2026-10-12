/**
 * D-admin-calendar (2026-10-12): 관리자 프로그램 캘린더 client-side 상호작용.
 *
 * 담당 영역:
 *  - 셀 클릭 → 우측 300px 패널 slide-in + 해당 날짜 카드 그룹만 표시
 *  - × 클릭 → 패널 닫힘
 *  - [오늘] 버튼 → 오늘 월로 이동 또는 오늘 셀 선택
 *  - pill 클릭 → 상세 이동 (버블 차단)
 *
 * read-only 뷰 (Q5): 캘린더 내 CRUD 없음. 모든 클릭은 상세 이동 또는 날짜 패널 토글 뿐.
 *
 * 사용자 program-calendar.js 와 패턴 공유. admin 네임스페이스 id 사용 (admin-program-calendar-panel).
 */
(function () {
  'use strict';

  function ready(fn) {
    if (document.readyState !== 'loading') fn();
    else document.addEventListener('DOMContentLoaded', fn);
  }

  function getLayout() {
    return document.querySelector('.admin-program-calendar-layout');
  }

  function clearSelected(layout) {
    layout.querySelectorAll('.program-calendar-cell--selected')
          .forEach(function (el) { el.classList.remove('program-calendar-cell--selected'); });
  }

  function selectDay(layout, day) {
    if (!day) return;
    var panel = document.getElementById('admin-program-calendar-panel');
    var panelDate = document.getElementById('admin-program-calendar-panel-date');
    var panelCount = document.getElementById('admin-program-calendar-panel-count');
    var panelEmpty = document.getElementById('admin-program-calendar-panel-empty');
    var groups = panel ? panel.querySelectorAll('[data-panel-day]') : [];
    var viewMonth = parseInt(layout.getAttribute('data-view-month'), 10);

    clearSelected(layout);
    var cell = layout.querySelector('.program-calendar-cell[data-day="' + day + '"][data-in-month="true"]');
    if (cell) cell.classList.add('program-calendar-cell--selected');

    var visible = 0;
    groups.forEach(function (g) {
      var gd = parseInt(g.getAttribute('data-panel-day'), 10);
      if (gd === day) {
        g.hidden = false;
        visible = g.querySelectorAll('.program-calendar-panel-card').length;
      } else {
        g.hidden = true;
      }
    });

    if (panelDate) panelDate.textContent = viewMonth + '월 ' + day + '일';
    if (panelCount) panelCount.textContent = '시작 ' + visible + '건';
    if (panelEmpty) panelEmpty.hidden = visible !== 0;
    if (panel) panel.hidden = false;
  }

  function closePanel(layout) {
    var panel = document.getElementById('admin-program-calendar-panel');
    clearSelected(layout);
    if (panel) panel.hidden = true;
  }

  ready(function () {
    document.body.addEventListener('click', function (e) {
      var layout = getLayout();
      if (!layout) return;
      if (!layout.contains(e.target) &&
          !e.target.closest('#admin-program-calendar-panel')) return;

      // pill 클릭은 상세 이동 (버블 차단)
      var pill = e.target.closest('[data-calendar-pill]');
      if (pill) {
        e.stopPropagation();
        return;
      }
      // 패널 내부 카드 클릭도 상세 이동 (버블 차단)
      var panelCard = e.target.closest('.program-calendar-panel-card');
      if (panelCard) {
        e.stopPropagation();
        return;
      }
      // 닫기
      if (e.target.closest('[data-calendar-close]')) {
        closePanel(layout);
        return;
      }
      // 오늘 버튼
      if (e.target.closest('[data-calendar-today]')) {
        var viewYear = parseInt(layout.getAttribute('data-view-year'), 10);
        var viewMonth = parseInt(layout.getAttribute('data-view-month'), 10);
        var todayYear = parseInt(layout.getAttribute('data-today-year'), 10);
        var todayMonth = parseInt(layout.getAttribute('data-today-month'), 10);
        var todayDay = parseInt(layout.getAttribute('data-today-day'), 10);
        if (viewYear === todayYear && viewMonth === todayMonth) {
          selectDay(layout, todayDay);
        } else {
          var params = new URLSearchParams(window.location.search);
          params.set('view', 'calendar');
          params.set('year', String(todayYear));
          params.set('month', String(todayMonth));
          window.location.href = '/admin/programs?' + params.toString();
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
