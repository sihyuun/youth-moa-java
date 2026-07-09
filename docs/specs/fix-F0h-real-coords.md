# 작업 명세: fix/F0h-real-coords — Center 실좌표 CSV 로드 (파생 시드 제거)

- **상태**: `spec_draft`
- **브랜치**: `fix/F0h-real-coords`
- **선행**: 없음. F0h 시리즈(c1/c2/c3/c4) 머지 완료 상태 위에서 진행
- **작업 단위**: 1 PR (CSV 로더 유틸 + DataInitializer 리팩터 + 회귀 테스트)
- **스코프 결정**: **B — CSV 8컬럼 전량 로드**. `description` / `imageUrl` 은 이번 스코프 밖 (기존 파생 로직 유지 또는 후속 티켓)

---

## 0. 배경·목표

### 0-1. 사고 회고 (2026-07-09 F0h 좌표 사고)

`DataInitializer.seedRegionsAndCenters()` 는 아래 파생 패턴으로 Center 좌표를 시드해 왔습니다.

```
Map<String, BigDecimal[]> regionCoords = new LinkedHashMap<>();
regionCoords.put("양평군", { 37.xxx, 127.xxx });  // 시·군청 대표 좌표만
...
for (Center c : centers) {
    BigDecimal[] base = regionCoords.get(c.getRegion());
    int idx = regionOffsetIdx.merge(c.getRegion(), 0, (a,b) -> a+1);
    BigDecimal offset = new BigDecimal("0.00" + String.format("%04d", idx * 15));
    c.updateCoordinates(base[0].add(offset), base[1].add(offset));
}
```

결과:
- 같은 시·군의 여러 센터가 대표 좌표 ± 약 100m 오프셋 지점에 뭉쳐 렌더
- 카카오맵 클러스터가 "3" 등으로 오표기 (실제로는 서로 수 km 떨어진 별개 시설)
- 실주소 데이터는 존재했지만 좌표에는 반영되지 않음
- 관리자 CRUD 로 좌표를 편집해도 재기동 시 파생 로직이 덮어써 무력화

### 0-2. CLAUDE.md 확장성 원칙 정합

`CLAUDE.md` §확장성 원칙 §파생 시드 금지 규칙(2026-07-09 추가) 을 정면으로 위반한 상태입니다.

> 엔티티 필드는 각 row 자체가 진리 소스. 다른 필드나 컬렉션 규칙으로부터 파생 시드하지 말 것.
> 시드 데이터는 자원 파일에서 로드. 대규모 데이터는 자원 파일 분리.

### 0-3. 목표

1. 파생 시드 로직(`regionCoords` map + offset for-loop) **완전 제거**
2. Center 좌표·전화·운영시간·활성여부를 CSV 8컬럼에서 **개별 row 단위**로 로드
3. 같은 시·군 내 서로 다른 센터가 **각자의 실좌표**로 렌더되도록 회귀 테스트로 방어
4. 후속 관리자 CRUD 편집이 재기동 시 덮어써지지 않는 구조의 토대 마련 (Flyway 도입 전까지는 여전히 create-drop 이지만, 파생 로직이 없어야 편집값 보존 논의가 가능)

### 0-4. 스코프 결정 (B — 확정)

CSV 8컬럼: `name, region, address, latitude, longitude, phone, operatingHours, isActive` → **모두 CSV 에서 로드**.

- `description`: 이번 스코프 **밖**. 기존 이름 키워드 기반 5종 로테이션 로직은 우선 유지 (또는 별도 티켓 `fix/F0h-center-desc-image` 로 이관)
- `imageUrl`: 이번 스코프 **밖**. 기존 unsplash 6장 로테이션 유지
- `isFeatured`: CSV 컬럼 없음 → 현행 시드 로직(별도 리스트) 유지

---

## 1. 변경 범위 (파일 단위)

