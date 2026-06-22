package massbank.db;

import massbank.AbstractRecord;
import massbank.DeprecatedRecord;
import massbank.Record;
import massbank.RecordParser;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.petitparser.context.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import(RecordServiceImplementation.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@EnabledIfSystemProperty(named = "massbank.benchmark.enabled", matches = "true")
class RecordPersistenceBenchmarkTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = Record.class)
    @EnableJpaRepositories(basePackages = "massbank.db")
    static class BenchmarkApplication {
    }

    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @DynamicPropertySource
    static void registerDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create");
        registry.add("spring.jpa.show-sql", () -> "false");
        registry.add("spring.jpa.properties.hibernate.format_sql", () -> "false");
        registry.add("spring.jpa.properties.hibernate.jdbc.batch_size", () -> benchmarkProperty("hibernate-batch-size", "100"));
        registry.add("spring.jpa.properties.hibernate.order_inserts", () -> "true");
        registry.add("spring.jpa.properties.hibernate.order_updates", () -> "true");
        registry.add("spring.jpa.properties.hibernate.batch_versioned_data", () -> "true");
        registry.add("massbank.persistence.chunk-size", () -> benchmarkProperty("chunk-size", "1000"));
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

    @Test
    void benchmarkRecordSaveAllWithMassBankData() throws IOException {
        Path configuredDataDir = resolveDataDir();
        assertTrue(Files.isDirectory(configuredDataDir), () -> "MassBank data directory does not exist: " + configuredDataDir
                + " (set -Dmassbank.benchmark.data-dir=/path/to/MassBank-data if needed)");
        Path dataDir = configuredDataDir.toRealPath();

        int limit = intBenchmarkProperty("limit", 0);
        int persistBatchSize = intBenchmarkProperty("persist-batch-size", 0);
        boolean validate = booleanBenchmarkProperty("validate", true);

        long discoverStarted = System.nanoTime();
        List<Path> files = discoverRecordFiles(dataDir, limit);
        long discoverNanos = System.nanoTime() - discoverStarted;
        assertTrue(!files.isEmpty(), () -> "No MSBNK-*.txt files found below " + dataDir);

        long parseStarted = System.nanoTime();
        ParsedRecords parsedRecords = parseRecords(files, validate);
        long parseNanos = System.nanoTime() - parseStarted;

        recordService.deleteAll();

        long persistStarted = System.nanoTime();
        persist(parsedRecords.records(), persistBatchSize);
        long persistNanos = System.nanoTime() - persistStarted;

        long activeCount = recordService.countActive();
        long deprecatedCount = recordService.countDeprecated();
        assertEquals(parsedRecords.activeCount(), activeCount);
        assertEquals(parsedRecords.deprecatedCount(), deprecatedCount);

        printBenchmarkResult(dataDir, files.size(), parsedRecords, discoverNanos, parseNanos, persistNanos,
                persistBatchSize, validate);
    }

    private static List<Path> discoverRecordFiles(Path dataDir, int limit) throws IOException {
        try (Stream<Path> stream = Files.walk(dataDir)) {
            Stream<Path> files = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        String name = path.getFileName().toString();
                        return name.startsWith("MSBNK-") && name.endsWith(".txt");
                    })
                    .sorted(Comparator.comparing(Path::toString));
            if (limit > 0) {
                files = files.limit(limit);
            }
            return files.toList();
        }
    }

    private static ParsedRecords parseRecords(List<Path> files, boolean validate) throws IOException {
        Set<String> config = new HashSet<>();
        if (validate) {
            config.add("validate");
        }
        RecordParser parser = new RecordParser(config);

        List<AbstractRecord> records = new ArrayList<>(files.size());
        int activeCount = 0;
        int deprecatedCount = 0;
        long peakCount = 0;

        for (Path file : files) {
            String content = Files.readString(file, StandardCharsets.UTF_8);
            Result result = parser.parse(content);
            if (!result.isSuccess()) {
                throw new IllegalStateException("Failed to parse " + file + ": " + result);
            }
            Object parsed = result.get();
            if (!(parsed instanceof AbstractRecord record)) {
                throw new IllegalStateException("Parser did not return an AbstractRecord for " + file + ": "
                        + parsed.getClass().getName());
            }
            records.add(record);
            if (record instanceof Record activeRecord) {
                activeCount++;
                peakCount += activeRecord.PK_NUM_PEAK();
            } else if (record instanceof DeprecatedRecord) {
                deprecatedCount++;
            }
        }
        return new ParsedRecords(records, activeCount, deprecatedCount, peakCount);
    }

    private void persist(List<AbstractRecord> records, int persistBatchSize) {
        if (persistBatchSize <= 0) {
            recordService.saveAll(records);
            return;
        }
        for (int from = 0; from < records.size(); from += persistBatchSize) {
            int to = Math.min(from + persistBatchSize, records.size());
            recordService.saveAll(records.subList(from, to));
        }
    }

    private static Path resolveDataDir() {
        String configured = System.getProperty("massbank.benchmark.data-dir");
        if (configured == null || configured.isBlank()) {
            configured = System.getenv("MASSBANK_BENCHMARK_DATA_DIR");
        }
        if (configured != null && !configured.isBlank()) {
            return Path.of(configured).toAbsolutePath().normalize();
        }

        Path workingDirectory = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        Path insideProject = workingDirectory.resolve("MassBank-data");
        if (Files.isDirectory(insideProject)) {
            return insideProject;
        }
        Path sibling = workingDirectory.resolveSibling("MassBank-data");
        if (Files.isDirectory(sibling)) {
            return sibling;
        }
        return sibling;
    }

    private static void printBenchmarkResult(Path dataDir, int fileCount, ParsedRecords parsedRecords,
                                             long discoverNanos, long parseNanos, long persistNanos,
                                             int persistBatchSize, boolean validate) {
        long totalNanos = discoverNanos + parseNanos + persistNanos;
        System.out.println();
        System.out.println("==== MassBank Record Persistence Benchmark ====");
        System.out.println("Data directory       : " + dataDir);
        System.out.println("PostgreSQL container : " + postgres.getDockerImageName());
        System.out.println("Files                : " + fileCount);
        System.out.println("Records              : " + parsedRecords.records().size());
        System.out.println("Active records       : " + parsedRecords.activeCount());
        System.out.println("Deprecated records   : " + parsedRecords.deprecatedCount());
        System.out.println("Peaks                : " + parsedRecords.peakCount());
        System.out.println("Parser validation    : " + validate);
        System.out.println("Service chunk size   : " + benchmarkProperty("chunk-size", "1000"));
        System.out.println("Hibernate batch size : " + benchmarkProperty("hibernate-batch-size", "100"));
        System.out.println("Persist batch size   : " + (persistBatchSize > 0 ? persistBatchSize : "all records in one saveAll"));
        System.out.println("Discover time        : " + formatSeconds(discoverNanos));
        System.out.println("Parse time           : " + formatSeconds(parseNanos));
        System.out.println("Persist time         : " + formatSeconds(persistNanos));
        System.out.println("Total time           : " + formatSeconds(totalNanos));
        System.out.println("Persist records/sec  : " + formatRate(parsedRecords.records().size(), persistNanos));
        System.out.println("Persist peaks/sec    : " + formatRate(parsedRecords.peakCount(), persistNanos));
        System.out.println("===============================================");
        System.out.println();
    }

    private static String benchmarkProperty(String name, String defaultValue) {
        return System.getProperty("massbank.benchmark." + name, defaultValue);
    }

    private static int intBenchmarkProperty(String name, int defaultValue) {
        String value = benchmarkProperty(name, Integer.toString(defaultValue));
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid integer value for massbank.benchmark." + name + ": " + value, e);
        }
    }

    private static boolean booleanBenchmarkProperty(String name, boolean defaultValue) {
        return Boolean.parseBoolean(benchmarkProperty(name, Boolean.toString(defaultValue)));
    }

    private static String formatSeconds(long nanos) {
        return String.format(Locale.ROOT, "%.3f s", nanos / 1_000_000_000.0);
    }

    private static String formatRate(long count, long nanos) {
        if (nanos <= 0) {
            return "n/a";
        }
        return String.format(Locale.ROOT, "%.1f", count / (nanos / 1_000_000_000.0));
    }

    private record ParsedRecords(List<AbstractRecord> records, int activeCount, int deprecatedCount, long peakCount) {
    }
}



