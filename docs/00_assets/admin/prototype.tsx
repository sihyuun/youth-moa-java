/**
 * 청년모아 관리자 페이지 — React + TypeScript 재구현 스캐폴드
 * ----------------------------------------------------------------
 * 이 파일은 `청년모아 Admin.dc.html` 디자인 레퍼런스를 실제 코드베이스로
 * 옮기기 위한 출발점입니다. 그대로 빌드되는 완성품이 아니라, 화면 라우팅 ·
 * 상태 · 데이터 모델 · 컴포넌트 분해 · 디자인 토큰의 골격입니다.
 *
 * 실제 구현 시:
 *  - 화면 전환은 단일 `screen` 상태 대신 라우터(react-router 등)로 매핑 권장
 *  - 상태는 Context/Zustand/Redux 등 코드베이스 컨벤션으로
 *  - 더미 데이터/API는 실제 백엔드로 교체
 *  - 인라인 토큰은 Tailwind / CSS 변수 / 디자인 시스템으로 대체
 *  - 아이콘은 lucide-react 등으로 대체
 *
 * 원본의 모든 상세(레이아웃·인터랙션·카피)는 HANDOFF.md 와
 * `청년모아 Admin.dc.html` 을 브라우저로 열어 확인하세요.
 */

import React, { useState, useMemo, useCallback } from "react";

/* ============================================================
 * 1. 디자인 토큰
 * ========================================================== */
export const tokens = {
  color: {
    // Primary — 인디고 (사용자 페이지와 통일)
    primary: "#3F30E9",
    primaryDark: "#3428CF",   // hover/pressed
    primaryLight: "#E5E1FB",  // 연한 강조 ≈ oklch(0.93 0.0425 280)
    primaryBg: "#F1EFFC",     // 배경 틴트 ≈ oklch(0.96 0.0255 280)
    secondary: "#F97316",     // light #FFF7ED, text #EA580C
    headerBg: "#111827",
    headerSurface: "#1E293B",
    headerBorder: "#334155",
    pageBg: "#FAFAFB",
    cardBg: "#FFFFFF",
    border: "#E3E1E8",
    borderLight: "#F0EFF3",   // 연한 보더·구분선·표면 틴트
    textStrong: "#2B2A3D",
    textMid: "#4A4759",
    textSub: "#6E6B82",
    textWeak: "#A6A3B3",
    textFaint: "#C9C6D3",
  },
  radius: { card: 12, control: 8, chip: 6, badge: 9999 },
  shadow: {
    card: "0 1px 3px rgba(0,0,0,0.06)",
    popover: "0 8px 24px rgba(0,0,0,0.12)",
    dropdown: "0 8px 30px rgba(0,0,0,0.18)",
  },
  font: {
    body: "'Pretendard','Inter',sans-serif",
    numeric: "'Inter',sans-serif",
  },
} as const;

/* 프로그램 상태 → 뱃지 */
export type ProgramStatus = "active" | "closed" | "upcoming";
export const statusConfig: Record<
    ProgramStatus,
    { label: string; bg: string; color: string }
> = {
  active: { label: "진행중", bg: "#D1FAE5", color: "#047857" },       // Success
  closed: { label: "마감", bg: "#F0EFF3", color: "#6E6B82" },          // 중립 그레이
  upcoming: { label: "진행 예정", bg: "#FFF7ED", color: "#EA580C" },   // Secondary 오렌지
};

/* 신청자/사용자 상태 → 뱃지 */
export type ApplicantStatus = "승인" | "대기" | "반려" | "취소";
export function applicantBadge(s: ApplicantStatus) {
  if (s === "승인") return { bg: "#D1FAE5", color: "#047857" };  // Success
  if (s === "대기") return { bg: "#E5E1FB", color: "#3428CF" };  // Primary Light
  return { bg: "#FEE2E2", color: "#DC2626" };                   // 반려/취소 Error
}

