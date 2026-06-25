package io.github.sihyuuun.youthmoa.common;

import io.github.sihyuuun.youthmoa.program.Program;
import io.github.sihyuuun.youthmoa.program.ProgramRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final ProgramRepository programRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (programRepository.count() > 0) {
            log.info("Programs already seeded (count={}), skip", programRepository.count());
            return;
        }

        LocalDate today = LocalDate.now();
        List<Program> seeds = List.of(
                Program.builder()
                        .title("취업역량 강화 워크숍")
                        .organization("내일스퀘어")
                        .category("취업").region("부천시")
                        .imageUrl("https://images.unsplash.com/photo-1524178232363-1fb2b075b655?w=400&h=280&fit=crop")
                        .content("이력서 작성, 면접 트레이닝, 자기소개 워크숍을 한 번에 진행합니다.")
                        .requirements("만 19~39세 경기도 거주 청년")
                        .startDate(today.minusDays(10)).endDate(today.plusDays(3))
                        .capacity(30).build(),

                Program.builder()
                        .title("청년 창업 아카데미")
                        .organization("상상대로")
                        .category("창업").region("수원시")
                        .imageUrl("https://images.unsplash.com/photo-1529156069898-49953e39b3ac?w=400&h=280&fit=crop")
                        .content("창업 아이디어 발굴부터 사업 모델 검증까지 8주 과정.")
                        .requirements("창업 관심 청년 누구나")
                        .startDate(today.minusDays(5)).endDate(today.plusDays(12))
                        .capacity(25).build(),

                Program.builder()
                        .title("마음건강 힐링 캠프")
                        .organization("범계역 청년출구")
                        .category("힐링").region("안양시")
                        .imageUrl("https://images.unsplash.com/photo-1517245386807-bb43f82c33c4?w=400&h=280&fit=crop")
                        .content("청년의 번아웃을 회복하는 1박 2일 자연 힐링 캠프.")
                        .requirements("만 19~34세 청년")
                        .startDate(today.minusDays(2)).endDate(today.plusDays(6))
                        .capacity(20).build(),

                Program.builder()
                        .title("디지털 마케팅 실전반")
                        .organization("원미청정구역")
                        .category("교육").region("부천시")
                        .imageUrl("https://images.unsplash.com/photo-1522202176988-66273c2fd55f?w=400&h=280&fit=crop")
                        .content("SNS·검색 광고 실전 캠페인 운영 실습.")
                        .requirements("디지털 마케팅 입문 청년")
                        .startDate(today.minusDays(30)).endDate(today.minusDays(5))
                        .capacity(15).build(),

                Program.builder()
                        .title("AI 활용 실무 교육")
                        .organization("비행지구")
                        .category("교육").region("고양시")
                        .imageUrl("https://images.unsplash.com/photo-1531482615713-2afd69097998?w=400&h=280&fit=crop")
                        .content("ChatGPT·Claude 등 LLM 활용 실무 워크숍 (전 6강).")
                        .requirements("개발자 또는 기획자 청년")
                        .startDate(today.plusDays(14)).endDate(today.plusDays(45))
                        .capacity(30).build(),

                Program.builder()
                        .title("소셜벤처 인큐베이팅")
                        .organization("오름")
                        .category("창업").region("용인시")
                        .imageUrl("https://images.unsplash.com/photo-1552664730-d307ca884978?w=400&h=280&fit=crop")
                        .content("사회 문제 해결형 비즈니스 모델 인큐베이팅 6개월 과정.")
                        .requirements("소셜벤처 관심 예비/초기 창업팀")
                        .startDate(today.plusDays(21)).endDate(today.plusDays(180))
                        .capacity(20).build(),

                Program.builder()
                        .title("주거 지원 청년 매칭")
                        .organization("딴딴회관")
                        .category("주거").region("군포시")
                        .imageUrl("https://images.unsplash.com/photo-1524758631624-e2822e304c36?w=460&h=340&fit=crop")
                        .content("청년 주거 상담 + 매물 매칭 + 보증금 지원 안내.")
                        .requirements("만 19~34세 무주택 청년")
                        .startDate(today.minusDays(3)).endDate(today.plusDays(21))
                        .capacity(null).build(),

                Program.builder()
                        .title("청년 문화예술 스쿨")
                        .organization("고천센터")
                        .category("교육").region("의왕시")
                        .imageUrl("https://images.unsplash.com/photo-1497366216548-37526070297c?w=460&h=340&fit=crop")
                        .content("연극·사진·뮤지컬 등 6개 트랙 문화예술 입문 강좌.")
                        .requirements("문화예술 입문 청년")
                        .startDate(today.minusDays(7)).endDate(today.plusDays(30))
                        .capacity(40).build(),

                Program.builder()
                        .title("청년 네트워킹 데이")
                        .organization("이루잡")
                        .category("창업").region("화성시")
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
