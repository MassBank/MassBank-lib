package massbank.db;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import massbank.Record;
import massbank.RecordParserTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class RecordDbTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = Record.class)
    @EnableJpaRepositories(basePackageClasses = RecordRepository.class)
    static class TestApplication {
    }

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

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void saveAndLoadFixtureRecords() throws IOException {
        Path resourcesDir = Paths.get("src/test/resources");
        assertTrue(Files.exists(resourcesDir), "src/test/resources not found");

        List<Path> recordFiles;
        try (Stream<Path> paths = Files.walk(resourcesDir)) {
            recordFiles = paths
                    .filter(p -> p.toString().endsWith(".txt"))
                    .filter(p -> p.getFileName().toString().startsWith("MSBNK-"))
                    .toList();
        }
        assertFalse(recordFiles.isEmpty(), "No .txt files in src/test/resources found");

        for (Path file : recordFiles) {
            RecordParserTest.ParseResult res = RecordParserTest.parseRecord(file.getFileName().toString());
            assertTrue(res.result().isSuccess());
            Record record = res.result().get();
            Record loaded = persistAndReload(record);
            assertMappedFieldsEqual(record, loaded);
        }

        assertEquals(recordFiles.size(), repository.count(), "unexpected number of persisted fixture records");
    }

    @Test
    void saveAndLoadManuallyCreatedRecord() {
        Record r = new Record();
        r.setAccession("TEST-001");
        r.setDate("2026.05.12 (Created 2026.05.12)");
        r.setAuthors("Test Author");
        r.setLicense("CC BY-SA");
        r.setCopyright("Copyright (C) 2026 Test Lab");
        r.setPublication("Example publication DOI:10.1000/test");
        r.setProject("Entity migration");

        Record loaded = persistAndReload(r);
        assertMappedFieldsEqual(r, loaded);
    }

    private Record persistAndReload(Record record) {
        repository.saveAndFlush(record);
        entityManager.flush();
        entityManager.clear();

        return repository.findById(record.getAccession())
                .orElseThrow(() -> new AssertionError("record not found: " + record.getAccession()));
    }

    private static void assertMappedFieldsEqual(Record expected, Record actual) {
        assertNotNull(actual, "loaded record is null");
        assertEquals(expected.getAccession(), actual.getAccession(), () -> "accession mismatch for " + expected.getAccession());
        assertEquals(expected.getDate(), actual.getDate(), () -> "date mismatch for " + expected.getAccession());
        assertEquals(expected.getAuthors(), actual.getAuthors(), () -> "authors mismatch for " + expected.getAccession());
        assertEquals(expected.getLicense(), actual.getLicense(), () -> "license mismatch for " + expected.getAccession());
        assertEquals(expected.getCopyright(), actual.getCopyright(), () -> "copyright mismatch for " + expected.getAccession());
        assertEquals(expected.getPublication(), actual.getPublication(), () -> "publication mismatch for " + expected.getAccession());
        assertEquals(expected.getProject(), actual.getProject(), () -> "project mismatch for " + expected.getAccession());
    }
}