| 파일 | 변경 | 비고 |
|---|---|---|
| `src/main/java/io/github/sihyuuun/youthmoa/common/DataInitializer.java` | 수정 | `seedRegionsAndCenters()` 내부에서 CSV 로더 호출로 대체. `regionCoords` / offset for-loop **삭제** |
| `src/main/java/io/github/sihyuuun/youthmoa/common/CenterCsvLoader.java` | **신규** | CSV 로더 유틸. `List<CenterCsvRow> load()` 반환 |
| `src/main/resources/data/centers.csv` | 이미 존재 (사용자 별도 세션에서 48행 작성) | 헤더 + 48행. UTF-8, LF 또는 CRLF 허용 |
| `src/test/java/io/github/sihyuuun/youthmoa/center/CenterCoordinateSeedTest.java` | **신규** | 회귀 방어 테스트 (`@SpringBootTest` 또는 `@DataJpaTest` + 로더 단위) |

**무변경 명시**:
- `Center` 엔티티: 필드·컬럼·Builder 변경 없음. 기존 `updateCoordinates(lat, lng)` 그대로 사용
- `CenterRepository`, `CenterController`, `list.html`, `list-fragments.html`: 무변경
- `description` / `imageUrl` 파생 블록(`DataInitializer.java:547~574`): 이번 PR 에서 **손대지 않음** (후속 티켓 분리)

---

## 2. CSV 스키마

### 2-1. 컬럼 정의 (8개, 순서 고정)

| # | 컬럼 | 타입 | 필수 | 예시 | 비고 |
|---|---|---|---|---|---|
| 1 | `name` | String(≤100) | ✅ | `내일꿈제작소` | Center.name |
| 2 | `region` | String(≤50) | ✅ | `고양시` | Region.name 과 반드시 일치해야 함 (검증 대상) |
| 3 | `address` | String(≤255) | ✅ | `경기도 고양시 덕양구 은빛로 72 (화정동)` | 쉼표·괄호 포함 가능 → 따옴표 이스케이프 필요 |
| 4 | `latitude` | BigDecimal(2,7) | ✅ | `37.637186` | WGS84 위도. -90.0 ~ 90.0 |
| 5 | `longitude` | BigDecimal(3,7) | ✅ | `126.836190` | WGS84 경도. -180.0 ~ 180.0 |
| 6 | `phone` | String(≤20) | ✅ | `031-8075-2873` | 하이픈 포함 원문 |
| 7 | `operatingHours` | String(≤200) | ✅ | `월~토 10:00~18:00, 일·공휴일 휴관` | 쉼표 포함 → 따옴표 이스케이프 필요 |
| 8 | `isActive` | boolean | ✅ | `true` | `true` / `false` 리터럴만 |

### 2-2. 인코딩·포맷

- **인코딩**: UTF-8 (BOM 없음)
- **줄바꿈**: LF / CRLF 모두 허용 (`BufferedReader.readLine()` 사용)
- **구분자**: `,` (콤마)
- **이스케이프**: RFC 4180 준수. 값에 `,` `"` `\n` 포함 시 `"` 로 감싸고, 값 내 `"` 는 `""` 로 이중 이스케이프
  - 예: `"경기도 광명시 광명로928번길 42-16 (어울리기행복센터 3~5층, 광명동)"`
- **헤더**: 1행 고정 (`name,region,address,latitude,longitude,phone,operatingHours,isActive`)

### 2-3. 현재 파일 상태 (2026-07-09 실측)

- 위치: `src/main/resources/data/centers.csv`
- 라인 수: **49** (헤더 1 + 데이터 48)
- 인코딩: UTF-8 (확인 필요 — impl 시 first-run 로그로 검증)

---

## 3. CSV 로드 로직

### 3-1. 위치 및 로딩 방식

- **자원 경로**: `classpath:/data/centers.csv` (`ClassPathResource` 사용)
- **호출 시점**: `DataInitializer.seedRegionsAndCenters()` 내부, Center 엔티티 build 직전
- **파서**: 표준 라이브러리 (`BufferedReader` + RFC 4180 수동 파싱). 외부 라이브러리(commons-csv 등) 도입하지 않음 (48행 규모, 학습 단계 의존성 최소화)

### 3-2. CenterCsvRow (DTO)

```java
package io.github.sihyuuun.youthmoa.common;

import java.math.BigDecimal;

public record CenterCsvRow(
    String name,
    String region,
    String address,
    BigDecimal latitude,
    BigDecimal longitude,
    String phone,
    String operatingHours,
    boolean isActive
) {}
```