/* 권한 → 뱃지 */
export type Role = "시스템 관리자" | "관리자" | "사용자";
export const roleConfig: Record<Role, { bg: string; color: string }> = {
  "시스템 관리자": { bg: "#FEF3C7", color: "#B45309" },
  관리자: { bg: "#E5E1FB", color: "#3428CF" },
  사용자: { bg: "#F0EFF3", color: "#475569" },
};

/* 신청률 progress bar 색상 */
export function applyRateColor(pct: number): string {
  if (pct >= 80) return "#EF4444";   // Error
  if (pct >= 50) return "#F59E0B";   // Warning
  return "#3F30E9";                  // Primary
}

/* D-day 색상 */
export function ddayColor(daysLeft: number) {
  if (daysLeft <= 2) return { bg: "#FEF2F2", color: "#E72D0F" };
  if (daysLeft <= 5) return { bg: "#FEF3C7", color: "#D97706" };
  return { bg: "#F0EFF3", color: "#6E6B82" };
}

/* 폼 검증 헬퍼 — 모든 폼 공통 (HANDOFF.md '최신 기능 > 폼 검증' 참고)
 * 적용: 프로그램 등록/수정, 사용자 등록, 사용자 상세, 회원가입, 설정-비밀번호 변경 */
export const validators = {
  required: (v: string) => (v && v.trim() ? null : "필수 입력이에요"),
  email: (v: string) =>
      /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v) ? null : "이메일 형식이 아니에요",
  phone: (v: string) =>
      /^01[0-9]-?\d{3,4}-?\d{4}$/.test(v) ? null : "핸드폰 번호 형식이 아니에요",
  password: (v: string) =>
      /^(?=.*[A-Za-z])(?=.*\d).{8,}$/.test(v) ? null : "영문·숫자 포함 8자 이상이어야 해요",
  match: (a: string, b: string) =>
      a === b ? null : "비밀번호가 일치하지 않아요",
  positiveInt: (v: string | number) =>
      Number.isInteger(Number(v)) && Number(v) >= 1 ? null : "1 이상 숫자여야 해요",
};

/** 필드별 검증 상태 — 인풋 보더색 + 에러 메시지 렌더에 사용 */
export interface FieldState {
  value: string;
  error: string | null; // null이면 통과
  touched: boolean;
}
/** 빨간 보더(#EF4444) / 평상시 보더(#E3E1E8) */
export const fieldBorder = (f: FieldState) =>
    f.touched && f.error ? "#EF4444" : "#E3E1E8";

/* ============================================================
 * 2. 데이터 모델
 * ========================================================== */
export interface Program {
  id: number;
  name: string;
  center: string;       // 청년센터명 (데이터 격리 기준)
  region: string;
  period: string;       // "YYYY-MM-DD ~ YYYY-MM-DD" 진행기간
  applyPeriod: string;  // 신청기간
  capacity: number;
  applied: number;
  views: number;
  status: ProgramStatus;
  hasCourses?: boolean; // 강좌 제공 여부
}

export interface AppUser {
  id: number;
  name: string;
  email: string;
  gender: "남" | "여";
  role: Role;
  phone: string;
  joinDate: string;     // YYYY-MM-DD
  lastAccess: string;
  status: "active" | "inactive";
  address?: string;
}

export interface Applicant {
  id: number;
  email: string;
  name?: string;
  gender: "남" | "여";
  phone: string;
  appliedAt: string;    // "YYYY-MM-DD HH:mm"
  visits: number;
  status: ApplicantStatus;
  answers?: { question: string; answer: string }[]; // 신청 상세 모달용
}

export type NotifType = "approval" | "deadline" | "user";
export interface NotifItem {
  id: number;
  type: NotifType;
  title: string;
  desc: string;
  time: string;
  read: boolean;
  screen: Screen; // 클릭 시 이동
}

/* ============================================================
 * 3. 화면(Screen) & 전역 상태
 * ========================================================== */
