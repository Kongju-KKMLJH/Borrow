package kkmljh.borrow.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 전체 컨텍스트 + MockMvc 통합 테스트.
 *
 * <p>DB는 인메모리 H2로 고정한다 — {@code application.yml}(MySQL)은 커밋되지 않으므로
 * 도커·로컬 설정 없이도 {@code ./gradlew test} 가 돌아야 한다.
 * 각 테스트는 트랜잭션 롤백으로 서로 격리된다.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:borrow-it;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.upload.dir=build/test-uploads"
})
@AutoConfigureMockMvc
@Transactional
public @interface IntegrationTest {
}