### 3-3. CenterCsvLoader (신규 유틸)

```java
package io.github.sihyuuun.youthmoa.common;

@Component
public class CenterCsvLoader {

    private static final String RESOURCE_PATH = "data/centers.csv";
    private static final String[] EXPECTED_HEADERS = {
        "name", "region", "address", "latitude", "longitude",
        "phone", "operatingHours", "isActive"
    };

    public List<CenterCsvRow> load() {
        // ① classpath 리소스 오픈 (없으면 IllegalStateException)
        // ② BufferedReader 로 한 줄씩 읽기
        // ③ 첫 줄은 헤더 → EXPECTED_HEADERS 와 일치 검증
        // ④ 이후 각 줄을 RFC 4180 규칙으로 split (따옴표 안 쉼표 보존)
        // ⑤ 8컬럼 아니면 fail-fast (line 번호와 함께 예외)
        // ⑥ latitude/longitude 는 BigDecimal 파싱 → 범위 검증 (-90~90 / -180~180)
        // ⑦ isActive 는 "true"/"false" 만 허용 → 그 외 IllegalArgumentException
        // ⑧ List<CenterCsvRow> 반환
    }

    // RFC 4180 파서 — 따옴표 상태 추적하며 쉼표 분리
    private List<String> parseCsvLine(String line) { ... }
}
```

### 3-4. DataInitializer 리팩터 (변경 delta)

**삭제 대상**:
- `Map<String, BigDecimal[]> regionCoords = new LinkedHashMap<>();` (line 386)
- `regionCoords.put(...)` × 30여 개 (line 387~536)
- `Map<String, Integer> regionOffsetIdx` + for-loop (line 537~545)

**대체 로직 (pseudo-code)**:

```java
private void seedRegionsAndCenters() {
    // 1. Region 시드 (기존 로직 유지)
    ...

    // 2. CSV 에서 Center 데이터 로드
    List<CenterCsvRow> rows = centerCsvLoader.load();

    // 3. Center 엔티티 build + save
    List<Center> centers = new ArrayList<>();
    for (CenterCsvRow row : rows) {
        Center c = Center.builder()
            .name(row.name())
            .region(row.region())
            .address(row.address())
            .phone(row.phone())
            .operatingHours(row.operatingHours())
            .latitude(row.latitude())
            .longitude(row.longitude())
            .isActive(row.isActive())
            .isFeatured(FEATURED_NAMES.contains(row.name()))  // 기존 리스트 유지
            .build();
        centers.add(c);
    }
    centerRepository.saveAll(centers);

    // 4. (스코프 밖) description / imageUrl 파생 블록 — 기존 로직 그대로 유지
    //    또는 후속 티켓 fix/F0h-center-desc-image 에서 정리
    seedContentDerivations(centers);
}
```

---

## 4. 방어 로직 정책

| 사례 | 정책 | 근거 |
|---|---|---|
| CSV 파일 부재 (`ClassPathResource` not found) | **fail-fast** — `IllegalStateException("centers.csv not found")` 던지고 부트 실패 | 시드가 없으면 앱이 무의미. 오픈 이슈 §9-1 |
| 헤더 불일치 (컬럼 순서·이름) | fail-fast — 기대 헤더 로그 출력 후 예외 | 스키마 계약 위반은 즉시 감지 |
| 컬럼 개수 미달 (8개 미만) | fail-fast — line 번호 + 원문 line 로그 후 예외 | |
| 좌표 파싱 실패 (BigDecimal 변환) | fail-fast — line 번호 + 값 로그 후 예외 | |
| 좌표 범위 초과 (`lat > 90` 등) | fail-fast | 오타 방어 |
| `isActive` 값이 true/false 외 | fail-fast | |
| CSV 의 region 이 Region 테이블에 없음 | **warn 로그만** (부트는 계속) | Region 시드는 별도 리스트. 향후 관리자 CRUD 로 Region 이 삭제될 수 있음. 오픈 이슈 §9-2 |
| 이름 중복 (같은 CSV 내) | **warn 로그** + 둘 다 저장 | 같은 이름의 별개 지점 존재 가능성 (예: 화성 2건). 유니크 제약은 엔티티에 없음 |
| 좌표 중복 (같은 lat/lng) | 허용 — 로그도 남기지 않음 | 화성 2건이 같은 건물이면 정상 케이스 |

