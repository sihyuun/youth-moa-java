# 작업 명세: fix/F0h-center-desc-image — Center.description / imageUrl 파생 시드 제거

- **상태**: `spec_draft` (결정 대기 — 옵션 A/B/C 중 하나 확정 필요)
- **브랜치**: `fix/F0h-center-desc-image`
- **선행**: `fix/F0h-real-coords` 머지 완료 후 착수 권장 (동일 `DataInitializer` 영역 편집)
- **작업 단위**: 1~2 PR (옵션 A 채택 시 엔티티 도입 + 마이그레이션 + 리팩터)
- **우선순위**: 착수 미정 — 좌표 티켓 커밋 후 사용자가 결정

---

## 0. 배경·목표

### 0-1. 좌표 사고 회고 요약

`fix-F0h-real-coords.md` §0 에서 정리한 대로, `DataInitializer.seedRegionsAndCenters()` 는 파생 시드 패턴으로 Center 데이터를 채워 왔습니다. 좌표(lat/lng) 부분은 `fix/F0h-real-coords` 티켓에서 CSV 로드 방식으로 해소하지만, **동일 메서드 내 잔여 파생 로직 3종** 이 남아 있습니다.

### 0-2. 잔여 파생 시드 로직 3종 (`DataInitializer` 기준)

| # | 위치 (라인) | 로직 요약 | 문제 |
|---|---|---|---|
| ① | L549~572 `imagePool` | Unsplash 이미지 6장 배열을 `imgIdx % 6` 로 로테이션 → `Center.imageUrl` | 파생 시드. admin 편집 시 재기동 후 덮어써짐 |
| ② | L558~574 desc 파생 | 센터명 키워드(창업/취업/역량/문화 등) 매칭 → 5종 문안 로테이션 → `Center.description` | 파생 시드. row 자체가 아닌 이름 규칙에서 유도 |
| ③ | L577~591 `featuredDesc` override | 대표 8개 센터 desc 만 하드코딩 override | 부분 override 라 규칙이 파편화. 나머지는 여전히 파생 |

### 0-3. CLAUDE.md §확장성 원칙 §파생 시드 금지 정합

`CLAUDE.md` 확장성 원칙(2026-07-09 좌표 사고 회고 후 추가):

> 엔티티 필드는 각 row 자체가 진리 소스. 다른 필드나 컬렉션 규칙으로부터 파생 시드하지 말 것.
> 시드 데이터는 자원 파일에서 로드. 대규모 데이터는 자원 파일 분리.

좌표와 정확히 동일한 성격의 위반이 `description` / `imageUrl` 두 필드에 남아 있습니다.

### 0-4. admin CRUD 실효성 목표

관리자 페이지 도입 후 운영자가 센터 소개 문구·대표 이미지를 편집할 때 **재기동 후에도 편집값이 유지** 되어야 합니다. 현재 구조는 `ddl-auto: create-drop` + 파생 시드 조합이라 편집 즉시 무력화됩니다.

---

## 1. 변경 범위 (옵션별 매트릭스)

| 파일 | 옵션 A (CenterContent) | 옵션 B (SiteImage slot) | 옵션 C (별도 CSV) |
|---|---|---|---|
| `Center.java` | 무변경 (imageUrl/description 컬럼 제거 여부는 결정 사항) | description 유지, imageUrl 제거 | 무변경 |
| `CenterContent.java` (신규) | 신규 엔티티 + Repository | — | — |
| `SiteImage.java` | — | 새 slot enum 값 (`CENTER_IMG_{id}`) | — |
| `centers-content.csv` (신규) | — | — | 신규 자원 파일 |
| `DataInitializer.java` | L549~591 파생 로직 삭제, `CenterContent` 시드 로직 추가 | 동일 라인 삭제, `SiteImage` 시드 라인 추가 | 동일 라인 삭제, CSV 로드 유틸 재사용 |
| `CenterMarketingCsvLoader.java` (신규 or 재사용) | 선택 (CSV 기반 시드로 초기값 이관 시) | — | 필수 |
| `CenterRepository` | `@EntityGraph("content")` 추가 검토 | 무변경 | 무변경 |
| 카드·상세 뷰 (`center/list.html`, `center/list-fragments.html`, `program/list.html` 등) | `center.content.description`, `center.content.imageUrl` 참조로 수정 | `center.description` 유지, imageUrl 은 `siteImageService.findBySlot(...)` 로 조회 | 무변경 |
| 렌더 테스트 (`CenterListRenderTest`, `CenterDetailPanelRenderTest` 등) | 참조 경로 갱신 | imageUrl 조회 경로 갱신 | 무변경 |

