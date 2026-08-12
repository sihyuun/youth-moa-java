/**
 * 신청 완료 화면 (`/apply/complete?applicationId={id}`) 디자인 계약.
 *
 * 도입 목적 (2026-08-11): 사용자 트랙 남은 9화면 계약 시리즈. apply.ts 와 세트로 산출.
 *
 * 추출 기준: `docs/00_assets/prototype.tsx` `ApplyComplete` L1217~1249
 *
 * 실행 조건 (visual-apply-complete.spec.ts):
 *  - auth 필요 + 완료 페이지는 **직접 URL 접근 불가**하지 않다 (권한 검증만 통과하면 GET 가능).
 *    → 계약 실행 순서:
 *      1) `login(page, seedEmail(30))` 로 fresh 유저 로그인
 *      2) `/programs/{id}/apply` → 3단계 위저드 통과 → 제출
 *      3) `redirect:/apply/complete?applicationId={saved.id}` 로 이동한 URL 을 capture
 *      4) 그 URL 을 spec 이 그대로 사용 (계약 실행 대상 페이지)
 *  - 다른 유저의 applicationId 로 접근 시 404 → apply-complete.spec.ts 가 커버 (계약 대상 아님)
 *
 * 특이 사항:
 *  - prototype 은 **헤더 미렌더** (proto L1219~1247 에 Header 컴포넌트 없음) — 구현 정합.
 *    F2c Q4 (`fix/apply-complete-header` 파생 큐) 로 향후 헤더 추가가 논의 중 → 결정 확정 전에는
 *    prototype 정합인 "헤더 없음" 을 계약으로 강제. Q4 확정 후 계약 갱신 (deviation 처리).
 *  - prototype ApplyComplete 는 신청번호(`#A{id}`) · appliedAt 을 프로퍼티로 안 가지고 있지만
 *    구현이 정보 밀도를 높이려 추가. POLICY P-5 로 계약 대상 제외, apply-complete.md §5 에만 기록.
 *  - 부제(`channelSubtitle`) 는 활성 알림 채널 수에 따라 서버가 4가지 문구로 조립 →
 *    계약은 "부제 노드 존재" 만 확인. 특정 문구는 단위 테스트가 커버.
 *  - CTA 좌측 "홈으로" (ghost 160px), 우측 "내 신청 현황 보기" (primary 200px).
 *    prototype 은 "신청 현황 보기" 인데 구현은 "내 신청 현황 보기" 로 카피 정정 → POLICY P-1
 *    (카피는 구현 유지). 링크 목적지 정합 확인.
 *  - 하단 mini-link (취소 안내) 는 prototype 에 없음 → POLICY P-5 로 §5 에만 기록.
 */

import type { ScreenContract } from './types';

