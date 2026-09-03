plugins {
	java
	id("org.springframework.boot") version "4.1.0"
	id("io.spring.dependency-management") version "1.1.7"
	id("com.diffplug.spotless") version "8.8.0"
	jacoco
}

group = "io.github.sihyuuun"
version = "0.0.1-SNAPSHOT"
description = "Youth MOA Java rewrite"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.thymeleaf.extras:thymeleaf-extras-springsecurity6")
	// 2.0.10 로 bump (#35) 이후 E2E 대량 실패 → 2.0.4 로 revert (2026-07-03). E2E 안정화 후 재상승 검토.
	implementation("org.webjars.npm:htmx.org:2.0.4")
	implementation("org.webjars:webjars-locator-core")
	// F0h-real-coords: CSV 시드 로더 (RFC 4180 파싱). 48행 규모, DataInitializer 에서만 사용.
	implementation("com.opencsv:opencsv:5.9")
	// F-signup-01: CoolSMS SDK — 실 SMS 발송용. youthmoa.coolsms.enabled=false 이면 MockSmsSender 사용.
	implementation("net.nurigo:sdk:4.3.0")
	// A-admin-notice-attachment (2026-09-03): SupabaseFileStorage REST 호출용 (Qn-6 파생 B).
	// Supabase Java SDK 대신 표준 REST + OkHttp 로 직접 호출 (학습 목적 + 의존성 최소).
	implementation("com.squareup.okhttp3:okhttp:4.12.0")
	// P0-1 Flyway (2026-07-22 활성화). Boot 4 는 flyway auto-config 를 별도 모듈로 분리 →
	// spring-boot-flyway 필수. 없으면 flyway 의존성이 있어도 auto-config 미동작 (validate 시 missing table).
	implementation("org.springframework.boot:spring-boot-flyway")
	implementation("org.flywaydb:flyway-core")
	runtimeOnly("org.flywaydb:flyway-database-postgresql")
	// chore-observability (2026-07-23): Actuator + Micrometer + Prometheus.
	// - actuator: 내부 상태를 HTTP 엔드포인트로 노출 (health/prometheus/metrics/info).
	// - micrometer-registry-prometheus: Prometheus scrape 포맷 어댑터.
	// 접근 제어는 별도 management 포트 9091 (application.yml). 8080 공개 포트엔 /actuator/** 미노출.
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	runtimeOnly("io.micrometer:micrometer-registry-prometheus")
	// F0h-operating-hours-badge (spec §9-2): 한국 공휴일 판정.
	// 초기 impl 은 하드코딩 KoreanHolidayRegistry (2026·2027 공휴일 리스트) 로 진행.
	// jollyday 라이브러리 정확한 Maven 좌표 확인 후 별도 티켓에서 전환 예정.
	compileOnly("org.projectlombok:lombok")
	developmentOnly("org.springframework.boot:spring-boot-devtools")
	runtimeOnly("org.postgresql:postgresql")
	annotationProcessor("org.projectlombok:lombok")
	testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
	testImplementation("org.springframework.boot:spring-boot-starter-security-test")
	testImplementation("org.springframework.boot:spring-boot-starter-thymeleaf-test")
	testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testImplementation("org.springframework.boot:spring-boot-testcontainers")
	testImplementation("org.testcontainers:testcontainers-junit-jupiter")
	testImplementation("org.testcontainers:testcontainers-postgresql")
	// H2: unit test + e2e profile (CI Playwright) 양쪽에서 사용 — runtimeOnly 로 승격해 boot jar 포함
	runtimeOnly("com.h2database:h2")
	testCompileOnly("org.projectlombok:lombok")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	testAnnotationProcessor("org.projectlombok:lombok")
	// TODO Springdoc: springdoc-openapi 2.x 는 Spring Boot 3.x 까지만 지원.
	// Boot 4 (Spring 7) 호환 버전 릴리즈 확인 후 도입 예정.
	// implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:<version>")
}

tasks.withType<Test> {
	useJUnitPlatform()
}

// bootRun 이 build/resources/main 대신 src/main/resources 를 직접 사용하도록.
// DevTools 가 소스 변경을 즉시 감지하려면 이 설정이 필수.
// (없으면 .html 변경 후 ./gradlew processResources 강제 실행 필요)
tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
	sourceResources(sourceSets["main"])
}

// JaCoCo: 테스트 커버리지 측정 + HTML 리포트 생성
// - ./gradlew test jacocoTestReport  : 리포트 생성 (build/reports/jacoco/test/html/index.html)
// - ./gradlew jacocoTestCoverageVerification : 최소 커버리지 강제 (CI 에서 사용)
jacoco {
	toolVersion = "0.8.12"
}

tasks.test {
	finalizedBy(tasks.jacocoTestReport)
}

// Entity, DTO, 설정 클래스 제외 (비즈니스 로직 커버리지에 집중) — report/verification 공유
val jacocoExcludePatterns = listOf(
	"**/YouthMoaApplication.class",
	"**/*Entity.class",
	"**/*Dto.class",
	"**/*Request.class",
	"**/*Response.class",
	"**/config/**",
	"**/common/DataInitializer.class"
)

tasks.jacocoTestReport {
	dependsOn(tasks.test)
	reports {
		xml.required = true   // CI coverage badge 툴·SonarQube 연동용
		html.required = true  // 로컬 브라우저 확인용
	}
	classDirectories.setFrom(
		files(classDirectories.files.map {
			fileTree(it) { exclude(jacocoExcludePatterns) }
		})
	)
}

// 커버리지 최소선 강제 — CI integration-test job 에서 전체 테스트 후 실행.
// 기준선 55% = 도입 시점(2026-07-10) 실측 LINE 64.6% 에서 여유를 둔 값. 커버리지가 오르면 점진 상향.
tasks.jacocoTestCoverageVerification {
	dependsOn(tasks.test)
	violationRules {
		rule {
			limit {
				counter = "LINE"
				value = "COVEREDRATIO"
				minimum = "0.55".toBigDecimal()
			}
		}
	}
	classDirectories.setFrom(
		files(classDirectories.files.map {
			fileTree(it) { exclude(jacocoExcludePatterns) }
		})
	)
}

// Spotless: Google Java Format 으로 코드 스타일 자동 통일
// - ./gradlew spotlessApply  : 포맷 자동 수정
// - ./gradlew spotlessCheck  : CI 에서 포맷 위반 검사
spotless {
	java {
		googleJavaFormat("1.22.0")
		removeUnusedImports()
		trimTrailingWhitespace()
		endWithNewline()
		// 생성 파일 제외
		targetExclude("build/**")
	}
}

// chore-observability Q7: /actuator/info 에 빌드 정보 (버전 · 빌드시각 · Git 커밋) 노출.
// META-INF/build-info.properties 파일이 자동 생성되며, InfoContributor 가 이를 읽어 응답에 포함.
// 비공개 management 포트(9091) 노출이라 커밋 SHA 유출 위험 낮음.
springBoot {
	buildInfo()
}
