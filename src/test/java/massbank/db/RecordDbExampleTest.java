package massbank.db;

import massbank.Record;
import massbank.RecordParserTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

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
    void saveAndLoadRecords() throws IOException {
        Path resourcesDir = Paths.get("src/test/resources");
        assertTrue(Files.exists(resourcesDir), "src/test/resources not found");

        List<Path> recordFiles = Files.walk(resourcesDir)
                .filter(p -> p.toString().endsWith(".txt"))
                .filter(p -> p.getFileName().toString().startsWith("MSBNK-"))
                .toList();
        assertFalse(recordFiles.isEmpty(), "No .txt files in src/test/resources found");

        for (Path file : recordFiles) {
            RecordParserTest.ParseResult res = RecordParserTest.parseRecord("MSBNK-test-TST00001.txt");
            assertTrue(res.result().isSuccess());
            Record record = (Record) res.result().get();
            repository.save(record);
            Record loaded = repository.findById(record.getAccession()).orElse(null);
            assertNotNull(loaded, () -> "record not found: " + record.getAccession());
            assertEquals(record.toString(), loaded.toString(), () -> "mismatch for " + record.getAccession());
        }


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