---

## 5. 회귀 방어 테스트

### 5-1. 신규 테스트 클래스

**파일**: `src/test/java/io/github/sihyuuun/youthmoa/center/CenterCoordinateSeedTest.java`

**전략**: `@SpringBootTest(webEnvironment = NONE)` + H2 로 부트 → `CenterRepository` 로 시드 결과 조회 → 단언

### 5-2. 테스트 케이스

| # | 케이스 | 단언 |
|---|---|---|
| TC-01 | 48개 시드 성공 | `centerRepository.count() == 48` |
| TC-02 | 모든 Center 의 lat/lng non-null | `findAll().forEach(c -> assertNotNull(c.getLatitude()); assertNotNull(c.getLongitude()));` |
| TC-03 | 좌표 범위 유효 | 모든 lat ∈ [33, 39], lng ∈ [125, 130] (한반도 범위) |
| TC-04 | **같은 region 내 서로 다른 좌표** (파생 미사용 확인) | 예: `findByRegion("고양시")` 결과 2건 이상이면 각 좌표가 서로 다름. 단, 화성 2건 같은 건물처럼 의도된 동일 좌표는 예외 허용 (TC-06 참조) |
| TC-05 | CSV 첫 행 원본 대조 | `내일꿈제작소` 조회 → lat=37.637186, lng=126.836190, phone=031-8075-2873, operatingHours 시작="월~토 10:00~18:00" |
| TC-06 | 화성 2건 동일 좌표 허용 | 만약 CSV 에 화성 2건이 동일 lat/lng 를 가지면 test skip 또는 whitelist 처리. **CSV 실측 후 결정** (오픈 이슈 §9-3) |
| TC-07 | **소스 회귀 방어**: `DataInitializer.java` 소스에 `regionCoords` 문자열 부재 | 파일 read 후 `assertThat(content).doesNotContain("regionCoords")`. 파생 로직 재도입 방지 |
| TC-08 | operatingHours 로드 검증 | 쉼표 포함 값(`"월~토 10:00~18:00, 일·공휴일 휴관"`) 이 온전히 저장됨 (RFC 4180 이스케이프 파싱 확인) |
| TC-09 | isActive false 케이스 로드 검증 | CSV 에 false 값이 하나라도 있으면 그 row 의 isActive == false. 전부 true 면 test skip |

### 5-3. 실행 명령

```powershell
.\gradlew.bat test --tests CenterCoordinateSeedTest
```

---

## 6. CLAUDE.md 확장성 원칙 정합

### 6-1. 파생 시드 금지 규칙 준수 절차 (impl 시 체크)

- [ ] `regionCoords` map, `regionOffsetIdx`, offset for-loop **모두 삭제**
- [ ] `DataInitializer` 내부에 `Map<String, BigDecimal[]>` 형태의 좌표 매핑 패턴 없음
- [ ] Center 좌표는 **CSV row 자체에서만** 로드
- [ ] 향후 관리자 CRUD 로 좌표 편집 → 재기동 시 CSV 로 다시 덮어써지긴 하지만, "지역 대표 좌표 + offset" 파생은 완전 제거 → 편집값을 CSV 에 반영하는 방식으로 대응 가능한 구조

### 6-2. 미래 대응 (Flyway 도입 이후)

Flyway 도입 후에는 `ddl-auto: none` + 마이그레이션 스크립트가 최초 1회만 CSV 를 로드. 그 이후 관리자 CRUD 편집값은 그대로 유지됨. 본 티켓은 그 사전 준비.

---

## 7. 다음 티켓 후보 (스코프 밖)

