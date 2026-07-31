/**
 * 인터랙션 계약 공통 타입.
 *
 * 시각 계약(`types.ts`)이 "요소의 모양·수치" 를 검증한다면, 인터랙션 계약은
 * **"요소를 클릭·입력하면 어떤 상태 변화가 일어나야 하는가"** 를 검증한다.
 *
 * 도입 배경: 2026-07-31 발견 — 개인정보 수정 화면 [비밀번호 변경하기] 링크가 `/find-password`
 * (비회원 흐름) 로 이동하는 버그가 시각 계약으로 감지되지 않았다 (링크 목적지는 시각 속성이 아님).
 * 이런 액션 갭을 자동 감지하기 위한 별도 계약 축.
 */

/** 클릭 결과 종류 — 이번 세션은 navigate 와 stay 만 다룬다. modal·submit 은 후속 확장. */
export type ExpectedKind =
    /** 다른 페이지로 이동. `toPattern` 는 정규식 또는 문자열 (contains 매칭) */
    | { kind: 'navigate'; toPattern: RegExp | string }
    /** 이동 없음 — 클릭 후 같은 URL 유지 (예: 모달 트리거, 클라이언트 사이드 토글) */
    | { kind: 'stay' };

export type Severity = 'P0' | 'P1' | 'P2';
export type AuthState = 'anon' | 'auth';

export interface Interaction {
    /** 안정 식별자. 실패 리포트에서 이 값 참조 */
    id: string;
    /** 사람이 읽는 설명 */
    desc: string;
    /** 시작 경로. 검사 시 이 경로로 이동 후 클릭 */
    startPath: string;
    /** 클릭 대상 CSS 셀렉터 */
    selector: string;
    /** 어느 인증 상태에서 실행할지 */
    auth?: AuthState;
    /** 기대 동작 */
    expected: ExpectedKind;
    /** wireframe / prototype 근거 */
    proto?: string;
    severity: Severity;
    /** 영구 이탈 — 검사 제외 사유 기록 */
    deviation?: string;
}

export interface InteractionContract {
    /** 계약 파일 이름 */
    name: string;
    /** 계약 설명 */
    description: string;
    interactions: Interaction[];
}
