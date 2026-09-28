# A7-createdBy-recipient — Program.createdBy 도입 + 알림 3축 union (B-3-A)

- 상태: **spec_confirmed → impl_done** (2026-09-28)
- 브랜치: `feature/A7-createdBy-recipient`
- 관련: A7 (admin 헤더 알림 벨), A9-a/b (Program.center FK)
- 이월: **`A7-watcher-ui`** (Q-B3-5 결정 · Watcher 축 UI + 저장소는 별도 티켓)

---

## 1. 목표

기존 A7 NEW_APPLICATION fan-out (B-2: `program.center` 소속 CENTER_ADMIN) 에 **B-3-A 축 (프로그램 작성자)** 을 병렬 추가하여 **distinct union** 으로 발행한다. Watcher 축은 후속 티켓으로 분리.

```
신청 이벤트
  ↓
Composite Resolver
  ├── B-2: program.center → CENTER_ADMIN 활성 전원 (기존 유지)
  └── B-3-A: program.createdBy (활성 시) (신규)
  ↓
distinct union (user.id 기준) → Notification INSERT
```

---

## 2. 변경 범위

### 신규
- `db/migration/V23__add_program_created_by.sql` (3단계 nullable → sysadmin 백필 → NOT NULL)
- `notification/admin/CreatedByRecipientResolver.java`
- `notification/admin/CompositeApplicationCreatedResolver.java` (@Primary)
- 테스트 2종

### 수정
- `program/Program.java` — `createdBy: User` NOT NULL FK 필드 + Builder 파라미터
- `admin/AdminProgramService.create()` — 현재 admin 을 createdBy 로 주입 (SecurityContext)
- `admin/AdminScope.currentUser()` — 헬퍼 신설
- `notification/admin/AdminNotificationEventListener` — Composite 위임으로 필드 교체
- `common/DataInitializer` — `seedAdmins()` 순서를 `seedPrograms()` 앞으로 이동, 24개 seed Program 에 `.createdBy(sysadmin)` 주입
- 테스트 fixture 12개 파일 (Program.builder() 42개 호출부에 `.createdBy(...)` 주입 및 소유자 시드 추가)

---

## 3. 사용자 결정 이력 (Q-B3)

| ID | 결정 | 근거 |
|---|---|---|
| Q-B3-1 | Program.createdBy 신설 | 기존 필드 없음 |
| Q-B3-2 | Watcher 저장소 = ProgramWatch 신설 (본 티켓 밖) | 관심사 분리 · 후속 티켓 |
| Q-B3-3 | distinct union | 중복 알림 방지 |
| Q-B3-4 | createdBy 비활성 시 = skip | 퇴사·정지 계정 알림 노이즈 방지 |
| Q-B3-5 | Watcher UI = 후속 티켓 (`A7-watcher-ui`) | 스코프 축소로 안전 배포 |
| Q-B3-6 | 3축 균등 (모두 알림) | 우선순위/억제 정책 도입 유예 |
| Q-B3-A | 백필 정책 = sysadmin 일괄 (Notice V9 선례) | 정합성 · 감사 추적 |
| Q-B3-B | createdBy 이관 UI 없음 (불변) | 감사 무결성 · 최소 스코프 |
| Q-B3-Δ | event record 무변경, Resolver 에서 lazy fetch | 발행자 코드 무변경 |

---

## 4. 구현 매핑

| 스펙 항목 | 코드 위치 |
|---|---|
| Program.createdBy FK NOT NULL | `program/Program.java:141~154` (@ManyToOne(LAZY, optional=false)) |
| Builder 파라미터 | `program/Program.java:157~181` |
| create() 주입 | `admin/AdminProgramService.java:126~163` (`adminScope.currentUser()` → `.createdBy(creator)`) |
| updateFromAdminForm() 무변경 (Q-B3-B) | `program/Program.java:236~276` (createdBy 미포함) |
| V23 마이그레이션 3단계 | `db/migration/V23__add_program_created_by.sql` |
| SecurityContext 조회 | `admin/AdminScope.java:78~92` (currentUser()) |
| B-3-A Resolver | `notification/admin/CreatedByRecipientResolver.java` |
| Composite union | `notification/admin/CompositeApplicationCreatedResolver.java:52~59` (LinkedHashMap distinct) |
| 리스너 위임 교체 | `notification/admin/AdminNotificationEventListener.java:52~57` (필드 타입 교체) |
| seed 순서 조정 | `common/DataInitializer.java:110~123` (seedAdmins() → seedPrograms()) |
| seed sysadmin 부착 | `common/DataInitializer.java:636~648` 및 24개 builder |

---

## 5. 검증

### 정적 검증
- `./gradlew compileJava compileTestJava` PASS
- 신규 테스트: `CreatedByRecipientResolverTest` (5 TC) · `CompositeApplicationCreatedResolverTest` (6 TC)
- 회귀 감시: `AdminNotificationEventListenerTest` (Composite 위임 후 결과 동일) · `JpaMappingTest` (NOT NULL FK 매핑)

### 동적 검증
- V23 은 3단계 안전 백필 — 기존 program row 는 sysadmin 소유로 승격 후 NOT NULL
- POST `/programs/{id}/apply` → 알림 fan-out 이 B-2 결과 + B-3-A 결과의 distinct union

### 함정 회고
- 초기 시도에서 Python 대체 시 `.build(),` 매치가 `ProgramEligibility.builder()` 내부까지 잡아 24 space indent 라인 47회 잘못 삽입 → 24 space createdBy 라인 필터로 되돌린 후 재실행 (16 space indent 로 재삽입)

---

## 6. 이월 사항

- `A7-watcher-ui` — Watcher 축 저장소 (`ProgramWatch`) + UI + Resolver 추가. Composite 는 축을 하나 더 추가하는 확장점만 제공
- `A7-createdBy-transfer-ui` — 이관 UI (Q-B3-B 재검토 필요 시)
