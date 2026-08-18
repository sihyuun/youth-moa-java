/**
 * 개인정보 수정 Step2 (`/mypage/profile/edit`) 디자인 계약.
 *
 * 도입 (2026-08-14): mypage.ts 와 동시 신설.
 * prototype `MyPage` tab==='profile' pwVerified 분기 (tsx L1505~1552) 기준.
 *
 * ─────────────────────────────────────────────────────────
 * 별도 계약으로 분리한 이유
 * ─────────────────────────────────────────────────────────
 * mypage.ts 의 다른 탭은 `/mypage?tab=X` 로 URL 이 같아 통합했지만, Step2 는:
 *   1) URL 이 별도 (`/mypage/profile/edit`) — 세션 flag 통과 후 리다이렉트
 *   2) 진입 전에 POST /mypage/profile/verify 를 통과해야 함 → 계약 실행 시 별도 세팅 필요
 *   3) mypage.ts 는 GET 만 · 이 계약은 사전 POST 필요 → 실행 흐름이 다름
 *
 * ─────────────────────────────────────────────────────────
 * 인증 + 세션 세팅: helpers.login + POST /mypage/profile/verify (seed 계정 비밀번호)
 *   → visual-mypage-profile-edit.spec.ts 에서 폼 submit 으로 세션 flag 부여 후 이동
 *   → TTL 10 분 · 검사 실행 시간 안엔 유지
 *
 * side-effect: 렌더만. POST /mypage/profile (저장) · POST /mypage/withdraw (탈퇴) 는 트리거 X.
 *   verify POST 는 세션 flag 만 세팅하고 사용자 필드는 변경하지 않으므로 seed 오염 없음.
 *
 * 갭 예상 (mypage-gap-backlog T9 등):
 *   P0: 없음 — 폼 골격은 완성
 *   P1: 관심 지역·분야 편집 UI 부재 (T9 · Q2 결정 대기) → deferred
 *        gender 편집 UI (prototype 은 남/여 선택 · 구현은 readonly pill)
 *   P2: 폼 grid 폭 · 라벨 폰트 등 미세 조정
 *
 * 함께: `docs/design-contracts/mypage-profile-edit.md` — 아키텍처·상태머신·CTA·POLICY 매핑·§8 결정 Q.
 */

import type { ScreenContract } from './types';

