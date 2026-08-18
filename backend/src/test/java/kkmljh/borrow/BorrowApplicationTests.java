package kkmljh.borrow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 컨텍스트 로딩 확인.
 *
 * <p>DB 설정은 {@code @SpringBootTest(properties=...)} 로 덮어써 인메모리 H2를 쓴다 —
 * 커밋되지 않는 로컬 {@code application.yml}(MySQL)이나 도커 기동 여부와 무관하게
 * {@code ./gradlew test} 품질 게이트가 돌아가야 하기 때문이다.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:borrow;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.upload.dir=build/test-uploads"
})
class BorrowApplicationTests {

    @Test
    @DisplayName("애플리케이션 컨텍스트가 정상적으로 로딩된다")
    void contextLoads() {
    }
}
