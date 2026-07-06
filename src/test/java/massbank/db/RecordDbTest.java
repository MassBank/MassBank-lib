package massbank.db;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import massbank.AbstractRecord;
import massbank.DeprecatedRecord;
import massbank.Peak;
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
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import(RecordServiceImplementation.class)
class RecordDbTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = Record.class)
    @EnableJpaRepositories(basePackages = "massbank.db")
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

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void saveAndLoadManuallyCreatedRecord_compareMappedFields() {
        Record r = createManualRecordFixture("TEST-001");

        Record loaded = (Record) persistAndReload(r);
        assertMappedFieldsEqual(r, loaded);
    }

    @Test
    void saveAbstractRecord_supportsRecordAndDeprecatedRecord() {
        recordService.deleteAll();

        Record activeRecord = createManualRecordFixture("TEST-002");
        DeprecatedRecord deprecatedRecord = createDeprecatedRecordFixture("TEST-DEPRECATED-001");

        AbstractRecord savedActive = recordService.save(activeRecord);
        AbstractRecord savedDeprecated = recordService.save(deprecatedRecord);
        entityManager.flush();
        entityManager.clear();

        assertInstanceOf(Record.class, savedActive);
        assertInstanceOf(DeprecatedRecord.class, savedDeprecated);
        assertInstanceOf(Record.class, recordService.findById(activeRecord.getAccession()));
        assertInstanceOf(DeprecatedRecord.class, recordService.findById(deprecatedRecord.getAccession()));
    }

    private static Record createManualRecordFixture(String accession) {
        Record r = new Record();
        r.setAccession(accession);
        r.setRecordTitle(List.of("Naringenin", "LC-ESI-QTOF", "MS2", "CE:15 eV", "[M+H]+"));
        r.setDate("2026.05.12 (Created 2026.05.12)");
        r.setAuthors("Test Author");
        r.setLicense("CC BY-SA");
        r.setCopyright("Copyright (C) 2026 Test Lab");
        r.setPublication("Example publication DOI:10.1000/test");
        r.setProject("Entity migration");
        r.setComment(List.of("First comment", "Second comment"));
        r.setChName(List.of("Primary name", "Secondary synonym"));
        r.setChCompoundClass(List.of("Lipid", "Ceramide"));
        r.setChFormula("C12H24O12");
        r.setChExactMass(new BigDecimal("360.12678"));
        r.setChSMILES("C1[C@H](OC2=CC(=CC(=C2C1=O)O)O)C3=CC=C(C=C3)O");
        r.setChIUPAC("InChI=1S/C15H12O5/c16-9-3-1-8(2-4-9)13-7-12(19)15-11(18)5-10(17)6-14(15)20-13/h1-6,13,16-18H,7H2/t13-/m0/s1");
        r.setChLink(List.of(
                new Record.KeyValue("INCHIKEY", "AAAA-BBBB"),
                new Record.KeyValue("CAS", "123-45-6")
        ));
        r.setSpScientificName("Mus musculus");
        r.setSpLineage("cellular organisms; Eukaryota; Metazoa; Chordata; Mammalia; Rodentia; Mus");
        r.setSpLink(List.of(
                new Record.KeyValue("TAXON_ID", "9606"),
                new Record.KeyValue("NCBI", "Homo sapiens")
        ));
        r.setSpSample(List.of("liver", "plasma"));
        r.setAcInstrument("Q Exactive Orbitrap");
        r.setAcInstrumentType("LC-ESI-QTOF");
        r.setAcMassSpectrometryMsType("MS2");
        r.setAcMassSpectrometryIonMode("POSITIVE");
        r.setAcMassSpectrometry(List.of(
                new Record.KeyValue("COLLISION_ENERGY", "35 eV"),
                new Record.KeyValue("RESOLUTION", "70000")
        ));
        r.setAcChromatography(List.of(
                new Record.KeyValue("COLUMN", "Waters Acquity UPLC BEH C18"),
                new Record.KeyValue("FLOW_GRADIENT", "0.3 mL/min")
        ));
        r.setMsFocusedIon(List.of(
                new Record.KeyValue("PRECURSOR_TYPE", "[M+H]+"),
                new Record.KeyValue("PRECURSOR_M/Z", "123.456")
        ));
        r.setMsDataProcessing(List.of(
                new Record.KeyValue("DEISOTOPING", "done"),
                new Record.KeyValue("CENTROIDING", "raw")
        ));

        r.setPkAnnotationHeader(List.of("m/z", "ion"));
        r.setPkAnnotation(List.of(
                new Record.PeakAnnotationRow(new BigDecimal("1278.12"), List.of("[LH-2NeuAc+Na]+")),
                new Record.PeakAnnotationRow(new BigDecimal("1306.21"), List.of("[M-2NeuAc+Na]+")),
                new Record.PeakAnnotationRow(new BigDecimal("1597.12"), List.of("[M-NeuAc+Na]+")),
                new Record.PeakAnnotationRow(new BigDecimal("1860.16"), List.of("[LH+Na]+"))
        ));

        r.addPeak(new Peak(BigDecimal.valueOf(147.044), BigDecimal.valueOf(218.845), 20));
        r.addPeak(new Peak(BigDecimal.valueOf(153.019), BigDecimal.valueOf(316.545), 30));
        r.addPeak(new Peak(BigDecimal.valueOf(273.076), BigDecimal.valueOf(10000.000), 999));
        r.addPeak(new Peak(BigDecimal.valueOf(274.083), BigDecimal.valueOf(318.003), 30));
        return r;
    }

    private static DeprecatedRecord createDeprecatedRecordFixture(String accession) {
        DeprecatedRecord deprecatedRecord = new DeprecatedRecord();
        deprecatedRecord.setAccession(accession);
        deprecatedRecord.setDeprecated("This record was deprecated and superseded by TEST-SUCCESSOR-001.");
        deprecatedRecord.setDeprecatedContent("""
        RECORD_TITLE: 11-HDoHE; LC-ESI-QTOF; MS2; CE: 20.0; R=N/A; [M-H]-
        DATE: 2026.05.12 (Created 2026.05.12)
        AUTHORS: Test Author
        DATE: 2018.11.21
        AUTHORS: Nils Hoffmann, Dominik Kopczynski, Bing Peng
        LICENSE: CC BY-SA""");
        return deprecatedRecord;
    }

    @Test
    void saveAndLoadAllFixtureRecords_compareMappedFields() throws IOException {
        List<Path> recordFiles = loadFixtureRecordFiles();

        for (Path file : recordFiles) {
            AbstractRecord expected = parseRecordFromFixture(file);
            AbstractRecord loaded = persistAndReload(expected);
            assertMappedFieldsEqual(expected, loaded);
        }

        assertEquals(recordFiles.size(), recordService.countAll(),
                "unexpected number of persisted fixture records");
    }


    @Test
    void countMethods_returnConsistentValuesAcrossBothTables() {
        recordService.deleteAll();

        recordService.save(createManualRecordFixture("TEST-ACTIVE-COUNT-001"));
        recordService.save(createManualRecordFixture("TEST-ACTIVE-COUNT-002"));
        recordService.save(createDeprecatedRecordFixture("TEST-DEPRECATED-COUNT-001"));

        assertEquals(2L, recordService.countActive());
        assertEquals(1L, recordService.countDeprecated());
        assertEquals(3L, recordService.countAll());
    }

    @Test
    void getAllAccessions_returnsAccessionsFromBothTables() {
        recordService.deleteAll();

        recordService.save(createManualRecordFixture("TEST-ACCESSION-ACTIVE-001"));
        recordService.save(createDeprecatedRecordFixture("TEST-ACCESSION-DEPRECATED-001"));

        Set<String> accessions = Set.copyOf(recordService.getAllAccessions());
        assertEquals(Set.of("TEST-ACCESSION-ACTIVE-001", "TEST-ACCESSION-DEPRECATED-001"), accessions);
    }

    @Test
    void activeRecordApis_returnOnlyActiveRecords() {
        recordService.deleteAll();

        Record active = createManualRecordFixture("TEST-ACTIVE-API-001");
        recordService.save(active);
        recordService.save(createDeprecatedRecordFixture("TEST-ACTIVE-API-DEPRECATED-001"));

        List<Record> activeRecords = recordService.findAllActive();
        assertEquals(1, activeRecords.size());
        assertEquals("TEST-ACTIVE-API-001", activeRecords.get(0).getAccession());

        Record loadedActive = recordService.findByIdAsRecord("TEST-ACTIVE-API-001");
        assertEquals("TEST-ACTIVE-API-001", loadedActive.getAccession());
        assertThrows(RuntimeException.class,
                () -> recordService.findByIdAsRecord("TEST-ACTIVE-API-DEPRECATED-001"));
    }

    @Test
    void deleteAll_clearsActiveAndDeprecatedRecords() {
        recordService.save(createManualRecordFixture("TEST-ACTIVE-DELETE-001"));
        recordService.save(createDeprecatedRecordFixture("TEST-DEPRECATED-DELETE-001"));
        assertEquals(2L, recordService.countAll());

        recordService.deleteAll();

        assertEquals(0L, recordService.countActive());
        assertEquals(0L, recordService.countDeprecated());
        assertEquals(0L, recordService.countAll());
        assertTrue(recordService.findAll().isEmpty());
    }

    @Test
    void accessionClaim_duplicateAccessionsAreCaughtAcrossRecordTypes() {
        recordService.deleteAll();

        // Persist two different records first to ensure valid baseline state.
        recordService.save(createManualRecordFixture("MSBNK-TEST-ACTIVE-001"));
        recordService.save(createDeprecatedRecordFixture("MSBNK-TEST-DEPRECATED-001"));
        assertEquals(2L, recordService.countAll());

        // Duplicate of an existing active accession must be rejected for deprecated records.
        DeprecatedRecord duplicateDeprecated = createDeprecatedRecordFixture("MSBNK-TEST-ACTIVE-001");
        IllegalStateException duplicateOfActive = assertThrows(IllegalStateException.class,
                () -> recordService.save(duplicateDeprecated));
        assertTrue(duplicateOfActive.getMessage().contains("Duplicate accession across record tables"));

        // Duplicate of an existing deprecated accession must be rejected for active records.
        Record duplicateActive = createManualRecordFixture("MSBNK-TEST-DEPRECATED-001");
        IllegalStateException duplicateOfDeprecated = assertThrows(IllegalStateException.class,
                () -> recordService.save(duplicateActive));
        assertTrue(duplicateOfDeprecated.getMessage().contains("Duplicate accession across record tables"));

        // saveAll must also reject duplicates inside a mixed batch before any persist happens.
        Record batchActive = createManualRecordFixture("MSBNK-TEST-BATCH-001");
        DeprecatedRecord batchDeprecated = createDeprecatedRecordFixture("MSBNK-TEST-BATCH-001");
        IllegalStateException duplicateInBatch = assertThrows(IllegalStateException.class,
                () -> recordService.saveAll(List.of(batchActive, batchDeprecated)));
        assertTrue(duplicateInBatch.getMessage().contains("Duplicate accession across record tables"));

        // Original two records remain the only persisted entries.
        assertEquals(1L, recordService.countActive());
        assertEquals(1L, recordService.countDeprecated());
        assertEquals(2L, recordService.countAll());
    }


    @Test
    void fullRoundtripAllFixtureRecords_textToRecordSaveLoadText_compareTextState() throws IOException {
        List<Path> recordFiles = loadFixtureRecordFiles();

        for (Path file : recordFiles) {
            RecordParserTest.ParseResult parseResult = parseRecordResultFromFixture(file);
            AbstractRecord parsedRecord = parseResult.result().get();

            AbstractRecord loaded = persistAndReload(parsedRecord);
            assertEquals(parseResult.content(), loaded.toString(),
                    () -> "Text state mismatch for " + parsedRecord.getAccession());
        }
    }

    private AbstractRecord persistAndReload(AbstractRecord record) {
        recordService.save(record);
        recordRepository.flush();
        deprecatedRecordRepository.flush();
        entityManager.flush();
        entityManager.clear();
        return recordService.findById(record.getAccession());
    }

    private static List<Path> loadFixtureRecordFiles() throws IOException {
        Path resourcesDir = Paths.get("src/test/resources");
        assertTrue(Files.exists(resourcesDir), "src/test/resources not found");

        List<Path> recordFiles;
        try (Stream<Path> paths = Files.walk(resourcesDir, FileVisitOption.FOLLOW_LINKS)) {
            recordFiles = paths
                    .filter(path -> path.toString().endsWith(".txt"))
                    .filter(path -> path.getFileName().toString().startsWith("MSBNK-"))
                    .sorted()
                    .peek(path -> System.out.println("Found file: " + path))
                    .toList();
        }
        assertFalse(recordFiles.isEmpty(), "No .txt files in src/test/resources found");
        return recordFiles;
    }

    private static AbstractRecord parseRecordFromFixture(Path file) throws IOException {
        return parseRecordResultFromFixture(file).result().get();
    }

    private static RecordParserTest.ParseResult parseRecordResultFromFixture(Path file) throws IOException {
        RecordParserTest.ParseResult parseResult = RecordParserTest.parseRecord(file.getFileName().toString());
        assertTrue(parseResult.result().isSuccess(), () -> "parse failed for " + file.getFileName());
        return parseResult;
    }


    private static void assertMappedFieldsEqual(AbstractRecord expected, AbstractRecord actual) {
        assertNotNull(actual, "loaded record is null");
        assertEquals(expected.getAccession(), actual.getAccession(), () -> "ACCESSION mismatch for " + expected.getAccession());
        if (expected instanceof Record exp && actual instanceof Record act) {
            assertRecordFieldsEqual(exp, act);
        } else if (expected instanceof DeprecatedRecord exp && actual instanceof DeprecatedRecord act) {
            assertDeprecatedRecordFieldsEqual(exp, act);
        } else {
            fail("Record type mismatch: " + expected.getClass() + " vs " + actual.getClass());
        }
    }

    private static void assertRecordFieldsEqual(Record expected, Record actual) {
        assertEquals(expected.getRecordTitle(), actual.getRecordTitle(), () -> "RECORD_TITLE mismatch for " + expected.getAccession());
        assertEquals(expected.getDate(), actual.getDate(), () -> "DATE mismatch for " + expected.getAccession());
        assertEquals(expected.getAuthors(), actual.getAuthors(), () -> "AUTHORS mismatch for " + expected.getAccession());
        assertEquals(expected.getLicense(), actual.getLicense(), () -> "LICENSE mismatch for " + expected.getAccession());
        assertEquals(expected.getCopyright(), actual.getCopyright(), () -> "COPYRIGHT mismatch for " + expected.getAccession());
        assertEquals(expected.getPublication(), actual.getPublication(), () -> "PUBLICATION mismatch for " + expected.getAccession());
        assertEquals(expected.getProject(), actual.getProject(), () -> "PROJECT mismatch for " + expected.getAccession());
        assertEquals(new ArrayList<>(expected.getComment()), new ArrayList<>(actual.getComment()), () -> "COMMENT mismatch for " + expected.getAccession());
        assertEquals(new ArrayList<>(expected.getChName()), new ArrayList<>(actual.getChName()), () -> "CH$NAME mismatch for " + expected.getAccession());
        assertEquals(new ArrayList<>(expected.getChCompoundClass()), new ArrayList<>(actual.getChCompoundClass()), () -> "CH$COMPOUND_CLASS mismatch for " + expected.getAccession());
        assertEquals(expected.getChFormula(), actual.getChFormula(), () -> "CH$FORMULA mismatch for " + expected.getAccession());
        assertEquals(0, expected.getChExactMass().compareTo(actual.getChExactMass()), () -> "CH$EXACT_MASS mismatch for " + expected.getAccession());
        assertEquals(expected.getChSMILES(), actual.getChSMILES(), () -> "CH$SMILES mismatch for " + expected.getAccession());
        assertEquals(expected.getChIUPAC(), actual.getChIUPAC(), () -> "CH$IUPAC mismatch for " + expected.getAccession());
        assertEquals(new ArrayList<>(expected.getChLink()), new ArrayList<>(actual.getChLink()), () -> "CH$LINK mismatch for " + expected.getAccession());
        assertEquals(expected.getSpScientificName(), actual.getSpScientificName(), () -> "SP$SCIENTIFIC_NAME mismatch for " + expected.getAccession());
        assertEquals(expected.getSpLineage(), actual.getSpLineage(), () -> "SP$LINEAGE mismatch for " + expected.getAccession());
        assertEquals(new ArrayList<>(expected.getSpLink()), new ArrayList<>(actual.getSpLink()), () -> "SP$LINK mismatch for " + expected.getAccession());
        assertEquals(new ArrayList<>(expected.getSpSample()), new ArrayList<>(actual.getSpSample()), () -> "SP$SAMPLE mismatch for " + expected.getAccession());
        assertEquals(expected.getAcInstrument(), actual.getAcInstrument(), () -> "AC$INSTRUMENT mismatch for " + expected.getAccession());
        assertEquals(expected.getAcInstrumentType(), actual.getAcInstrumentType(), () -> "AC$INSTRUMENT_TYPE mismatch for " + expected.getAccession());
        assertEquals(expected.getAcMassSpectrometryMsType(), actual.getAcMassSpectrometryMsType(), () -> "AC$MASS_SPECTROMETRY: MS_TYPE mismatch for " + expected.getAccession());
        assertEquals(expected.getAcMassSpectrometryIonMode(), actual.getAcMassSpectrometryIonMode(), () -> "AC$MASS_SPECTROMETRY: ION_MODE mismatch for " + expected.getAccession());
        assertEquals(new ArrayList<>(expected.getAcMassSpectrometry()), new ArrayList<>(actual.getAcMassSpectrometry()), () -> "AC$MASS_SPECTROMETRY mismatch for " + expected.getAccession());
        assertEquals(new ArrayList<>(expected.getAcChromatography()), new ArrayList<>(actual.getAcChromatography()), () -> "AC$CHROMATOGRAPHY mismatch for " + expected.getAccession());
        assertEquals(new ArrayList<>(expected.getMsFocusedIon()), new ArrayList<>(actual.getMsFocusedIon()), () -> "MS$FOCUSED_ION mismatch for " + expected.getAccession());
        assertEquals(new ArrayList<>(expected.getMsDataProcessing()), new ArrayList<>(actual.getMsDataProcessing()), () -> "MS$DATA_PROCESSING mismatch for " + expected.getAccession());
        assertEquals(new ArrayList<>(expected.getPkAnnotationHeader()), new ArrayList<>(actual.getPkAnnotationHeader()), () -> "PK$ANNOTATION header mismatch for " + expected.getAccession());
        assertPkAnnotationRowsEqual(expected.getPkAnnotation(), actual.getPkAnnotation(), expected.getAccession());
        assertEquals(new ArrayList<>(expected.getPkPeak()), new ArrayList<>(actual.getPkPeak()), () -> "PK$PEAK mismatch for " + expected.getAccession());
    }

    private static void assertPkAnnotationRowsEqual(List<Record.PeakAnnotationRow> expectedRows, List<Record.PeakAnnotationRow> actualRows, String accession) {
        assertEquals(expectedRows.size(), actualRows.size(), () -> "PK$ANNOTATION row count mismatch for " + accession);
        for (int i = 0; i < expectedRows.size(); i++) {
            final int rowIndex = i;
            Record.PeakAnnotationRow expected = expectedRows.get(i);
            Record.PeakAnnotationRow actual = actualRows.get(i);
            assertEquals(0, expected.getMz().compareTo(actual.getMz()), () -> "PK$ANNOTATION m/z mismatch at row " + rowIndex + " for " + accession);
            assertEquals(new ArrayList<>(expected.getColumns()), new ArrayList<>(actual.getColumns()), () -> "PK$ANNOTATION columns mismatch at row " + rowIndex + " for " + accession);
        }
    }

    private static void assertDeprecatedRecordFieldsEqual(DeprecatedRecord expected, DeprecatedRecord actual) {
        assertEquals(expected.getDeprecated(), actual.getDeprecated(), () -> "DEPRECATED mismatch for " + expected.getAccession());
        assertEquals(expected.getDeprecatedContent(), actual.getDeprecatedContent(), () -> "DEPRECATED_CONTENT mismatch for " + expected.getAccession());
    }

}
