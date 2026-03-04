package massbank.db;

import massbank.Record;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = RecordDbExample.class)
@Testcontainers
class RecordDbExampleTest {

    static final PostgreSQLContainer postgres = new PostgreSQLContainer(
            "postgres:17-alpine");

    @DynamicPropertySource
    static void registerDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
    }

    @BeforeAll
    static void beforeAll() {
        postgres.start();
    }

    @AfterAll
    static void afterAll() {
        postgres.stop();
    }

    @Autowired
    private RecordRepository repository;

    @Test
    void saveAndLoadRecord() {
        Record r = new Record();
        r.setAccession("TEST-001");
        r.RECORD_TITLE(List.of("Test Compound"));
        r.CH_FORMULA("H2O");
        r.CH_EXACT_MASS(new BigDecimal("18.01056"));
        r.CH_SMILES("O");

        repository.save(r);

        Record loaded = repository.findById("TEST-001").orElse(null);
        assertNotNull(loaded);
        assertEquals("TEST-001", loaded.getAccession());
        assertEquals("H2O", loaded.CH_FORMULA());
    }
}