---

## 2. 도메인 설계 옵션 (A/B/C)

### 옵션 A — `CenterContent` 별도 엔티티 (@OneToOne)

```java
@Entity
@Table(name = "center_contents")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CenterContent extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "center_id", nullable = false, unique = true)
    private Center center;

    @Lob
    @Column(nullable = false)
    private String description;

    @Column(nullable = false, length = 500)
    private String imageUrl;

    @Builder
    private CenterContent(Center center, String description, String imageUrl) { ... }

    public void updateDescription(String description) { this.description = description; }
    public void updateImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}
```

- **장점**
  - JPA 표준 관계 모델, admin CRUD 화면에서 `CenterContentRepository.save(...)` 자연스러움
  - Center 엔티티 자체는 무변경 유지 가능 (팩트 vs 마케팅 카피 분리)
  - 학습 프로젝트에 `@OneToOne` 실습 가치
- **단점**
  - JOIN 필요 (카드 리스트 렌더 시 `@EntityGraph("content")` 필수, 성능 검토)
  - 스키마 복잡도 소폭 증가
  - 기존 `Center.description` / `Center.imageUrl` 컬럼 이관 마이그레이션 스크립트 필요 (Flyway 없어 create-drop 학습 단계에선 시드 재작성으로 대체)

### 옵션 B — `SiteImage` slot 확장 + description 은 Center 유지

- `Center.imageUrl` 컬럼 제거, `SiteImage(slot="CENTER_IMG_{centerId}", imageUrl=..., isActive=true)` 로 이관
- `Center.description` 은 CSV single-source-of-truth 로 유지 (팩트로 취급)
- **장점**
  - 기존 `SiteImage` 인프라 재활용 (HERO_BANNER / HOME_SPACE_* 와 통일)
  - imageUrl 관리 화면 하나로 통합
- **단점**
  - description 편집 위치 (CSV) 와 imageUrl 편집 위치 (DB) 가 분리 → admin UX 파편화
  - `SiteImage.slot` 이 `String` 이면 동적 키 생성 필요 (`CENTER_IMG_1`, `CENTER_IMG_2`...) → slot enum 이면 확장 불가
  - description 을 팩트로 취급하는 것은 마케팅 카피 성격상 부적절 (편집 빈도 높음)

### 옵션 C — `centers-content.csv` 별도 자원 파일

- `centers.csv` (팩트: 좌표·주소·연락처) + `centers-content.csv` (id, description, imageUrl) 두 파일 분리
- 기동 시 두 파일 각각 로드 후 Center 엔티티에 합쳐 저장
- **장점**
  - 스키마 변경 없음 (`Center.description`, `Center.imageUrl` 컬럼 유지)
  - 관심사 분리 명확 (팩트 vs 마케팅)
  - `fix/F0h-real-coords` 의 CSV 로더 유틸 재사용 가능
- **단점**
  - 파일 2개 유지 부담
  - admin CRUD 도입 시 DB 편집 → 재기동하면 CSV 가 덮어씀 (좌표 사고 재발 가능성)
  - "CSV 는 팩트, DB 는 편집 가능" 규칙이 필요한데 description/imageUrl 성격상 후자 → 옵션 C 는 admin CRUD 목표와 상충

### 권장

**옵션 A (`CenterContent` 엔티티)** 를 권장합니다.

- admin CRUD 실효성 확보에 가장 자연스러움 (`CenterContentRepository.save()` 만으로 완결)
- Center 엔티티 무변경 유지 가능 → 좌표 티켓과의 병합 리스크 낮음
- `@OneToOne` + lazy fetch 학습 소재로 적합
- 카드 리스트 성능은 `@EntityGraph("content")` 로 fetch join 하면 N+1 없이 처리 가능

---

## 3. 시드 데이터 소스 결정

`description` 과 `imageUrl` 의 초기값을 어디에 두느냐. 옵션 A 채택 전제 하위 결정 사항:

| 방식 | 설명 | 장단점 |
|---|---|---|
| A-1. Java 코드 하드코딩 | `DataInitializer` 안에 `Map<Long, CenterContent>` 형태로 각 센터 desc/imageUrl 개별 명시 | 소규모(~20건) 학습 단계엔 적합. 자원 파일 로드 부담 없음. 하지만 코드 팽창 |
| A-2. `centers-content.csv` 자원 파일 | `id,description,imageUrl` 3컬럼 CSV. 좌표 CSV 로더 재사용 | 확장성 좋음. 파일 분리로 리뷰 시 diff 명확. admin CRUD 도입 후엔 CSV 는 초기값 seeding 만 담당 |
| A-3. `centers-content.yml` YAML | description 이 여러 줄일 때 CSV 이스케이프 번거로우면 YAML 고려 | 가독성 좋으나 로더 추가 개발 필요 |

**권장**: **A-2 (`centers-content.csv`)** — 좌표 티켓에서 만든 CSV 로더 유틸을 재사용해 일관성 유지. description 이 여러 줄이면 `\n` 리터럴 또는 `"..."` quote 로 처리.

---

## 4. admin CRUD 실효성 계획

`ddl-auto: create-drop` 학습 단계에서는 재기동 시 스키마·시드가 통째로 재생성되어 admin 편집값이 어차피 사라집니다. Flyway 도입 이후 실효성이 확보됩니다.

### 단계별 계획

1. **현재 (create-drop 학습 단계)**
   - `CenterContent` 엔티티 시드는 매 기동 시 CSV 로부터 재로드 → admin 편집값은 재기동으로 초기화됨 (수용)
   - 학습 목적상 문제 없음 (좌표 티켓과 동일 정책)
2. **Flyway 도입 후 (후속 티켓)**
   - `ddl-auto: validate` 로 전환
   - `DataInitializer` 는 최초 기동 시 (테이블 empty 조건) 만 시드
   - `if (centerContentRepository.count() > 0) return;` 스킵 가드 추가
   - admin 편집값 재기동 후에도 유지 확인
3. **회귀 테스트**
   - `CenterContentSeedSkipTest` — 시드 후 편집 → 재기동 시뮬레이션 → 편집값 유지 assert

---

## 5. 화면 영향도

`description` / `imageUrl` 이 참조되는 지점 (변경 최소화 목표):

| 화면 | 필드 | 현재 참조 | 옵션 A 적용 시 |
|---|---|---|---|
| 센터 카드 리스트 (`/centers`) | imageUrl, description | `center.imageUrl`, `center.description` | `center.content.imageUrl`, `center.content.description` |
| 센터 상세 패널 (인라인) | imageUrl, description | 동일 | 동일 (경로만 변경) |
| 카카오맵 인포윈도우 | imageUrl | `center.imageUrl` | `center.content.imageUrl` |
| 홈 프로그램 카드 (F0e) | imageUrl (프로그램 상세 배경) | Program 자체 이미지 or Center imageUrl | 결정 필요 (§9 오픈 이슈) |
| 홈 Hero / SiteImage 영역 | — | 무관 | 무관 |

**Thymeleaf 변경 최소화 전략**: `center.getContent()` 헬퍼 메서드를 Center 엔티티에 추가하거나, `CenterView` DTO 로 감싸 템플릿에서는 여전히 `${center.description}` 스타일로 접근 가능하게 설계. 렌더 테스트는 assert 문구는 그대로 유지되도록 배려.

---

## 6. 회귀 방어 테스트

1. **`DataInitializerNoDerivedSeedTest`** (신규)
   - `DataInitializer.java` 소스 파일에서 `imagePool`, `featuredDesc`, `descByKeyword` 등 파생 시드 흔적 문자열이 부재함을 assert
   - 좌표 티켓의 동일 패턴 테스트와 세트
2. **`CenterContentSeedTest`** (신규)
   - 기동 후 모든 Center 가 `CenterContent` 관계를 가짐
   - `content.description` / `content.imageUrl` non-null
   - 8개 대표 센터의 desc 는 기존 `featuredDesc` 문안과 정확히 일치 (초기값 이관 검증)
3. **`CenterListRenderTest` 갱신**
   - `${center.description}` → `${center.content.description}` 마이그레이션 후 렌더 결과가 동일하도록 확인
4. **`CenterContentAdminEditPersistTest`** (Flyway 도입 후)
   - 편집 → 재기동 시뮬레이션 → 편집값 유지 검증

---