export type Screen =
// 인증 (별도 레이아웃)
    | "login" | "signup" | "find-id" | "find-pw"
    // 관리자 본문
    | "dashboard" | "stats"
    | "programs" | "program-detail" | "program-form" | "course-detail"
    | "users" | "user-detail" | "user-register"
    | "calendar" | "settings"
    | "mypage-pw-check" | "mypage-edit";

const AUTH_SCREENS: Screen[] = ["login", "signup", "find-id", "find-pw"];

export type ProgramView = "list" | "card" | "calendar";
export type FormMode = "create" | "edit";
export type ChartMode = "month" | "year";
export type CalendarView = "month" | "week" | "list";
export type SettingsTab = "account" | "notification" | "system";
export type ToastType = "success" | "error" | "warning" | "info";
/** 'system' = 시스템 관리자(전체), 'center' = 센터 관리자(소속 센터만) */
export type AdminRole = "system" | "center";

export interface AppState {
  screen: Screen;

  // 센터 데이터 격리 (RBAC / 멀티테넌시)
  adminRole: AdminRole;
  adminCenter: string;       // 센터 관리자 소속 센터
  centerScope: string;       // 시스템 관리자가 보는 범위 ('전체' | 센터명)

  // 프로그램
  programFilter: "all" | ProgramStatus;
  programSearch: string;
  programView: ProgramView;
  programPage: number;
  programSelected: number[];

  // 사용자
  userRoleFilter: "all" | Role;
  userSearch: string;
  userPage: number;
  userSelected: number[];

  // 폼 / 선택 항목
  formMode: FormMode;
  formTab: "info" | "apply" | "terms";
  selectedProgram: Program | null;
  selectedUser: AppUser | null;

  // 통계 / 캘린더
  chartMode: ChartMode;
  calendarView: CalendarView;
  calendarYear: number;
  calendarMonth: number; // 0-based

  // 설정 / 알림 / 토스트
  settingsTab: SettingsTab;
  notifItems: NotifItem[];
  toast: { msg: string; type: ToastType } | null;

  // 삭제 확인 모달 (파괴적 액션 1단계 차단)
  confirmDialog:
      | { type: string; title: string; message: string; count?: number }
      | null;

  // 로딩/에러 상태 (스켈레톤 · 재시도)
  loading?: boolean;
  loadError?: boolean;

  // 폼 검증: 각 폼은 필드별 FieldState 묶음을 둔다 (validators 참고)
  //   예) programForm: Record<string, FieldState>, userForm: Record<string, FieldState> …
  //   저장 시 일괄 검증 → 통과 시 이동+성공 토스트 / 실패 시 에러 노출+토스트
}

const PROGRAMS_PER_PAGE = 8;
const USERS_PER_PAGE = 10;

export const CENTERS = [
  "고천센터", "더누림플랫폼", "동안 청년오피스", "딴딴회관",
  "만안 청년오피스", "상상대로", "양주 청년센터", "범계역 청년출구", "오름",
];

/* ============================================================
 * 4. 더미 데이터 (→ 실제 API로 교체)
 * ========================================================== */
export const samplePrograms: Program[] = [
  { id: 1, name: "프로그램 A", center: "고천센터", region: "안양시", period: "2026-07-01 ~ 2026-07-31", applyPeriod: "2026-06-01 ~ 2026-06-30", capacity: 20, applied: 10, views: 132, status: "active" },
  { id: 2, name: "프로그램 B", center: "더누림플랫폼", region: "안산시", period: "2026-08-01 ~ 2026-08-31", applyPeriod: "2026-07-01 ~ 2026-07-31", capacity: 20, applied: 8, views: 98, status: "active" },
  { id: 3, name: "프로그램 C", center: "동안 청년오피스", region: "안양시", period: "2026-07-01 ~ 2026-07-31", applyPeriod: "2026-06-01 ~ 2026-06-30", capacity: 20, applied: 0, views: 45, status: "closed" },
  // … HTML 원본의 programs 배열 참고
];

