/*
 * HTMX SSE Extension (self-hosted copy for youth-moa-java A7-realtime)
 *
 * Source: https://github.com/bigskysoftware/htmx-extensions/blob/main/src/sse/sse.js
 * Version: 2.2.3-ish (compatible with htmx.org 2.0.4). BSD-2-Clause licensed.
 *
 * webjar 좌표 `org.webjars.npm:htmx-ext-sse` 가 2026-10 기준 미공개라 레포에 소스를 두고 서빙.
 * HTMX 공식 레포 라이선스 보존 — 수정 시 LICENSE 조건 유지.
 *
 * 지원 속성:
 *   hx-ext="sse"               : 확장 활성
 *   sse-connect="/path"        : EventSource 연결 생성
 *   sse-swap="event1,event2"   : 이벤트 수신 시 요소 내용 교체 (hx-swap 전략 적용)
 *   sse-close="done"           : 특정 이벤트 수신 시 EventSource close
 *   hx-trigger="sse:event"     : 이벤트 수신을 htmx trigger 로 사용
 */
(function () {
  /** @type {import("../htmx").HtmxInternalApi} */
  var api;

  htmx.defineExtension('sse', {

    init: function (apiRef) {
      api = apiRef;
      if (htmx.createEventSource === undefined) {
        htmx.createEventSource = createEventSource;
      }
    },

    getSelectors: function () {
      return ['[sse-connect]', '[data-sse-connect]', '[sse-swap]', '[data-sse-swap]'];
    },

    onEvent: function (name, evt) {
      var parent = evt.target || evt.detail.elt;
      switch (name) {
        case 'htmx:beforeCleanupElement':
          var internalData = api.getInternalData(parent);
          if (internalData.sseEventSource) {
            internalData.sseEventSource.close();
          }
          return;

        case 'htmx:afterProcessNode':
          ensureEventSourceOnElement(parent);
          registerSSE(parent);
      }
    }
  });

  function createEventSource(url) {
    return new EventSource(url, { withCredentials: true });
  }

  function ensureEventSourceOnElement(elt) {
    if (elt == null) return null;
    var sseURL = api.getAttributeValue(elt, 'sse-connect');
    if (sseURL == null) return;
    ensureEventSource(elt, sseURL, 0);
  }

  function ensureEventSource(elt, url, retryCount) {
    var source = htmx.createEventSource(url);
    source.onerror = function (err) {
      api.triggerErrorEvent(elt, 'htmx:sseError', { error: err, source: source });
      if (maybeCloseSSESource(elt)) return;
      if (source.readyState === EventSource.CLOSED) {
        retryCount = retryCount || 0;
        var timeout = Math.random() * (2 ^ retryCount) * 500;
        window.setTimeout(function () {
          ensureEventSource(elt, url, Math.min(7, retryCount + 1));
        }, timeout);
      }
    };
    source.onopen = function () {
      api.triggerEvent(elt, 'htmx:sseOpen', { source: source });
      if (retryCount && retryCount > 0) {
        var childrenToFix = elt.querySelectorAll("[sse-swap], [data-sse-swap], [hx-trigger], [data-hx-trigger]");
        for (var i = 0; i < childrenToFix.length; i++) {
          registerSSE(childrenToFix[i]);
        }
        api.triggerEvent(elt, 'htmx:sseReconnected', { source: source });
      }
    };
    api.getInternalData(elt).sseEventSource = source;
    var closeAttribute = api.getAttributeValue(elt, 'sse-close');
    if (closeAttribute) {
      source.addEventListener(closeAttribute, function () {
        api.triggerEvent(elt, 'htmx:sseClose', { source: source, type: 'message' });
        maybeCloseSSESource(elt);
      });
    }
  }

  function registerSSE(elt) {
    // sse-swap 처리
    var sseSwapAttr = api.getAttributeValue(elt, 'sse-swap');
    if (sseSwapAttr) {
      var sseEventNames = sseSwapAttr.split(',');
      for (var i = 0; i < sseEventNames.length; i++) {
        var sseEventName = sseEventNames[i].trim();
        var listener = function (event) {
          if (maybeCloseSSESource(elt)) return;
          if (!api.bodyContains(elt)) {
            sourceElement.removeEventListener(sseEventName, listener);
            return;
          }
          var response = event.data;
          api.withExtensions(elt, function (extension) {
            response = extension.transformResponse(response, null, elt);
          });
          var swapSpec = api.getSwapSpecification(elt);
          var target = api.getTarget(elt);
          api.swap(target, response, swapSpec);
          api.triggerEvent(elt, 'htmx:sseMessage', { event: event });
        };
        var sourceElement = getSSEEventSource(elt);
        if (sourceElement == null) {
          api.triggerErrorEvent(elt, 'htmx:sseError', { error: 'NoSSESourceError' });
          return;
        }
        api.getInternalData(elt).sseEventListener = listener;
        sourceElement.addEventListener(sseEventName, listener);
      }
    }

    // hx-trigger="sse:eventName" 처리
    var hxTriggerAttr = api.getAttributeValue(elt, 'hx-trigger');
    if (hxTriggerAttr == null) return;
    var triggerSpecs = api.getTriggerSpecs(elt);
    triggerSpecs.forEach(function (triggerSpec) {
      if (triggerSpec.trigger.slice(0, 4) !== 'sse:') return;
      var sseEventName = triggerSpec.trigger.slice(4);
      var listener = function (event) {
        if (maybeCloseSSESource(elt)) return;
        if (!api.bodyContains(elt)) {
          sourceElement.removeEventListener(sseEventName, listener);
          return;
        }
        api.triggerEvent(elt, 'htmx:sseMessage', { event: event });
        api.addTriggerHandler(elt, triggerSpec, api.getInternalData(elt), event);
      };
      var sourceElement = getSSEEventSource(elt);
      if (sourceElement == null) {
        api.triggerErrorEvent(elt, 'htmx:sseError', { error: 'NoSSESourceError' });
        return;
      }
      api.getInternalData(elt).sseEventListener = listener;
      sourceElement.addEventListener(sseEventName, listener);
    });
  }

  function getSSEEventSource(elt) {
    while (elt != null) {
      var sseEventSource = api.getInternalData(elt).sseEventSource;
      if (sseEventSource) return sseEventSource;
      elt = elt.parentElement;
    }
    return null;
  }

  function maybeCloseSSESource(elt) {
    if (!api.bodyContains(elt)) {
      var source = api.getInternalData(elt).sseEventSource;
      if (source != null) {
        source.close();
        return true;
      }
    }
    return false;
  }
})();
