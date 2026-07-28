# F4 — 프로그램 문의처 데이터 정합

- 상태: `spec_draft`
- 우선순위: 2
- 브랜치 후보: `feature/F4-program-contact`
- 작성일: 2026-07-28

---

## 1. 배경

2026-07-28 배치 (미커밋) 로 프로그램 상세의 문의처는 `Program.organization` → `CenterRepository.findByName()` → `Center.phone` 조회 방식으로 임시 구현됨 (`ProgramController.java:131~138`, `templates/program/detail.html:123~133`).

**한계:**
1. `Program.organization` 이 자유 텍스트라 `centers.csv` 실제 명과 오탈자 하나만 달라도 매칭 실패 → "문의처 미등록" 노출
2. 청년센터가 아닌 외부 주관사 (예: 시청 부서, 민간 파트너) 는 Center 자체가 없어 매칭 불가
3. 관리자가 Program 별로 문의처를 override 할 방법 없음 (admin 트랙 대비 확장성 부족 — CLAUDE.md "확장성 원칙" 위반)
4. 이메일 문의처 노출 요구 (prototype fallback 문구에 helpmoa@naver.com 존재, L2602/L2623)

---

## 2. 디자인 출처 (3자산)

| 자산 | 위치 | 내용 |
|---|---|---|
| prototype.tsx | ProgramDetail 문의처 meta L995 | `[['bell','문의처','031-123-4567']]` — 5개 meta 항목 중 하나로 노출. Icon: bell. 하드코딩 mock 값 |
| prototype.tsx | Footer / 에러 페이지 L2602 L2623 | `문의 helpmoa@naver.com · 031-123-4567` — **이메일 + 전화 조합 fallback** |
| prototype.tsx | L2119 (안내 문구) | `· 문의: 청년센터 대표번호 031-000-0000` |
| prototype.html | 동일 위치 | 동일 (렌더 결과) |
| HANDOFF.md | 문의처 특별 언급 없음 | — |
| 현재 구현 | `ProgramController.java:131~138` + `templates/program/detail.html:123~133` | Center.phone 매칭 후 없으면 "문의처 미등록" |

### 2-A. 프로토가 명세하지 않은 영역

프로토는 **하드코딩 mock 값 하나**만 두었고 매칭 실패·이메일 노출·admin override 등 데이터 정합 정책은 다루지 않음. **본 스펙은 프로토 매칭이 아닌 백엔드 데이터 모델 개선이 주 스코프**이며 UI 는 기존 meta 행 유지 + 이메일 라인 추가 여부만 결정.

---

## 3. 자산 간 갭 표

| 항목 | prototype.tsx | prototype.html | HANDOFF | 채택 |
|---|---|---|---|---|
| 문의처 노출 위치 | detail meta 5행 중 마지막 | 동일 | — | 유지 |
| 문의처 아이콘 | `bell` | 동일 | — | 유지 |
| 이메일 노출 | detail meta 에는 없음. footer/에러 페이지에만 | 동일 | — | **결정 필요 (Q4)** — detail 에도 노출할지 |
| "미등록" fallback | 프로토엔 없음 (항상 값 있음) | — | — | 현재 구현 유지 or 대표번호 fallback (Q3) |

---

## 4. 데이터 모델 gap 표

| prototype 필드 | 현재 엔티티 | 조치 |
|---|---|---|
| `문의처` (전화) | `Program.organization` → `Center.phone` (indirect) | **`Program.contactPhone` 컬럼 신설** (Q1 결정 시) |
| 이메일 | 없음 | `Program.contactEmail` 컬럼 신설 (Q4 결정 시) |
| admin override 여부 | — | contactPhone/Email 이 신설되면 자동 지원 |
| Center 매칭 fallback | — | contactPhone 이 null 이면 Center.phone 조회, 그것도 null 이면 대표번호 or "문의처 미등록" |

**제안 스키마 (Q1-A 채택 시):**
```sql
-- V<N>__add_program_contact.sql
ALTER TABLE program ADD COLUMN contact_phone VARCHAR(30);
ALTER TABLE program ADD COLUMN contact_email VARCHAR(255);
```
기존 row 는 null → 런타임에 Center 매칭 fallback.

---

## 5. 데이터 소비 지점

| 소비 지점 | prototype 참조 | 현재 상태 | 갭 |
|---|---|---|---|
| `/programs/{id}` detail meta 문의처 행 | tsx L995 | 임시 구현 (Center 매칭) | contactPhone override 지원 |
| `/programs/{id}` detail 이메일 (선택) | tsx footer only | 없음 | Q4 결정에 따라 |
| admin 프로그램 등록·수정 폼 | (미구현) | — | contactPhone/Email 입력 필드 추가 |
| admin 프로그램 목록 | (미구현) | — | 문의처 컬럼 표시 (선택) |
| 신청 완료 페이지 안내 문구 | tsx L2119 "청년센터 대표번호" | 미확인 | detail 과 별개, 이번 스코프 밖 |
| Footer / 에러 페이지 하드코딩 helpmoa@naver.com | tsx L2602 L2623 | 확인 필요 | 별건 (site 상수) |