export const sampleUsers: AppUser[] = [
  { id: 1, name: "김청년", email: "abc123@naver.com", gender: "여", role: "사용자", phone: "010-1234-5678", joinDate: "2023-09-01", lastAccess: "2026-09-01", status: "active" },
  { id: 6, name: "박시현", email: "abg123@naver.com", gender: "남", role: "시스템 관리자", phone: "010-1234-5678", joinDate: "2026-01-01", lastAccess: "2026-09-18", status: "active" },
  // … HTML 원본의 users 배열 참고
];

/* ============================================================
 * 5. 파생 로직 (필터링 / 페이지네이션 / 데이터 격리)
 * ========================================================== */
export function effectiveCenter(s: AppState): string | null {
  if (s.adminRole === "center") return s.adminCenter;
  return s.centerScope === "전체" ? null : s.centerScope;
}

export function useFilteredPrograms(s: AppState, all: Program[]) {
  return useMemo(() => {
    const center = effectiveCenter(s);
    const filtered = all
        .filter((p) => !center || p.center === center)
        .filter((p) => s.programFilter === "all" || p.status === s.programFilter)
        .filter(
            (p) =>
                !s.programSearch ||
                p.name.includes(s.programSearch) ||
                p.center.includes(s.programSearch)
        );
    const totalPages = Math.max(1, Math.ceil(filtered.length / PROGRAMS_PER_PAGE));
    const page = Math.min(s.programPage, totalPages);
    const pageItems = filtered.slice(
        (page - 1) * PROGRAMS_PER_PAGE,
        page * PROGRAMS_PER_PAGE
    );
    return { filtered, pageItems, totalPages, page, isEmpty: filtered.length === 0 };
  }, [s, all]);
}

export function useFilteredUsers(s: AppState, all: AppUser[]) {
  return useMemo(() => {
    const filtered = all
        .filter((u) => s.userRoleFilter === "all" || u.role === s.userRoleFilter)
        .filter(
            (u) =>
                !s.userSearch ||
                u.name.includes(s.userSearch) ||
                u.email.includes(s.userSearch)
        );
    const totalPages = Math.max(1, Math.ceil(filtered.length / USERS_PER_PAGE));
    const page = Math.min(s.userPage, totalPages);
    const pageItems = filtered.slice(
        (page - 1) * USERS_PER_PAGE,
        page * USERS_PER_PAGE
    );
    return { filtered, pageItems, totalPages, page, isEmpty: filtered.length === 0 };
  }, [s, all]);
}

/* CSV 내보내기 헬퍼 */
export function downloadCSV(filename: string, headers: string[], rows: (string | number)[][]) {
  const esc = (v: string | number) => `"${String(v ?? "").replace(/"/g, '""')}"`;
  const csv = [headers.map(esc).join(","), ...rows.map((r) => r.map(esc).join(","))].join("\n");
  const blob = new Blob(["\uFEFF" + csv], { type: "text/csv;charset=utf-8" });
  const a = document.createElement("a");
  a.href = URL.createObjectURL(blob);
  a.download = filename;
  a.click();
}

/* ============================================================
 * 6. 루트 컴포넌트 (라우팅 골격)
 * ========================================================== */