| 티켓 후보 | 내용 | 근거 |
|---|---|---|
| `fix/F0h-operating-hours-badge` | 카드/상세의 "운영 중" 배지가 `isActive` 를 그대로 사용 중. **접속시간 기준 실시간 판단**으로 재정의 필요. `Center.isCurrentlyOpen(now)` 도메인 메서드 + `operatingHours` 파서 도입. `isActive` 는 "센터 운영 자체가 활성/폐쇄" 의미로 재정의 | 배지 의미와 데이터 의미 불일치 |
| `fix/F0h-center-desc-image` | `description`, `imageUrl` 을 CSV 컬럼(9·10번) 추가 또는 별도 자원 파일(`center-content.yml`) 로 분리. 현재 이름 키워드 기반 5종 로테이션 로직 제거 | 파생 시드 금지 규칙의 잔재. 본 티켓 스코프에서 제외됨 |
| `chore/admin-crud-seed-idempotency` | Flyway 도입 → `ddl-auto: none` 전환 → 관리자 CRUD 편집값이 재기동 후 유지되도록 마이그레이션 스크립트 설계. 시드는 최초 1회만 실행 | 확장성 원칙 §관리자 CRUD 실효성 |

---

## 8. 검증 시나리오

### 8-1. 정적 검증

- `./gradlew compileJava` 통과
- `./gradlew test --tests CenterCoordinateSeedTest` — 위 §5.2 의 TC-01~09 통과
- `./gradlew test --tests JpaMappingTest` — Center 매핑 회귀 없음 (엔티티 무변경이므로 통과 필수)
- **`*RenderTest` 실행**: `CenterListRenderTest` 통과 확인 (좌표 값 변경 시 카드 순서·좌표 렌더에 영향 있는지)

### 8-2. 동적 검증

- `.claude/scripts/bootrun-e2e.cmd` 로 e2e 프로파일(H2 + 시드) 기동 (포트 8090)
- `curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8090/centers` → **200 OK**
- `curl -s http://localhost:8090/centers | grep -c 'data-center-lat'` → **48 라인 이상** (전체 Center 렌더 확인)
- 같은 시·군 두 센터 좌표가 다름을 HTML 에서 확인:
  ```
  curl -s http://localhost:8090/centers | grep 'data-region="고양시"'
  → data-center-lat="37.637186", "37.637415" 등 서로 다른 값 확인
  ```
- 부트 로그에 `Loaded 48 centers from data/centers.csv` (impl 시 로그 문구 확정) 출력 확인

### 8-3. 시각 검증 (사용자 영역)

- 카카오맵에서 고양시·수원시·성남시 등 다중 센터 지역이 **더 이상 대표 좌표 근처에 뭉치지 않고**, 각자 실주소 위치에 마커가 표시되는지
- 클러스터 숫자가 실제 지리적 근접성 기준으로만 표시되는지 (지도 축소 시 자연스러운 클러스터링)
- 화성 2건이 정말 같은 건물이면 겹쳐 표시되는 것 자체는 정상

---

## 9. 결정 확정 (2026-07-09)

### 9-1. CSV 파일 부재 시 — **A 채택 (fail-fast)**
`IllegalStateException("centers.csv not found")` 으로 부트 실패. 시드 없이는 앱 동작 무의미하므로 조용한 실패 금지 (F0h 사고 재발 방지).

### 9-2. CSV region 이 Region 테이블에 없을 때 — **A 채택 (warn 만)**
Center 는 저장. `/centers` 지역 필터 드롭다운에서만 노출 안 됨. 미래 admin CRUD 에서 Region 자유 편집 + 주소 검색 API 로 좌표 자동 반영 예정이므로 유연성 우선. 참조 무결성은 향후 FK 도입 시 재검토.

### 9-3. CSV 파일 실측 (완료)
- ✅ 48행 (헤더 제외)
- ✅ UTF-8 **BOM 없음** (첫 3바이트 `n a m`)
- ✅ 화성 2건 동일 좌표 (37.207268, 127.034057 — 같은 건물 4·5층, 정상 케이스로 TC-06 whitelist 처리)
- ⚠️ **isActive false 케이스 0건** — 모두 true. **TC-09 는 skip** (또는 assumption 으로 처리)

### 9-4. desc / imageUrl — **A 채택 (스코프 밖)**
이번 PR 에선 손대지 않음. 후속 티켓 `fix/F0h-center-desc-image` 로 이관. admin 화면 개발 세션에서 자연스럽게 정리 예정.

