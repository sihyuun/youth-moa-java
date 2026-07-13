# Youth-Moa 개발 핸드오프 명세서
## 경기도 청년센터 프로그램 조회·신청 플랫폼

---

## 1. 기술 스택

| 항목 | 기술 |
|------|------|
| 프레임워크 | **React 18 + Next.js (App Router)** |
| 스타일 | **Tailwind CSS** + CSS Variables (디자인 토큰) |
| 폰트 | **Pretendard** (CDN: `pretendard.min.css`) |
| 아이콘 | **Lucide React** (stroke 1.5px 기준) |
| 상태관리 | React Context + useState (소규모) |
| 인증 | NextAuth.js 또는 커스텀 JWT |
| DB | Prisma + PostgreSQL (추천) |

---

## 2. 디자인 토큰 (CSS Variables)

> **구현 위치:** `src/app/globals.css` — Tailwind v4 `@theme` 블록으로 이미 적용됨

```css
/* src/app/globals.css */
@import "tailwindcss";

@theme {
  /* Primary — 메인 컬러 (확정: #3F30E9) */
  --color-primary:       #3F30E9;   /* 메인 액션, 링크, 활성 탭 */
  --color-primary-dark:  #3428CF;   /* hover 상태 */
  --color-primary-light: oklch(0.93 0.0425 280); /* 뱃지 배경, 사이드바 활성 */
  --color-primary-bg:    oklch(0.96 0.0255 280); /* 섹션 배경, 고정글 행 */

  /* Semantic */
  --color-secondary: #F97316;   /* 보조 (행사 뱃지) */
  --color-success:   #10B981;   /* 성공, 운영 뱃지 */
  --color-warning:   #F59E0B;   /* 경고, 즐겨찾기 별 */
  --color-error:     #EF4444;   /* 에러, 탈퇴 링크 */

  /* Neutrals */
  --color-bg:           oklch(0.985 0.004 280);  /* 페이지 배경 */
  --color-surface:      #FFFFFF;                  /* 카드, 모달 */
  --color-text:         oklch(0.22 0.051 280);   /* 본문 텍스트 */
  --color-text-sec:     oklch(0.55 0.034 280);   /* 보조 텍스트 */
  --color-text-tri:     oklch(0.7 0.02 280);     /* 플레이스홀더 */
  --color-border:       oklch(0.9 0.014 280);    /* 인풋/카드 보더 */
  --color-border-light: oklch(0.95 0.007 280);   /* 구분선 */

  /* Shadows */
  --shadow-sm: 0 1px 3px rgba(67,56,202,0.06);
  --shadow-md: 0 4px 12px rgba(67,56,202,0.07);
  --shadow-lg: 0 10px 24px rgba(67,56,202,0.09);

  /* Radii */
  --radius-sm:   8px;
  --radius-md:   12px;   /* 카드, 컨테이너 */
  --radius-pill: 20px;   /* 뱃지, 탭, 태그 */

  /* Font */
  --font-sans: 'Pretendard', -apple-system, BlinkMacSystemFont, system-ui, Helvetica, Arial, sans-serif;
}

:root {
  --header-h:    68px;
  --max-width:   1440px;
  --content-px:  80px;   /* 좌우 패딩 */
  --banner-grad: linear-gradient(135deg,
    oklch(0.34 0.17 280) 0%,
    oklch(0.46 0.17 280) 45%,
    oklch(0.66 0.119 280) 100%);
}
```

### 2-1. 컬러 토큰 HEX 대조표 (admin 전달용)

oklch 미지원 환경(예: admin)에서는 아래 HEX 근사값을 사용하세요. **admin 기존 블루 `#0264FB` → `#3F30E9`로 통일.**

| 토큰 | HEX | oklch (원본) | 용도 |
|------|-----|--------------|------|
| **primary** | `#3F30E9` | — | 메인 액션·링크·활성 탭 |
| primary-dark | `#3428CF` | — | hover/pressed |
| primary-light | `#E5E1FB` | `oklch(0.93 0.0425 280)` | 뱃지 배경·사이드바 활성 보더 |
| primary-bg | `#F1EFFC` | `oklch(0.96 0.0255 280)` | 섹션 배경·고정글 행·info 박스 |
| secondary | `#F97316` | — | 행사 뱃지·진행예정 |
| success | `#10B981` | — | 운영·승인 |
| warning | `#F59E0B` | — | 마감임박·즐겨찾기 |
| error | `#EF4444` | — | 마감·삭제·탈퇴 |
| text | `#2B2A3D` | `oklch(0.22 0.051 280)` | 본문 |
| text-sec | `#6E6B82` | `oklch(0.55 0.034 280)` | 보조 |
| text-tri | `#A6A3B3` | `oklch(0.7 0.02 280)` | 플레이스홀더 |
| bg | `#FAFAFB` | `oklch(0.985 0.004 280)` | 페이지 배경 |
| surface | `#FFFFFF` | — | 카드·모달 |
| border | `#E3E1E8` | `oklch(0.9 0.014 280)` | 인풋·카드 보더 |
| border-light | `#F0EFF3` | `oklch(0.95 0.007 280)` | 구분선 |

> **핵심 6색**(admin 최소 전달): primary `#3F30E9` · primary-bg `#F1EFFC` · success `#10B981` · warning `#F59E0B` · error `#EF4444` · text `#2B2A3D`

/* ── Utility Classes (프로토타입과 동일 적용) ── */
.card-hover { transition: all 200ms ease; cursor: pointer; }
.card-hover:hover {
transform: translateY(-3px);
box-shadow: 0 8px 24px rgba(63,48,233,0.14) !important;
border-color: #3F30E9 !important;
}
.btn-hover { transition: all 150ms ease; cursor: pointer; }
.btn-hover:hover  { filter: brightness(0.92); transform: translateY(-1px); }
.btn-hover:active { transform: scale(0.97); }
```

---

## 3. 타이포그래피

| 용도 | 크기 | 굵기 | 색상 |
|------|------|------|------|
| 페이지 타이틀 | 28px | 700 | `--color-text` |
| 히어로 헤드라인 | 42px | 800 | `#FFFFFF` |
| 섹션 타이틀 | 24px | 700 | `--color-text` |
| 서브 섹션 타이틀 | 20px | 600 | `--color-text` |
| 카드 제목 | 15px | 600 | `--color-text` |
| 본문 | 14px | 400 | `--color-text` |
| 보조 텍스트 | 13px | 400 | `--color-text-sec` |
| 캡션 / 플레이스홀더 | 12px | 400 | `--color-text-tri` |
| 뱃지 | 12px | 600 | `#FFFFFF` |

---

## 4. 공통 컴포넌트

### 4.1 Button (`<YMButton>`)

| Prop | 타입 | 기본값 | 설명 |
|------|------|--------|------|
| `size` | `'s' \| 'm' \| 'l'` | `'m'` | 크기 |
| `variant` | `'primary' \| 'secondary' \| 'outline' \| 'ghost' \| 'danger'` | `'primary'` | 스타일 |
| `icon` | `string` | — | 좌측 아이콘 (Lucide 이름) |
| `fullWidth` | `boolean` | `false` | 100% 너비 |

**크기 스펙:**

| Size | Height | Padding X | Font | Border Radius |
|------|--------|-----------|------|---------------|
| S | 34px | 14px | 13px/600 | 8px |
| M | 42px | 20px | 14px/600 | 8px |
| L | 50px | 28px | 16px/600 | 8px |

**상태(States):**

| 상태 | 스타일 |
|------|--------|
| Default | background: `--color-primary` |
| **Hover** | background: `--color-primary-dark`, transition: 150ms ease |
| **Focus** | outline: 3px solid `--color-primary-light`, outline-offset: 2px |
| **Disabled** | background: `--color-border`, color: `--color-text-tri`, cursor: not-allowed, pointer-events: none |
| **Loading** | 버튼 너비 고정 + 스피너(흰색 원형) + "처리중..." 텍스트 |

**Variant 스타일:**

| Variant | Background | Text | Border |
|---------|------------|------|--------|
| primary | `--color-primary` | `#FFFFFF` | none |
| secondary | `--color-surface` | `--color-text` | `1px solid --color-text` |
| outline | transparent | `--color-primary` | `1px solid --color-primary` |
| ghost | transparent | `--color-text-sec` | `1px solid --color-border` |
| danger | `--color-error` | `#FFFFFF` | none |

### 4.2 InputField (`<YMInput>`)

| Prop | 타입 | 설명 |
|------|------|------|
| `placeholder` | `string` | 플레이스홀더 텍스트 |
| `value` | `string` | 입력값 |
| `disabled` | `boolean` | 비활성 (배경: `--color-border-light`) |
| `icon` | `string` | 우측 아이콘 |
| `type` | `'text' \| 'password' \| 'email'` | 입력 타입 |

**스펙:** Height 46px, border-radius 8px, padding 0 16px, border 1.5px solid `--color-border`

**상태(States):**

| 상태 | 스타일 |
|------|--------|
| Default | border: 1.5px solid `--color-border` |
| **Focus** | border: 1.5px solid `--color-primary`, outline: 3px solid `--color-primary-light` |
| **Error** | border: `--color-error`, background: #FFF5F5, 하단 에러 메시지(빨강 + X 아이콘) |
| **Success** | border: `--color-success`, background: #F0FDF9, 우측 체크 아이콘 |
| **Disabled** | background: `--color-border-light`, opacity: 0.6, cursor: not-allowed |

**폼 필드 패턴:** `라벨 (필수 * 빨강) → 인풋 → 헬퍼/에러 텍스트 (12px)`

### 4.3 Badge

| Variant | Background | 용도 |
|---------|------------|------|
| primary | `--color-primary` | 진행중, 공지 뱃지 |
| secondary | `--color-secondary` | 행사 뱃지 |
| success | `--color-success` | 운영 뱃지 |
| muted | `--color-border` | 기타, 종료 뱃지 |
| outline | transparent + border | 태그 |

**스펙:** padding 3px 10px, border-radius `--radius-pill`, font 12px/600