export default function YouthMoaAdmin() {
  const [state, setState] = useState<AppState>({
    screen: "login",
    adminRole: "system",
    adminCenter: "고천센터",
    centerScope: "전체",
    programFilter: "all",
    programSearch: "",
    programView: "list",
    programPage: 1,
    programSelected: [],
    userRoleFilter: "all",
    userSearch: "",
    userPage: 1,
    userSelected: [],
    formMode: "create",
    formTab: "info",
    selectedProgram: null,
    selectedUser: null,
    chartMode: "month",
    calendarView: "month",
    calendarYear: 2026,
    calendarMonth: 5,
    settingsTab: "account",
    notifItems: [],
    toast: null,
    confirmDialog: null,
  });

  const patch = useCallback(
      (p: Partial<AppState>) => setState((s) => ({ ...s, ...p })),
      []
  );
  const go = useCallback((screen: Screen) => patch({ screen }), [patch]);

  const showToast = useCallback(
      (msg: string, type: ToastType = "success") => {
        patch({ toast: { msg, type } });
        window.setTimeout(() => patch({ toast: null }), 2800);
      },
      [patch]
  );

  const isAuth = AUTH_SCREENS.includes(state.screen);

  return (
      <div style={{ fontFamily: tokens.font.body, minHeight: "100vh" }}>
        {isAuth ? (
            <AuthLayout state={state} go={go} />
        ) : (
            <AdminLayout state={state} patch={patch} go={go} showToast={showToast} />
        )}
        {state.toast && <Toast toast={state.toast} />}
      </div>
  );
}

/* ============================================================
 * 7. 레이아웃
 * ========================================================== */
function AuthLayout({ state, go }: { state: AppState; go: (s: Screen) => void }) {
  // 밝은 헤더(logo_primary.png) + 가운데 카드(max-width 420px)
  // login / signup / find-id / find-pw 분기
  return (
      <div>
        {/* <AuthHeader /> */}
        {state.screen === "login" && <LoginScreen go={go} />}
        {/* signup / find-id / find-pw … */}
      </div>
  );
}

function AdminLayout({
                       state, patch, go, showToast,
                     }: {
  state: AppState;
  patch: (p: Partial<AppState>) => void;
  go: (s: Screen) => void;
  showToast: (msg: string, type?: ToastType) => void;
}) {
  return (
      <div style={{ display: "flex", flexDirection: "column", minHeight: "100vh" }}>
        <AdminHeader state={state} patch={patch} go={go} />
        <main style={{ flex: 1, padding: "24px 28px", background: tokens.color.pageBg }}>
          <ScreenRouter state={state} patch={patch} go={go} showToast={showToast} />
        </main>
        <AdminFooter />
      </div>
  );
}

function ScreenRouter(props: {
  state: AppState;
  patch: (p: Partial<AppState>) => void;
  go: (s: Screen) => void;
  showToast: (msg: string, type?: ToastType) => void;
}) {
  switch (props.state.screen) {
    case "dashboard": return <DashboardScreen {...props} />;
    case "stats": return <StatsScreen {...props} />;
    case "programs": return <ProgramsScreen {...props} />;
    case "program-detail": return <ProgramDetailScreen {...props} />;
    case "program-form": return <ProgramFormScreen {...props} />;
    case "course-detail": return <CourseDetailScreen {...props} />;
    case "users": return <UsersScreen {...props} />;
    case "user-detail": return <UserDetailScreen {...props} />;
    case "user-register": return <UserRegisterScreen {...props} />;
    case "calendar": return <CalendarScreen {...props} />;
    case "settings": return <SettingsScreen {...props} />;
    default: return <DashboardScreen {...props} />;
  }
}

/* ============================================================
 * 8. 화면/컴포넌트 스텁 — HANDOFF.md & HTML 원본 참고해 구현
 * ========================================================== */
// 헤더: 로고 / 센터 스코프 셀렉터 / GNB(중앙) / 검색 / 알림 / 유저 드롭다운
function AdminHeader(_: any) { return <header /* sticky, #111827, 56px */ />; }
function AdminFooter() { return <footer /* 스크롤, 버전·저작권 */ />; }
function Toast({ toast }: { toast: { msg: string; type: ToastType } }) {
  return <div /* 하단 중앙, 타입별 아이콘·색 */>{toast.msg}</div>;
}