### 9-5. CSV 파서 — **opencsv:5.9 도입**
`build.gradle.kts` 의존성 추가:
```kotlin
implementation("com.opencsv:opencsv:5.9")
```
표준 BufferedReader 자체 RFC 4180 파싱 대비 안전. 라이브러리 dependency 1개 증가 감수.

### 9-6. `.gitattributes` CSV 개행 통일
`src/main/resources/data/*.csv text eol=lf` 규칙 추가 (Win/Mac 크로스 개행 이슈 방어).

### 9-7. `isActive` 파싱 — **대소문자 무시 + fail-fast**
`"true"`/`"TRUE"`/`"True"` → `true`, `"false"`/`"FALSE"`/`"False"` → `false`. 그 외 값은 fail-fast (예: `1`/`0`/`Y`/`N` 금지).

### 9-8. **[신규]** 운영시간 렌더 — ~~B 채택 (`, ` split)~~ → **재개정 A 채택 (CSS 자동 줄바꿈)**

**초기 결정 (B)**: `operatingHours` 를 `", "` 로 split 하여 라인별 `<div>` 렌더
**재개정 사유 (2026-07-09 시각 검증 후)**:
1. `"확인 불가 (입주기업 전용, 직접 문의)"` 같은 괄호 안 쉼표 케이스에서 라인이 어색하게 깨짐
2. 인포윈도우에서 🕒 아이콘이 첫 라인과 나란히 안 붙고 홀로 위 라인에 노출
3. `<span>` 안 `<div>` HTML5 유효성 위반 (span=phrasing / div=flow)
4. 사용자 시각 검증 결과 "약간 어색" 피드백

**A 채택 (재개정)**: 컨테이너 폭 넘어가면 CSS 자동 줄바꿈
- `list-fragments.html`: 단일 `<span class="centers-detail-hours">` + `word-break: keep-all; overflow-wrap: anywhere;`
- `center-map.js` 인포윈도우: flex 컨테이너 + 아이콘·텍스트 span 분리. 텍스트 span 만 `word-break/overflow-wrap`

**적용 위치**:
- `list-fragments.html:82` 운영시간 span (auto-wrap 클래스 부여)
- `main.css` `.centers-detail-hours` + `.center-info-window-hours*` 규칙 신설
- `center-map.js:440~446` split 로직 제거, flex 마크업으로 조립

**적용 위치 (구 B안, 폐기)**: ~~`list-fragments.html` detail-panel-content fragment 의 운영시간 span, `center-map.js` 인포윈도우 HTML 조립부~~
상세 패널 (`list-fragments.html:70` 부근 `detailCenter.operatingHours` 표시) 및 인포윈도우 (`center-map.js` 운영시간 표시부) 에서:
- `operatingHours` 문자열을 `", "` 로 split
- 각 세그먼트를 별도 라인으로 렌더 (`<div>` 또는 `<br>` 구분)
- 의미 단위 (요일별 시간·휴관 정보) 로 자연스럽게 줄바꿈

**예시 렌더**:
```
화~일 07:00~22:00
일 09:00~18:00
월·공휴일 휴관
```
(원문: `"화~일 07:00~22:00, 일 09:00~18:00, 월·공휴일 휴관"`)

**적용 위치**: `list-fragments.html` detail-panel-content fragment 의 운영시간 span, `center-map.js` 인포윈도우 HTML 조립부

**회귀 방어 TC 추가**: `CenterListRenderTest` 에 `/centers/1/detail-fragment` 응답이 원문 문자열 그대로가 아닌 split 결과 마크업 (`<div>`·`<br>` 등) 을 포함하는지 assert

---

## 부록: 관련 사고 · 원칙 참조

- `CLAUDE.md` §확장성 원칙 §파생 시드 금지 (2026-07-09 추가)
- `CLAUDE.md` §확장성 원칙 §관리자 CRUD 실효성 체크
- `docs/specs/F0h-c1-center-data-model.md` — description/operatingHours/imageUrl 필드 도입 히스토리
- `docs/STATE.md` §다음 작업 후보 — F0h 좌표 사고 회고
