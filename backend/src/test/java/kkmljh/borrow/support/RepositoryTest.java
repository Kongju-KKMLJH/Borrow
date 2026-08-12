package kkmljh.borrow.support;

import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * JPA 슬라이스 테스트 — 리포지토리의 {@code @Query} 와 파생 쿼리를 실제로 실행해 본다.
 * DB는 인메모리 H2로 고정한다(로컬 MySQL·도커 불필요).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:borrow-repo;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
public @interface RepositoryTest {
}