function LoginScreen(_: { go: (s: Screen) => void }) { return <div />; }
function DashboardScreen(_: any) { return <div /* 스탯카드 + 최근/마감임박 프로그램 */ />; }
function StatsScreen(_: any) { return <div /* 방문자(막대/꺾은선) + 프로그램 통계 + 성별·연령 도넛 */ />; }
function ProgramsScreen(_: any) { return <div /* 뷰토글(목록/카드/캘린더) + 필터 + 검색 + 체크박스 일괄 + CSV + 페이지네이션 */ />; }
function ProgramDetailScreen(_: any) { return <div /* 정보+설명 + 신청현황(상태변경/대기자) + 수정/⋯(복제·삭제) */ />; }
function ProgramFormScreen(_: any) { return <div /* 탭: 정보/신청/약관, formMode create|edit */ />; }
function CourseDetailScreen(_: any) { return <div /* 강좌별 신청 현황 */ />; }
function UsersScreen(_: any) { return <div /* 권한필터 + 검색 + 체크박스 일괄 + CSV + 페이지네이션 */ />; }
function UserDetailScreen(_: any) { return <div /* 정보수정 + 주소검색 + 신청현황(신청상세 모달) */ />; }
function UserRegisterScreen(_: any) { return <div />; }
function CalendarScreen(_: any) { return <div /* 월/주/목록 + 날짜팝업 + 슬라이드패널 */ />; }
function SettingsScreen(_: any) { return <div /* 계정/알림/시스템 탭 */ />; }

/* ============================================================
 * 9. 재사용 컴포넌트 가이드 (구현 권장 목록)
 * ============================================================
 *  - <Badge status>            상태/권한 뱃지 (statusConfig / roleConfig / applicantBadge)
 *  - <DataTable>               헤더 #F0EFF3, 네이티브 체크박스(accent #3F30E9),
 *                              No. 컬럼(# 아님), 일괄 액션바, Empty state, Pagination
 *  - <BulkActionBar>           "N건 선택됨" + CSV 내보내기 + 삭제 + 선택 해제 (다크 #1E293B)
 *  - <ViewToggle>              목록/카드/캘린더 세그먼트
 *  - <Pagination>              활성 #3F30E9
 *  - <ProgressBar pct>         applyRateColor()
 *  - <Donut data>              SVG 도넛 (성별/연령)
 *  - <BarChart hover>          막대 + hover 수치 툴팁
 *  - <LineChart>               꺾은선 + area gradient
 *  - <Dropdown>                바깥 클릭 닫힘 (검색/알림/유저/센터스코프)
 *  - <Modal>                   신청상세/상태변경 확인/주소검색/캘린더 날짜팝업
 *  - <RadioGroup>              테두리 없는 통일 스타일, 선택 시 primary
 *  - <ToggleSwitch>            알림 설정 / 대기자 자동승인
 *  - <Toast type>              4타입(success/error/warning/info), 존댓말 톤
 *  - <ConfirmDialog>           삭제 등 파괴적 액션 1단계 확인 (confirmDialog 상태)
 *  - <FormField>               value + validators + FieldState → 빨간 보더(#EF4444) + 하단 에러텍스트
 *  - <Skeleton/.sk>            로딩 시 shimmer 스켈레톤 (테이블/차트/상세)
 *  - <ErrorState>             "불러오지 못했어요" + 재시도 버튼 (Empty와 구분)
 *  - 포커스: :focus-visible 인디고 링(#3F30E9), 모달 role="dialog"
 *
 *  반응형(적응형):
 *  - ≤1024px: 스탯 4열→2열, 2단 레이아웃 폭 축소, 패딩 축소
 *  - ≤680px: 모든 그리드 1열 스택, GNB 가로스크롤, 테이블은 overflow-x 유지
 *  - HTML 원본은 @media 속성선택자([style*="…"])로 구현 → 실구현엔 CSS 클래스/브레이크포인트로 정규화
 *
 *  주의:
 *  - 프로그램 status는 신청기간·정원으로 자동 파생 → 일괄 상태변경 기능 없음(CSV/삭제만)
 *  - 헤더 "권한 전환" 버튼은 데모용 → 실제로는 로그인 권한으로 결정, 제거
 *  - 체크박스는 네이티브 input[type=checkbox] + accent-color 사용
 */
