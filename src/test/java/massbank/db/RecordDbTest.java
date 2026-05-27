package massbank.db;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import massbank.AbstractRecord;
import massbank.DeprecatedRecord;
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
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
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
            AbstractRecord record = res.result().get();
            AbstractRecord loaded = persistAndReload(record);
            assertMappedFieldsEqual(record, loaded);
        }

        assertEquals(recordFiles.size(), repository.count(), "unexpected number of persisted fixture records");
    }

    @Test
    void saveAndLoadManuallyCreatedRecord() {
        Record r = new Record();
        r.setAccession("TEST-001");
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

        Record loaded = (Record) persistAndReload(r);
        assertMappedFieldsEqual(r, loaded);
    }

    private AbstractRecord persistAndReload(AbstractRecord record) {
        repository.saveAndFlush(record);
        entityManager.flush();
        entityManager.clear();

        return repository.findById(record.getAccession())
                .orElseThrow(() -> new AssertionError("record not found: " + record.getAccession()));
    }

    private static void assertMappedFieldsEqual(AbstractRecord expected, AbstractRecord actual) {
        assertNotNull(actual, "loaded record is null");
        assertEquals(expected.getAccession(), actual.getAccession(), () -> "ACCESSION mismatch for " + expected.getAccession());
        if (expected instanceof Record exp && actual instanceof Record act) {
            assertEquals(exp.getRecordTitle(), act.getRecordTitle(), () -> "RECORD_TITLE mismatch for " + expected.getAccession());
            assertEquals(exp.getDate(), act.getDate(), () -> "DATE mismatch for " + expected.getAccession());
            assertEquals(exp.getAuthors(), act.getAuthors(), () -> "AUTHORS mismatch for " + expected.getAccession());
            assertEquals(exp.getLicense(), act.getLicense(), () -> "LICENSE mismatch for " + expected.getAccession());
            assertEquals(exp.getCopyright(), act.getCopyright(), () -> "COPYRIGHT mismatch for " + expected.getAccession());
            assertEquals(exp.getPublication(), act.getPublication(), () -> "PUBLICATION mismatch for " + expected.getAccession());
            assertEquals(exp.getProject(), act.getProject(), () -> "PROJECT mismatch for " + expected.getAccession());
            assertEquals(new ArrayList<>(exp.getComment()), new ArrayList<>(act.getComment()), () -> "COMMENT mismatch for " + expected.getAccession());
            assertEquals(new ArrayList<>(exp.getChName()), new ArrayList<>(act.getChName()), () -> "CH$NAME mismatch for " + expected.getAccession());
            assertEquals(new ArrayList<>(exp.getChCompoundClass()), new ArrayList<>(act.getChCompoundClass()), () -> "CH$COMPOUND_CLASS mismatch for " + expected.getAccession());
            assertEquals(exp.getChFormula(), act.getChFormula(), () -> "CH$FORMULA mismatch for " + expected.getAccession());
            assertEquals(0, exp.getChExactMass().compareTo(act.getChExactMass()), () -> "CH$EXACT_MASS mismatch for " + expected.getAccession());
            assertEquals(exp.getChSMILES(), act.getChSMILES(), () -> "CH$SMILES mismatch for " + expected.getAccession());
            assertEquals(exp.getChIUPAC(), act.getChIUPAC(), () -> "CH$IUPAC mismatch for " + expected.getAccession());
            assertEquals(new ArrayList<>(exp.getChLink()), new ArrayList<>(act.getChLink()), () -> "CH$LINK mismatch for " + expected.getAccession());
            assertEquals(exp.getSpScientificName(), act.getSpScientificName(), () -> "SP$SCIENTIFIC_NAME mismatch for " + expected.getAccession());
            assertEquals(exp.getSpLineage(), act.getSpLineage(), () -> "SP$LINEAGE mismatch for " + expected.getAccession());
            assertEquals(new ArrayList<>(exp.getSpLink()), new ArrayList<>(act.getSpLink()), () -> "SP$LINK mismatch for " + expected.getAccession());
            assertEquals(new ArrayList<>(exp.getSpSample()), new ArrayList<>(act.getSpSample()), () -> "SP$SAMPLE mismatch for " + expected.getAccession());
            assertEquals(exp.getAcInstrument(), act.getAcInstrument(), () -> "AC$INSTRUMENT mismatch for " + expected.getAccession());
            assertEquals(exp.getAcInstrumentType(), act.getAcInstrumentType(), () -> "AC$INSTRUMENT_TYPE mismatch for " + expected.getAccession());
            assertEquals(exp.getAcMassSpectrometryMsType(), act.getAcMassSpectrometryMsType(), () -> "AC$MASS_SPECTROMETRY: MS_TYPE mismatch for " + expected.getAccession());
            assertEquals(exp.getAcMassSpectrometryIonMode(), act.getAcMassSpectrometryIonMode(), () -> "AC$MASS_SPECTROMETRY: ION_MODE mismatch for " + expected.getAccession());
            assertEquals(new ArrayList<>(exp.getAcMassSpectrometry()), new ArrayList<>(act.getAcMassSpectrometry()), () -> "AC$MASS_SPECTROMETRY mismatch for " + expected.getAccession());
            assertEquals(new ArrayList<>(exp.getAcChromatography()), new ArrayList<>(act.getAcChromatography()), () -> "AC$CHROMATOGRAPHY mismatch for " + expected.getAccession());
            assertEquals(new ArrayList<>(exp.getMsFocusedIon()), new ArrayList<>(act.getMsFocusedIon()), () -> "MS$FOCUSED_ION mismatch for " + expected.getAccession());
            assertEquals(new ArrayList<>(exp.getMsDataProcessing()), new ArrayList<>(act.getMsDataProcessing()), () -> "MS$DATA_PROCESSING mismatch for " + expected.getAccession());
        } else if (expected instanceof DeprecatedRecord exp && actual instanceof DeprecatedRecord act) {
            assertEquals(exp.getDeprecated(), act.getDeprecated(), () -> "DEPRECATED mismatch for " + expected.getAccession());
            assertEquals(exp.getDeprecatedContent(), act.getDeprecatedContent(), () -> "DEPRECATED_CONTENT mismatch for " + expected.getAccession());
        } else {
            fail("Record type mismatch: " + expected.getClass() + " vs " + actual.getClass());
        }
    }
}
