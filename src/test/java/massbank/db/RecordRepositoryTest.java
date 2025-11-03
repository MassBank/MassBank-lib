/*******************************************************************************
 * Copyright (C) 2025 MassBank consortium
 *
 * This file is part of MassBank.
 *
 * MassBank is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *
 ******************************************************************************/
package massbank.db;

import massbank.Record;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class RecordRepositoryTest {

    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    private Connection connection;
    private RecordRepository repository;

    @BeforeEach
    void setUp() throws Exception {
        connection = DatabaseConnection.getConnection(
                postgres.getJdbcUrl(),
                postgres.getUsername(),
                postgres.getPassword()
        );
        DatabaseConnection.initializeSchema(connection);
        repository = new RecordRepository(connection);
        
        // Clear any existing data from previous tests
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("TRUNCATE TABLE records");
        }
    }

    @AfterEach
    void tearDown() throws Exception {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    @Test
    void testStoreAndRetrieveSimpleRecord() throws SQLException {
        Record record = createSimpleTestRecord();
        
        repository.store(record);
        Record retrieved = repository.retrieve(record.ACCESSION());
        
        assertNotNull(retrieved);
        assertEquals(record.ACCESSION(), retrieved.ACCESSION());
        assertEquals(record.AUTHORS(), retrieved.AUTHORS());
        assertEquals(record.LICENSE(), retrieved.LICENSE());
        assertEquals(record.CH_FORMULA(), retrieved.CH_FORMULA());
        assertEquals(record.CH_EXACT_MASS(), retrieved.CH_EXACT_MASS());
    }

    @Test
    void testStoreAndRetrieveComplexRecord() throws SQLException {
        Record record = createComplexTestRecord();
        
        repository.store(record);
        Record retrieved = repository.retrieve(record.ACCESSION());
        
        assertNotNull(retrieved);
        assertEquals(record.ACCESSION(), retrieved.ACCESSION());
        assertEquals(record.RECORD_TITLE(), retrieved.RECORD_TITLE());
        assertEquals(record.CH_NAME(), retrieved.CH_NAME());
        assertEquals(record.CH_COMPOUND_CLASS(), retrieved.CH_COMPOUND_CLASS());
        assertEquals(record.CH_LINK(), retrieved.CH_LINK());
        assertEquals(record.COMMENT(), retrieved.COMMENT());
        assertEquals(record.PK_PEAK().size(), retrieved.PK_PEAK().size());
    }

    @Test
    void testUpdateRecord() throws SQLException {
        Record record = createSimpleTestRecord();
        repository.store(record);
        
        record.AUTHORS("Updated Author");
        record.LICENSE("CC BY-SA");
        repository.store(record);
        
        Record retrieved = repository.retrieve(record.ACCESSION());
        assertNotNull(retrieved);
        assertEquals("Updated Author", retrieved.AUTHORS());
        assertEquals("CC BY-SA", retrieved.LICENSE());
    }

    @Test
    void testRetrieveNonExistentRecord() throws SQLException {
        Record retrieved = repository.retrieve("NONEXISTENT");
        assertNull(retrieved);
    }

    @Test
    void testDeleteRecord() throws SQLException {
        Record record = createSimpleTestRecord();
        repository.store(record);
        
        assertTrue(repository.exists(record.ACCESSION()));
        assertTrue(repository.delete(record.ACCESSION()));
        assertFalse(repository.exists(record.ACCESSION()));
    }

    @Test
    void testDeleteNonExistentRecord() throws SQLException {
        assertFalse(repository.delete("NONEXISTENT"));
    }

    @Test
    void testRetrieveAll() throws SQLException {
        Record record1 = createSimpleTestRecord();
        Record record2 = createComplexTestRecord();
        
        repository.store(record1);
        repository.store(record2);
        
        List<Record> allRecords = repository.retrieveAll();
        assertEquals(2, allRecords.size());
    }

    @Test
    void testRetrieveAllEmptyDatabase() throws SQLException {
        List<Record> allRecords = repository.retrieveAll();
        assertTrue(allRecords.isEmpty());
    }

    @Test
    void testExistsMethod() throws SQLException {
        Record record = createSimpleTestRecord();
        
        assertFalse(repository.exists(record.ACCESSION()));
        repository.store(record);
        assertTrue(repository.exists(record.ACCESSION()));
    }

    @Test
    void testStoreDeprecatedRecord() throws SQLException {
        Record record = createSimpleTestRecord();
        record.isDeprecated(true);
        record.DEPRECATED("Replaced by TEST00002");
        record.DEPRECATED_CONTENT("This record has been deprecated.");
        
        repository.store(record);
        Record retrieved = repository.retrieve(record.ACCESSION());
        
        assertNotNull(retrieved);
        assertTrue(retrieved.isDeprecated());
        assertEquals("Replaced by TEST00002", retrieved.DEPRECATED());
        assertEquals("This record has been deprecated.", retrieved.DEPRECATED_CONTENT());
    }

    @Test
    void testStorePeakData() throws SQLException {
        Record record = createSimpleTestRecord();
        
        record.PK_PEAK_ADD_LINE(ImmutableTriple.of(
                new BigDecimal("100.0"), 
                new BigDecimal("5000.0"), 
                100
        ));
        record.PK_PEAK_ADD_LINE(ImmutableTriple.of(
                new BigDecimal("150.5"), 
                new BigDecimal("2500.0"), 
                50
        ));
        
        repository.store(record);
        Record retrieved = repository.retrieve(record.ACCESSION());
        
        assertNotNull(retrieved);
        assertEquals(2, retrieved.PK_PEAK().size());
        assertEquals(new BigDecimal("100.0"), retrieved.PK_PEAK().get(0).getLeft());
        assertEquals(100, retrieved.PK_PEAK().get(0).getRight());
    }

    @Test
    void testStoreAnnotationData() throws SQLException {
        Record record = createSimpleTestRecord();
        
        record.PK_ANNOTATION_HEADER(Arrays.asList("m/z", "formula", "ion"));
        record.PK_ANNOTATION_ADD_LINE(ImmutablePair.of(
                new BigDecimal("100.0"),
                Arrays.asList("C6H12O", "[M+H]+")
        ));
        
        repository.store(record);
        Record retrieved = repository.retrieve(record.ACCESSION());
        
        assertNotNull(retrieved);
        assertEquals(3, retrieved.PK_ANNOTATION_HEADER().size());
        assertEquals(1, retrieved.PK_ANNOTATION().size());
    }

    private Record createSimpleTestRecord() {
        Record record = new Record();
        record.ACCESSION("TEST-00001");
        record.RECORD_TITLE(Arrays.asList("Test Compound", "LC-ESI-QTOF", "MS2"));
        record.DATE("2025.01.01");
        record.AUTHORS("Test Author");
        record.LICENSE("CC BY");
        record.CH_NAME(Arrays.asList("Test Compound"));
        record.CH_FORMULA("C6H12O6");
        record.CH_EXACT_MASS(new BigDecimal("180.063388"));
        record.CH_SMILES("OCC1OC(O)C(O)C(O)C1O");
        record.CH_IUPAC("InChI=1S/C6H12O6/c7-1-2-3(8)4(9)5(10)6(11)12-2/h2-11H,1H2");
        record.AC_INSTRUMENT("Test Instrument");
        record.AC_INSTRUMENT_TYPE("LC-ESI-QTOF");
        record.AC_MASS_SPECTROMETRY_MS_TYPE("MS2");
        record.AC_MASS_SPECTROMETRY_ION_MODE("POSITIVE");
        record.PK_SPLASH("splash10-test-0000000000-test");
        
        return record;
    }

    private Record createComplexTestRecord() {
        Record record = createSimpleTestRecord();
        record.ACCESSION("TEST-00002");
        
        record.COPYRIGHT("Copyright 2025 Test");
        record.PUBLICATION("Test Publication DOI:10.1234/test");
        record.PROJECT("Test Project");
        record.COMMENT(Arrays.asList("Test comment 1", "Test comment 2"));
        
        record.CH_COMPOUND_CLASS(Arrays.asList("Natural Products", "Carbohydrates"));
        
        LinkedHashMap<String, String> chLink = new LinkedHashMap<>();
        chLink.put("CAS", "50-99-7");
        chLink.put("CHEBI", "4167");
        chLink.put("INCHIKEY", "WQZGKKKJIJFFOK-GASJEMHNSA-N");
        record.CH_LINK(chLink);
        
        record.SP_SCIENTIFIC_NAME("Homo sapiens");
        record.SP_LINEAGE("cellular organisms; Eukaryota");
        
        LinkedHashMap<String, String> spLink = new LinkedHashMap<>();
        spLink.put("NCBI", "9606");
        record.SP_LINK(spLink);
        
        record.SP_SAMPLE(Arrays.asList("Blood plasma", "Fasting"));
        
        record.AC_MASS_SPECTROMETRY(Arrays.asList(
                ImmutablePair.of("COLLISION_ENERGY", "20 eV"),
                ImmutablePair.of("FRAGMENTATION_MODE", "CID")
        ));
        
        record.AC_CHROMATOGRAPHY(Arrays.asList(
                ImmutablePair.of("COLUMN_NAME", "Test Column"),
                ImmutablePair.of("FLOW_RATE", "0.2 ml/min")
        ));
        
        record.MS_FOCUSED_ION(Arrays.asList(
                ImmutablePair.of("PRECURSOR_M/Z", "181.071")
        ));
        
        record.MS_DATA_PROCESSING(Arrays.asList(
                ImmutablePair.of("WHOLE", "Test processing")
        ));
        
        record.PK_ANNOTATION_HEADER(Arrays.asList("m/z", "formula"));
        record.PK_ANNOTATION_ADD_LINE(ImmutablePair.of(
                new BigDecimal("100.0"),
                Arrays.asList("C5H8O")
        ));
        
        record.PK_PEAK_ADD_LINE(ImmutableTriple.of(
                new BigDecimal("100.0"), 
                new BigDecimal("10000.0"), 
                100
        ));
        record.PK_PEAK_ADD_LINE(ImmutableTriple.of(
                new BigDecimal("200.0"), 
                new BigDecimal("5000.0"), 
                50
        ));
        
        return record;
    }
}
