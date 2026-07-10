/**
 * F0h — 청년센터 3-column 인터랙션.
 *
 * - 커스텀 지역 드롭다운 (검색 + 하이라이트 + 클릭 시 URL 반영)
 * - 정렬 pill (name / programs) → hidden input 세팅 후 form submit
 * - 운영중 토글 스위치 → change 시 form submit
 * - 카카오맵: 커스텀 오버레이 마커 + 인포윈도우 + "이 지역에서 검색" (dirty flag)
 * - 카드 hover 지도 동기화
 *
 * SDK 미로드(appkey 미설정 or 실패) 시 지도 관련 로직만 skip, 필터 UX 는 정상 동작.
 */
(function () {
  'use strict';

  // FAIL-1 fix: centers-detail.js 로부터 상세 open/close 이벤트를 수신해 마커 selected 상태 동기화.
  // 모듈 스코프 selectMarker/clearSelection 참조를 위해 initListMap 내부에서 위 함수를 module-scope 로 승격.
  var _selectMarker = null;
  var _clearSelection = null;
  var _applyOverlayFilter = null;
  document.addEventListener('centers:detail-open', function (e) {
    if (_selectMarker && e.detail && e.detail.centerId) {
      _selectMarker(String(e.detail.centerId));
    }
  });
  document.addEventListener('centers:detail-close', function () {
    if (_clearSelection) _clearSelection();
  });
  // 2026-07-10 필터 partial swap: centers-detail.js 가 form submit 을 intercept 하고
  // 여기로 visible ID 리스트를 전달. 지도는 풀 리로드 없이 overlay setMap toggle 만 수행.
  document.addEventListener('centers:filter-changed', function (e) {
    if (_applyOverlayFilter && e.detail && Array.isArray(e.detail.visibleIds)) {
      _applyOverlayFilter(e.detail.visibleIds);
    }
  });

  // 전역 공통 toast 헬퍼 (프로젝트 최초 도입). 인포윈도우 공유 버튼 외에도 다른 화면에서 재사용 가능.
  // 사용법: window.showToast('메시지', 'error'|undefined, 3000)
  if (!window.showToast) {
    window.showToast = function (message, variant, duration) {
      var stack = document.querySelector('.toast-stack');
      if (!stack) {
        stack = document.createElement('div');
        stack.className = 'toast-stack';
        document.body.appendChild(stack);
      }
      var toast = document.createElement('div');
      toast.className = 'toast' + (variant === 'error' ? ' toast--error' : '');
      toast.textContent = message;
      stack.appendChild(toast);
      // reflow → transition 발동
      requestAnimationFrame(function () { toast.classList.add('is-visible'); });
      var ms = typeof duration === 'number' ? duration : 2000;
      setTimeout(function () {
        toast.classList.remove('is-visible');
        setTimeout(function () { toast.remove(); }, 250);
      }, ms);
    };
  }

  function ready(fn) {
    if (document.readyState !== 'loading') fn();
    else document.addEventListener('DOMContentLoaded', fn);
  }

  function escapeHtml(s) {
    return String(s == null ? '' : s)
      .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
  }

  function getForm() {
    return document.querySelector('.centers-filter-form');
  }

  function setUrlParamAndSubmit(name, value) {
    var form = getForm();
    if (!form) return;
    // remove existing extra hidden of same name
    var existing = form.querySelectorAll('input[type="hidden"][name="' + name + '"]');
    for (var i = 0; i < existing.length; i++) existing[i].remove();
    if (value !== null && value !== undefined && value !== '') {
      var input = document.createElement('input');
      input.type = 'hidden';
      input.name = name;
      input.value = value;
      form.appendChild(input);
    }
    form.requestSubmit();
  }

  // ── 커스텀 지역 드롭다운 ─────────────────────────
  function initRegionDropdown() {
    var root = document.querySelector('[data-region-dropdown]');
    if (!root) return;
    var trigger = root.querySelector('[data-region-trigger]');
    var panel = root.querySelector('[data-region-panel]');
    var searchInput = root.querySelector('[data-region-search]');
    var list = root.querySelector('[data-region-list]');
    var clearBtn = root.querySelector('[data-region-clear]');

    // × 클리어 (트리거 안쪽) — 상위 클릭 전파 막기
    if (clearBtn) {
      clearBtn.addEventListener('click', function (e) {
        e.stopPropagation();
        setUrlParamAndSubmit('region', '');
      });
    }

    trigger.addEventListener('click', function (e) {
      // × 영역 클릭이면 위에서 처리됨
      if (e.target && e.target.hasAttribute('data-region-clear')) return;
      var isOpen = !panel.hasAttribute('hidden');
      if (isOpen) {
        panel.setAttribute('hidden', '');
      } else {
        panel.removeAttribute('hidden');
        if (searchInput) {
          searchInput.value = '';
          searchInput.focus();
          filterRegionList('');
        }
      }
    });

    document.addEventListener('click', function (e) {
      if (!root.contains(e.target)) {
        panel.setAttribute('hidden', '');
      }
    });

    if (searchInput) {
      searchInput.addEventListener('input', function () {
        filterRegionList(searchInput.value);
      });
    }

    list.addEventListener('click', function (e) {
      var item = e.target.closest('.centers-filter-region-item');
      if (!item) return;
      var region = item.getAttribute('data-region');
      setUrlParamAndSubmit('region', region);
    });

    function filterRegionList(query) {
      var q = (query || '').trim().toLowerCase();
      var items = list.querySelectorAll('.centers-filter-region-item');
      items.forEach(function (item) {
        var raw = item.getAttribute('data-region') || '';
        var lower = raw.toLowerCase();
        if (!q) {
          item.style.display = '';
          item.innerHTML = escapeHtml(raw);
          return;
        }
        var idx = lower.indexOf(q);
        if (idx < 0) {
          item.style.display = 'none';
        } else {
          item.style.display = '';
          var before = escapeHtml(raw.substring(0, idx));
          var match = escapeHtml(raw.substring(idx, idx + q.length));
          var after = escapeHtml(raw.substring(idx + q.length));
          item.innerHTML = before + '<mark>' + match + '</mark>' + after;
        }
      });
    }
  }

  // ── 정렬 pill ─────────────────────────
  function initSortPill() {
    var pills = document.querySelectorAll('.centers-sort-pill-item');
    pills.forEach(function (btn) {
      btn.addEventListener('click', function () {
        var sort = btn.getAttribute('data-sort');
        setUrlParamAndSubmit('sort', sort);
      });
    });
  }

  // ── 운영중 토글 스위치 ─────────────────────────
  function initOnlyActiveToggle() {
    var input = document.querySelector('[data-only-active-toggle]');
    if (!input) return;
    input.addEventListener('change', function () {
      // native form submit — checked 여부에 따라 hidden name=onlyActive 를 form 이 자동 전송
      var form = getForm();
      if (form) form.requestSubmit();
    });
  }

  // ── 카카오맵 ─────────────────────────
  // FAIL-3 fix: map instance 를 module-scope 로 캐시. HTMX afterSwap 시 리스트 카드만 재바인딩하고
  // 이미 로드된 map / 마커는 재사용 (map re-init 은 SDK 재요청·bounds 재계산 비용 큼).
  var _mapInitialized = false;
  function initMap() {
    var mapEl = document.getElementById('center-map');
    if (!mapEl) return;
    if (typeof kakao === 'undefined' || !kakao.maps) return;
    if (_mapInitialized) return; // 이미 초기화됨 — skip

    kakao.maps.load(function () {
      initListMap(mapEl);
      _mapInitialized = true;
    });
  }

  function initListMap(mapEl) {
    var cards = document.querySelectorAll('.center-card[data-lat][data-lng]');
    var validCards = [];
    for (var i = 0; i < cards.length; i++) {
      var lat = parseFloat(cards[i].getAttribute('data-lat'));
      var lng = parseFloat(cards[i].getAttribute('data-lng'));
      if (!isNaN(lat) && !isNaN(lng) && lat !== 0 && lng !== 0) {
        validCards.push({ card: cards[i], lat: lat, lng: lng });
      }
    }

    var defaultCenter = new kakao.maps.LatLng(37.4138, 127.5183);
    var map = new kakao.maps.Map(mapEl, { center: defaultCenter, level: 10 });

    // (B) ZoomControl UI — 사용자 수동 zoom 조절 (레퍼런스: 네이버 부동산, 카카오맵)
    map.addControl(new kakao.maps.ZoomControl(), kakao.maps.ControlPosition.RIGHT);

    // (C) "내 위치" 버튼 — geolocation 기반 사용자 위치로 이동
    //   - HTTPS 필수 (localhost 예외). HTTP 배포 시 permission denied.
    //   - 사용자 permission 프롬프트 → 성공: level 6 확대, 실패: 원상태 유지 + alert
    //   - 경기도 밖 사용자 (서울/인천) 도 자연스럽게 이동 (지도가 그 위치로 pan)
    var myLocationBtn = document.querySelector('[data-my-location]');
    var myLocationOverlay = null;
    if (myLocationBtn && navigator.geolocation) {
      myLocationBtn.addEventListener('click', function () {
        myLocationBtn.classList.add('is-loading');
        navigator.geolocation.getCurrentPosition(
          function (pos) {
            var lat = pos.coords.latitude;
            var lng = pos.coords.longitude;
            var here = new kakao.maps.LatLng(lat, lng);
            map.setCenter(here);
            map.setLevel(6);
            // 내 위치 표식 (파란 점) — 기존 표시 있으면 제거
            if (myLocationOverlay) myLocationOverlay.setMap(null);
            var dot = document.createElement('div');
            dot.style.cssText =
              'width:16px;height:16px;border-radius:50%;background:#4285F4;' +
              'border:3px solid #fff;box-shadow:0 0 0 2px rgba(66,133,244,0.35);';
            myLocationOverlay = new kakao.maps.CustomOverlay({
              position: here, content: dot, yAnchor: 0.5, xAnchor: 0.5, zIndex: 50
            });
            myLocationOverlay.setMap(map);
            myLocationBtn.classList.remove('is-loading');
            myLocationBtn.classList.add('is-active');
          },
          function (err) {
            myLocationBtn.classList.remove('is-loading');
            var msg = err.code === 1
              ? '위치 정보 사용이 거부되었습니다. 브라우저 설정에서 허용해주세요.'
              : '위치를 가져올 수 없습니다.';
            alert(msg);
          },
          { enableHighAccuracy: false, timeout: 8000, maximumAge: 60000 }
        );
      });
    } else if (myLocationBtn) {
      // 브라우저가 geolocation 미지원 → 버튼 숨김
      myLocationBtn.style.display = 'none';
    }

    var searchHereBtn = document.querySelector('[data-search-here]');
    var overlays = [];
    var selectedId = null;
    var infoOverlay = null;

    if (validCards.length === 0) return;

    var bounds = new kakao.maps.LatLngBounds();

    validCards.forEach(function (item) {
      var pos = new kakao.maps.LatLng(item.lat, item.lng);
      bounds.extend(pos);
      var card = item.card;
      var centerId = card.getAttribute('data-center-id');
      var name = (card.querySelector('.center-card-name') || card.querySelector('.center-card-compact-name'));
      var nameText = name ? name.textContent.trim() : '';
      // F0h-operating-hours-badge (spec §9-5): isActive + isOpenNow 조합. schedule 없으면 배지 자체 미표시.
      var isActive = card.getAttribute('data-center-active') === 'true';
      var isOpenNow = card.getAttribute('data-center-open-now') === 'true';
      var hasSchedule = card.getAttribute('data-center-has-schedule') === 'true';
      // F0h-c4 spec 3-2 준수: 인포윈도우 콘텐츠용 데이터 (list.html data-* attr 에서 조회)
      var addressText = card.getAttribute('data-center-address') || '';
      var hoursText = card.getAttribute('data-center-hours') || '';
      var imageUrlText = card.getAttribute('data-center-image') || '';

      // 마커 파스텔 색상은 kill-switch (isActive) 만 반영 — 지도상 폐업 센터 구분용.
      var markerEl = buildMarkerElement(nameText, isActive);
      markerEl.setAttribute('data-marker-id', centerId);

      var overlay = new kakao.maps.CustomOverlay({
        position: pos,
        content: markerEl,
        yAnchor: 1,
        zIndex: 10
      });
      // setMap 은 아래 registerOverlays() 에서 일괄 처리 (클러스터러 사용 여부에 따라 분기)

      markerEl.addEventListener('click', function (e) {
        e.stopPropagation();
        selectMarker(centerId);
      });

      overlays.push({
        id: centerId,
        card: card,
        pos: pos,
        overlay: overlay,
        el: markerEl,
        name: nameText,
        isActive: isActive,
        isOpenNow: isOpenNow,
        hasSchedule: hasSchedule,
        address: addressText,
        hours: hoursText,
        imageUrl: imageUrlText
      });

      // §3-1 zIndex: 콘텐츠 div 의 CSS z-index 는 kakao 오버레이 wrapper stacking context 에 갇혀
      // 무효할 수 있음 → 반드시 overlay.setZIndex() 로 wrapper 자체를 조정 (hover 999 / 선택 20 / 기본 10)
      card.addEventListener('mouseenter', function () {
        overlay.setZIndex(999);
        card.classList.add('is-hover');
      });
      card.addEventListener('mouseleave', function () {
        overlay.setZIndex(selectedId === centerId ? 20 : 10);
        card.classList.remove('is-hover');
      });
    });

    // §3-6 클러스터링: 센터 20개 이상일 때 clusterer 로 일괄 관리, 미만이면 개별 렌더.
    //   - 공식 문서의 addMarkers 타입은 Marker 전용이지만 CustomOverlay 도 getPosition/setMap
    //     인터페이스 호환으로 동작 (duck-typing, 공식 미보장) → 실패 시 개별 렌더 fallback.
    //   - 클러스터 스타일은 기본 유지 (커스텀 스킨은 별도 티켓).
    var _clusterer = null;   // 필터 partial swap 을 위해 module-scope 로 (2026-07-10)
    (function registerOverlays() {
      var CLUSTER_THRESHOLD = 20;
      if (validCards.length >= CLUSTER_THRESHOLD && typeof kakao.maps.MarkerClusterer === 'function') {
        try {
          _clusterer = new kakao.maps.MarkerClusterer({
            map: map,
            averageCenter: true,
            disableClickZoom: false
          });
          _clusterer.addMarkers(overlays.map(function (o) { return o.overlay; }));
          return;
        } catch (err) {
          _clusterer = null;
          // CustomOverlay 미지원 SDK 버전 → 아래 개별 렌더로 fallback
        }
      }
      overlays.forEach(function (o) { o.overlay.setMap(map); });
    })();

    // 필터 partial swap (2026-07-10): visible ID 리스트에 맞춰 overlay 표시/숨김 토글.
    // - clusterer 사용 중이면 clusterer 재구성 (clear + addMarkers)
    // - 개별 렌더면 setMap(null/map) 로 토글
    // - 인포윈도우·selected 상태는 유지 (필터로 인해 사라진 마커에 인포윈도우가 걸려있으면 정리)
    function applyOverlayFilter(visibleIds) {
      var visibleSet = {};
      visibleIds.forEach(function (id) { visibleSet[String(id)] = true; });
      var visible = [];
      var hidden = [];
      overlays.forEach(function (o) {
        if (visibleSet[String(o.id)]) visible.push(o);
        else hidden.push(o);
      });
      if (_clusterer) {
        _clusterer.clear();
        _clusterer.addMarkers(visible.map(function (o) { return o.overlay; }));
      } else {
        visible.forEach(function (o) { o.overlay.setMap(map); });
        hidden.forEach(function (o) { o.overlay.setMap(null); });
      }
      // 선택된 마커가 숨겨졌다면 인포윈도우도 정리
      if (selectedId && !visibleSet[String(selectedId)]) {
        clearSelection();
      }
    }
    _applyOverlayFilter = applyOverlayFilter;

    // (A + C) bounds fit + zoom clamp
    //   - 필터 결과 (validCards) 만 bounds 에 포함되므로 지역 필터 시 자동으로 그 지역 중심 fit (C)
    //   - clamp: level 5 (너무 확대 방지) ~ 9 (너무 축소 방지). 레퍼런스: 에어비앤비/Google Maps maxZoom clamp (A)
    //   - 단일 마커 케이스: setBounds 는 level 1 로 확대해버림 → minLevel 5 로 clamp (동네 시야)
    function fitAndClamp() {
      map.relayout();
      map.setBounds(bounds);
      var lv = map.getLevel();
      var MIN_LEVEL = 3;   // 확대 상한 (건물 단위까지 안 가게). 3 = 동 단위 시야
      var MAX_LEVEL = 7;   // 축소 상한. 초기 fit 이 7 로 clamp 되어 반경 15~20km 시야로 더 확대된 default
      // 필터 결과 개수 별 미세 조정
      if (validCards.length === 1) {
        map.setLevel(MIN_LEVEL);
      } else if (lv < MIN_LEVEL) {
        map.setLevel(MIN_LEVEL);
      } else if (lv > MAX_LEVEL) {
        map.setLevel(MAX_LEVEL);
      }
    }
    fitAndClamp();
    setTimeout(fitAndClamp, 100);

    function selectMarker(id) {
      selectedId = id;
      overlays.forEach(function (o) {
        var isSel = o.id === id;
        o.el.classList.toggle('is-selected', isSel);
        o.overlay.setZIndex(isSel ? 20 : 10);
      });
      var target = overlays.find(function (o) { return o.id === id; });
      if (target) {
        highlightCard(target.card);
        // spec §3-4-A (2026-07-09 개정): 마커가 뷰포트 밖일 때 selected 상태 시각화 보장.
        // + 사용자 요청 (2026-07-09 재개정): 동 단위까지 확대 + 인포윈도우 지도 중앙 정렬.
        //   - kakao level 4 ≈ 동/블록 단위 (약 250~500m 시야)
        //   - 인포윈도우가 마커 위에 뜨므로, 마커를 지도 중앙보다 약간 아래로 배치해야 인포윈도우가 중앙에 옴
        //     → 마커 좌표에서 setCenter 한 뒤 panBy 로 지도를 위로 이동 (마커는 아래로 밀림)
        var targetLat = typeof target.pos.getLat === 'function' ? target.pos.getLat() : null;
        var targetLng = typeof target.pos.getLng === 'function' ? target.pos.getLng() : null;
        if (targetLat != null && targetLng != null) {
          var latlng = new kakao.maps.LatLng(targetLat, targetLng);
          map.setLevel(4);                            // 동 단위 확대
          map.setCenter(latlng);                       // 마커를 지도 중앙에
          openInfoWindow(target);                      // 인포윈도우 open (마커 위에 뜸)
          // 인포윈도우 CustomOverlay yAnchor:1.4 (js:536) 기준 계산:
          //   - content center = position.y − 0.9 × infoH  (yAnchor 1.4 → position 은 content 하단에서 40% 아래)
          //   - viewport 중앙 정렬 위해 marker 를 viewport center + 0.9 × infoH 로 이동 필요
          //   - panBy(0, -N) = map view 를 N px 위로 이동 = content(marker) 는 N px 아래로 이동
          // setTimeout: DOM 렌더·이미지 로드 대기 (setLevel/setCenter 애니메이션 완료 후 실제 높이 측정)
          setTimeout(function () {
            var infoEl = document.querySelector('.center-info-window');
            var infoH = infoEl ? infoEl.offsetHeight : 260; // fallback
            map.panBy(0, -(0.9 * infoH));
          }, 180);
        } else {
          openInfoWindow(target);
        }
      }
    }
    // FAIL-1 fix: module-scope 공개 (centers:detail-open / centers:detail-close 리스너 사용)
    _selectMarker = selectMarker;

    // §3-4 마커 클릭 → 대응 카드 하이라이트 + 리스트 스크롤 (card=null 이면 전체 해제)
    function highlightCard(card) {
      var scope = document.getElementById('centers-list') || document;
      scope.querySelectorAll('.center-card.is-highlighted').forEach(function (c) {
        if (c !== card) c.classList.remove('is-highlighted');
      });
      if (card) {
        card.classList.add('is-highlighted');
        card.scrollIntoView({ block: 'nearest', behavior: 'smooth' });
      }
    }

    function clearSelection() {
      selectedId = null;
      overlays.forEach(function (o) {
        o.el.classList.remove('is-selected');
        o.overlay.setZIndex(10);
      });
      highlightCard(null);
      if (infoOverlay) { infoOverlay.setMap(null); infoOverlay = null; }
    }
    _clearSelection = clearSelection;

    // F0h-c4 spec 3-2: 이미지(110px+gradient) + 상태뱃지 + 센터명 + 주소 + 🕒 운영시간 + [상세보기]+[공유]
    function openInfoWindow(target) {
      if (infoOverlay) infoOverlay.setMap(null);
      var container = document.createElement('div');
      container.className = 'center-info-window';

      var imageBlock = target.imageUrl
        ? '<div class="center-info-window-image" style="background-image:url(' +
            encodeURI(target.imageUrl).replace(/"/g, '') + ');"></div>'
        : '<div class="center-info-window-image is-placeholder"></div>';
      // F0h-operating-hours-badge (spec §9-5): schedule 있는 센터만 인포윈도우 배지 표시.
      // 판정식은 리스트/상세와 동일 — isActive kill-switch + isOpenNow 조합.
      var openCombined = target.isActive && target.isOpenNow;
      var badgeClass = openCombined ? 'active' : 'inactive';
      var badgeText = openCombined ? '운영중' : '운영종료';
      var badgeBlock = target.hasSchedule
        ? '<span class="center-info-window-badge ' + badgeClass + '">' + badgeText + '</span>'
        : '';

      container.innerHTML =
        '<div class="center-info-window-media">' +
          imageBlock +
          '<div class="center-info-window-scrim"></div>' +
          '<button type="button" class="center-info-window-close" data-info-close aria-label="닫기">×</button>' +
          badgeBlock +
        '</div>' +
        '<div class="center-info-window-body">' +
          '<div class="center-info-window-name">' + escapeHtml(target.name) + '</div>' +
          (target.address
            ? '<div class="center-info-window-address">' + escapeHtml(target.address) + '</div>'
            : '') +
          // F0h-real-coords §9-8 (2026-07-09 재개정): 컨테이너 폭 넘어가면 CSS 자동 줄바꿈.
          // 아이콘은 flex 로 첫 라인과 나란히 정렬. ", " split 방식 폐기 (괄호 케이스 어색).
          (target.hours
            ? '<div class="center-info-window-hours">' +
                '<span class="center-info-window-hours-icon">🕒</span>' +
                '<span class="center-info-window-hours-text">' + escapeHtml(target.hours) + '</span>' +
              '</div>'
            : '') +
          '<div class="center-info-window-actions">' +
            // FAIL-2 fix: full page reload 회피 — button 태그 + centers:request-detail 이벤트 dispatch.
            // centers-detail.js 가 이 이벤트를 수신해 client-side openDetail 실행.
            '<button type="button" class="center-info-window-cta" data-info-detail ' +
              'data-center-id="' + encodeURIComponent(target.id) + '">상세보기</button>' +
            '<button type="button" class="center-info-window-share" data-info-share aria-label="공유">' +
              '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" ' +
              'stroke-width="2" stroke-linecap="round" stroke-linejoin="round">' +
              '<circle cx="18" cy="5" r="3"/><circle cx="6" cy="12" r="3"/><circle cx="18" cy="19" r="3"/>' +
              '<line x1="8.59" y1="13.51" x2="15.42" y2="17.49"/>' +
              '<line x1="15.41" y1="6.51" x2="8.59" y2="10.49"/></svg>' +
            '</button>' +
          '</div>' +
        '</div>';

      container.querySelector('[data-info-close]').addEventListener('click', function (e) {
        e.stopPropagation();
        clearSelection();
      });
      // FAIL-2 fix: CTA click → centers:request-detail dispatch (centers-detail.js 가 수신)
      var ctaBtn = container.querySelector('[data-info-detail]');
      if (ctaBtn) {
        ctaBtn.addEventListener('click', function (e) {
          e.stopPropagation();
          e.preventDefault();
          var cid = ctaBtn.getAttribute('data-center-id');
          document.dispatchEvent(new CustomEvent('centers:request-detail', { detail: { centerId: cid } }));
        });
      }
      var shareBtn = container.querySelector('[data-info-share]');
      shareBtn.addEventListener('click', function (e) {
        e.stopPropagation();
        var shareUrl = window.location.origin + '/centers/' + encodeURIComponent(target.id);
        var isMobile = /Mobi|Android|iPhone|iPad/i.test(navigator.userAgent);

        function onSuccess() {
          shareBtn.classList.add('is-copied');
          window.showToast && window.showToast('링크가 복사되었어요');
          setTimeout(function () { shareBtn.classList.remove('is-copied'); }, 1600);
        }
        function onFailure() {
          window.showToast && window.showToast('복사에 실패했어요. 브라우저 권한을 확인해주세요.', 'error');
        }
        // execCommand('copy') 먼저 시도 (데스크톱 Chrome/Firefox 폭넓게 지원). 실패 시 clipboard API.
        function tryExecCommand() {
          var ta = document.createElement('textarea');
          ta.value = shareUrl;
          ta.setAttribute('readonly', '');
          ta.style.cssText = 'position:fixed;left:-1000px;top:-1000px;opacity:0;';
          document.body.appendChild(ta);
          ta.select();
          ta.setSelectionRange(0, ta.value.length);
          var ok = false;
          try { ok = document.execCommand('copy'); } catch (err) { ok = false; }
          document.body.removeChild(ta);
          return ok;
        }

        if (isMobile && navigator.share) {
          navigator.share({ title: target.name, url: shareUrl }).catch(function () {});
          return;
        }
        if (tryExecCommand()) {
          onSuccess();
          return;
        }
        if (navigator.clipboard && navigator.clipboard.writeText) {
          navigator.clipboard.writeText(shareUrl).then(onSuccess, onFailure);
        } else {
          onFailure();
        }
      });
      infoOverlay = new kakao.maps.CustomOverlay({
        position: target.pos,
        content: container,
        yAnchor: 1.4,
        zIndex: 100
      });
      infoOverlay.setMap(map);

      // §3-2 화면 벗어남 방지: 오픈 직후 인포윈도우(300px)가 지도 좌우 경계를 넘으면
      // 콘텐츠 div 에 translateX 보정. anchor(오버레이 wrapper) 는 그대로 → 좌표 정합 유지.
      // 픽셀 계산은 실 SDK 렌더가 필요 — SDK 환경 확인 대기. 실패 시 보정만 skip (방어적).
      requestAnimationFrame(function () {
        try {
          if (!container.isConnected) return;
          var mapRect = mapEl.getBoundingClientRect();
          var rect = container.getBoundingClientRect();
          var PAD = 8;
          var shift = 0;
          if (rect.left < mapRect.left + PAD) {
            shift = (mapRect.left + PAD) - rect.left;
          } else if (rect.right > mapRect.right - PAD) {
            shift = (mapRect.right - PAD) - rect.right;
          }
          if (shift !== 0) {
            container.style.transform = 'translateX(' + Math.round(shift) + 'px)';
          }
        } catch (err) { /* 계산 실패 시 보정 없이 기본 위치 유지 */ }
      });
    }

    kakao.maps.event.addListener(map, 'click', clearSelection);

    // dirty flag: 최초 setBounds 로 인한 idle 은 무시
    var dirtySuppressed = true;
    setTimeout(function () { dirtySuppressed = false; }, 500);
    kakao.maps.event.addListener(map, 'idle', function () {
      if (dirtySuppressed) return;
      if (searchHereBtn) searchHereBtn.hidden = false;
    });

    if (searchHereBtn) {
      searchHereBtn.addEventListener('click', function () {
        var b = map.getBounds();
        overlays.forEach(function (o) {
          if (b.contain(o.pos)) o.card.classList.remove('is-out-of-bounds');
          else o.card.classList.add('is-out-of-bounds');
        });
        // 카운트 재계산
        var count = document.querySelector('.centers-list-count strong');
        if (count) {
          var visible = 0;
          document.querySelectorAll('.center-card').forEach(function (c) {
            if (!c.classList.contains('is-out-of-bounds')) visible++;
          });
          count.textContent = visible;
        }
        searchHereBtn.hidden = true;
      });
    }
  }

  // feather map-pin SVG (prototype Icon n="pin" 대체 — 이모지 대신 실 벡터).
  var PIN_SVG =
    '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" ' +
    'stroke-linecap="round" stroke-linejoin="round">' +
    '<path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/>' +
    '<circle cx="12" cy="10" r="3"/></svg>';

  // CSS 클래스 기반 마커.
  //   - 인라인 스타일 재할당(cssText) 방식은 매 selectMarker 마다 kakao 의 hit-box 재계산과 충돌하여
  //     클릭 통과·위치 흔들림 문제 발생. 상태는 className toggle 로만 관리.
  //   - wrapper 는 tail bottom 이 좌표에 anchor. body 는 absolute-positioned, 선택 시 옆으로 확장하되
  //     tail(=coord anchor) 은 항상 중앙 고정 → 시각 위치 이동 없음.
  //   - hit box 는 wrapper 자체 (padding 없이 body+tail 합). SVG 는 body div 자식이므로 SVG 투과 시
  //     body div 가 배경색으로 클릭 수신 → SVG pointer-events 별도 조작 불필요.
  function buildMarkerElement(nameText, isActive) {
    var wrapper = document.createElement('div');
    wrapper.className = 'center-marker' + (isActive ? '' : ' is-inactive');
    wrapper.innerHTML =
      '<div class="center-marker__body">' +
        '<span class="center-marker__icon">' + PIN_SVG + '</span>' +
        '<span class="center-marker__label">' + escapeHtml(nameText) + '</span>' +
      '</div>' +
      '<div class="center-marker__tail"></div>';
    return wrapper;
  }

  // 초기화 2계층 분리 (afterSwap 중복 바인딩 방지):
  //   - initStatic: 필터 바 (지역 드롭다운·운영중 토글) — swap 범위(.centers-container) 밖.
  //     afterSwap 마다 재실행하면 기존 요소에 리스너가 누적되어 "드롭다운이 열리자마자 닫힘"
  //     (document 클릭 리스너 중복) 회귀 발생 → 최초 로드 1회만 바인딩.
  //   - initSwapScoped: 정렬 pill + 지도·카드 연동 — swap 범위 안. swap 시 요소가 새로
  //     생성되므로 매번 재바인딩 필요 (기존 리스너는 요소와 함께 폐기됨 → 중복 없음).
  function initStatic() {
    initRegionDropdown();
    initOnlyActiveToggle();
  }
  function initSwapScoped() {
    initSortPill();
    initMap();
  }
  ready(function () {
    initStatic();
    initSwapScoped();
  });
  // FAIL-3 fix: afterSwap 스코프 축소 — 리스트 카드 컨테이너가 swap 될 때만 재바인딩.
  // map instance 는 _mapInitialized 캐시로 유지 (initMap 이 skip). 정렬 pill/카드 hover 리스너만 재장착.
  // (기존: .centers-container 전체 swap 시 initMap 재실행 → SDK 다시 로드 + 마커 flicker)
  document.body.addEventListener('htmx:afterSwap', function (evt) {
    var t = evt.target;
    if (!t) return;
    var isListSwap =
      (t.hasAttribute && t.hasAttribute('data-centers-list-scroll')) ||
      (t.querySelector && t.querySelector('[data-centers-list-scroll]'));
    if (isListSwap) {
      initSwapScoped(); // initMap 은 캐시로 skip. initSortPill 만 재바인딩.
    }
  });
})();