### 4.4 Header (`<YMHeader>`)

- 높이: 68px, 하단 border
- **배경 2모드:** 홈은 **투명**으로 시작(hero 위에 흰 글자) → 스크롤 시 흰 배경으로 전환. 그 외 페이지는 항상 흰 배경
- 좌측: 로고 (클릭 시 홈으로 이동)
- 중앙: 네비게이션 (프로그램, 청년센터, 공지사항) — 활성 탭: primary 색상 + 하단 2px border. ￦홈￧은 로고가 대신하므로 메뉴에서 제외
- 우측: 검색 · 알림 · 구분선 · 로그인(아이콘, 로그인 후 마이페이지/알림으로 전환)
- **전체 너비 1440px, 패딩 0 80px**

### 4.5 Footer (`<YMFooter>`)

- 배경: #FAFAFA, 상단 border
- 좌측: 로고 + 정책 링크 + SNS 아이콘 (IG, YT, KT, FB) + Copyright
- 우측: 이메일 + 제작 크레딧
- **전체 너비 1440px, 패딩 36px 80px 32px**

### 4.6 ProgramCard

- 너비: 280px (compact: 240px, 즐겨찾기 4열용)
- 이미지 영역: height 175px (compact: 140px), 좌상단 상태 뱃지, 우상단 즐겨찾기 별
- 정보: 제목(15px/600, compact: 14px), 센터명(12px), 기간(12px)
- **신청하기 버튼**: primary 보더 + primary 텍스트 + 체크 아이콘 (outline 스타일)
- border-radius: `--radius-md`, shadow: `--shadow-md`

**카드 상태(States):**

| 상태 | 스타일 |
|------|--------|
| Default | shadow: `--shadow-md`, border: `--color-border-light` |
| **Hover** | shadow 강화(`0 8px 24px rgba(63,48,233,0.14)`) + border: `--color-primary`, transition: 200ms |
| **마감** | opacity: 0.55, 신청 버튼 → border: `--color-border`, color: `--color-text-tri`, cursor: not-allowed |
| :active | transform: scale(0.98) |

### 4.7 NoticeCard

- 너비: 280px, 이미지 140px, 카테고리 뱃지
- 제목 + 설명(2줄 말줄임) + 날짜/조회수

### 4.8 Toast (`<YMToast>`)