export const applyCompleteContract: ScreenContract = {
    screen: 'apply-complete',
    // path 는 spec 이 동적으로 지정 (제출 후 redirect URL 사용). 계약 정의상 lint 를 위해 자리표시자.
    path: '/apply/complete?applicationId=0',
    source: 'prototype.tsx ApplyComplete L1217~1249 · 2026-08-11 신설',
    viewport: { width: 1440, height: 900 },
    checks: [
        // ══════════════════════════════════════════════════════
        // 헤더 미렌더 (proto 정합)
        // ══════════════════════════════════════════════════════
        {
            id: 'header.absent',
            desc: '완료 페이지에 공용 헤더 없음 (prototype 정합, Q-1(A) 확정 2026-08-11 · F2c Q4 해소)',
            selector: 'header.header, .site-header',
            kind: 'count',
            expected: 0,
            proto: 'tsx L1219~1247 Header 미렌더',
            severity: 'P1',
            states: ['auth'],
        },

        // ══════════════════════════════════════════════════════
        // 컨테이너 (proto L1220 padding:56 80)
        // ══════════════════════════════════════════════════════
        {
            id: 'container.padding',
            desc: 'apply-complete-inner padding 56 80 (prototype L1220)',
            selector: '.apply-complete-inner',
            kind: 'css',
            prop: 'padding',
            expected: '56px 80px',
            proto: 'tsx L1220 padding:"56px 80px"',
            severity: 'P2',
            states: ['auth'],
        },
        {
            id: 'container.text-align',
            desc: 'inner 중앙 정렬 (align-items:center)',
            selector: '.apply-complete-inner',
            kind: 'css',
            prop: 'align-items',
            expected: 'center',
            proto: 'tsx L1220 alignItems:center',
            severity: 'P2',
            states: ['auth'],
        },

        // ══════════════════════════════════════════════════════
        // 성공 아이콘 (proto L1221 88x88, success-light bg, 46 SVG)
        // ══════════════════════════════════════════════════════
        {
            id: 'icon.width',
            desc: '성공 아이콘 컨테이너 width 88',
            selector: '.apply-complete-icon',
            kind: 'box',
            prop: 'width',
            expected: 88,
            tolerance: 1,
            proto: 'tsx L1221 width:88',
            severity: 'P1',
            states: ['auth'],
        },
        {
            id: 'icon.height',
            desc: '성공 아이콘 컨테이너 height 88',
            selector: '.apply-complete-icon',
            kind: 'box',
            prop: 'height',
            expected: 88,
            tolerance: 1,
            proto: 'tsx L1221 height:88',
            severity: 'P1',
            states: ['auth'],
        },
        {
            id: 'icon.border-radius',
            desc: '성공 아이콘 완전 원형 (50%)',
            selector: '.apply-complete-icon',
            kind: 'css',
            prop: 'border-radius',
            expected: '50%',
            proto: 'tsx L1221 borderRadius:"50%"',
            severity: 'P2',
            states: ['auth'],
        },
        {
            id: 'icon.svg.exists',
            desc: '체크 SVG 존재 (POLICY P-3)',
            selector: '.apply-complete-icon svg',
            kind: 'exists',
            expected: true,
            proto: 'tsx L1222~1225 SVG circle + check path',
            severity: 'P0',
            states: ['auth'],
        },
        {
            id: 'icon.svg.width',
            desc: 'SVG width 46',
            selector: '.apply-complete-icon svg',
            kind: 'box',
            prop: 'width',
            expected: 46,
            tolerance: 1,
            proto: 'tsx L1222 width:46',
            severity: 'P2',
            states: ['auth'],
        },

        // ══════════════════════════════════════════════════════
        // 타이틀 (proto L1227 fontSize:26/700)
        // ══════════════════════════════════════════════════════
        {
            id: 'title.text',
            desc: '타이틀 텍스트',
            selector: '.apply-complete-title',
            kind: 'text',
            expected: '프로그램 신청이 완료되었습니다',
            proto: 'tsx L1227',
            severity: 'P0',
            states: ['auth'],
        },
        {
            id: 'title.font-size',
            desc: '타이틀 font-size 26',
            selector: '.apply-complete-title',
            kind: 'css',
            prop: 'font-size',
            expected: '26px',
            proto: 'tsx L1227 fontSize:26',
            severity: 'P1',
            states: ['auth'],
        },
        {
            id: 'title.font-weight',
            desc: '타이틀 굵기 700',
            selector: '.apply-complete-title',
            kind: 'css',
            prop: 'font-weight',
            expected: '700',
            proto: 'tsx L1227 fontWeight:700',
            severity: 'P1',
            states: ['auth'],
        },

        // ══════════════════════════════════════════════════════
        // 부제 (proto L1228 · 활성 채널에 따라 서버 조립 · 존재만 확인)
        // ══════════════════════════════════════════════════════
        {
            id: 'subtitle.exists',
            desc: '부제 노드 존재',
            selector: '.apply-complete-subtitle',
            kind: 'exists',
            expected: true,
            proto: 'tsx L1228~1231',
            severity: 'P1',
            states: ['auth'],
        },
        {
            id: 'subtitle.font-size',
            desc: '부제 font-size 15',
            selector: '.apply-complete-subtitle',
            kind: 'css',
            prop: 'font-size',
            expected: '15px',
            proto: 'tsx L1228 fontSize:15',
            severity: 'P2',
            states: ['auth'],
        },

        // ══════════════════════════════════════════════════════
        // 요약 카드 (proto L1232 width:520 · padding:20 · border · gap:16)
        // ══════════════════════════════════════════════════════
        {
            id: 'card.width',
            desc: '요약 카드 width 520',
            selector: '.apply-complete-card',
            kind: 'box',
            prop: 'width',
            expected: 520,
            tolerance: 1,
            proto: 'tsx L1232 width:520',
            severity: 'P1',
            states: ['auth'],
        },
        {
            id: 'card.padding',
            desc: '요약 카드 padding 20',
            selector: '.apply-complete-card',
            kind: 'css',
            prop: 'padding',
            expected: '20px',
            proto: 'tsx L1232 padding:20',
            severity: 'P2',
            states: ['auth'],
        },
        {
            id: 'card.gap',
            desc: '요약 카드 gap 16 (썸네일↔메타)',
            selector: '.apply-complete-card',
            kind: 'css',
            prop: 'gap',
            expected: '16px',
            proto: 'tsx L1232 gap:16',
            severity: 'P2',
            states: ['auth'],
        },
        {
            id: 'card.border-radius',
            desc: '요약 카드 radius-lg (12) — prototype T.radius (Q-2(A) 확정 2026-08-11)',
            selector: '.apply-complete-card',
            kind: 'css',
            prop: 'border-radius',
            expected: '12px',
            proto: 'tsx L1232 borderRadius:T.radius (12)',
            severity: 'P2',
            states: ['auth'],
        },
        {
            id: 'card.thumb.width',
            desc: '요약 카드 썸네일 width 80',
            selector: '.apply-complete-thumb',
            kind: 'box',
            prop: 'width',
            expected: 80,
            tolerance: 1,
            proto: 'tsx L1233 width:80',
            severity: 'P2',
            states: ['auth'],
        },
        {
            id: 'card.thumb.height',
            desc: '요약 카드 썸네일 height 80',
            selector: '.apply-complete-thumb',
            kind: 'box',
            prop: 'height',
            expected: 80,
            tolerance: 1,
            proto: 'tsx L1233 height:80',
            severity: 'P2',
            states: ['auth'],
        },
        {
            id: 'card.badge.text',
            desc: '요약 카드 뱃지 "승인 대기"',
            selector: '.apply-complete-badge',
            kind: 'text',
            expected: '승인 대기',
            proto: 'tsx L1235',
            severity: 'P0',
            states: ['auth'],
        },
        {
            id: 'card.program-title.font-size',
            desc: '요약 카드 프로그램 제목 font-size 16',
            selector: '.apply-complete-program-title',
            kind: 'css',
            prop: 'font-size',
            expected: '16px',
            proto: 'tsx L1236 fontSize:16',
            severity: 'P2',
            states: ['auth'],
        },

        // ══════════════════════════════════════════════════════
        // CTA (proto L1241~1244)
        // ══════════════════════════════════════════════════════
        {
            id: 'actions.gap',
            desc: 'CTA 간 gap 12',
            selector: '.apply-complete-actions',
            kind: 'css',
            prop: 'gap',
            expected: '12px',
            proto: 'tsx L1241 gap:12',
            severity: 'P2',
            states: ['auth'],
        },
        {
            id: 'action.ghost.text',
            desc: '왼쪽 ghost 버튼 "홈으로"',
            selector: '.apply-complete-btn--ghost',
            kind: 'text',
            expected: '홈으로',
            proto: 'tsx L1242',
            severity: 'P0',
            states: ['auth'],
        },
        {
            id: 'action.ghost.width',
            desc: 'ghost 버튼 width 160',
            selector: '.apply-complete-btn--ghost',
            kind: 'box',
            prop: 'width',
            expected: 160,
            tolerance: 1,
            proto: 'tsx L1242 style width:160',
            severity: 'P1',
            states: ['auth'],
        },
        {
            id: 'action.ghost.height',
            desc: 'ghost 버튼 Btn size:l → height 50 (Q-3(A) 확정 2026-08-11)',
            selector: '.apply-complete-btn--ghost',
            kind: 'box',
            prop: 'height',
            expected: 50,
            tolerance: 1,
            proto: 'tsx L85 sizes.l h:50',
            severity: 'P1',
            states: ['auth'],
        },
        {
            id: 'action.primary.text',
            desc: '오른쪽 primary 버튼 "내 신청 현황 보기" (proto: "신청 현황 보기" — 카피 정정 P-1)',
            selector: '.apply-complete-btn--primary',
            kind: 'text',
            expected: '내 신청 현황 보기',
            proto: 'tsx L1243 "신청 현황 보기"',
            severity: 'P1',
            states: ['auth'],
            deviation:
                'POLICY P-1 카피 정책 — 구현 "내 신청 현황 보기" 유지. "내"를 붙여 소유격을 명확히 함',
        },
        {
            id: 'action.primary.width',
            desc: 'primary 버튼 width 200',
            selector: '.apply-complete-btn--primary',
            kind: 'box',
            prop: 'width',
            expected: 200,
            tolerance: 1,
            proto: 'tsx L1243 style width:200',
            severity: 'P1',
            states: ['auth'],
        },
        {
            id: 'action.primary.height',
            desc: 'primary 버튼 Btn size:l → height 50 (Q-3(A) 확정 2026-08-11)',
            selector: '.apply-complete-btn--primary',
            kind: 'box',
            prop: 'height',
            expected: 50,
            tolerance: 1,
            proto: 'tsx L85 sizes.l h:50',
            severity: 'P1',
            states: ['auth'],
        },

        // ══════════════════════════════════════════════════════
        // Footer (proto L1246 <Footer/>)
        // ══════════════════════════════════════════════════════
        {
            id: 'footer.exists',
            desc: '푸터 존재',
            selector: 'footer.site-footer, footer',
            kind: 'count',
            expected: 1,
            proto: 'tsx L1246 <Footer/>',
            severity: 'P1',
            states: ['auth'],
        },
    ],
};
