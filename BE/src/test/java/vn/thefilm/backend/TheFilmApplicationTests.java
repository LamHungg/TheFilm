package vn.thefilm.backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@DisplayName("THEFILM Spring Boot context")
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:thefilmtest;DB_CLOSE_DELAY=-1;MODE=Oracle",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=classpath:db/admin-fixture/schema.sql",
        "spring.sql.init.data-locations=classpath:db/admin-fixture/data.sql"
})
class TheFilmApplicationTests {

    @Test
    @DisplayName("loads successfully with the local H2 schema and seed data")
    void contextLoads() {
    }
}