## 7. 다음 티켓 후보

- `chore/flyway-introduce` — Flyway 도입, `ddl-auto: validate` 전환
- `feature/admin-center-crud` — admin 센터 CRUD 화면 (주소 검색 API 연동 + 좌표 자동 반영 + description WYSIWYG + 이미지 업로드)
- `chore/site-image-slot-unify` — 옵션 B 채택 시 SiteImage 통합 정책

---

## 8. 검증 시나리오

### 정적 검증

- `./gradlew compileJava` 통과
- `./gradlew test --tests JpaMappingTest` 통과 (CenterContent 매핑 검증)
- `./gradlew test --tests DataInitializerNoDerivedSeedTest` 통과
- `./gradlew test --tests CenterContentSeedTest` 통과
- `./gradlew test --tests CenterListRenderTest` 통과 (템플릿 경로 갱신 회귀 방어)

### 동적 검증

- `.claude/scripts/bootrun-e2e.cmd` 로 e2e 프로파일 기동
- `curl -s http://localhost:8090/centers | grep "<img"` — 각 카드 imageUrl 이 렌더되는지
- `curl -s http://localhost:8090/centers | grep -c "unsplash"` — 초기 이관 시 6개 이미지 URL 노출 유지 확인
- 상세 패널 SSR 경로도 동일 검증

### 시각 검증 (사용자 영역)

- 카드 리스트 이미지가 옵션 A 이관 후에도 기존과 동일하게 표시되는지
- 상세 패널 desc 문안이 대표 8개 센터에서 하드코딩 기존값과 일치하는지
- 카카오맵 인포윈도우 이미지 정상 표시

---

## 9. 오픈 이슈 (사용자 결정 필요)

| # | 이슈 | 옵션 | 권장 |
|---|---|---|---|
| 1 | **도메인 설계** | A: `CenterContent` 엔티티 / B: `SiteImage` slot + Center desc 유지 / C: `centers-content.csv` 별도 CSV | **A** |
| 2 | **description 초기값 정책** | (a) 현재 파생 로직으로 만들어진 문안을 그대로 이관 / (b) 처음부터 새로 작성 | **(a) 파생 결과 이관** — 관리자 편집 전제, 초기값은 현행 유지로 회귀 최소화 |
| 3 | **imageUrl unsplash 6장 유지** | (a) 초기값 그대로 이관, admin 편집으로 실 이미지 대체 / (b) 처음부터 실 이미지 준비 | **(a)** — prototype 도 unsplash 사용, 실 이미지는 admin CRUD 화면 도입 후 교체 |
| 4 | **`featuredDesc` 대표 8개 override** | (a) CSV 초기값에 반영 / (b) 폐기하고 균일 desc 로 재작성 | **(a) 반영** — 기존 톤 유지 |
| 5 | **F0e 홈 프로그램 카드 imageUrl** | 옵션 A 채택 시 `program.center.content.imageUrl` 접근 경로 vs Program 자체 imageUrl 필드 신설 | 결정 필요 — Program 이미지는 별도 필드 방향이 자연스러움 (Program 은 회차별 배너가 다를 수 있음) |
| 6 | **`Center.description` / `Center.imageUrl` 컬럼 처리** | (a) 옵션 A 도입 시 Center 컬럼 제거 / (b) `@Deprecated` 로 유지하며 점진 이관 | **(a) 즉시 제거** — create-drop 학습 단계이므로 데이터 손실 리스크 없음 |
| 7 | **CSV 파일 위치** | `src/main/resources/data/centers-content.csv` (좌표 CSV 와 동일 폴더) | 좌표 CSV 와 세트로 관리 |
| 8 | **description 여러 줄 처리** | CSV `\n` 리터럴 vs `"..."` quote + 실제 개행 | quote + 개행 (가독성) |

**결정 후**: 옵션 확정 시 이 문서를 `spec_confirmed` 로 승격하고 §1~§4 를 채택안 기준으로 재정리한 뒤 ym-impl 인계.

---

## 참고

- `docs/specs/fix-F0h-real-coords.md` §7 후속 티켓 인계 항목 (B안: description/imageUrl 별도 자원 또는 CenterContent 엔티티)
- `CLAUDE.md` §확장성 원칙 §파생 시드 금지 (2026-07-09 추가)
- `DataInitializer.java` L549~591 (현행 파생 시드 로직)
