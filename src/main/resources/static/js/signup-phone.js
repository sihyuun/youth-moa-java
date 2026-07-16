/*
 * F-signup-01: 휴대폰 인증 UI 상태 머신 (prototype 재디자인 반영본).
 *
 * 상태: IDLE → SENDING → SENT → VERIFYING → VERIFIED / EXPIRED
 *
 * UI 규칙 (2026-07-16 재작성):
 *  - 사이드 버튼 1개 (btn-send-code) 로 상태별 라벨 전환:
 *      IDLE / EXPIRED → "인증요청"
 *      SENT           → "재요청"
 *      VERIFIED       → "재인증"  (클릭 시 resetAll 로 IDLE 복귀 → phone 재입력 가능)
 *  - 재전송 30초 cooldown 개념 완전 제거 (즉시 재요청 가능)
 *  - 타이머 span 은 코드 input 내부 우측 absolute 배치 (.signup-code-input-wrap)
 *  - 확인 버튼(btn-verify-code) 활성 조건: code.length === 6 && timerLeft > 0
 *  - 남은 시간 ≤ 30s 시 타이머에 .is-warn 클래스 부여 (색상 강조)
 *  - VERIFIED 진입 시: phone readonly · code-row 숨김 · 배지 노출
 *  - 서버 미신뢰: phoneVerifiedHidden 은 UX 힌트, 서버는 세션 phoneVerifiedAt/Number 로 재검증
 */
