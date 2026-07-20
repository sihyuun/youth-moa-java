/*
 * F0f-fix-1: 프로그램 목록 카드 CTA 오픈알림 / 빈자리알림 모달 트리거.
 *
 * 이번 스코프는 mock UI 만. Modal.confirm 확인 시 Toast 표시.
 * 실제 알림 구독 백엔드는 F0g 티켓에서 fetch POST /programs/{id}/alerts 로 교체.
 */
(function () {
  document.addEventListener('click', function (e) {
    var btn = e.target.closest('[data-alert-type]');
    if (!btn) return;
    if (!window.Modal || !window.Toast) return;

    var type = btn.getAttribute('data-alert-type');
    var title = type === 'openAlert' ? '오픈 알림 받기' : '빈자리 알림 받기';
    var msg = type === 'openAlert'
      ? '신청이 시작되면 알려드릴게요.'
      : '자리가 나면 알려드릴게요.';

    window.Modal.confirm({
      title: title,
      message: msg,
      confirmText: '알림 신청',
      cancelText: '취소',
      variant: 'primary'
    }).then(function () {
      // F0g 이관: 실제 API POST /programs/{programId}/alerts (type/channels)
      window.Toast.show('알림 신청되었어요.', 'success');
    }).catch(function () {
      /* cancel: no-op */
    });
  });
})();
