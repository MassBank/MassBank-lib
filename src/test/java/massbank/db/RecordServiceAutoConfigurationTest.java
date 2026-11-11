package massbank.db;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = RecordServiceAutoConfigurationTest.TestApplication.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class RecordServiceAutoConfigurationTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApplication {
    }

    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

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
    private RecordService recordService;

    @Autowired
    private RecordRepository recordRepository;

    @Autowired
    private DeprecatedRecordRepository deprecatedRecordRepository;

    @Autowired
    private DataSource dataSource;

    @Test
    void peakIndex_isCreatedOnlyAfterImportAndRepeatedCallsRetainIt() throws Exception {
        try (var connection = dataSource.getConnection();
             var statement = connection.createStatement()) {
            try (var result = statement.executeQuery(
                    "SELECT to_regclass('massbank_peaks_record_id_mz_idx')")) {
                assertTrue(result.next());
                assertNull(result.getString(1), "Schema creation must leave the index absent");
            }
            recordService.ensurePeakIndex();
            long indexOid;
            try (var result = statement.executeQuery("""
                    SELECT i.indexrelid, i.indisvalid, i.indisready, pg_get_indexdef(i.indexrelid)
                    FROM pg_index i
                    WHERE i.indrelid = '"massbank-peaks"'::regclass
                      AND i.indexrelid = 'massbank_peaks_record_id_mz_idx'::regclass
                    """)) {
                assertTrue(result.next());
                indexOid = result.getLong(1);
                assertTrue(result.getBoolean(2));
                assertTrue(result.getBoolean(3));
                assertTrue(result.getString(4).contains("(record_id, mz)"));
            }
            recordService.ensurePeakIndex();
            try (var result = statement.executeQuery(
                    "SELECT 'massbank_peaks_record_id_mz_idx'::regclass::oid")) {
                assertTrue(result.next());
                assertEquals(indexOid, result.getLong(1));
            }
        }
    }

    @Test
    void autoConfiguration_registersRecordBeans() {
        assertNotNull(recordService);
        assertNotNull(recordRepository);
        assertNotNull(deprecatedRecordRepository);
    }
}