export const mypageProfileEditContract: ScreenContract = {
    screen: 'mypage-profile-edit',
    path: '/mypage/profile/edit',
    source: 'prototype.tsx MyPage L1505~1552 (pwVerified 분기) · 2026-08-14 신설',
    viewport: { width: 1440, height: 900 },
    checks: [
        // ══════════════════════════════════════════════════════
        // 컨테이너 (상단 요약·탭바는 mypage.ts 계약이 커버)
        // ══════════════════════════════════════════════════════
        {
            id: 'container.max-width',
            desc: 'mypage-page inner max-width 1080 (prototype L1351)',
            selector: '.mypage-page .container',
            kind: 'css',
            prop: 'max-width',
            expected: '1080px',
            proto: 'tsx L1351 maxWidth:1080',
            states: ['auth'],
            severity: 'P2',
        },
        {
            id: 'section.exists',
            desc: '개인정보 수정 섹션 카드 존재',
            selector: '.mypage-profile-edit',
            kind: 'exists',
            expected: true,
            proto: 'tsx L1506~1550 pwVerified 분기 폼',
            states: ['auth'],
            severity: 'P0',
        },
        {
            id: 'section.title.text',
            desc: '섹션 제목 (prototype L1488 · pwVerified 후에도 동일)',
            selector: '.mypage-profile-edit .mypage-section-title',
            kind: 'text',
            expected: '개인 정보 수정',
            proto: 'tsx L1488 개인 정보 수정',
            states: ['auth'],
            severity: 'P1',
        },
        {
            id: 'section.hint.text',
            desc: '섹션 안내 (prototype L1507)',
            selector: '.mypage-profile-edit .mypage-section-hint',
            kind: 'text',
            expected: '변경할 정보만 수정한 뒤 저장해주세요.',
            proto: 'tsx L1507 변경할 정보만 수정한 뒤 저장해주세요.',
            states: ['auth'],
            severity: 'P2',
        },

        // ══════════════════════════════════════════════════════
        // 폼 grid (110px 라벨 + 1fr, prototype L1508)
        // ══════════════════════════════════════════════════════
        {
            id: 'form.display',
            desc: '폼 grid 레이아웃 (prototype L1508)',
            selector: '.mypage-edit-form-grid',
            kind: 'css',
            prop: 'display',
            expected: 'grid',
            proto: 'tsx L1508 gridTemplateColumns:110px 1fr',
            states: ['auth'],
            severity: 'P1',
        },
        {
            id: 'form.max-width',
            desc: '폼 max-width 560 (prototype L1508)',
            selector: '.mypage-edit-form-grid',
            kind: 'css',
            prop: 'max-width',
            expected: '560px',
            proto: 'tsx L1508 maxWidth:560',
            states: ['auth'],
            severity: 'P2',
        },
        {
            id: 'label.font-size',
            desc: '라벨 font-size 13.5 (prototype L1509)',
            selector: '.mypage-edit-label',
            kind: 'css',
            prop: 'font-size',
            expected: '13.5px',
            proto: 'tsx L1509 fontSize:13.5',
            states: ['auth'],
            severity: 'P2',
        },
        {
            id: 'label.font-weight',
            desc: '라벨 font-weight 600 (prototype L1509)',
            selector: '.mypage-edit-label',
            kind: 'css',
            prop: 'font-weight',
            expected: '600',
            proto: 'tsx L1509 fontWeight:600',
            states: ['auth'],
            severity: 'P2',
        },

        // ══════════════════════════════════════════════════════
        // readonly 필드 (아이디 · 이메일, prototype L1510~1513)
        // 배경 패턴 A: readonly = 회색 배경 (--color-bg or border-light)
        // ══════════════════════════════════════════════════════
        {
            id: 'email-readonly.exists',
            desc: '아이디(이메일) readonly 표시 (prototype L1511)',
            selector: '#editEmail',
            kind: 'exists',
            expected: true,
            proto: 'tsx L1511 inputRO 아이디',
            states: ['auth'],
            severity: 'P0',
        },
        {
            id: 'email-readonly.class',
            desc: '아이디 표시 form-input--readonly 클래스',
            selector: '#editEmail.form-input--readonly',
            kind: 'exists',
            expected: true,
            proto: 'tsx L1511 inputRO 배경 회색 패턴',
            states: ['auth'],
            severity: 'P2',
        },

        // ══════════════════════════════════════════════════════
        // 비밀번호 변경 (optional, 2 필드, prototype L1515~1518)
        // ══════════════════════════════════════════════════════
        {
            id: 'password.input.exists',
            desc: '새 비밀번호 input (prototype L1516)',
            selector: '#editNewPassword',
            kind: 'exists',
            expected: true,
            proto: 'tsx L1516 새 비밀번호',
            states: ['auth'],
            severity: 'P0',
        },
        {
            id: 'password.confirm.exists',
            desc: '새 비밀번호 확인 input (prototype L1517)',
            selector: '#editNewPasswordConfirm',
            kind: 'exists',
            expected: true,
            proto: 'tsx L1517 새 비밀번호 확인',
            states: ['auth'],
            severity: 'P0',
        },
        // eye 토글은 wireframe #9 · prototype 초과지만 유지
        {
            id: 'password.eye.svg',
            desc: '비밀번호 눈 토글 SVG (POLICY P-3)',
            selector: '.mypage-edit-pw-field .auth-eye-btn svg',
            kind: 'exists',
            expected: true,
            proto: 'wireframe #9',
            states: ['auth'],
            severity: 'P1',
        },

        // ══════════════════════════════════════════════════════
        // 연락처 · 성별 · 생년월일 (prototype L1520~1534)
        // ══════════════════════════════════════════════════════
        {
            id: 'phone.input.exists',
            desc: '핸드폰 번호 input (prototype L1521)',
            selector: '#editPhone',
            kind: 'exists',
            expected: true,
            proto: 'tsx L1521 phone',
            states: ['auth'],
            severity: 'P0',
        },
        // ⚠️ deferred: gender 편집 UI (prototype 은 남/여 pill 선택 · 구현은 readonly)
        //    성별 변경은 정책적으로 잠금 결정 시 deviation 으로 승격
        {
            id: 'gender.pill.count',
            desc: '성별 pill 2개 (남·여, prototype L1526)',
            selector: '.gender-pill',
            kind: 'count',
            expected: 2,
            proto: 'tsx L1526 남/여 pill',
            states: ['auth'],
            severity: 'P1',
        },
        {
            id: 'gender.pill.active.svg',
            desc: '활성 성별 pill 안 check SVG (POLICY P-3, prototype L1528)',
            selector: '.gender-pill.is-on svg',
            kind: 'exists',
            expected: true,
            proto: 'tsx L1528 Icon n="check" size:15 (on 상태만)',
            states: ['auth'],
            severity: 'P2',
            deferred: 'seed 계정에 gender 시드 부재 — DataInitializer 갱신 후 검사 활성. 렌더 자체는 Q-5 구현 완료 (JS 클릭 시 SVG 삽입)',
        },
        {
            id: 'birthdate.input.exists',
            desc: '생년월일 input (prototype L1534)',
            selector: '#editBirthDate',
            kind: 'exists',
            expected: true,
            proto: 'tsx L1534 생년월일',
            states: ['auth'],
            severity: 'P0',
        },

        // ══════════════════════════════════════════════════════
        // 주소 (우편·검색·주소·상세, prototype L1535~1543)
        // ══════════════════════════════════════════════════════
        {
            id: 'zipcode.input.exists',
            desc: '우편번호 input (prototype L1538)',
            selector: '#editZipcode',
            kind: 'exists',
            expected: true,
            proto: 'tsx L1538 우편 readOnly',
            states: ['auth'],
            severity: 'P0',
        },
        {
            id: 'addr-search-btn.exists',
            desc: '주소 검색 버튼 (prototype L1539)',
            selector: '#btnAddrSearch',
            kind: 'text',
            expected: '검색',
            proto: 'tsx L1539 Btn icon="search"',
            states: ['auth'],
            severity: 'P1',
        },
        {
            id: 'addr-search-btn.svg',
            desc: '주소 검색 버튼 안 search SVG (POLICY P-3, prototype L1539 icon="search")',
            selector: '#btnAddrSearch svg',
            kind: 'exists',
            expected: true,
            proto: 'tsx L1539 icon="search"',
            states: ['auth'],
            severity: 'P2',
        },
        {
            id: 'address.input.exists',
            desc: '주소 input (prototype L1541)',
            selector: '#editAddress',
            kind: 'exists',
            expected: true,
            proto: 'tsx L1541 addr readOnly',
            states: ['auth'],
            severity: 'P0',
        },
        {
            id: 'address-detail.input.exists',
            desc: '상세주소 input (prototype L1542)',
            selector: '#editAddressDetail',
            kind: 'exists',
            expected: true,
            proto: 'tsx L1542 상세주소',
            states: ['auth'],
            severity: 'P0',
        },

        // ══════════════════════════════════════════════════════
        // ⚠️ deferred: 관심 지역·분야 편집 (T9 · Q2 결정 대기)
        //    prototype 은 MyPage 안에 InterestEditModal 로 편집. 구현은 편집 경로 부재.
        //    CLAUDE.md 데이터 소비 지점 규칙 위반 상태.
        // ══════════════════════════════════════════════════════
        {
            id: 'interest.edit.exists',
            desc: '관심 지역·분야 편집 진입 (prototype L1374 · InterestEditModal L1798)',
            selector: '.mypage-edit-interest-section',
            kind: 'exists',
            expected: true,
            proto: 'tsx L1374 setShowInterest 진입',
            states: ['auth'],
            severity: 'P1',
        },

        // ══════════════════════════════════════════════════════
        // 하단 액션: 탈퇴 · 저장 (prototype L1547~1549)
        // ══════════════════════════════════════════════════════
        {
            id: 'divider.exists',
            desc: '액션 상단 구분선 (prototype L1546)',
            selector: '.mypage-edit-divider',
            kind: 'exists',
            expected: true,
            proto: 'tsx L1546 height:1 borderLight',
            states: ['auth'],
            severity: 'P2',
        },
        {
            id: 'withdraw-btn.text',
            desc: '탈퇴하기 버튼 (prototype L1548)',
            selector: '#btnWithdraw',
            kind: 'text',
            expected: '탈퇴하기',
            proto: 'tsx L1548 탈퇴하기',
            states: ['auth'],
            severity: 'P1',
        },
        {
            id: 'save-btn.exists',
            desc: '저장 버튼 (prototype L1549)',
            selector: 'button[type="submit"][form="profileEditForm"]',
            kind: 'text',
            expected: '저장',
            proto: 'tsx L1549 저장',
            states: ['auth'],
            severity: 'P1',
        },
        {
            id: 'actions.gap',
            desc: '액션 버튼 간격 gap 10 (prototype L1547)',
            selector: '.mypage-edit-actions-center',
            kind: 'css',
            prop: 'gap',
            expected: '10px',
            proto: 'tsx L1547 gap:10',
            states: ['auth'],
            severity: 'P2',
        },

        // ══════════════════════════════════════════════════════
        // 탈퇴 확인 모달 (렌더된 hidden 상태 · POLICY P-3 확인)
        // ══════════════════════════════════════════════════════
        {
            id: 'withdraw-modal.exists',
            desc: '탈퇴 확인 모달 마크업 존재 (hidden)',
            selector: '#withdrawModal',
            kind: 'exists',
            expected: true,
            proto: 'tsx L1585~1587 ConfirmDialog variant="danger"',
            states: ['auth'],
            severity: 'P1',
        },
    ],
};
