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
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Benchmark for parsing and persisting MassBank records into a temporary PostgreSQL instance
 * (Testcontainers).
 *
 * <p>The test is disabled by default and only runs with
 * {@code -Dmassbank.benchmark.enabled=true}.</p>
 *
 * <p>Run from the command line (from the project directory):</p>
 * <pre>
 * ./mvnw -Dtest=RecordPersistenceBenchmarkTest -Dmassbank.benchmark.enabled=true test \
 * </pre>
 *
 * <p>Useful optional parameters:</p>
 * <ul>
 *   <li>{@code -Dmassbank.benchmark.limit=1000} limits the number of files read.</li>
 *   <li>{@code -Dmassbank.persistence.chunk-size=2000} controls the service chunk size.</li>
 *   <li>{@code -Dspring.jpa.properties.hibernate.jdbc.batch_size=2000} configures Hibernate batching.</li>
 *   <li>{@code -Dmassbank.benchmark.data-dir=/path/to/MassBank-data} location of test data.</li>
 * </ul>
 */
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

    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17")
            .withUrlParam("reWriteBatchedInserts", "true")
            .withCommand(
                    "postgres",
                    "-c", "shared_buffers=2GB",
                    "-c", "effective_cache_size=6GB",
                    "-c", "work_mem=16MB",
                    "-c", "maintenance_work_mem=512MB",
                    "-c", "max_wal_size=8GB",
                    "-c", "checkpoint_timeout=15min",
                    "-c", "wal_compression=on",
                    "-c", "random_page_cost=1.1",
                    "-c", "effective_io_concurrency=200",
                    "-c", "fsync=on",
                    "-c", "synchronous_commit=on",
                    "-c", "full_page_writes=on"
            )
            .withCreateContainerCmdModifier(cmd ->
                    cmd.getHostConfig()
                            .withNanoCPUs(4_000_000_000L)      // 4 vCPU
                            .withMemory(8L * 1024 * 1024 * 1024) // 8 GB
            );

    @DynamicPropertySource
    static void registerDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create");
        registry.add("spring.jpa.show-sql", () -> "false");
        registry.add("spring.jpa.properties.hibernate.format_sql", () -> "false");
        registry.add("spring.jpa.properties.hibernate.jdbc.batch_size",
                () -> System.getProperty("spring.jpa.properties.hibernate.jdbc.batch_size", "2000"));
        registry.add("spring.jpa.properties.hibernate.order_inserts", () -> "true");
        registry.add("spring.jpa.properties.hibernate.order_updates", () -> "true");
        registry.add("spring.jpa.properties.hibernate.batch_versioned_data", () -> "true");
        registry.add("massbank.persistence.chunk-size", () -> System.getProperty("massbank.persistence.chunk-size", "2000"));
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

    private enum BenchmarkOperation {
        SAVE_ALL,
        IMPORT_REPLACE
    }

    @Test
    void benchmarkRecordSaveAllWithMassBankData() throws IOException {
        Path configuredDataDir = resolveDataDir();
        assertTrue(Files.isDirectory(configuredDataDir), () -> "MassBank data directory does not exist: " + configuredDataDir
                + " (set -Dmassbank.benchmark.data-dir=/path/to/MassBank-data if needed)");
        Path dataDir = configuredDataDir.toRealPath();

        int limit = intBenchmarkProperty("limit", 0);
        BenchmarkOperation operation = benchmarkOperation();

        long discoverAndParseStarted = System.nanoTime();
        ParsedRecords parsedRecords = discoverAndParseRecords(dataDir, limit);
        long discoverAndParseNanos = System.nanoTime() - discoverAndParseStarted;

        recordService.deleteAll();

        long persistStarted = System.nanoTime();
        if (operation == BenchmarkOperation.IMPORT_REPLACE) {
            recordService.importAllReplacingData(parsedRecords.records());
        } else {
            recordService.saveAll(parsedRecords.records());
        }
        long persistNanos = System.nanoTime() - persistStarted;

        long activeCount = recordService.countActive();
        long deprecatedCount = recordService.countDeprecated();
        assertEquals(parsedRecords.activeCount(), activeCount);
        assertEquals(parsedRecords.deprecatedCount(), deprecatedCount);

        printBenchmarkResult(dataDir, parsedRecords.fileCount(), parsedRecords,
                discoverAndParseNanos, persistNanos, operation);
    }

    private static ParsedRecords discoverAndParseRecords(Path dataDir, int limit) throws IOException {
        List<Path> files;
        try (Stream<Path> stream = Files.walk(dataDir)) {
            Stream<Path> fileStream = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        String name = path.getFileName().toString();
                        return name.startsWith("MSBNK-") && name.endsWith(".txt");
                    })
                    .sorted(Comparator.comparing(Path::toString));
            if (limit > 0) {
                fileStream = fileStream.limit(limit);
            }
            files = fileStream.toList();
        }

        assertFalse(files.isEmpty(), () -> "No MSBNK-*.txt files found below " + dataDir);

        java.util.concurrent.atomic.LongAdder activeCount = new java.util.concurrent.atomic.LongAdder();
        java.util.concurrent.atomic.LongAdder deprecatedCount = new java.util.concurrent.atomic.LongAdder();
        java.util.concurrent.atomic.LongAdder peakCount = new java.util.concurrent.atomic.LongAdder();
        RecordParser parser = new RecordParser(java.util.Set.of("legacy"));
        List<AbstractRecord> records = files.parallelStream()
                .map(file -> {
                    try {
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
                        if (record instanceof Record activeRecord) {
                            activeCount.increment();
                            peakCount.add(activeRecord.PK_NUM_PEAK());
                        } else if (record instanceof DeprecatedRecord) {
                            deprecatedCount.increment();
                        }
                        return record;
                    } catch (IOException e) {
                        throw new IllegalStateException("Failed to read " + file, e);
                    }
                })
                .toList();

        return new ParsedRecords(records, activeCount.intValue(), deprecatedCount.intValue(),
                peakCount.longValue(), files.size());
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
        return workingDirectory.resolve("MassBank-data");
    }

    private static void printBenchmarkResult(Path dataDir,
                                             int fileCount,
                                             ParsedRecords parsedRecords,
                                             long discoverAndParseNanos,
                                             long persistNanos,
                                             BenchmarkOperation operation) {
        long totalNanos = discoverAndParseNanos + persistNanos;
        System.out.println();
        System.out.println("==== MassBank Record Persistence Benchmark ====");
        System.out.println("Data directory       : " + dataDir);
        System.out.println("PostgreSQL container : " + postgres.getDockerImageName());
        System.out.println("Files                : " + fileCount);
        System.out.println("Records              : " + parsedRecords.records().size());
        System.out.println("Active records       : " + parsedRecords.activeCount());
        System.out.println("Deprecated records   : " + parsedRecords.deprecatedCount());
        System.out.println("Peaks                : " + parsedRecords.peakCount());
        System.out.println("Benchmark operation  : " + (operation == BenchmarkOperation.IMPORT_REPLACE ? "importReplace" : "saveAll"));
        System.out.println("Service chunk size   : " + System.getProperty("massbank.persistence.chunk-size", "2000"));
        System.out.println("Hibernate batch size : " + System.getProperty("spring.jpa.properties.hibernate.jdbc.batch_size", "2000"));
        System.out.println("Discover+parse time  : " + formatSeconds(discoverAndParseNanos));
        System.out.println("Persist time         : " + formatSeconds(persistNanos));
        if (operation == BenchmarkOperation.IMPORT_REPLACE) {
            System.out.println("Import.replaceAll    : " + formatSeconds(persistNanos));
        }
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

    private static BenchmarkOperation benchmarkOperation() {
        String operation = benchmarkProperty("operation", "saveAll").trim();
        if ("importReplace".equalsIgnoreCase(operation)) {
            return BenchmarkOperation.IMPORT_REPLACE;
        }
        if ("saveAll".equalsIgnoreCase(operation)) {
            return BenchmarkOperation.SAVE_ALL;
        }
        throw new IllegalArgumentException("Invalid massbank.benchmark.operation: " + operation
                + " (supported: saveAll, importReplace)");
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


    private record ParsedRecords(List<AbstractRecord> records, int activeCount, int deprecatedCount,
                                 long peakCount, int fileCount) {
    }
}