---

## 6. 변경 범위

**엔티티 / 마이그레이션**
- [ ] `Program.java` — `contactPhone`, `contactEmail` 필드 추가 (nullable), `@Builder` / `update()` 시그니처 확장
- [ ] `V<N>__add_program_contact.sql` — 컬럼 추가

**서비스 / 컨트롤러**
- [ ] `ProgramController.detail()` — 문의처 해석 로직을 서비스로 이동. 우선순위: `program.contactPhone` > `Center.phone` (organization 매칭) > null
- [ ] `ProgramContactResolver` (신규, service 클래스 or Program 도메인 메서드) — 문의처 해석 규칙 캡슐화

**템플릿**
- [ ] `templates/program/detail.html:123~133` — contactPhone 표시 로직 유지 (해석 서비스에서 완성된 값 전달). Q4 결정 시 이메일 라인 추가
- [ ] admin 트랙: 프로그램 등록·수정 폼 (별도 PR)

**시드**
- [ ] `DataInitializer` — 기존 프로그램에 대해 시연용 contactPhone 값을 부여할지 (Q2)

---

## 7. PR 분할 제안

- **PR-1**: 엔티티 + 마이그레이션 + resolver + detail 뷰 (사용자 페이지 스코프)
- **PR-2**: admin 프로그램 등록·수정 폼에 contactPhone/Email 필드 추가 (ADMIN 트랙과 통합)
- **PR-3 (선택)**: 이메일 라인 detail 노출 (Q4 결정 시)

---

## 8. 검증 시나리오

### 정적
- `./gradlew compileJava test --tests ProgramContactResolverTest`
- `./gradlew test --tests JpaMappingTest` (contact_phone/contact_email 컬럼 매핑)
- `./gradlew test --tests ProgramDetailRenderTest`

### 동적 (curl)
- `GET /programs/{id_with_contactPhone}` → HTML 에 `031-XXX-XXXX` 노출
- `GET /programs/{id_no_override_but_matching_center}` → Center.phone fallback
- `GET /programs/{id_no_override_no_match}` → "문의처 미등록" or 대표번호 (Q3)

### write→read 왕복 (admin 트랙 PR-2 에서)
- admin 프로그램 수정 폼에서 contactPhone 저장 → detail 재조회 시 저장값 표시
- contactPhone 을 비우고 저장 → Center 매칭 fallback 으로 되돌아감

### 시각
- prototype detail meta 5행 중 마지막 "문의처" 행이 bell 아이콘 + 전화번호로 렌더

---

## 9. Q 리스트

| # | 질문 | 옵션 | 기본 제안 |
|---|---|---|---|
| Q1 | Program 에 contactPhone 컬럼 신설 여부 | (a) 신설, override 지원 / (b) 유지, Center 매칭만 사용 | **(a)** — admin 확장성 · 외부 주관사 대응 · CLAUDE.md 확장성 원칙 |
| Q2 | 시연 시드 데이터 | (a) 기존 프로그램 모두 null 유지 / (b) 절반 정도에 샘플 전화 부여 | (a) — Center fallback 이 자연스러운 시연 |
| Q3 | 전 계층 매칭 실패 시 최종 fallback | (a) "문의처 미등록" 유지 / (b) 사이트 대표번호 (`031-000-0000` tsx L2119) / (c) 이메일만 노출 | (a) — 대표번호 하드코딩은 데이터 오염, Q4-이메일과 조합 |
| Q4 | detail 페이지에 이메일 라인 추가 | (a) 추가 (contactEmail 컬럼 함께 신설) / (b) 미추가 (프로토엔 detail 이메일 없음) | (b) — 프로토 준수. 필요 시 후속 PR |
| Q5 | admin CRUD 우선순위 | (a) PR-1 에 포함 / (b) admin 트랙 PR-2 분리 | (b) — admin 트랙 전체와 함께 |
| Q6 | organization 매칭 실패 로그 남길지 | (a) 매 detail 요청마다 debug 로그 / (b) 관리자 대시보드에 "매칭 실패 프로그램 리스트" / (c) 안 함 | (c) — 관리자 override 로 근본 해결 |
| Q7 | contactPhone 저장 형식 정규화 | (a) 저장 시 하이픈 제거 / (b) 자유 형식 (관리자 입력 그대로) | (b) — 관리자 편의, 표시는 그대로 |

---

## 10. 위험 / 주의

- **머지된 V 파일 수정 금지** (CLAUDE.md Flyway 규칙) — 컬럼명·타입 확정 후 마이그레이션 작성
- Center 매칭 fallback 은 유지되어야 함 — 기존 시드 데이터 하위호환
- `Program.organization` 은 표시용으로 계속 사용됨 (문의처와 별개)
