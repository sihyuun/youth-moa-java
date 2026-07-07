# 작업 명세: F0h-c1 — 청년센터 데이터 모델 확장 (description / operatingHours / imageUrl)

- **상태**: `spec_confirmed`
- **브랜치 후보**: `feature/F0h-c1-center-data-model`
- **선행**: 없음. F0h-c2/c3/c4 의 선행 티켓
- **작업 단위**: 1 PR (엔티티 3필드 + 시드 갱신 + 매핑 테스트)

---

## 1. 디자인 출처 (3자산)

- **prototype.html**: `docs/00_assets/prototype.html` 의 Centers Screen (info panel + detail 오버레이) — c2/c3 에서 상세 재확인
- **prototype.tsx**: `docs/00_assets/prototype.tsx` **line 1883–1892** `CENTER_DATA` 배열 (mock)
  - `line 1907–1911` filter/sort — region·open·검색어·프로그램수 정렬
  - `line 1913–1914` `detailCenter` / `infoCenter` = 카드/오버레이 데이터 소스
- **HANDOFF.md**: `docs/00_assets/HANDOFF.md` — 이미지 컴포넌트 토큰 (c3 인라인 상세에서 사용)
- **wireframe.png**: `docs/00_assets/wireframe.png` — Centers 화면 이미지·설명 영역 시각 확인
- **현재 코드 대상**: `src/main/java/io/github/sihyuuun/youthmoa/center/Center.java`, `CenterListItem.java`, `common/DataInitializer.java`

## 1-A. 자산 간 갭 (prototype.tsx CENTER_DATA ↔ 현재 Center 엔티티)

| prototype.tsx 프로퍼티 | 예시 값 | 현재 Center 매핑 | 조치 |
|---|---|---|---|
| `id` | 1 | `id` (Long) | 유지 |
| `name` | `'상상대로'` | `name` (String 100) | 유지 |
| `region` | `'수원시'` | `region` (String 50) | 유지 |
| `addr` | `'경기도 수원시 팔달구 매산로 89'` | `address` (String 255) | 유지 |
| `hours` | `'평일 09:00~18:00'` | ❌ 없음 | **신규 `operatingHours`** |
| `tel` | `'031-228-1234'` | `phone` (String 20) | 유지 |
| `programs` | 7 | ❌ 없음 (동적 count) | Program 조회로 계산 (c2 에서 처리) — 컬럼 신설 X |
| `x`, `y` | 42, 58 | `latitude`, `longitude` | 유지 (실좌표 시드로 매핑) |
| `open` | true | `isActive` (boolean) | 유지 (의미 매칭: 운영 중 = active) |
| `desc` | `'청년 창업과 네트워킹을 위한 복합문화공간'` | ❌ 없음 | **신규 `description`** |
| — (이미지) | prototype 카드/상세에 표시 | ❌ 없음 | **신규 `imageUrl`** — c3 인라인 상세에서 필수 |

**채택**: Center 엔티티에 3필드 직접 추가 (사용자 결정 Q2 A). `SiteImage` slot 방식은 오버엔지로 기각.

## 2. 변경 범위 (파일 단위)

- [ ] `src/main/java/io/github/sihyuuun/youthmoa/center/Center.java` — 3필드 + Builder 인자 + 도메인 메서드 `updateContent(...)`
- [ ] `src/main/java/io/github/sihyuuun/youthmoa/common/DataInitializer.java` — `seedRegionsAndCenters()` 에서 실좌표 8개 센터에 desc/hours/imageUrl 시드값 주입 (좌표 없는 나머지 센터는 기본 hours 만 채움, desc/imageUrl null 허용)
- [ ] `src/test/java/io/github/sihyuuun/youthmoa/center/JpaMappingTest.java` (또는 기존 매핑 테스트) — 3필드 왕복 저장 검증

> `CenterListItem` 은 **본 티켓에서 수정하지 않음** — c2 에서 카드에 desc/imageUrl 필요 시 확장 (책임 분리).
> `Region` 엔티티는 무관 — region 은 여전히 문자열 컬럼. c2 지역 드롭다운은 `RegionRepository` 로 조회.

## 3. 필드 명세

| 필드 | 컬럼명 | 타입 | Nullable | 길이/정밀 | 기본값 | 비고 |
|---|---|---|---|---|---|---|
| `description` | `description` | `String` | ✅ (nullable) | `@Column(length = 500)` | null | 카드/상세 서브텍스트. prototype `desc` 최대 30자 안팎이지만 여유 500 |
| `operatingHours` | `operating_hours` | `String` | ✅ | `length = 100` | null | 예: `"평일 09:00~18:00"`. 관리자가 자유서식 입력 |
| `imageUrl` | `image_url` | `String` | ✅ | `length = 500` | null | `/images/centers/xxx.png` 상대경로 또는 외부 URL. null 이면 c3 상세에서 placeholder 표시 |

**Builder 갱신** — 3필드 파라미터 추가, 모두 nullable 허용:

```java
@Builder
private Center(String name, String region, String address, String phone,
               Boolean isActive, Boolean isFeatured,
               BigDecimal latitude, BigDecimal longitude,
               String description, String operatingHours, String imageUrl) { ... }
```

**도메인 메서드 추가**:

```java
public void updateContent(String description, String operatingHours, String imageUrl) {
    this.description = description;
    this.operatingHours = operatingHours;
    this.imageUrl = imageUrl;
}
```

## 4. DataInitializer 시드 방침

### 4-1. 8개 실좌표 센터 — desc + hours + imageUrl 모두 채움

prototype.tsx CENTER_DATA 의 이름은 mock 명("상상대로", "내일스퀘어" 등)이라 실제 시드 이름과 매칭 안 됨. **desc 는 prototype 의 8개 문장을 실좌표 센터에 순서대로 매핑** (개념·톤 유지).

