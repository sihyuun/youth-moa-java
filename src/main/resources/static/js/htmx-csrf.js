// P0-2: HTMX CSRF 자동 첨부.
// SecurityConfig 가 CSRF 를 활성화(세션 저장)한 이후, 모든 HTMX 상태 변경 요청은
// X-CSRF-TOKEN (또는 서버가 지정한 header name) 헤더를 실어야 한다.
// - meta[name="_csrf"] · meta[name="_csrf_header"] 두 태그를 각 페이지 <head> 에 두어야 함
// - GET / HEAD 는 CSRF 검증 대상 아니므로 skip
document.body.addEventListener('htmx:configRequest', function (evt) {
  var method = (evt.detail.verb || '').toUpperCase();
  if (method === 'GET' || method === 'HEAD') return;
  var tokenMeta = document.querySelector('meta[name="_csrf"]');
  var headerMeta = document.querySelector('meta[name="_csrf_header"]');
  if (tokenMeta && headerMeta) {
    evt.detail.headers[headerMeta.content] = tokenMeta.content;
  }
});