(function () {
  const phoneInput = document.getElementById('phone');
  const sendBtn = document.getElementById('btn-send-code');
  const verifyBtn = document.getElementById('btn-verify-code');
  const codeRow = document.getElementById('code-row');
  const codeInput = document.getElementById('verify-code');
  const timerEl = document.getElementById('code-timer');
  const badge = document.getElementById('phone-verified-badge');
  const helpMsg = document.getElementById('phone-verify-help');
  const errMsg = document.getElementById('phone-verify-msg');
  const hidden = document.getElementById('phoneVerifiedHidden');
  if (!phoneInput || !sendBtn) return;

  const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
  const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content') || 'X-CSRF-TOKEN';

  const CODE_TTL = 180;
  const WARN_THRESHOLD = 30;

  let state = 'IDLE';
  let ttlTimerId = null;
  let remainingTtl = 0;

  // ─── 안내 문구 유틸 ─────────────────────────────
  function clearMessages() {
    helpMsg.hidden = true; helpMsg.textContent = '';
    errMsg.hidden = true; errMsg.textContent = '';
  }
  function showHelp(text) {
    clearMessages();
    helpMsg.textContent = text;
    helpMsg.hidden = false;
  }
  function showError(text) {
    clearMessages();
    errMsg.textContent = text;
    errMsg.hidden = false;
  }

  function fmt(sec) {
    const m = Math.floor(sec / 60);
    const s = sec % 60;
    return m + ':' + String(s).padStart(2, '0');
  }

  // ─── 사이드 버튼 라벨 ───────────────────────────
  function updateSendBtnLabel(s) {
    if (s === 'SENT' || s === 'SENDING' || s === 'VERIFYING') {
      sendBtn.textContent = '재요청';
    } else if (s === 'VERIFIED') {
      sendBtn.textContent = '재인증';
    } else {
      // IDLE, EXPIRED
      sendBtn.textContent = '인증요청';
    }
  }

  // ─── 확인 버튼 활성/비활성 재평가 ───────────────
  function refreshVerifyBtn() {
    if (!verifyBtn) return;
    const code = (codeInput?.value || '').trim();
    verifyBtn.disabled = !(code.length === 6 && remainingTtl > 0 && (state === 'SENT' || state === 'VERIFYING'));
  }

  // ─── 타이머 ─────────────────────────────────────
  function stopTtl() {
    if (ttlTimerId) { clearInterval(ttlTimerId); ttlTimerId = null; }
  }
  function startTtl() {
    stopTtl();
    remainingTtl = CODE_TTL;
    timerEl.textContent = fmt(remainingTtl);
    timerEl.classList.remove('is-warn');
    ttlTimerId = setInterval(() => {
      remainingTtl--;
      if (remainingTtl <= 0) {
        remainingTtl = 0;
        timerEl.textContent = '0:00';
        timerEl.classList.add('is-warn');
        setState('EXPIRED');
        return;
      }
      timerEl.textContent = fmt(remainingTtl);
      if (remainingTtl <= WARN_THRESHOLD) timerEl.classList.add('is-warn');
      else timerEl.classList.remove('is-warn');
      refreshVerifyBtn();
    }, 1000);
  }

  // ─── 상태 전이 ──────────────────────────────────
  function setState(next) {
    state = next;
    updateSendBtnLabel(next);

    if (next === 'IDLE') {
      badge.hidden = true;
      codeRow.hidden = true;
      hidden.value = 'false';
      sendBtn.disabled = false;
      phoneInput.readOnly = false;
      phoneInput.classList.remove('signup-phone-locked');
      stopTtl();
      remainingTtl = 0;
      if (codeInput) codeInput.value = '';
      refreshVerifyBtn();
      clearMessages();
    } else if (next === 'SENT') {
      badge.hidden = true;
      codeRow.hidden = false;
      hidden.value = 'false';
      sendBtn.disabled = false;
      showHelp('문자로 받은 인증번호를 3분 안에 입력해주세요.');
      refreshVerifyBtn();
    } else if (next === 'EXPIRED') {
      stopTtl();
      remainingTtl = 0;
      hidden.value = 'false';
      sendBtn.disabled = false;
      timerEl.textContent = '0:00';
      timerEl.classList.add('is-warn');
      showError('유효시간이 만료되었어요. 인증번호를 재요청해주세요.');
      refreshVerifyBtn();
    } else if (next === 'VERIFIED') {
      badge.hidden = false;
      codeRow.hidden = true;
      hidden.value = 'true';
      phoneInput.readOnly = true;
      phoneInput.classList.add('signup-phone-locked');
      // code input hide 시 focus 가 phone input 으로 자연 이동해 readonly 스타일이
      // :focus primary border 로 override 되는 이슈 방지 → 명시적 blur.
      phoneInput.blur();
      if (document.activeElement && document.activeElement.blur) {
        document.activeElement.blur();
      }
      sendBtn.disabled = false;
      stopTtl();
      clearMessages();  // 배지 자체가 완료 표시
    }
    // SENDING/VERIFYING 은 라벨/버튼 상태만 갱신
  }

  function resetAll() {
    // VERIFIED → 재인증 시 IDLE 로 완전 복귀
    setState('IDLE');
    phoneInput.focus();
  }

  function headers() {
    const h = { 'Content-Type': 'application/json' };
    if (csrfToken) h[csrfHeader] = csrfToken;
    return h;
  }

  async function sendCode() {
    const phone = (phoneInput.value || '').replace(/\D/g, '');
    if (!/^0\d{9,10}$/.test(phone)) {
      showError('올바른 휴대폰 번호를 입력해주세요.');
      return;
    }
    state = 'SENDING';
    sendBtn.disabled = true;
    try {
      const res = await fetch('/api/phone/send-code', {
        method: 'POST',
        headers: headers(),
        body: JSON.stringify({ phone })
      });
      if (!res.ok) {
        const data = await res.json().catch(() => ({}));
        showError(data.error || '인증번호 발송에 실패했습니다.');
        setState('IDLE');
        return;
      }
      // 재요청 시 code input 초기화
      if (codeInput) codeInput.value = '';
      setState('SENT');
      startTtl();
      codeInput?.focus();
    } catch (e) {
      showError('네트워크 오류가 발생했습니다.');
      setState('IDLE');
    }
  }

  async function verifyCode() {
    const phone = (phoneInput.value || '').replace(/\D/g, '');
    const code = (codeInput.value || '').trim();
    if (!/^\d{6}$/.test(code)) {
      showError('6자리 인증번호를 입력해주세요.');
      return;
    }
    state = 'VERIFYING';
    verifyBtn.disabled = true;
    try {
      const res = await fetch('/api/phone/verify-code', {
        method: 'POST',
        headers: headers(),
        body: JSON.stringify({ phone, code })
      });
      const data = await res.json().catch(() => ({}));
      if (!res.ok || data.ok === false) {
        showError(data.error || '인증번호가 올바르지 않습니다.');
        state = 'SENT';
        refreshVerifyBtn();
        return;
      }
      setState('VERIFIED');
    } catch (e) {
      showError('네트워크 오류가 발생했습니다.');
      state = 'SENT';
      refreshVerifyBtn();
    }
  }

  // ─── 이벤트 바인딩 ──────────────────────────────
  sendBtn.addEventListener('click', () => {
    if (state === 'VERIFIED') {
      resetAll();
    } else {
      sendCode();
    }
  });

  verifyBtn?.addEventListener('click', verifyCode);

  // Code input: 숫자만 6자리 sanitize + 확인 버튼 재평가
  codeInput?.addEventListener('input', () => {
    codeInput.value = codeInput.value.replace(/\D/g, '').slice(0, 6);
    refreshVerifyBtn();
  });

  // phone 값 변경 → 인증 상태 리셋 (VERIFIED 인 경우만 유의미)
  phoneInput.addEventListener('input', () => {
    if (phoneInput.readOnly) return;
    if (state !== 'IDLE') {
      setState('IDLE');
    }
  });

  // 초기 라벨
  updateSendBtnLabel('IDLE');
})();