| 실좌표 시드 센터명 | description (prototype 매핑) | operatingHours | imageUrl |
|---|---|---|---|
| 청년바람지대 (수원시) | `청년 창업과 네트워킹을 위한 복합문화공간` | `평일 09:00~18:00` | `/images/centers/placeholder-1.png` |
| 청년이봄 (성남시) | `취업·역량강화 특화 청년지원센터` | `평일 09:00~18:00` | `/images/centers/placeholder-2.png` |
| 안양청년1번가 (안양시) | `정신건강·힐링 프로그램 전문 센터` | `평일 09:00~18:00` | `/images/centers/placeholder-3.png` |
| 소사청년공간 소사로움 (부천시) | `취업·역량강화 특화 청년지원센터` | `평일 09:00~18:00` | `/images/centers/placeholder-4.png` |
| 화성시청년지원센터 H.E.Y (화성시) | `취업·진로 전문 지원 청년센터` | `평일 09:00~18:00` | `/images/centers/placeholder-5.png` |
| 광명시 청년동 (광명시) | `지역사회 연계 청년 커뮤니티 허브` | `평일 09:00~18:00` | `/images/centers/placeholder-6.png` |
| 양평청년공간 오름 (양평군) | `소셜벤처·사회적 경제 청년 지원` | `평일 09:00~18:00` | `/images/centers/placeholder-7.png` |
| 의왕청년발전소 (의왕시) | `지역사회 연계 청년 커뮤니티 허브` | `평일 09:00~18:00` | `/images/centers/placeholder-8.png` |

> `imageUrl` 은 placeholder 파일명. `src/main/resources/static/images/centers/placeholder-{1..8}.png` **파일은 c3 티켓에서 준비**하거나, 본 티켓에서 기존 `banner_*.png` 를 임시 심볼릭 링크로 재사용해도 무방 (구현자 재량, 시드값만 확정).

### 4-2. 나머지 40개 센터 (좌표 미확정)

- `operatingHours`: 일괄 `"평일 09:00~18:00"` (관리자 편집 전 기본값)
- `description`: null
- `imageUrl`: null

시드 로직 예시:
```java
String DEFAULT_HOURS = "평일 09:00~18:00";
Map<String, String> centerDesc = Map.of(
    "청년바람지대", "청년 창업과 네트워킹을 위한 복합문화공간",
    // ... 8건
);
// centers 루프에서 name 매칭되면 desc/imageUrl 세팅, 아니면 hours 만 세팅
```

## 5. Region 정합성 확인

- `Region` 엔티티는 별도 유지 (`Region.name`, `isFeatured`)
- `Center.region` 은 여전히 String → Region.name 과 값 일치 필요
- **c2 지역 드롭다운 소스**: `RegionRepository.findAllByOrderByNameAsc()` (기존)
- 본 티켓에서 Region 스키마 변경 없음

## 6. 갭 리스트 (현재 코드 vs prototype)

| # | 항목 | 현재 상태 | 목표 | 우선순위 |
|---|---|---|---|---|
| 1 | Center.description | 없음 | 컬럼 추가 + 8건 시드 | 높음 |
| 2 | Center.operatingHours | 없음 | 컬럼 추가 + 전체 시드 (기본값) | 높음 |
| 3 | Center.imageUrl | 없음 | 컬럼 추가 + 8건 시드 (placeholder) | 높음 |
| 4 | 프로그램 수 (`programs`) | Program count 미노출 | c2 에서 `countByCenterId` 처리 (본 티켓 X) | 낮음 |
| 5 | 이미지 실파일 | 없음 | c3 에서 준비 (본 티켓 X) | 낮음 |

## 7. 검증 시나리오 (ym-qa)

### 정적 검증
- `./gradlew compileJava` 통과
- `./gradlew test --tests JpaMappingTest` — Center 3필드 왕복 저장/조회 검증 TC 추가:
  ```java
  @Test void centerContentFieldsPersist() {
      Center c = Center.builder()
          .name("t").region("수원시")
          .description("desc").operatingHours("평일 09~18")
          .imageUrl("/images/centers/x.png")
          .build();
      centerRepository.saveAndFlush(c);
      Center loaded = centerRepository.findById(c.getId()).orElseThrow();
      assertThat(loaded.getDescription()).isEqualTo("desc");
      assertThat(loaded.getOperatingHours()).isEqualTo("평일 09~18");
      assertThat(loaded.getImageUrl()).isEqualTo("/images/centers/x.png");
  }
  ```
- `updateContent(...)` 호출 후 반영 검증 TC 1건

### 동적 검증
- `./gradlew bootRun` 기동 후 로그에 `Seeded 48 centers` (또는 현행 개수) 정상 출력
- H2 콘솔 or 로그로 `SELECT description, operating_hours, image_url FROM center WHERE name='청년바람지대'` 결과 3필드 채워짐 확인
- 기존 `/centers` 화면 200 OK (본 티켓은 화면 변경 없음, 회귀 방지)

### 시각 확인 (사용자 영역)
- 본 티켓 범위 아님 (c2/c3 에서 진행)

## 8. 의존성 / 후속

- 후속: **F0h-c2** (3-col 목록) — CenterListItem 확장 + 카드에 desc·image 반영
- 후속: **F0h-c3** (인라인 상세) — imageUrl + operatingHours 렌더링
- 후속: **F0h-c4** (지도 인터랙션)

## 9. 작업 큐 메타

- 작업 ID: F0h-c1
- 우선순위: c2/c3/c4 선행이므로 **최상위**
- 추정 단위: 1 PR (엔티티 diff + 시드 diff + 매핑 테스트)
- 예상 라인 수: ~120 lines
- 상태: **spec_confirmed**
