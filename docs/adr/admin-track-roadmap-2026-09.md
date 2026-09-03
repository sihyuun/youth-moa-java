# Admin 트랙 로드맵 (2026-09) — PM Review

- **날짜**: 2026-09-03
- **상태**: 결정됨 (2026-09-03, 사용자 "모두 권장 OK" 로 Q1~Q6 일괄 채택)
- **결정 요약**: Q1 A · Q2 A · Q3 A · Q4 A · Q5 A · Q6 A (§8 권장값 그대로)
- **다음 액션**: `ym-spec` 호출 → `docs/specs/A1-admin-shell.md` 산출
- **관련 문서**:
  - 상세 지시서: [`docs/specs/ADMIN-00-master-directive.md`](../specs/ADMIN-00-master-directive.md) (2026-07-09 확정)
  - 파생 큐: [`docs/specs/README.md § 파생 큐`](../specs/README.md)
  - 디자인 자산: `docs/00_assets/admin/{prototype.html, prototype.tsx, HANDOFF.md}`
  - 사용자 트랙 계약: [`docs/design-contracts/README.md`](../design-contracts/README.md)

> ADMIN-00 마스터 지시서(2026-07-09)는 P0-1~4·A1~A9 Phase·Q1~Q10 결정까지 완료된 상태입니다. 본 ADR 은 그 위에서 **① admin 트랙 진입 시점이 실제로 도래한 지금(2026-09-03) 무엇이 바뀌었는가 ② 파생 큐 4건과 A1~A9 를 어떤 순서로 붙일 것인가 ③ 첫 파일럿 PR 을 무엇으로 잡을 것인가** 를 재판정하는 문서입니다.

---

## 1. 배경 (2026-09-03 현재)