- 위치: 화면 상단 중앙 (top: 24px), z-index 최상위
- 크기: auto width, padding 14px 24px, border-radius 12px
- 그림자: `0 8px 32px rgba(0,0,0,0.12)`
- 아이콘: 22px 녹색(#22C55E) 원 + 흰색 체크
- 애니메이션: slideDown 300ms → 3초 유지 → fadeOut 300ms 자동 사라짐
- 사용 예: "가입되었습니다", "신청되었습니다.", "취소되었습니다.", "URL이 클립보드에 복사되었습니다.", "즐겨찾기 해제되었습니다."

### 4.9 모달 시스템 (공통 — 3계층)

모든 팝업/알림/확인창은 아래 공통 컴포넌트로 통일. **개별 모달을 손으로 짜지 말 것.**

**① `Modal` — 백드롭 프리미티브**
| Prop | 타입 | 설명 |
|------|------|------|
| `onClose` | `()=>void` | 배경 클릭 / **Esc 키**로 닫기 |
| `children` | `node` | 임의 콘텐츠 |
- 고정 dim `rgba(0,0,0,0.45)`, z-index 500, 중앙 정렬, fadeIn 180ms. 자체 카드 없이 콘텐츠만 감쌀.

**② `ModalCard` — 제목+닫기 있는 카드 셰** (콘텐츠형 모달: 지도, 알림 설정 등)
| Prop | 타입 | 설명 |
|------|------|------|
| `title` | `string` | 상단 제목 (+우측 X 버튼) |
| `width` | `number` | 기본 460, max 90vw |
| `footer` | `node` | (선택) 하단 고정 액션 영역 |
| `onClose` | `()=>void` | 닫기 |
- border-radius 16, 그림자 `0 20px 60px rgba(0,0,0,0.18)`, 헤더 구분선.

**③ `ConfirmDialog` — 확인/알림 다이얼로그** (아이콘+제목+메시지+버튼)
| Prop | 타입 | 설명 |
|------|------|------|
| `icon` | `string` | 56px 원형 아이콘 (선택) |
| `variant` | `'default'\|'danger'\|'success'` | 아이콘/버튼 색 |
| `title` `message` | `string` | 제목·본문 (중앙 정렬, keep-all) |
| `confirmText` `cancelText` | `string` | 버튼 라벨 |
| `alert` | `boolean` | true면 단일 확인 버튼 (알림용) |
| `disabled` | `boolean` | 확인 버튼 비활성 |
| `children` | `node` | (선택) 본문 아래 추가 UI — 취소사유 라디오 등 |
| `onConfirm` `onClose` | `()=>void` | 확인 / 닫기 |
- 너비 440, padding 32/28/24, 중앙 정렬. 확인=danger면 danger 버튼, 아니면 primary. 좌=ghost(취소).
- **사용 예:** 신청 취소(라디오 children, danger), 로그아웃 확인, 회원 탈퇴, 단순 알림(alert)

### 4.10 CancelModal — 신청 취소 사유 선택 (`ConfirmDialog` 조합 예시)

- 너비: 460px, border-radius 16px
- 제목: "신청 취소하시겠습니까?" + 안내 문구
- 취소 사유 (라디오, 필수): 단순 변심 / 일정이 맞지 않음 / 중복 신청 / 개인 사유 / 기타
- [기타] 선택 시 우측(하단) Input 활성화 → 직접 입력
- 미선택 후 제출 시 헬프 텍스트: "필수 선택 항목입니다." (빨강)
- 버튼: 돌아가기(ghost) / 신청 취소(danger)
- 완료 시: 모달 닫힘 → 토스트 "취소되었습니다." → 신청 상태 '취소'로 변경

### 4.11 FilterModal (`<YMFilterModal>`) — 지역/센터 전체 보기

- 너비: 560px, max-height 560px, border-radius 16px
- 헤더: "{지역|청년센터} 전체 보기" + 닫기(X)
- 검색 인풋 + 3열 체크박스 그리드 (다중 선택)
- 항목: 지역명/센터명 + 건수
- 푸터: 선택 초기화 / 취소(ghost) / N개 적용(primary)
- 프로그램 목록 필터의 "지역 더보기 / 청년센터 더보기" 클릭 시 노출

### 4.12 NotificationPanel (`<NotificationPanel>`) — 알림 드롭다운

- 너비: 380px, border-radius 14px. 헤더 벨 아이콘 클릭 시 노출
- 헤더: "알림" + 안읽음 카운트 뱃지 + "모두 읽음"
- 항목: 상태별 색 점(승인=success/마감임박=warning/공지=primary/반려=error) + 텍스트(한 줄 말줄임) + 시간
- **안읽음:** primary 좌측 액센트 바(3px) + primaryBg 틴트 + 볼드 텍스트 + "NEW" 뱃지
- **읽음:** 흰 배경 + 액센트 없음 + 회색 텍스트 + "읽음" 라벨
- 푸터: "알림 전체보기"

### 4.13 UserMenu (`<UserMenu>`) — 계정 드롭다운

- 너비: 240px. 로그인 상태 헤더의 아바타+이름 클릭 시 노출
- 상단: 아바타(이니셜) + {이름}님 + 이메일
- 메뉴: 프로그램 신청 내역 / 즐겨찾기한 프로그램 / **알림 설정** / 개인 정보 수정
- **각 메뉴는 마이페이지 해당 탭으로 딥링크** (신청내역→history, 즐겨찾기→favorites, 알림 설정→noti, 개인정보→profile). 아바타+이름 클릭은 마이페이지 기본 탭으로 이동
- 하단(구분): 로그아웃
- > 로그아웃은 헤더에 단독 노출하지 않고 이 메뉴 안에 배치

---

## 4-S. 공통화 표준 결정 (Toast / Modal / Dropdown)

> **배경(현행 코드베이스 결함):** Toast JS가 `center-map.js` 내부에만 존재해 재사용 불가 · 범용 Modal 클래스 없이 `.mypage-modal` 1건만 존재 · `alert()` 14곳 산재(notice/program/signup 등) · 헤더 드롭다운이 CSS `:hover`/`:focus-within`만으로 열려 JS 토글·`slideDown` 미적용.
> 아래는 프로토타입(`Youth-Moa Prototype.html`)에 이미 구현된 표준을 기준으로 한 **확정 스펙**. 각 프리미티브를 전역 공용 모듈로 1벌만 두고 개별 화면은 이를 호출한다.

### 4-S.1 Toast — 전역 단일 인스턴스 (`alert()` 전면 대체)
- **구조:** 앱 루트에 `ToastProvider` 1개 → `useToast()` 훅이 반환하는 `toast(msg)` 호출. 개별 화면·스크립트에 Toast DOM/타이머를 두지 않는다(center-map.js 내부 정의 제거).
- **스펙:** 상단 중앙 `top:90px`, `z-index:1000`, 흰 배경 카드(radius 12 · shadow `0 8px 32px rgba(0,0,0,0.14)` · `1px borderLight`), 좌측 성공 원형 아이콘(22px · `#22C55E` 체크) + 15px/500 메시지. 진입 `slideDown 300ms`, **2800ms 후 자동 소멸**.
- **`alert()` 14곳 → `toast()` 치환**: 저장/신청/취소/복사/즐겨찾기 등 단순 피드백은 전부 Toast. **파괴적 확인이 필요한 것만** `ConfirmDialog`로(신청취소·로그아웃·회원탈퇴). 로딩 지연 안내 등 긴 문구는 Toast 대신 인라인 상태.
- `role="status"` / `aria-live="polite"` 부착(§10-A).

### 4-S.2 Modal — 범용 3계층 (§4.9 재확인) + M1~M3 결정
`.mypage-modal` 같은 화면 전용 클래스 금지. **모든 팝업 = `Modal`(백드롭) → `ModalCard`(제목셸) / `ConfirmDialog`(확인·알림)** 조합으로만 생성.

- **M1 — 포커스 트랩(Tab 순환 범위):** 트랩 대상 = **모달 카드(`role="dialog"` `aria-modal="true"`) 내부의 포커서블 요소 전체**(닫기 X 버튼 포함, 백드롭 제외). 열릴 때 첫 포커스 = 제목 다음 첫 인터랙티브 요소(없으면 카드 컨테이너 `tabindex=-1`). Tab이 마지막 요소를 넘어가면 첫 요소로, Shift+Tab이 첫 요소 앞이면 마지막으로 순환. `Esc` 닫기 + **닫힐 때 열기 전 트리거로 포커스 복귀**. 배경 본문은 `inert`(또는 `aria-hidden`)로 스크린리더·탭 이동 차단.
- **M2 — 모바일 풀스크린 전환:** **`<768px`(모바일 브레이크포인트, §9)에서 중앙 카드 → 화면 꽉 채우는 시트**로 전환.
  - 기본(콘텐츠형 `ModalCard`, 예: 필터 전체보기·지도·알림설정) = **하단 시트**: `width:100vw`, 하단 정렬, 상단만 radius 16, `max-height:90vh` 내부 스크롤, 상단 그랩 핸들 표시.
  - 짧은 확인창(`ConfirmDialog`) = 모바일에서도 중앙 카드 유지(풀스크린 과함), 좌우 마진 20px·`width:calc(100vw-40px)`.
- **M3 — 다중 모달 스태킹:** z-index를 겹칠 때마다 **백드롭 1겹만** 쓴다(백드롭 누적 금지 — 어두워짐 방지). 규칙: 최상위 모달만 자기 백드롭 `rgba(0,0,0,0.45)`를 렌더, 그 아래 모달은 백드롭 없이 카드만 유지. z-index는 base 500에서 스택마다 +10(백드롭 500 / 카드 501, 다음 스택 510 / 511…). 파생 모달을 닫으면 이전 모달과 그 포커스 트랩으로 복귀. **가능하면 스택은 2단까지만** — 그 이상은 단일 모달 내부 스텝(멀티스텝) 패턴으로 대체 권장.

### 4-S.3 Dropdown — D1~D3 결정 (헤더 유저/알림 · 필터 드롭다운 공통)
- **D1 — 트리거 정책: 클릭 토글(hover 아님).** 데스크톱·모바일·터치 모두 동일하게 **click/tap 토글**. CSS `:hover`/`:focus-within` 개폐 제거 → JS `open` 상태로 제어. 열림 시 `slideDown 180ms`(`.dropdown-enter`). 닫힘: 바깥 클릭(전체 오버레이 캐치) · `Esc` · 항목 선택. `aria-haspopup="menu"` `aria-expanded` 부착, 방향키 항목 이동 권장(§10-A). *(hover 오픈은 터치·키보드·모바일에서 불가·오작동하므로 배제.)*
- **D2 — 유저 드롭다운 항목(§4.13 정정):** 현재 확정본은 **A안 = 프로필 요약 행 1개(아바타+{이름}님+이메일, 클릭 시 마이페이지) + 구분선 + 로그아웃**, 2개 항목만. 신청내역·즐겨찾기·**알림 설정**·개인정보 수정 딥링크는 드롭다운이 아니라 **마이페이지 상단 탭 바**로 이동(§5.8 A안). → §4.13의 4개 메뉴 리스트는 **폐기**, 이 A안이 최신. (이메일은 노출 유지.)
  - 알림 벨 드롭다운(§4.12 `NotificationPanel`)은 별도 유지: 리스트 + 모두읽음 + 빈상태 + "알림 전체보기".
- **D3 — shadow / radius / hover 값(스크린샷: `screenshots/d3-user-dropdown.png`):**
  - 패널: `background #FFFFFF`, `border 1px borderLight`(oklch(0.95 0.007 280)), **radius 14px**, **shadow `0 12px 40px rgba(0,0,0,0.14)`**, `overflow:hidden`. 유저메뉴 width 220~240px, 알림 패널 380px.
  - 항목 hover: `.btn-hover` = `filter:brightness(0.92)` + `translateY(-1px)`, transition 150ms. (드롭다운 항목엔 translateY 없이 배경 `primaryBg`(oklch(0.96 0.0255 280)) hover만 적용 권장.)
  - 항목 구분선 `1px borderLight`, 항목 padding `11~16px 18px`, 아이콘-라벨 gap 10px.
  - 필터 드롭다운(§5.2 지역/센터): 위와 동일 셸 + radius 12 · `border 1px border` · 항목 8개↑면 상단 내부 검색창 + 스크롤.

---

## 5. 페이지 구조

### 5.1 홈 (`/`)
```
Header
├─ Hero Banner (488px, 배경 이미지 로테이션 + Method A scrim + 검색바)
├─ Quick Stats (진행중 프로그램 / 참여 센터 / 누적 참여자)
├─ 프로그램 섹션 (4열 카드 그리드)
├─ 공지사항 섹션 (4열 카드 그리드)
├─ 공간안내 섹션 (3열 이미지 + overlay 텍스트)
Footer
```

#### 5.1.1 히어로 배경 이미지 로테이션 (Method A — scrim 오버레이)
- **동작:** 배경 사진 여러 장을 **8초 간격**으로 순환, 전환은 **opacity 1.2s ease-in-out 크로스페이드**. 사진에는 텍스트를 넣지 않고(반응형·가독성), 카피·검색바는 HTML로 얹음.
- **구현:** 각 이미지를 `position:absolute; inset:0`로 겹쳐 쌓고 현재 인덱스만 `opacity:1`, 나머지는 `opacity:0`. `setInterval`로 인덱스 순환(언마운트 시 `clearInterval`). 사진 위에 **① 브랜드 틴트 scrim**(인디고 그라데이션 135deg, 0.80→0.48) + **② 하단 darken scrim**(하단 42% 어둡게, 검색바·인기검색어 가독성 확보)을 순서대로 덮음. `prefers-reduced-motion` 시 전환/자동순환 비활성 권장.
- **후보 8종 (전부 텍스트 없는 landscape, 실 로드 확인):** 시안 파일 `Hero 배경 시안.html` 참조. 프로토타입 기본 로테이션은 아래 6종(A·C·E·F·G·H) 사용, 필요 시 교체.

| 코드 | Unsplash photo-id | 설명 | 그룹 |
|------|-------------------|------|------|
| A | `1531482615713-2afd69097998` | 협업 테이블 (현재/기본) | 실내·협업 |
| B | `1522071820081-009f0129c71c` | 코워킹(오버헤드) | 실내·협업 |
| C | `1543269865-cbf427effbad` | 학습 그룹 (밝은·다양성) | 실내·협업 |
| D | `1517048676732-d65bc937f952` | 노트 필기 미팅 (차분·전문) | 실내·협업 |
| E | `1523580494863-6f3031224c94` | 세미나·강연장 | 실외·커뮤니티/이벤트 |
| F | `1511632765486-a01980e01a18` | 야외 청년 모임 (노을·유대감) | 실외·커뮤니티/이벤트 |
| G | `1528605248644-14dd04022da1` | 커뮤니티 모임 테이블 | 실외·커뮤니티/이벤트 |
| H | `1540575467063-178a50c2df87` | 행사·관객 (역동적) | 실외·커뮤니티/이벤트 |

> URL 형식: `https://images.unsplash.com/photo-{id}?w=1440&h=560&fit=crop` — 운영 시엔 청년센터 실제 사진(가로형, 텍스트 없음)으로 교체 권장. scrim 2겹은 그대로 유지하면 어떤 사진이 와도 카피 가독성이 확보됨.


### 5.2 프로그램 목록 (`/programs`)
```
Header
├─ 페이지 타이틀 "프로그램"
├─ [좌측] 필터 사이드바 (220px)
│   ├─ 지역 체크박스 (12개 시·군)
│   └─ 청년센터 체크박스 (14개 센터)
├─ [우측] 메인 콘텐츠
│   ├─ 정렬 바 (전체 N건, 진행중만 보기 토글, 정렬 옵션)
│   ├─ 3열 카드 그리드
│   └─ 페이지네이션
Footer
```

### 5.3 프로그램 상세 (`/programs/[id]`)
```
Header
├─ max-width 1000px 중앙 정렬
├─ [좌측] 썸네일 (340×340)
├─ [우측] 정보 패널
│   ├─ 상태 뱃지 (카테고리 미표시 — 제목 아래 센터·지역 노출)
│   ├─ 제목 + 센터명·지역
│   ├─ 정보 테이블 (신청기간, 진행기간, 장소, 모집인원, 문의처, 첨부파일)
│   └─ 액션 (즐겨찾기, 공유, 신청하기 CTA)
├─ 프로그램 설명 (에디터 콘텐츠)
Footer
```

### 5.4 프로그램 신청 (`/programs/[id]/apply`)
```
Header
├─ max-width 700px 중앙 정렬
├─ 프로그램 요약 카드
├─ 신청자 정보 (이름, 연락처, 이메일, 주소 — 회원정보 자동채움)
├─ 추가 정보 (주관식, 객관식, 파일 업로드 — 프로그램별 동적)
├─ 개인정보 수집 동의
├─ 신청하기 버튼
Footer
```

### 5.5 로그인 (`/login`)
```
Header
├─ 중앙 정렬 (400px)
├─ 아이디 / 비밀번호 입력
├─ 아이디 저장 체크박스 + 아이디/비밀번호 찾기 링크
├─ 로그인 버튼 (L/primary) + 회원가입 버튼 (L/secondary)
Footer
```

### 5.6 아이디 찾기 (`/find-id`)
```
Header
├─ 중앙 정렬 (480px)
├─ Stepper (가입정보 입력 → 아이디 찾기) — 활성 스텝: primary 색상
├─ 이름 + 핸드폰 번호 입력
├─ 확인 버튼 (M/primary)
Footer
```

### 5.7 회원가입 (`/signup`)
```
Header
├─ max-width 800px 중앙 정렬
├─ 계정 정보 섹션 (2단 그리드: 라벨 140px + 필드)
│   ├─ 아이디 + 중복확인 버튼
│   ├─ 비밀번호
│   └─ 비밀번호 확인
├─ 개인 정보 섹션
│   ├─ 이름
│   ├─ 핸드폰 번호 + 인증요청 버튼
│   ├─ 성별 (라디오)
│   ├─ 생년월일 (캘린더 피커)
│   └─ 주소 (우편번호 검색 + 주소 + 상세주소)
├─ 이용약관 동의 (체크박스 + 약관보기 링크)
├─ 취소 (L/ghost) + 회원가입 (L/primary)
Footer
```

### 5.8 마이페이지 공통 레이아웃 — **대시보드형 (A안)**
```
[콘텐츠 최대폭 1080px 중앙 정렬 — 사이드바 없음]
├─ 프로필 요약 카드
│   ├─ Avatar(58px) + "반가워요, {이름}님!" + 이메일
│   ├─ 관심 태그 칩 (관심 지역 · 관심 분야, primaryLight)
│   └─ [우측 KPI] 진행중인 신청 N · 종료된 신청 N · 즐겨찾기 N (구분선, 클릭 시 해당 탭 이동)
├─ 탭 바 (세그먼트형): 신청 현황 / 즐겨찾기 / 알림 설정 / 개인정보 수정 — pill 컨테이너(연회색 배경 + 활성 primaryLight 알약), 언더라인 미사용(헤더/카드 하단선과 라인 중복 방지)
└─ 탭 콘텐츠 (흰 라운드 카드로 감쌈 — h3 하단 하드 구분선 제거, 카드 경계로 섹션 구분)
```
> 구 사이드바(220px) 레이아웃 폐기. 모바일과 구조 동일(프로필 카드 + 탭)해서 반응형 전환 자연스러움.
> 헤더 유저 메뉴 딥링크는 탭 key(history/favorites/noti/profile)로 그대로 유지.

### 5.9 마이페이지 — 신청현황 (`/mypage/history`)
```
(프로필 카드 + 탭 바)
├─ "프로그램 신청 내역"
│   ├─ 부제: "최대 지난 3년간의 프로그램 신청 내역까지 확인할 수 있어요"
│   ├─ 기간 필터 (3개월 / 6개월 / 1년 / 3년)
│   ├─ 구분선
│   └─ 신청 카드 리스트: 신청일시 + "신청 상세 ›" 링크 + 썸네일 + 정보(제목 클릭 → 프로그램 상세) + 상태별 버튼(신청취소/재신청)
│       운영중단(status='중단') 프로그램은 상태 뱃지 옆 "운영중단" 칩(muted) 추가
│       또는 빈 상태: 큰 아이콘 + "신청한 프로그램이 없습니다." + "프로그램 보기" CTA
Footer
```
> 신청 상세(`/mypage/history/[id]`): 신청자 정보 + 신청 이력 아코디언 + (대기/승인 시) 신청 취소 버튼. WF-3-001 기획서 정합.
> 상태색: 대기=검정 · 승인=파랑 · 반려=빨강 · 취소=회색
```

### 5.10 마이페이지 — 즐겨찾기 (`/mypage/favorites`)
```
(사이드바)
├─ [우측 콘텐츠]
│   ├─ "즐겨찾기한 프로그램"
│   ├─ 구분선
│   └─ 4열 ProgramCard 그리드 (compact 사이즈, gap 14px)
│       또는 빈 상태: 큰 별 아이콘 + "즐겨찾기한 프로그램이 없습니다." + "프로그램 보기" CTA
Footer
```

### 5.11 마이페이지 — 개인정보수정 Step 1: 비밀번호 재확인 (`/mypage/profile`)
```
(사이드바)
├─ [우측 콘텐츠]
│   ├─ "개인 정보 수정"
│   ├─ 구분선
│   ├─ "비밀번호 재확인" 소제목
│   ├─ "회원님의 정보를 안전하게 보호하기 위해 비밀번호를 다시 한번 확인해주세요."
│   ├─ 구분선
│   ├─ 2단 그리드: 아이디(읽기전용) + 비밀번호 입력
│   └─ 확인 버튼 (M/primary, 중앙)
Footer
```

### 5.12 마이페이지 — 개인정보수정 Step 2: 수정 폼 (`/mypage/profile/edit`)
```
(사이드바)
├─ [우측 콘텐츠]
│   ├─ "개인 정보 수정"
│   ├─ 구분선
│   ├─ 2단 그리드 (라벨 120px + 필드)
│   │   ├─ 아이디 * (읽기전용)
│   │   ├─ 새 비밀번호 *
│   │   ├─ 새 비밀번호 확인 *
│   │   ├─ 이름 * (읽기전용)
│   │   ├─ 핸드폰 번호 * (읽기전용)
│   │   ├─ 주소 * (읽기전용 + 재검색 버튼) + 상세주소
│   │   ├─ 성별 * (라디오: 남/여)
│   │   └─ 생년월일 * (캘린더 피커)
│   ├─ 구분선
│   └─ 탈퇴하기 (L/ghost) + 회원정보 수정 (L/primary) — 중앙 정렬
Footer
```

### 5.13 공지사항 목록 (`/notices`)
```
Header
├─ 카테고리 탭 (전체/행사/공지/운영/기타) — pill 버튼
├─ 테이블 (No, 구분, 제목, 작성일, 조회수)
│   ├─ 고정글: primaryBg 배경 + 📌 아이콘
│   └─ 신규글: 'N' 뱃지
├─ 페이지네이션
Footer
```

### 5.14 공지사항 상세 (`/notices/[id]`)
```
Header
├─ max-width 900px 중앙 정렬
├─ 카테고리 뱃지 + 고정 표시
├─ 제목 + 작성일/조회수
├─ 본문 (에디터 콘텐츠)
├─ 첨부파일 다운로드
├─ 이전글/다음글 네비게이션
├─ 목록으로 버튼
Footer
```

### 5.15 청년센터 찾기 (`/centers`)
```
Header
├─ 페이지 타이틀 "청년센터 찾기"
├─ [좌측 360px] 검색 + 시·군 필터 + 정렬(이름순/프로그램많은순) + 센터 카드 리스트(독립 스크롤)
│   └─ 센터 카드: 이름 + 운영중/종료 뱃지 + 주소 + 운영시간/전화 + 진행중 프로그램 수
├─ [우측 flex] 지도 (카카오맵)
│   ├─ 마커 (센터 위치) — 선택 시 라벨 표시 + 강조
│   ├─ "이 지역에서 검색" 버튼 (지도 이동 시 상단 중앙 노출)
│   └─ 선택 센터 정보 팝업 (이름/주소/운영시간/전화 + 프로그램 보기 CTA)
Footer
```
> **개발 메모:** 지도는 **카카오맵 JavaScript SDK** 연동 필요. 센터 데이터에 위경도(lat/lng) 필드 추가 → 마커 렌더링. 시안의 MapPlaceholder는 연동 전 대체물.
> **모바일:** 지도 풀폭 + **드래그 핸들 바텀시트**(센터 리스트). 마커 탭 → 시트 상단에 해당 센터 노출.
>
> **⚠️ 센터 수가 많을 때(40~60+) 처리 — 개발 필수 반영:**
> - **마커 클러스터링**: 카카오맵 `MarkerClusterer`로 인접 마커를 "N개" 원으로 묶고, 줌인/클릭 시 펼침 (마커 그대로 찍으면 겹쳐서 사용 불가)
> - **"이 지역에서 검색"**: 지도 `idle`(이동/줌 종료) 이벤트 → 버튼 노출 → 현재 bounds 내 센터만 목록·마커 갱신 (전체 로드 대신 영역 기반)
> - **목록 무한 스크롤**: 리스트는 독립 스크롤 영역, 스크롤 하단 도달 시 추가 페치(커서/offset). 상단에 "총 N개" 카운트 고정
> - **지도↔목록 동기화**: 카드 hover→마커 강조, 마커 클릭→해당 카드로 스크롤 (양방향)
> - **정렬**: 이름순 / 프로그램많은순 / (위치 권한 시) 내 주변순
> - 시안엔 정렬·"이 지역에서 검색"·독립 스크롤 적용됨. 클러스터링·내 주변순·동기화는 SDK 연동 단계에서 구현

### 5.16 검색 결과 (`/search?q=`)
```
Header
├─ 검색바 (쿼리 + 지우기 + 검색 버튼)
├─ "'{쿼리}' 검색 결과 N건"
├─ 탭: 프로그램 / 공지사항 (각 건수)
├─ 4열 카드 그리드
│   └─ 결과 없음: 빈 아이콘 + 안내 + 추천 키워드 칩
Footer
```

### 5.17 시스템 화면 (404 / 500 / 403 / 503 / 세션만료 / 로딩)
```
공통 레이아웃(SystemLayout): 88px 원형 아이콘 배지 + 24px 제목 + 15px 안내 + 버튼 + 하단 에러코드/문의
404 — 돋보기+물음표 아이콘(primary) + "페이지를 찾을 수 없습니다" + [이전][홈] + Error code 404
500 — 경고 삼각형(빨강) + "일시적 오류" + [새로고침][홈] + Error code·문의
403 — 방패+자물쇠 아이콘(primary) + "접근 권한이 없습니다" + 관리자 문의 카드 + [이전][홈] — 관리자 전용 라우트 무권한 접근 시
503 — 렌치 아이콘(secondary) + "서비스 점검 중" + 점검시간 카드 + [새로고침] — 점검 모드 전체 리다이렉트, 헤더 숨김
세션만료 — 시계 아이콘(primary) + "로그인이 만료되었습니다" + [홈][다시 로그인] — 토큰 만료 감지 시, 헤더 숨김
로딩 — 스피너 + "불러오는 중" + 스켈레톤 카드 그리드(4열)
```
> 모든 에러/안내 화면은 동일한 SystemLayout 구조 사용. 처리 트리거: 404=존재·경로 오류의 fallback, 403=권한 가드, 503=점검 플래그, 세션만료=토큰 인터셉터.
> 스켈레톤: 카드 형태 유지 + borderLight 블록 + (선택) shimmer 애니메이션. 데이터 로딩 중 레이아웃 시프트 방지.

### 5.18 신청 완료 페이지 (`/programs/[id]/apply/complete`)
```
Header
├─ 성공 체크 아이콘 (success 원)
├─ "프로그램 신청이 완료되었습니다"
├─ 안내: 승인 대기 상태 + 이메일/마이페이지 확인 경로
├─ 신청 요약 카드 (썸네일 + 승인대기 뱃지 + 제목/일정/신청일시)
└─ [홈으로] [신청 현황 보기]
Footer
```
> 토스트로 사라지게 하지 않고 완료 페이지로 전환. 가벼운 피드백(즐겨찾기/복사/취소)만 토스트 사용.

### 5.19 법적 문서 — 공통 레이아웃 (`/privacy`, `/terms`, `/email-policy`)
```
Header
├─ [좌측 사이드바 220px] — "법적 문서" 섹션 레이블 + 3개 항목
│   ├─ 개인정보처리방침 (활성 시 primaryBg + primary 테두리)
│   ├─ 이용약관
│   └─ 이메일주소무단수집거부
└─ [우측 콘텐츠]
    ├─ h1 제목 + 시행일 (textTri)
    ├─ 본문 (장 제목 16px/700, 조 제목 14px/600, 본문 13px)
    └─ 개인정보처리방침: 수집 항목 테이블 (3열)
Footer
```
> **모바일:** 제목 헤더(뒤로가기+타이틀) + 본문 + **하단 관련 문서 링크** (다른 2개 문서 버튼)
> **적응형:** 데스크톱=사이드바, 모바일=타이틀 헤더만. 탭 바 미사용 (헤더와 충돌)

### 5.20 법적 문서 — 이메일주소무단수집거부
```
(사이드바 동일)
├─ 이메일 아이콘 (primary 원) + 빨간 X
├─ h1 "이메일주소 무단수집거부"
└─ 안내 텍스트 카드 (centered, surface bg)
```

---

## 5-E. 사용성 개선 기능 (✨ 추가 제안 — 권장)

레퍼런스: 공공 청년플랫폼/강좌·예약 서비스 패턴. "조회→신청" 퍼널 전환율과 탐색 경험을 높이기 위한 제안.

### 5-E.1 모집현황 시각화 (`CapacityBar`, `DdayChip`)
```
· D-day 칩: 카드 썸네일 좌상단. D-3 이하=error(빨강), 그 외=반투명 검정. 마감="마감" 칩
· 정원 진행바: applied/cap 비율. 90%+=error, 70%+=warning, 그 외=primary
· 상세: 모집현황 패널(신청률 %, 경쟁률 N:1, 남은 일수)
· 데이터: Program에 currentApplicants/capacity 필요 (이미 존재), dday=applyEnd-now 계산
```

### 5-E.2 큐레이션 탐색 (`HomeCurationSection`)
```
· ⚠️ 분야(카테고리) 칩은 현재 미적용 — 카테고리 체계 확정 후 도입 예정
· 홈 "지금 주목할 프로그램": 마감임박 / 이번 주 신규 / 인기 탭 (4열 카드)
· 목록 필터는 지역 + 청년센터 (모집상태 필터는 미적용 — 상태는 카드 D-day/정원바로 이미 노출)
· 카테고리 도입 시: Program.category 필드 + 분야 칩 + /api/categories 활성화
```

### 5-E.3 캘린더 뷰 (`ProgramCalendarView`)
```
· 목록 우측 상단 뷰 전환 토글 (그리드 / 캘린더)
· 월 캘린더: 날짜 셀마다 시작 프로그램 칩(모집상태별 색상 점), 최대 2개+"+N건"
· 우측 패널: 선택일 시작 프로그램 리스트 + 모집현황
· 모집상태별 색상 범례 표기 (모집중/마감임박/마감)
· (선택) iCal(.ics) 내보내기로 구글/애플 캘린더 연동
```

### 5-E.4 신청 폼 개선 (`ProgramApplyEnhanced`)
```
· 신청자 정보: 회원정보 자동채움 + "자동 입력됨" 뱃지 (읽기전용 카드, 수정은 링크)
· 추가 정보: 프로그램별 커스텀 질문만 노출 (입력 최소화)
· 3단계 스테퍼 (정보 확인 → 추가 정보 → 완료)
· 알림 안내 박스: 승인 결과 + D-1 리마인더 채널 명시
```

### 5-E.5 대기신청 / 빈자리 알림 (`WaitlistModal`)
```
· 마감된 프로그램: 신청버튼 → "빈자리 알림 받기" (outline) 으로 교체
· 모달: 대상 프로그램 + 알림 채널 선택(체크박스, 복수 선택: 알림톡/이메일)
· 빈자리 발생 시 대기자 순대로 알림 발송
```

### 5-E.6 알림 설정 (`MyPageNotiSettings`)
```
· 사이드바 최상단 "알림 설정" 메뉴 추가
· 알림 채널(복수 선택): 카카오 알림톡 / 문자(SMS) / 이메일 — 각각 토글
· 알림 항목: 승인·반려(필수,잠금) / D-1 리마인더 / 빈자리 / 관심분야 신규 / 마케팅
· 마케팅 동의 변경 시 동의/철회 일시 문자 발송 (정보통신망법 준수)
```

### 5-E.7 프로그램 비활성(운영 중단) 처리 (`ProgramDetailEnhanced inactive`)
admin이 프로그램을 비활성화했을 때 사용자 측 동작. **모달이 아닌 상태 표시 방식**(이커머스 판매중지 패턴).
```
· 목록/검색/추천 → 숨김 (노출 제외)
· 상세 직접 접근(즐겨찾기·공유·검색엔진) → 페이지는 유지, 상단 "운영 중단" 배너 + 이미지 그레이 + 신청 버튼 비활성("신청이 중단된 프로그램입니다")
· **배너 문구 분기**: 신청 이력 있음(pg.appStatus) → "회원님의 신청 내역은 그대로 유효합니다" + [내 신청 현황 보기] 링크 / 신청 이력 없음 → "운영 사정으로 신청이 중단되었어요"(링크 없음)
· **모집중→중단 전환 후 새로고침**(미신청자 실제 진입 경로): 하드 404로 튕기지 않음. 페이지 리소스는 존재하므로 **200 OK + 비활성 상태 UI**로 렌더 (판매중지 상품 패턴). 404는 "URL 오류"라는 다른 의미라 사용 금지
· **신청 클릭 직후 비활성된 경우**: 신청 API가 `409 Conflict`(또는 `410 Gone`) 반환 → "방금 이 프로그램의 모집이 중단되었어요" 안내 후 상세를 비활성 상태로 리로드
· **이미 신청한 사용자**: 마이페이지 신청내역에 절대 숨기지 않음 → 항상 표시, "운영중단" 뱃지(muted) + 안내 문구(타임라인 대신)
· (권장) 비활성 전환 시 신청자에게 알림(알림톡/이메일) 발송 — 알림 설정 "운영 변경" 항목
· 데이터: Program.status에 '중단'(inactive) 추가. 목록 쿼리에서는 제외, 상세·마이페이지에서는 조회 허용
· 프로토타입: 목록에 "청년 목공 클래스 (비활성 데모)" 카드 1개 노출(id:7, status:'중단') — 미신청자 진입 확인용. **실제 서비스에선 목록에서 숨김**

### 5-E.8 지원대상·자격요건 (프로그램 상세)
· 상세 페이지에 "지원 대상 · 자격요건" 섹션 (2열 카드)
· 필드: 연령 / 거주지 / 기타  — **소득기준은 제외**(청년정책 특성상 불필요)
· 하단 주의 문구: "신청 전 자격요건을 반드시 확인" + 미충족 시 반려 가능 안내
· ⚠️ **admin 추가 필요**: 관리자 프로그램 등록/수정 화면에 자격요건 입력 필드(연령/거주지/기타)를 추가해야 상세에 연동됨. Program.eligibility = { age, region, etc }

## 6. API 엔드포인트 (권장)

| Method | Path | 설명 |
|--------|------|------|
| GET | `/api/programs` | 프로그램 목록 (필터: region, center, status, sort, page) |
| GET | `/api/programs/[id]` | 프로그램 상세 |
| POST | `/api/programs/[id]/apply` | 프로그램 신청 |
| GET | `/api/notices` | 공지사항 목록 (필터: category, page) |
| GET | `/api/notices/[id]` | 공지사항 상세 |
| POST | `/api/auth/login` | 로그인 |
| POST | `/api/auth/signup` | 회원가입 |
| GET | `/api/auth/find-id` | 아이디 찾기 |
| GET | `/api/auth/find-password` | 비밀번호 찾기 (인증번호 발송/확인) |
| GET | `/api/mypage/history` | 내 신청 내역 |
| GET | `/api/mypage/favorites` | 즐겨찾기 |
| PUT | `/api/mypage/profile` | 개인정보 수정 |
| POST | `/api/programs/[id]/favorite` | 즐겨찾기 토글 |
| DELETE | `/api/applications/[id]` | 신청 취소 (취소 사유 전송) |
| GET | `/api/centers` | 청년센터 목록 (위경도 포함, 지역 필터) |
| GET | `/api/search?q=` | 통합 검색 (프로그램/공지) |
| GET | `/api/programs/calendar?month=` | 월별 캘린더 — 시작일 기준 프로그램 |
| GET | `/api/categories` | 분야 카테고리 목록 |
| POST | `/api/programs/[id]/waitlist` | 빈자리(대기) 알림 신청 |
| GET·PUT | `/api/mypage/notifications` | 알림 설정 조회/수정 (채널·항목) |

---

## 7. 데이터 모델 (핵심)

```typescript
// 프로그램
interface Program {
  id: number;
  title: string;
  center: string;       // 청년센터명
  region: string;       // 시·군명
  status: '진행중' | '마감' | '예정';
  applyStart: Date;
  applyEnd: Date;
  programStart: Date;
  programEnd: Date;
  location: string;
  capacity: number;
  currentApplicants: number;
  contact: string;
  description: string;  // Rich text / HTML
  thumbnailUrl: string;
  attachments: { name: string; url: string; size: string }[];
  category: string;     // 취업, 창업, 힐링, 교육 등
}

// 공지사항
interface Notice {
  id: number;
  category: '행사' | '공지' | '운영' | '기타';
  title: string;
  content: string;      // Rich text / HTML
  createdAt: Date;
  views: number;
  isPinned: boolean;
  attachments: { name: string; url: string; size: string }[];
}

// 사용자
interface User {
  id: number;
  username: string;
  name: string;
  phone: string;
  email: string;
  address: string;
  addressDetail: string;
  gender: '남' | '여';
  birthDate: Date;
}

// 청년센터
interface Center {
  id: number;
  name: string;
  region: string;       // 시·군명
  address: string;
  lat: number;          // 위도 (카카오맵 마커용)
  lng: number;          // 경도
  hours: string;        // 운영시간
  tel: string;
  isOpen: boolean;      // 운영중 여부
  programCount: number; // 진행중 프로그램 수
}

// 신청 (취소 포함)
interface Application {
  id: number;
  programId: number;
  userId: number;
  status: '대기' | '승인' | '반려' | '취소';
  appliedAt: Date;
  processedAt?: Date;
  cancelReason?: string; // 단순 변심 / 일정이 맞지 않음 / 중복 신청 / 개인 사유 / 기타
}
```

---

## 8. Next.js 폴더 구조 (권장)

```
src/
├── app/
│   ├── layout.tsx              # 루트 레이아웃 (Header + Footer)
│   ├── page.tsx                # 홈
│   ├── login/page.tsx
│   ├── signup/page.tsx
│   ├── find-id/page.tsx
│   ├── programs/
│   │   ├── page.tsx            # 목록
│   │   └── [id]/
│   │       ├── page.tsx        # 상세
│   │       └── apply/page.tsx  # 신청
│   ├── notices/
│   │   ├── page.tsx            # 목록
│   │   └── [id]/page.tsx       # 상세
│   ├── mypage/
│   │   ├── layout.tsx          # 마이페이지 사이드바 레이아웃
│   │   ├── history/page.tsx
│   │   ├── favorites/page.tsx
│   │   └── profile/page.tsx
│   └── api/                    # API Routes
├── components/
│   ├── ui/                     # 공통 UI
│   │   ├── Button.tsx          # YMButton (S/M/L, 5 variants)
│   │   ├── Input.tsx           # YMInput
│   │   ├── Badge.tsx
│   │   ├── Pagination.tsx
│   │   └── Icon.tsx
│   ├── layout/
│   │   ├── Header.tsx
│   │   └── Footer.tsx
│   ├── program/
│   │   ├── ProgramCard.tsx
│   │   ├── ProgramFilter.tsx
│   │   └── ProgramApplyForm.tsx
│   ├── notice/
│   │   ├── NoticeCard.tsx
│   │   └── NoticeTable.tsx
│   └── mypage/
│       └── MyPageSidebar.tsx
├── lib/
│   ├── tokens.ts               # 디자인 토큰 상수
│   └── api.ts                  # API 클라이언트
└── styles/
    └── globals.css             # CSS Variables + Tailwind config
```

---

## 9. 반응형 브레이크포인트

### 9.0 구현 전략 — 적응형(Adaptive), 단일 코드베이스
- **원칙:** "보기엔 모바일 화면을 따로 만든 것" 같지만, **코드는 한 벌 + 레이아웃만 분기**한다. 완전 분리(m.도메인/별도 코드)는 금지.
- **공유:** 디자인 토큰, 색·타이포, 공통 컴포넌트(Button/Input/Badge/Card), 페이지 로직(데이터 패칭·상태)은 **1벌만** 유지.
- **분기:** 레이아웃/내비게이션만 화면 폭에 따라 바꾼다.
	- 데스크톱 상단 네비 ↔ 모바일 하단 탭바
	- 필터 사이드바 ↔ 바텀시트 모달
	- 지도+리스트 분할 ↔ 지도+바텀시트
	- 다열 그리드 ↔ 1~2열
- **구현(Next.js):** `useMediaQuery` 훅으로 `<DesktopX/>` vs `<MobileX/>` 선택하거나, Tailwind `hidden md:flex` / `flex md:hidden`로 같은 페이지에서 두 레이아웃 토글. 페이지 컴포넌트는 데이터·상태를 1벌로 들고, 프레젠테이션만 분기.


| 이름 | 너비 | 설명 |
|------|------|------|
| Desktop | ≥1440px | 기본 디자인 |
| Laptop | 1024–1439px | 패딩 축소, 카드 3열→2열 |
| Tablet | 768–1023px | 사이드바 접힘, 카드 2열 |
| Mobile | <768px | 단일 열, 하단 탭바 네비게이션 |

### 9.1 모바일 공통 컴포넌트 (baseline 390px)

| 컴포넌트 | 설명 |
|----------|------|
| `MHeader` | 높이 56px. 홈: 로고 + 검색/알림 / 하위 페이지: 뒤로가기 + 중앙 타이틀 + 검색 (핵심 메뉴는 하단 탭바가 담당) |
| `MBottomNav` | 높이 64px 고정(sticky bottom). 4탭: 홈 / 프로그램 / 센터 / 마이 — 활성 탭 primary 색상 (공지사항은 헤더 알림/더보기로 이동) |
| `MFooter` | 축약형 — 로고 + 정책 링크(줄바꿈) + SNS + 카피라이트 |
| `MProgramCard` | 2열 그리드용. 정사각 이미지(aspect 1:1) + 상태 뱃지 + 즐겨찾기 + 신청하기(primary 보더) |

### 9.2 모바일 화면별 레이아웃

**모바일 홈** (`/`)
```
MHeader (로고 + 검색/알림)
├─ Hero (300px, 배너 + gradient + 검색바)
├─ 통계 row (진행중 / 청년센터 / 참여자 — 3분할)
├─ 프로그램 (2열 그리드, 4개)
├─ 공지사항 (리스트형, primaryBg 틴트)
├─ 공간안내 (가로 스크롤 카드)
MFooter
MBottomNav (홈 활성)
```

**모바일 프로그램 목록** (`/programs`)
```
MHeader (타이틀형)
├─ "진행중인 프로그램만 보기" 토글
├─ 전체 N건 + 정렬 + 필터 row
├─ 2열 카드 그리드
├─ 페이지네이션
MFooter / MBottomNav (프로그램 활성)
```

**모바일 프로그램 상세** (`/programs/[id]`)
```
MHeader (타이틀형)
├─ Hero 이미지 (260px, 풀블리드)
├─ 상태 뱃지 + 제목 + 센터
├─ 정보 테이블 (라벨 84px + 값)
├─ 프로그램 설명 이미지
└─ [하단 고정 바] 즐겨찾기 + 공유 + 신청하기 CTA (sticky bottom)
```

**모바일 프로그램 신청** (`/programs/[id]/apply`)
```
MHeader (타이틀형)
├─ 프로그램 정보 요약 카드
├─ 신청자 정보 (이름/연락처/이메일 — 자동채움)
├─ 추가 정보 (주관식/객관식/파일 업로드)
├─ 개인정보 수집 동의 + 체크박스
├─ 신청하기 버튼 (L/primary, full width)
MFooter
```

> **참고:** 데스크톱 상단 네비게이션(홈/프로그램/청년센터/공지사항)은 모바일에서 **하단 탭바(MBottomNav)**로 전환됩니다. 탭바는 홈/프로그램/센터/마이를 전담하고, 헤더는 검색/알림만 둡니다. 공지사항은 헤더 알림 또는 더보기로 접근.

---

---

## 10-A. 웹접근성 가이드 (KWCAG 2.2 — 공공기관 의무)

청년모아는 경기도 공공 서비스이므로 **웹접근성 인증(KWCAG)** 준수가 필수입니다. 개발 시 아래를 반영하세요.

### 마크업 / 시맨틱
- 클릭 요소는 `<div onClick>` 대신 `<button>` / `<a>` 사용 (프로토타입은 시연용 div — 실제 구현은 시맨틱 태그로 전환)
- 페이지 구조: `<header> <nav> <main> <footer>`, 제목 계층 `<h1>`→`<h2>`→`<h3>` 순서 유지
- 폼: 모든 `<input>`에 연결된 `<label>` (또는 `aria-label`), 에러는 `aria-describedby`로 연결
- 아이콘 전용 버튼: `aria-label` 필수 (예: 검색·알림·즐겨찾기·닫기)
- 이미지: 의미 있는 이미지는 `alt`, 장식 이미지는 `alt=""`

### 키보드 / 포커스
- 모든 인터랙션은 키보드(Tab/Enter/Esc)로 동작 — 모달은 포커스 트랩 + Esc 닫기 + 열기 전 트리거로 포커스 복귀
- 포커스 표시: `outline: 3px solid var(--color-primary-light); outline-offset: 2px` (제거 금지)
- 드롭다운(지역 필터·유저 메뉴·알림)은 방향키 탐색 지원 권장

### 색·대비 (대비율 ≥ 4.5:1)
- 본문 `--color-text`(#2B2A3D) on 흰 배경 ✅. **보조 텍스트 `--color-text-tri`(#A6A3B3)는 작은 글자에 단독 사용 금지** — 12px 이하 중요한 정보엔 `--color-text-sec` 이상 사용
- 상태는 **색만으로 구분 금지**: 신청 상태·D-day·정원바는 색 + 텍스트/아이콘 병행 (이미 적용됨 ✅)
- primary `#3F30E9` on 흰색 대비 ✅, 흰 텍스트 on primary ✅

### 동적 콘텐츠
- 토스트·알림은 `role="status"` / `aria-live="polite"`로 스크린리더 통지
- 로딩 상태 `aria-busy="true"`
- `prefers-reduced-motion` 존중 — 애니메이션 축소 옵션

### 기타
- 터치 타깃 최소 44×44px (모바일 하단 탭·버튼 — 이미 충족)
- 자동 재생·자동 이동 캐러셀 지양, 있으면 정지 컨트롤 제공

---

## 10. 디자인 참조

이 명세서와 함께 `Youth-Moa Design.html` 파일을 참조하세요.
디자인 캔버스에서 각 아트보드를 클릭하면 풀스크린으로 확인할 수 있습니다.

**전체 14개 화면 (6개 섹션) + 컴포넌트/모달 라이브러리:**

| 섹션 | 화면 |
|------|------|
| 공통 컴포넌트 | Button · Input · Badge · Color · Typography · Card · Header |
| 모달 & 토스트 | 토스트 · 확인 모달 · 신청 취소 모달 · 필터 전체보기 모달 |
| 홈 | 메인 랜딩 |
| 프로그램 | 목록 · 상세 · 신청 |
| 인증 | 로그인 · 아이디 찾기 · 회원가입 |
| 마이페이지 | 신청현황(데이터/빈상태) · 즐겨찾기(데이터/빈상태) · 개인정보(비밀번호재확인/수정폼) |
| 공지사항 | 목록 · 상세 |
| 검색 · 시스템 | 검색 결과 · 빈 결과 · 404 · 500 · 403 · 503 · 세션만료 · 로딩 · 신청 완료 |
| 🗺️ 센터 찾기 | 지도+리스트 (데스크톱) · 지도+바텀시트 (모바일) |
| 📄 법적 문서 | 개인정보처리방침 · 이용약관 · 이메일무단수집거부 (데스크톱+모바일) |
| 📱 모바일 | 홈 · 목록 · 상세 · 신청 · 완료 · 로그인 · 회원가입 · 마이페이지 · 공지목록 · 공지상세 · 아이디/비밀번호찾기 · 검색 · 404 · 로딩 (390px) |

---

## 11. 변경 이력

| 날짜 | 내용 |
|------|------|
| 2024-05 | 초기 디자인 시안 (12개 화면) |
| — | 메인 컬러 확정: 인디고 → **#3F30E9** (사용자 globals.css 반영) |
| — | 공통 컴포넌트 통합 (YMButton S/M/L, YMInput) |
| — | 마이페이지 기획서 정합화 (빈 상태, 비밀번호 재확인, 수정 폼 분리) |
| — | 모달 & 토스트 추가 (확인/취소사유/필터 전체보기) |
| — | 브랜드 로고 적용 (헤더·푸터), 신청하기 버튼 primary 보더 |
| — | 공지사항 상세 이미지 첨부형, 홈 공지 리스트형 재구성 |
| — | 모바일 보완: 로그인/회원가입/마이페이지/공지목록/공지상세 |
| — | 적응형(Adaptive) 구현 전략 명시 (단일 코드베이스 + 레이아웃 분기) |
| — | 컴포넌트 상태 명세 추가 (Button/Input/Card hover·focus·disabled·loading) |
| — | 디자인 토큰 → `src/app/globals.css` Tailwind v4 `@theme` 동기화, Utility Class 추가 |
| — | 알림 패널 interactive (삭제/모두읽음/빈상태), 스크롤 감지 헤더 |
| — | 모바일 보조: 아이디/비밀번호찾기 · 검색결과 · 404 · 로딩 |
| — | 청년센터 찾기 지역 필터 → 드롭다운+내부검색 (31개 시·군 대응) |
| — | 청년센터 찾기 다수 센터 대응: 정렬(이름순/프로그램많은순) + 목록 독립 스크롤 + "이 지역에서 검색" 버튼, 클러스터링은 HANDOFF 명세 |
| — | 법적 문서 3종 추가 (개인정보처리방침/이용약관/이메일무단수집거부) |
| — | 법적 문서 네비 → 탭 바 제거, 데스크톱=사이드바·모바일=타이틀+하단 관련 문서 링크 |
| 2026-06 | 프로토타입 보완: 로고 심벌+텍스트 콤보, 실제 SNS 아이콘 이미지, 푸터 리디자인 |
| — | 프로그램 목록: **진행예정 필터 + 정렬(기본/마감임박순/인기순)** 추가, 진행예정 카드 처리(오픈 D-N·오픈 알림 받기) |
| — | 빈자리/오픈 알림 채널 선택 = 체크박스(복수), 버튼명 "빈자리 알림 받기"·"오픈 알림 받기"로 통일 |
| — | 캘린더 뷰: 기본 전체 폭, 날짜 클릭 시 우측 패널·오늘 버튼·빈 칸 선택 버그 수정 |
| — | **메인 컬러 인디고 `#3F30E9` 최종 확정** (admin 블루 `#0264FB` → 통일), HEX 대조표 추가 |
| — | 상세: 지원대상·자격요건 섹션 추가 (연령/거주지/기타 — 소득기준 제외, admin 등록 필드 필요) |
| — | 신청내역을 기획서(WF-3-001)와 일치: 기간필터(3/6개월·1/3년) + 신청일시/신청상세 링크 + 상태별 버튼(신청취소/재신청) + 신청 상세 페이지(신청자정보·신청이력 아코디언) |
| — | 마이페이지 신청현황: 접수→검토→승인/반려 상태 타임라인 + 뱃지 매핑 통일(대기/승인/반려/취소) |
| — | 프로그램 목록 페이지네이션 추가 |
| — | 검색 화면 신설: 자동완성 + 최근/인기 검색어 |
| — | 홈 맞춤 추천 섹션(로그인 시, 관심지역·분야 기반) |
| — | 웹접근성(KWCAG) 가이드 섹션 추가 |
| — | 시스템 화면 확장: 403(권한없음)·503(점검)·세션만료 추가, 404를 아이콘 배지형으로 통일(SystemLayout 일원화), 404=라우트 fallback |
| — | 프로그램 비활성(운영 중단) 처리: 목록 숨김 + 상세 비활성 배너/버튼 + 마이페이지 신청내역 "운영중단" 뱃지 유지 |
| — | 헤더 유저 메뉴 딜링크(신청내역/즐겨찾기/알림/개인정보 → 각 탭 직결), Avatar 컴포넌트(이미지 지원·디폴트 이니셜) |
| — | 홈 중복 제거: 로그인 시 맞춤추천만 / 비로그인 시 일반 목록만 노출 |
| — | 공통 모달 컴포넌트 통합: Modal(백드롭,Esc닫기)+ModalCard(제목셸)+ConfirmDialog(확인/알림) 3계층, 취소·지도 모달 리팩토링 |
| — | 신청 취소 모달: "기타" 선택 시 직접 입력 textarea 노출(최대 100자) |
| — | 홈 투명 헤더 스크롤 전환 버그 수정(컨테이너 직접 onScroll), hero 배너 0px 고정(홈=페이드만) |
| — | 캘린더 뷰: 기본 전체 폭, 날짜 클릭 시 우측 패널·오늘 버튼, 요약 칩 필터 통합 |
| — | 상세 진행 장소 "지도" 버튼(ModalCard 지도 미리보기), 구분값·소득기준 제거 |
| — | 청년센터 다수 대응: 정렬 토글·독립 스크롤·"이 지역에서 검색" 버튼 |
| — | **마이페이지 A안 재디자인**: 사이드바 → 프로필 요약 카드(Avatar+관심태그+KPI) + 상단 탭 바 (모바일 동일 구조) |
| — | 비활성 상세 배너 신청자/비신청자 분기("신청 내역 유효" + 신청 현황 링크), 프로토타입에 운영중단 샘플 데이터 추가(마이페이지서 진입 가능) |
| — | **이메일 발송 정책 확정**(§14): 임시비밀번호+환영메일(MVP), 공통 HTML 셸로 통일(헤더·푸터 공유) |
| — | HANDOFF 정리: 신청완료 5.18 중복 제거, 상세 카테고리→센터·지역, 알림설정 메뉴 반영, 헤더 투명모드 명시 |
| — | 정합성 수정: 공지사항 목록에 카테고리 탭(전체/행사/공지/운영/기타)+페이지네이션 구현, 공지 상세(§5.14) 프로토타입 신설(§5.13 명세와 일치) |
| — | 마이페이지 세그먼트형 탭+흰 카드 그룹(하드 라인 중복 제거), 상태 필터 칩(전체/승인/대기/반려/취소) |
| — | **최신 UX/UI 8종 적용**: ①상세 Sticky 하단 CTA바 ②홈 히어로 검색+인기키워드+퀵메뉴 그리드 ③목록 상단 필터바(좌측 사이드바 대체)+정렬 ④상세 정보 카드 그리드 ⑤신청 3단계 위저드(스텝 프로그레스) ⑥스켈레톤 로딩(shimmer) ⑦센터 마커↔리스트 양방향 하이라이트 동기화 ⑧마이크로 인터랙션(버튼 press·카드 lift) |
| — | **홈 히어로 배경 로테이션**(§5.1.1): Method A scrim(브랜드 틴트+하단 darken 2겹) 위에 텍스트 없는 사진 6종 8초 크로스페이드 순환. 후보 8종 시안 `Hero 배경 시안.html` 커밋, 운영 시 실제 청년센터 사진 교체 권장 |
| — | **공통화 표준 결정 §4-S**: Toast 전역 단일화(alert() 14곳 대체) · Modal 범용 3계층 + M1 포커스트랩/M2 모바일 풀스크린(768px)/M3 다중모달 백드롭 1겹 · Dropdown D1 클릭토글(hover 폐지)/D2 유저메뉴 A안 2항목(§4.13 정정)/D3 radius14·shadow 0 12px 40px rgba(0,0,0,.14). 스크린샷 `screenshots/d3-user-dropdown.png` |

---

## 12. 최종 산출물 요약

### 디자인 시안 (Claude 프로젝트)

| 파일 | 내용 |
|------|------|
| `Youth-Moa Design.html` | 전체 디자인 캔버스 (9서션, 30+ 아트보드) |
| `Youth-Moa Prototype.html` | 인터랙티브 프로토타입 (실제 클릭/화면전환) |
| `Hero 배경 시안.html` | 히어로 배경 후보 8종 비교 캔버스 (Method A scrim, §5.1.1) |
| `prototype.tsx` | **위 프로토타입의 React+TS 소스 (개발 직접 참조용, 항상 .html과 동기화)** |
| `HANDOFF.md` | 이 문서 — 코드 구현 시 유일한 참조 자료 |
| `assets/` | 로고, 심볼, SNS 아이콘 |
| `design-tokens.jsx` 등 | 디자인 시안의 컴포넌트 소스 |

### GitHub 저장소 (`sihyuun/youth-moa@dev`)

| 파일 | 내용 |
|------|------|
| `src/app/globals.css` | 디자인 토큰 (이미 적용됨) |
| `docs/00_assets/prototype.tsx` | 배포된 프로토타입 |
| `docs/03_implementation/HANDOFF.md` | 배포된 핸드오프 문서 |
| `docs/00_assets/assets/` | 로고, SNS 아이콘 원본 |

### Claude Code 개발 시 우선 참조 순서
1. 이 `HANDOFF.md` — 토큰, 컴포넌트 명세, 페이지 구조
2. `Youth-Moa Design.html` — 시각적 레이아웃 참조 (캔버스 클릭 희 화면 확대)
3. `prototype.tsx` / `Youth-Moa Prototype.html` — 인터랙션 패턴·컴포넌트 구조 참조 (클릭 흐름, 모달 동작). **개발 중에는 `prototype.tsx`를 직접 참조** — .html과 동일 소스이며 최신본으로 동기화됨
4. `src/app/globals.css` — 실제 코드베이스의 토큰 구현체

> **prototype.tsx 사용 메모:**
> - `react` / `react-dom/client` ESM import 기준. 파일 하단에서 `#root`에 자동 마운트 + `export default App`.
> - 시연용이라 `// @ts-nocheck` 상단에 포함(엄격 타입 미적용). 실제 구현 시 컴포넌트별로 분리하고 타입 부여 권장.
> - **styled-jsx 없이** 동작하도록 인라인 스타일 위주. 단, 아래 애니메이션/유틸 클래스는 `globals.css`에 동일 정의 필요:
> ```css
> @keyframes fadeSlideUp { from { opacity:0; transform:translateY(16px) } to { opacity:1; transform:translateY(0) } }
> @keyframes fadeIn { from { opacity:0 } to { opacity:1 } }
> @keyframes slideDown { from { opacity:0; transform:translateY(-8px) } to { opacity:1; transform:translateY(0) } }
> .screen-enter { animation: fadeSlideUp 280ms ease forwards }
> .screen-fade  { animation: fadeIn 300ms ease forwards }   /* 홈: 위치이동 없이 페이드만 (hero 0px 고정) */
> .overlay-enter{ animation: fadeIn 180ms ease forwards }
> .dropdown-enter{ animation: slideDown 180ms ease forwards }
> .btn-hover  { transition: all 150ms ease; cursor:pointer }
> .btn-hover:hover { filter: brightness(0.92); transform: translateY(-1px) }
> .btn-hover:active { transform: scale(0.97) }
> .card-hover { transition: all 200ms ease; cursor:pointer }
> .card-hover:hover { transform: translateY(-3px); box-shadow: 0 8px 24px rgba(63,48,233,0.14); border-color:#3F30E9 }
> ```

---

## 13. Claude Code 개발 가이드

### 13.1 작업 순서 (권장)
1. **토큰 확인** — `src/app/globals.css`의 `@theme` 블록이 §2와 일치하는지 확인 (인디고 `#3F30E9` 기준)
2. **공통 컴포넌트부터** — `Button`(S/M/L · primary/outline/ghost), `Input`, `Badge`, `Card`, `Avatar`, `Modal`, `Toast`, `Pagination`, `Toggle`을 먼저 구현 (§4 명세)
3. **레이아웃 셸** — `Header`(투명/일반 2모드 · 비로그인=로그인아이콘 / 로그인=Avatar+드롭다운), `Footer`, 모바일 하단 탭바
4. **페이지** — 홈 → 프로그램(목록/상세/신청/완료) → 인증 → 마이페이지 → 공지 → 센터찾기 → 검색 → 법적문서 → 시스템(404/500/로딩)
5. **반응형** — §의 Adaptive 전략(단일 코드베이스, `md:` 분기). 모바일 390px·데스크톱 1440px 기준

### 13.2 데이터 모델 핵심 필드
```ts
// Program
{ id, title, center, region, status: '진행중'|'진행예정'|'마감',
  applyStart, applyEnd, startDate, endDate,
  capacity, currentApplicants,   // 정원바·경쟁률 계산
  thumbnail, description, eligibility: { age, region, etc } }   // 소득기준 제외
// Application
{ id, programId, userId, appStatus: '대기'|'승인'|'반려'|'취소',
  appliedAt, extraAnswers, rejectReason? }
// 상태색: 대기=warning · 승인=success · 반려=error · 취소=muted (마이페이지 타임라인 공통)
```

### 13.3 구현 시 반드시 지킬 것
- **D-day·정원바·상태**는 색만으로 구분 금지 → 색 + 텍스트/아이콘 병행 (접근성)
- **버튼 카피 통일**: 마감 프로그램 = `빈자리 알림 받기`, 진행예정 = `오픈 알림 받기`, 일반 = `신청하기`
- **알림 채널 선택 = 체크박스(복수)**, 라디오 아님
- **헤더 유저영역** = 드롭다운 토글 전용(이동 X), 각 메뉴가 마이페이지 해당 탭으로 딥링크
- **프로토타입의 `<div onClick>`은 시연용** → 실제 구현은 `<button>`/`<a>` 시맨틱 태그 + `aria-label` (§10-A)
- **신청 플로우**: 비로그인 상태로 신청 시 → 로그인 유도, 완료는 모달 아닌 `/apply/complete` 페이지

### 13.4 API 엔드포인트 (§9 참조)
프로그램 목록/상세/신청, 센터, 검색, 캘린더, 카테고리, 대기신청, 알림설정 — §9의 표 그대로 구현.

### 13.5 주의 (현재 프로토타입 = 정적 데모)
- 프로토타입 데이터는 하드코딩(`PROGRAMS` 6건). 실제는 API 연동
- `isLoggedIn` 기본 `true`(시연 편의) → 실제는 세션 기반
- 지도(센터찾기)는 이미지 플레이스홀더 → 실제는 카카오/네이버 지도 SDK
- 카테고리(분야) 칩은 **미적용** — 분류체계 확정 후 도입 (`/api/categories` 준비됨)

---

## 14. 이메일 발송 정책

거래성(transactional) 이메일의 발송 대상·형식 정책. 공공 서비스 표준 + 정보통신망법 기준.

### 14.1 발송 대상 범위 (MVP = 임시 비밀번호 + 환영 메일)

| 메일 | 발송 시점 | MVP | 형식 |
|------|----------|-----|------|
| **임시 비밀번호** | 비밀번호 찾기 요청 | ✅ 필수 | **HTML 멀티파트** (공통 셸·텍스트 폴백 포함) |
| **회원가입 환영** | 가입 완료 직후 | ✅ 포함 | **HTML 멀티파트** (공통 셸·텍스트 폴백 포함) |
| 보안 인시던트 통지 | 이상 로그인·유출 의심 | ⏳ 2차 | HTML 멀티파트 |

> **보안 인시던트 통지는 2차로 분리** — SOP·로깅·법무 검토 체계가 갖춰진 뒤 도입. 미완성 SOP에 의존하지 않도록 MVP에서 제외.

### 14.2 발송 형식 기준 — 공통 템플릿 셸

- **모든 거래성 메일은 하나의 HTML 셸을 공유** — 동일 헤더(인디고 바 + 흰 로고)·동일 푸터(발신전용 안내·고객센터·수신설정·개인정보처리방침)를 쓰고 **본문만 교체**(토스·카카오·Stripe·배민 패턴). 일관된 브랜드 경험 + 유지보수 단순화.
- **HTML + plain text 멀티파트로 발송** — 두 메일 모두 텍스트 폴백을 함께 실어 접근성·구형 클라이언트에 대응. 600px 단일 폭(Outlook 데스크톱 안전 폭) + 테이블 기반 + 인라인 스타일.
- **전달률** — `plain text`가 아니라 SPF/DKIM/DMARC 인증으로 확보. 보안 메일이라도 인증만 정상이면 HTML 전달률 문제 없음(과거 "보안 메일=텍스트" 통념은 현행 기준과 맞지 않음). 단, 임시 비밀번호 메일에는 **마케팅·추적 픽셀·외부 이미지 남용을 금지**해 피싱 오인을 피한다.

### 14.3 필수 포함 요소

- **임시 비밀번호 메일**: ① 임시 비밀번호 ② **만료 시간 명시(예: 30분)** ③ "본인이 요청하지 않았다면 무시하고 비밀번호를 변경하세요" 안내 ④ 로그인 후 비밀번호 변경 유도
- **환영 메일**: ① 가입 완료 확인 ② 서비스 소개 1~2줄 ③ CTA 버튼 ④ 수신 거부/문의 경로(푸터)
- 공통: 발신 전용 주소 안내, 고객센터 연락처

### 14.4 구현 메모

- 발송 트리거: `POST /api/auth/find-password`(임시 비밀번호), `POST /api/auth/signup` 성공 후(환영)
- 템플릿은 `lib/email/templates/`에 분리. **공통 레이아웃(`layout.html` 헤더·푸터)** + 본문 파셜(`temp-password.html`/`.txt`, `welcome.html`/`.txt`)
- 발송 실패 시 가입·재발급 트랜잭션은 롤백하지 않음(메일은 비동기/재시도 큐 권장)
