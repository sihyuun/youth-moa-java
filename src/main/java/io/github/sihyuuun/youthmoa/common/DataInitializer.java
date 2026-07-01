package io.github.sihyuuun.youthmoa.common;

import io.github.sihyuuun.youthmoa.center.Center;
import io.github.sihyuuun.youthmoa.center.CenterRepository;
import io.github.sihyuuun.youthmoa.program.Program;
import io.github.sihyuuun.youthmoa.program.ProgramRepository;
import io.github.sihyuuun.youthmoa.region.Region;
import io.github.sihyuuun.youthmoa.region.RegionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final ProgramRepository programRepository;
    private final RegionRepository regionRepository;
    private final CenterRepository centerRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedRegionsAndCenters();
        seedPrograms();
    }

    private void seedRegionsAndCenters() {
        if (regionRepository.count() > 0) {
            log.info("Regions already seeded (count={}), skip", regionRepository.count());
        } else {
            // 30 시·군 가나다순. 상위 5개 isFeatured=true (사이드바 노출 기본)
            List<String> regionNames = List.of(
                    "고양시", "과천시", "광명시", "광주시", "구리시",
                    "군포시", "김포시", "남양주시", "동두천시", "부천시",
                    "성남시", "수원시", "시흥시", "안산시", "안성시",
                    "안양시", "양주시", "양평군", "여주시", "연천군",
                    "오산시", "용인시", "의왕시", "의정부시", "이천시",
                    "파주시", "평택시", "포천시", "하남시", "화성시"
            );
            List<Region> regions = new ArrayList<>();
            for (int i = 0; i < regionNames.size(); i++) {
                regions.add(Region.builder()
                        .name(regionNames.get(i))
                        .isFeatured(i < 5)
                        .build());
            }
            regionRepository.saveAll(regions);
            log.info("Seeded {} regions ({} featured)", regions.size(), 5);
        }

        if (centerRepository.count() > 0) {
            log.info("Centers already seeded (count={}), skip", centerRepository.count());
            return;
        }

        // region_center_list.md — 48 청년공간 / 30 시·군
        Map<String, List<String>> centersByRegion = new LinkedHashMap<>();
        centersByRegion.put("고양시", List.of("내일꿈제작소", "28청춘창업소"));
        centersByRegion.put("과천시", List.of("과천시 청년공간 비행지구"));
        centersByRegion.put("광명시", List.of("청춘곳간", "광명시 청년동"));
        centersByRegion.put("광주시", List.of("광주시 청년지원센터"));
        centersByRegion.put("구리시", List.of("청년내일센터"));
        centersByRegion.put("군포시", List.of("군포시 청년공간 플라잉"));
        centersByRegion.put("김포시", List.of("김포시청년지원센터"));
        centersByRegion.put("남양주시", List.of("남양주시 청년창업센터 / 청년꽃간"));
        centersByRegion.put("동두천시", List.of("동두천시 청년창업지원센터"));
        centersByRegion.put("부천시", List.of("소사청년공간 소사로움", "원미청(년)정(점)구역", "오정청년공간"));
        centersByRegion.put("성남시", List.of("청년이봄", "청년이봄 야탑", "청년이봄 정자"));
        centersByRegion.put("수원시", List.of("청누리", "청년바람지대"));
        centersByRegion.put("시흥시", List.of("청년협업마을", "청년스테이션"));
        centersByRegion.put("안산시", List.of("안산시 청년센터 상상대로", "안산시 청년센터 상상스테이션"));
        centersByRegion.put("안성시", List.of("안성시청년문화공간 '청년톡톡'"));
        centersByRegion.put("안양시", List.of("범계역 청년출구", "동안 청년오피스", "만안 청년오피스", "안양청년1번가"));
        centersByRegion.put("양주시", List.of("양주시청년센터"));
        centersByRegion.put("양평군", List.of("양평청년공간 딴딴회관", "내일스퀘어 양평", "양평청년공간 오름"));
        centersByRegion.put("여주시", List.of("여주시청년활동지원센터 푸릇"));
        centersByRegion.put("연천군", List.of("연천군일자리통합지원센터"));
        centersByRegion.put("오산시", List.of("오산청년일자리지원센터 이루잡"));
        centersByRegion.put("용인시", List.of("용인청년LAB 기흥", "용인청년LAB 수지", "용인청년LAB 처인"));
        centersByRegion.put("의왕시", List.of("의왕청년발전소"));
        centersByRegion.put("의정부시", List.of("의정부시 청년공감터", "의정부시 청년다락방"));
        centersByRegion.put("이천시", List.of("청년일자리카페 '청년e-room'"));
        centersByRegion.put("파주시", List.of("파주시청년공간 GP1939"));
        centersByRegion.put("평택시", List.of("청년쉼표"));
        centersByRegion.put("포천시", List.of("포천시 청년센터"));
        centersByRegion.put("하남시", List.of("하남시청년지원센터"));
        centersByRegion.put("화성시", List.of("화성시 청년취업끝까지 지원센터", "화성시청년지원센터 H.E.Y"));

        // 전체 (region, name) 펼친 뒤 name 가나다순으로 상위 5개 isFeatured=true
        List<Center> centers = new ArrayList<>();
        centersByRegion.forEach((region, names) -> names.forEach(n -> centers.add(Center.builder()
                .name(n)
                .region(region)
                .build())));

        // name 가나다순으로 상위 5개 featured 표시
        Set<String> featuredNames = centers.stream()
                .map(Center::getName)
                .sorted()
                .limit(5)
                .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
        for (Center c : centers) {
            if (featuredNames.contains(c.getName())) {
                c.markFeatured();
            }
        }

        centerRepository.saveAll(centers);
        log.info("Seeded {} centers ({} featured)", centers.size(), featuredNames.size());
    }

    private void seedPrograms() {
        if (programRepository.count() > 0) {
            log.info("Programs already seeded (count={}), skip", programRepository.count());
            return;
        }

        LocalDate today = LocalDate.now();
        List<Program> seeds = List.of(
                Program.builder()
                        .title("취업역량 강화 워크숍")
                        .organization("내일스퀘어 양평")
                        .region("양평군")
                        .imageUrl("https://images.unsplash.com/photo-1524178232363-1fb2b075b655?w=400&h=280&fit=crop")
                        .content("이력서 작성, 면접 트레이닝, 자기소개 워크숍을 한 번에 진행합니다.")
                        .requirements("만 19~39세 경기도 거주 청년")
                        .startDate(today.minusDays(10)).endDate(today.plusDays(3))
                        .capacity(30).build(),

                Program.builder()
                        .title("청년 창업 아카데미")
                        .organization("안산시 청년센터 상상대로")
                        .region("안산시")
                        .imageUrl("https://images.unsplash.com/photo-1529156069898-49953e39b3ac?w=400&h=280&fit=crop")
                        .content("창업 아이디어 발굴부터 사업 모델 검증까지 8주 과정.")
                        .requirements("창업 관심 청년 누구나")
                        .startDate(today.minusDays(5)).endDate(today.plusDays(12))
                        .capacity(25).build(),

                Program.builder()
                        .title("마음건강 힐링 캠프")
                        .organization("범계역 청년출구")
                        .region("안양시")
                        .imageUrl("https://images.unsplash.com/photo-1517245386807-bb43f82c33c4?w=400&h=280&fit=crop")
                        .content("청년의 번아웃을 회복하는 1박 2일 자연 힐링 캠프.")
                        .requirements("만 19~34세 청년")
                        .startDate(today.minusDays(2)).endDate(today.plusDays(6))
                        .capacity(20).build(),

                Program.builder()
                        .title("디지털 마케팅 실전반")
                        .organization("원미청(년)정(점)구역")
                        .region("부천시")
                        .imageUrl("https://images.unsplash.com/photo-1522202176988-66273c2fd55f?w=400&h=280&fit=crop")
                        .content("SNS·검색 광고 실전 캠페인 운영 실습.")
                        .requirements("디지털 마케팅 입문 청년")
                        .startDate(today.minusDays(30)).endDate(today.minusDays(5))
                        .capacity(15).build(),

                Program.builder()
                        .title("AI 활용 실무 교육")
                        .organization("과천시 청년공간 비행지구")
                        .region("과천시")
                        .imageUrl("https://images.unsplash.com/photo-1531482615713-2afd69097998?w=400&h=280&fit=crop")
                        .content("ChatGPT·Claude 등 LLM 활용 실무 워크숍 (전 6강).")
                        .requirements("개발자 또는 기획자 청년")
                        .startDate(today.plusDays(14)).endDate(today.plusDays(45))
                        .capacity(30).build(),

                Program.builder()
                        .title("소셜벤처 인큐베이팅")
                        .organization("양평청년공간 오름")
                        .region("양평군")
                        .imageUrl("https://images.unsplash.com/photo-1552664730-d307ca884978?w=400&h=280&fit=crop")
                        .content("사회 문제 해결형 비즈니스 모델 인큐베이팅 6개월 과정.")
                        .requirements("소셜벤처 관심 예비/초기 창업팀")
                        .startDate(today.plusDays(21)).endDate(today.plusDays(180))
                        .capacity(20).build(),

                Program.builder()
                        .title("청년 문화예술 스쿨")
                        .organization("의왕청년발전소")
                        .region("의왕시")
                        .imageUrl("https://images.unsplash.com/photo-1497366216548-37526070297c?w=460&h=340&fit=crop")
                        .content("연극·사진·뮤지컬 등 6개 트랙 문화예술 입문 강좌.")
                        .requirements("문화예술 입문 청년")
                        .startDate(today.minusDays(7)).endDate(today.plusDays(30))
                        .capacity(40).build(),

                Program.builder()
                        .title("청년 네트워킹 데이")
                        .organization("오산청년일자리지원센터 이루잡")
                        .region("오산시")
                        .imageUrl("https://images.unsplash.com/photo-1497366811353-6870744d04b2?w=460&h=340&fit=crop")
                        .content("창업·취업 동료와 함께하는 월간 네트워킹 밋업.")
                        .requirements("청년 누구나")
                        .startDate(today.minusDays(1)).endDate(today.plusDays(2))
                        .capacity(60).build()
        );

        programRepository.saveAll(seeds);
        log.info("Seeded {} programs", seeds.size());
    }
}