### 사용자 트랙 상태
- 확정 명세 큐 = **0건** (PR #202/203 감사·회고 완료)
- 최근 5 PR (#199~#203) 은 전부 E2E flaky 청산·문서 정리 — 신규 기능 없음
- 계약 검사 5화면 완료, 갭 77건은 저순위 이슈로 이월
- 사용자 트랙의 "다음 큐 대기" 라는 조건이 **처음으로 실제 만족**

### ADMIN-00 지시서 이후 실제로 반영된 것
| 항목 | ADMIN-00 계획 | 2026-09-03 실제 |
|---|---|---|
| P0-1 Flyway | 선행 필수 | ✅ 완료 (PR #109, `chore-flyway-activation`) |
| P0-2 SecurityConfig 매처 | `/admin/**` → `hasAnyRole('CENTER_ADMIN','SYSTEM_ADMIN')` + CSRF | ✅ 매처·CSRF 완료 (P0-2 PR #89). `/admin/login` **formLogin 재설정만 이월** (SecurityConfig 라인 81~83 주석 확인) |
| P0-3 파일 업로드 | multipart + Supabase Storage | ❌ 미착수 (`spring.servlet.multipart` 설정 없음) |
| P0-4 actuator | health endpoint | ✅ 의존성 존재 (build.gradle.kts) |
| A1~A9 화면 | 사용자 트랙 완료 후 | ❌ 미착수 |
| 파생 큐 4건 | admin 트랙에 흡수 | ❌ 대기 |

### ADMIN-00 이후 변화 (2개월간)
1. **디자인 계약 장치가 성숙** — 사용자 트랙에서 5화면·438 assertions·자동 갭 리포트까지 실증됨. admin 트랙에서 같은 장치를 도입할지 결정할 시점
2. **DataInitializer 시드 정책 확정** — "파생 시드 금지" 규칙 (CLAUDE.md L376~397), Center 좌표 사고 회고. admin CRUD 실효성 검사 프레임 완비
3. **E2E 안정화 완료** — PR #198~#201 로 seed pollution·flaky 3층 청산. admin 이 붙어도 회귀 감지 가능한 상태
4. **Toast/Confirm 등 공통 컴포넌트** 사용자 트랙에서 이미 준비됨 (PR #192/193) — admin fragment 로 재활용 가능

---

## 2. 스코프 (P0/P1/P2 재분류)

ADMIN-00 Phase A1~A9 + 파생 큐 4건 + P0-1~4 를 **현재 시점 우선순위** 로 재분류합니다.

### P0 — 파일럿 착수 전 반드시 선행
| ID | 내용 | 근거 |
|---|---|---|
| **P0-2 이월분** | `/admin/login` formLogin 재설정 + `SYSTEM_ADMIN`/`CENTER_ADMIN` 시드 계정 (env 주입) | A1 시작 시점에 로그인 안 되면 화면 검증 자체 불가 |

### P1 — 파일럿 후보 (첫 PR)
| ID | 내용 | 재활용률 |
|---|---|---|
| **A1 admin-shell** | 다크 헤더 + GNB + `/admin` 대시보드 + 인증 레이아웃 + 사용자↔관리자 왕복 진입 동선 | 사용자 트랙 헤더/드롭다운/toast 패턴 재사용 가능 |

### P2 — 순차 진행 (파일럿 안정화 후)
ADMIN-00 §9 권장 순서 유지: **A2 → A3 (+ F0c-dynamic-fields 흡수) → A4 → A5 → (A6 ∥ A7) → A8 → A9**

파생 큐 흡수 매핑:
| 파생 큐 | 흡수 Phase | 근거 |
|---|---|---|
| **F0c-dynamic-fields** | A3 `admin-program-form` | ApplyQuestion 엔티티가 admin 에서 먼저 생성돼야 사용자 F0c 가 소비 가능 (ADMIN-00 §3-1) |
| **F4 자격요건 입력** | A3 (같은 폼 내) | `ProgramEligibility` 도 admin 프로그램 등록 폼의 한 섹션 |
| **admin 공지사항 첨부 업로드** | 별도 소형 PR (A1 후·A2 전 삽입 가능) | 사용자 다운로드 완료(PR #130), POST endpoint 만 추가 — P0-3 파일 업로드 인프라 병행 |
| **admin 약관 CRUD** | 별도 소형 PR (A1 후 언제든) | Term 엔티티 준비 완료(PR #125), 순수 CRUD 만 |

### 지연 (P3) — 지금 판단 불필요
- A9 센터 CRUD (Q1 확정 — 착수 전 최신 디자인 번들 갱신)
- P0-3 Supabase Storage 는 A3 착수 전까지만 준비되면 됨. A1/A2 단계에서는 사용자 트랙 이미지 슬롯 방식 유지

---

## 3. 파일럿 후보 트레이드오프

첫 PR 1건에 담을 후보 3안. **리스크 최소 · 학습 효과 최대 · 재활용률 최대** 3축으로 평가.

### 안 A — A1 `admin-shell` + `/admin` 대시보드 (권장)
- **범위**: 다크 헤더·GNB·인증 레이아웃·`/admin` 대시보드(스탯카드+최근 프로그램+마감임박)·사용자↔관리자 왕복 링크
- **재활용률**: 헤더 드롭다운·Toast·confirm 모달·icon fragment 대부분 사용자 트랙에서 이식 가능
- **리스크**: 헤더 fragment 를 사용자/관리자로 분리하는 결정이 필요 (main.css 오염 금지 규칙 준수)
- **학습 효과**: admin.css 토큰·layout 패턴·RBAC 매처 실동작을 한 번에 검증. 이후 A2~A9 의 뼈대
- **선행**: P0-2 이월분 (`/admin/login` formLogin) + 시드 계정 1건
- **완료 판정**: `/admin` 200 OK + USER 계정 접근 시 403 + `/admin/login` 렌더 + Claude Preview snapshot `admin/prototype.html` 대시보드 대조 4항목 (헤더 GNB·스탯카드·최근 프로그램·마감 임박)

### 안 B — admin 약관 CRUD 만 (파생 큐 우선)
- **범위**: `/admin/terms` 목록·`POST/PUT /admin/terms` (Term 엔티티는 이미 존재)
- **재활용률**: 사용자 트랙 form pattern·검증 메시지 규칙 그대로
- **리스크 (낮음)**: 화면 1개, 도메인 1개 — Bounded scope
- **학습 효과 (낮음)**: admin 레이아웃·헤더·RBAC 라우팅을 우회. **A1 없이 CRUD 만 만들면 admin 진입 동선이 없음** — 로그인 후 어디로 가서 목록에 접근하는지 미확정
- **완료 판정**: CRUD 4개 endpoint + Term 목록 렌더

### 안 C — P0-3 파일 업로드 인프라 + admin 공지사항 첨부 업로드
- **범위**: `spring.servlet.multipart` + `FileStorageService` 인터페이스 (local 구현) + `POST /admin/notices/{id}/attachments`
- **재활용률**: 인프라 계층 — 사용자 트랙과 무관
- **리스크**: Supabase Storage 통합은 별도 세션 필요. 로컬 파일시스템만이면 prod 배포에서 Fly.io ephemeral 이슈
- **학습 효과 (중)**: multipart·Storage 추상화. 하지만 admin UI 없이 API 만 만들면 통합 검증 어려움

### 판정
| 축 | 안 A | 안 B | 안 C |
|---|---|---|---|
| 리스크 | 중 (헤더 분리 결정) | 낮 | 중 (파일 저장소 선택) |
| 재활용률 | 높 | 중 | 낮 |
| 학습 효과 | 높 | 낮 | 중 |
| 후속 확장성 | 높 (뼈대) | 낮 (독립 화면) | 중 (인프라만) |
| **총평** | ⭐ **권장** | 병행 소형 PR 가능 | A3 착수 직전에 |

**PM 권장: 안 A (A1 admin-shell 파일럿)**. B/C 는 A1 완료 후 P2 큐로 편입.

---

## 4. 디자인 계약 신설 여부

### 사용자 트랙 계약 장치의 실증 결과
- 5화면 361/438 assertions · 자동 갭 리포트 · CI 논블로킹 편입
- **정량 갭(px·색·폰트)을 사람이 놓치는 문제 해결됨** — 재현·회귀 방지 확보
- 미착수 화면 계약 신설 규칙: 화면 작업과 함께 계약 신설 (기존 규칙)

### admin 트랙에서의 판단
| 항목 | 계약 신설 | prototype 원문 대조만 |
|---|---|---|
| 초기 투자 | 화면당 수 시간 계약 작성 | 매 PR prototype.html 정독 |
| 회귀 감지 | 자동 (CI) | 사람 (놓칠 가능성 높음) |
| 사용자 트랙과 정합성 | ✅ 동일 장치 | ❌ 다른 방식 병행 |
| admin prototype 확정도 | hifi 픽셀 확정 (HANDOFF 명시) | 동일 |
| 사고 이력 | 사용자 트랙에서 60갭 사고 실증 | 재발 여지 |

**PM 권장**: **계약 신설** (안 A 채택 시 A1 부터 `docs/design-contracts/admin/dashboard.md` + `e2e/contracts/admin-dashboard.ts` 세트로). admin 은 화면 수가 많고 hifi 픽셀 확정이므로 계약 장치의 ROI 가 오히려 사용자 트랙보다 높음.

계약 파일 구조 제안:
```
docs/design-contracts/admin/
  ├── README.md          # admin 전용 계약 목록
  ├── POLICY.md          # admin 공통 정책 (다크헤더·인디고·존댓말 톤 "…했어요")
  ├── shell.md           # 헤더·GNB·푸터
  ├── dashboard.md
  └── ...
e2e/contracts/admin-*.ts
e2e/tests/visual-admin-*.spec.ts
```

---

## 5. P0-2 이월분 처리 순서

`/admin/login` formLogin 재설정 (SecurityConfig L81~83 주석) 은 **A1 스펙에 포함** 이 자연스럽습니다:
- A1 의 인증 레이아웃 (밝은 헤더 + 420px 카드) 이 곧 `/admin/login` 화면
- `SYSTEM_ADMIN`/`CENTER_ADMIN` 시드 계정 없이는 A1 검증 자체 불가
- 별도 P0-2b PR 로 분리 시 A1 브랜치가 대기 상태로 대기 → 컨텍스트 손실

**PM 권장**: A1 첫 PR 에 `/admin/login` formLogin + 시드 2계정 (env 주입) 을 함께 담는다.

---

## 6. 청년 정책 서비스 · 최신 UX 레퍼런스

### 채택할 UX 패턴
1. **당근 admin 툴 style — 좌측 GNB 대신 상단 GNB (hifi 이미 채택)** + **센터 스코프 셀렉터를 헤더 좌측 상시 노출**. 청년 정책 관리자는 여러 센터를 왕복하지 않고 자기 센터만 담당하는 경우가 대부분 → 상시 노출로 "지금 어느 센터 데이터를 보는가" 를 매 화면 확인 가능
2. **토스 admin 마이크로카피** — "삭제할까요? 삭제 후 복구할 수 없어요." 같은 존댓말 confirm. HANDOFF §7 이미 명시. 검증 메시지 "…해야 합니다" 규칙과 **다른 톤** 인 것 재확인 (ADMIN-00 §1-B 매핑 참조)

### 회피할 안티패턴
1. **정부24·청년몽땅정보통 관리자 콘솔 스타일** — 다단계 트리 GNB + 팝업 창 남발. hifi 는 이미 회피했지만 A7 알림/설정 확장 시 재발 위험
2. **일괄 상태 변경 버튼** — HANDOFF 는 명시적으로 제외 (status 파생). 사용자 요청이 있어도 도입하지 않음. 대신 개별 상태 드롭다운 + 확인 모달 유지

---

## 7. 테스트 전략

### 사용자 트랙 대비 확장 규모
| 검증 유형 | 사용자 트랙 | admin 트랙 추가 |
|---|---|---|
| 정적 (RenderTest) | 화면당 1개 | 화면당 1개 + `@WithMockUser(roles=...)` 슬라이스 |
| 동적 (curl / Preview) | 200 OK · 정적 리소스 | + 비인가 접근 403/302 · 센터 격리 회귀 |
| 인터랙션 (Playwright) | 65 tests | + admin 시나리오 (CRUD 왕복 · 삭제 확인 모달 · 일괄 선택) — 화면당 3~5 tests 추정 |
| 계약 (contracts) | 5화면 | + admin 9화면 (안 채택 시) |

### admin 특유 필수 시나리오
1. **CRUD 왕복 E2E** — 등록 → 목록 노출 → 상세 → 수정 → 목록 반영 → 삭제 → 목록 미노출. Phase 별 표준 시나리오화
2. **센터 격리 회귀** — CENTER_ADMIN 시드 계정으로 타 센터 데이터 접근 시도 → 목록 미노출 + 직접 URL 403 을 매 admin PR CI 에 포함 (ADMIN-00 §7 이미 규정)
3. **CSRF retrofit 검증** — CSRF 활성 후 사용자 트랙 form 전수 회귀 (이미 통과 상태 유지 확인)

### 예상 E2E 확장
- A1: +3 tests (로그인·대시보드 렌더·왕복 진입)
- A2: +5 tests (목록·필터·검색·페이지네이션·일괄 선택 모달)
- A3: +6 tests (탭 전환·검증 실패·검증 성공 등록·수정·질문 드래그·강좌 조건부)
- 총 A1~A9: **+40~50 tests** 추정. 현 65 → 105~115 tests

---

## 8. 결정 필요 항목 (사용자 원샷 결정용)

### Q1 — 첫 파일럿 화면
| 안 | 내용 |
|---|---|
| **A (권장)** | A1 admin-shell + `/admin` 대시보드 + `/admin/login` formLogin + 시드 2계정 |
| B | admin 약관 CRUD 만 (Term 엔티티 활용) |
| C | P0-3 파일 업로드 인프라 + admin 공지사항 첨부 업로드 |

### Q2 — 디자인 계약 신설
| 안 | 내용 |
|---|---|
| **A (권장)** | 신설 — `docs/design-contracts/admin/` + `e2e/contracts/admin-*.ts` + POLICY.md (다크헤더·존댓말 톤) |
| B | 미신설 — prototype 원문 대조만, 계약은 사용자 트랙에만 유지 |
| C | 파일럿(A1) 은 원문 대조로만, A2 부터 계약 도입 |

### Q3 — P0-2 이월분 처리
| 안 | 내용 |
|---|---|
| **A (권장)** | A1 PR 에 포함 — `/admin/login` formLogin + 시드 계정 함께 |
| B | 별도 선행 PR (P0-2b) 로 분리 후 A1 착수 |

### Q4 — 파생 큐 흡수 순서
| 안 | 내용 |
|---|---|
| **A (권장)** | admin 공지사항 첨부(소형·P0-3 병행) → admin 약관 CRUD(소형·독립) → F0c-dynamic-fields·F4 자격요건은 A3 에 흡수 |
| B | 파생 큐 4건 전부 먼저 소진 후 A1 착수 |
| C | 파생 큐 4건 전부 A1~A9 Phase 내부로 흡수 (독립 소형 PR 없음) |

### Q5 — P0-3 파일 업로드 저장소
| 안 | 내용 |
|---|---|
| **A (권장)** | ADMIN-00 Q4 결정 유지 — 로컬(dev) + Supabase Storage(prod) 하이브리드. A3 착수 직전 세션에서 구현 |
| B | 로컬만 우선 구현, Supabase Storage 는 배포 시점에 |
| C | Fly.io Volume 사용 (ephemeral 이슈 감수) |

### Q6 — admin 트랙 브랜치 네이밍
| 안 | 내용 |
|---|---|
| **A (권장)** | ADMIN-00 §9 유지 — `feature/A1-admin-shell`, `feature/A2-admin-programs-list` 등 |
| B | prefix 변경 — `feature/admin-*` 로 통일 (Phase 코드 제거) |

---

## 9. 권장 요약 + 결정 요청

**PM 권장 세트 (안 A 조합)**:
- Q1: **안 A** — A1 admin-shell 파일럿
- Q2: **안 A** — 디자인 계약 신설
- Q3: **안 A** — P0-2 이월분 A1 PR 에 포함
- Q4: **안 A** — 소형 파생(공지첨부·약관 CRUD) 우선 소진 → A3 에 F0c/F4 흡수
- Q5: **안 A** — 로컬+Supabase Storage 하이브리드 (A3 직전)
- Q6: **안 A** — Phase 코드 네이밍 유지

### 결정 요청
- **"모두 권장 OK"** — 위 6개 권장값 일괄 채택
- **"권장 OK, Qn 만 X 안"** — 일부만 다른 안
- **개별 답변** — 각 Q 마다 명시

권장 채택 시 다음 세션은 ym-spec → A1 `feature/A1-admin-shell` 상세 명세 산출 착수.

---

## 10. 다음 세션 착수 절차 (사용자 결정 후)

1. **ym-spec 호출** — 입력: 본 ADR + ADMIN-00 §5-A1 + `admin/prototype.html` A1 화면
   - 산출: `docs/specs/A1-admin-shell.md` (Bean Validation 매핑·fragment 목록·시드 계정 스펙·계약 초안)
2. **사용자 컨펌** — spec 의 세부 Q (헤더 fragment 분리 방식·시드 계정 credential 정책·왕복 링크 라벨) 결정
3. **ym-impl 호출** — `feature/A1-admin-shell` 브랜치 생성 → 구현
4. **ym-qa** — 정적 (`AdminShellRenderTest`) + 동적 (`preview_start(name: "youth-moa-e2e")`) + 계약 (`visual-admin-shell.spec.ts`) + 인터랙션 (E2E CRUD 왕복)
5. **ym-verify** — 적대적 검증 (RBAC 우회 시도·CSRF 누락 form 스캔)
6. **PR 머지** — impl_done 처리 + ADMIN-00 §9 큐에서 A1 체크

## 트레이드오프

- 파일럿을 A1(안 A)로 잡을 경우 **첫 PR 이 상대적으로 크다** (헤더·GNB·인증·대시보드·시드·계약). 단, 이후 A2~A9 가 이 뼈대 위에서 소형화되는 이득이 명백
- 디자인 계약을 admin 에도 신설하면 초기 투자 대비 회귀 방지 이득이 커지지만, 사용자 트랙 갭 77건이 아직 남아 있는 상태에서 admin 갭이 추가되면 리포트 볼륨이 배증 — 우선순위 관리 필요
- 파생 큐 4건 중 F0c-dynamic-fields 는 사용자 화면과 물려 있어 A3 에 흡수하되, 사용자 F0c 착수 시점에는 admin A3 가 **선행 완료** 되어 있어야 함. 지금 결정하지 않으면 F0c 가 계속 이월됨
