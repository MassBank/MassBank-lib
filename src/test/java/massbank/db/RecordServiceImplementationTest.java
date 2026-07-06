package massbank.db;

import jakarta.persistence.EntityManager;
import massbank.AbstractRecord;
import massbank.DeprecatedRecord;
import massbank.Record;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecordServiceImplementationTest {

    @Mock
    private RecordRepository recordRepository;

    @Mock
    private DeprecatedRecordRepository deprecatedRecordRepository;

    @Mock
    private AccessionClaimRepository accessionClaimRepository;

    @Mock
    private EntityManager entityManager;

    private RecordServiceImplementation service;

    @BeforeEach
    void setUp() {
        service = new RecordServiceImplementation(recordRepository, deprecatedRecordRepository, accessionClaimRepository, entityManager);
    }

    @Test
    void saveAll_savesActiveRecords() {
        Record first = createRecord("ACT-001");
        Record second = createRecord("ACT-002");
        List<AbstractRecord> input = List.of(first, second);

        when(accessionClaimRepository.claimAccessions(any(String[].class))).thenReturn(2);

        List<AbstractRecord> saved = service.saveAll(input);

        assertEquals(2, saved.size());
        assertEquals("ACT-001", saved.get(0).getAccession());
        assertEquals("ACT-002", saved.get(1).getAccession());
        verify(entityManager).persist(first);
        verify(entityManager).persist(second);
        verify(entityManager, times(2)).persist(any());
        verify(entityManager, never()).merge(any());
        verify(accessionClaimRepository).claimAccessions(any(String[].class));
        verify(recordRepository, never()).saveAll(any(List.class));
        verify(recordRepository, never()).save(any());
        verify(deprecatedRecordRepository, never()).saveAll(any(List.class));
        verify(deprecatedRecordRepository, never()).save(any());
    }

    @Test
    void saveAll_savesDeprecatedRecords() {
        DeprecatedRecord first = createDeprecatedRecord("DEP-001");
        DeprecatedRecord second = createDeprecatedRecord("DEP-002");
        List<AbstractRecord> input = List.of(first, second);

        when(accessionClaimRepository.claimAccessions(any(String[].class))).thenReturn(2);

        List<AbstractRecord> saved = service.saveAll(input);

        assertEquals(2, saved.size());
        assertEquals("DEP-001", saved.get(0).getAccession());
        assertEquals("DEP-002", saved.get(1).getAccession());
        verify(entityManager).persist(first);
        verify(entityManager).persist(second);
        verify(entityManager, times(2)).persist(any());
        verify(entityManager, never()).merge(any());
        verify(accessionClaimRepository).claimAccessions(any(String[].class));
        verify(deprecatedRecordRepository, never()).saveAll(any(List.class));
        verify(deprecatedRecordRepository, never()).save(any());
        verify(recordRepository, never()).saveAll(any(List.class));
        verify(recordRepository, never()).save(any());
    }

    @Test
    void saveAll_savesMixedRecords() {
        Record active = createRecord("MIX-ACT-001");
        DeprecatedRecord deprecated = createDeprecatedRecord("MIX-DEP-001");
        Record activeSecond = createRecord("MIX-ACT-002");
        List<AbstractRecord> input = List.of(active, deprecated, activeSecond);

        when(accessionClaimRepository.claimAccessions(any(String[].class))).thenReturn(3);

        List<AbstractRecord> saved = service.saveAll(input);

        assertEquals(3, saved.size());
        assertEquals("MIX-ACT-001", saved.get(0).getAccession());
        assertEquals("MIX-DEP-001", saved.get(1).getAccession());
        assertEquals("MIX-ACT-002", saved.get(2).getAccession());
        verify(entityManager).persist(active);
        verify(entityManager).persist(deprecated);
        verify(entityManager).persist(activeSecond);
        verify(entityManager, times(3)).persist(any());
        verify(entityManager, never()).merge(any());
        verify(recordRepository, never()).saveAll(any(List.class));
        verify(deprecatedRecordRepository, never()).saveAll(any(List.class));
        verify(accessionClaimRepository).claimAccessions(any(String[].class));
    }

    @Test
    void saveAll_emptyList_returnsEmptyList() {
        List<AbstractRecord> saved = service.saveAll(List.of());
        assertTrue(saved.isEmpty());
        verify(recordRepository, never()).saveAll(any(List.class));
        verify(deprecatedRecordRepository, never()).saveAll(any(List.class));
        verify(entityManager, never()).persist(any());
        verify(entityManager, never()).merge(any());
    }


    @Test
    void saveAll_duplicateClaim_throwsIllegalStateException() {
        Record active = createRecord("EXISTING-ACT-001");
        DeprecatedRecord deprecated = createDeprecatedRecord("EXISTING-DEP-001");
        when(accessionClaimRepository.claimAccessions(any(String[].class))).thenReturn(1);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> service.saveAll(List.of(active, deprecated)));

        assertTrue(exception.getMessage().contains("Duplicate accession across record tables"));
        verify(entityManager, never()).merge(any());
    }

    @Test
    void save_conflictingClaimWithoutExistingActiveRecord_throwsIllegalStateException() {
        Record active = createRecord("CONFLICT-ACT-001");
        when(accessionClaimRepository.claimAccession("CONFLICT-ACT-001")).thenReturn(0);
        when(recordRepository.existsById("CONFLICT-ACT-001")).thenReturn(false);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> service.save(active));

        assertTrue(exception.getMessage().contains("Duplicate accession across record tables"));
        verify(recordRepository, never()).save(any());
    }


    @Test
    void saveAll_nullInput_throwsIllegalArgumentException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> service.saveAll(null));
        assertEquals("records must not be null", exception.getMessage());
    }

    @Test
    void saveAll_keepsTransactionalBehavior() {
        Transactional transactional = RecordServiceImplementation.class.getAnnotation(Transactional.class);
        assertTrue(transactional != null);
        assertTrue(transactional.readOnly() == false);
    }

    private static Record createRecord(String accession) {
        Record record = new Record();
        record.setAccession(accession);
        return record;
    }

    private static DeprecatedRecord createDeprecatedRecord(String accession) {
        DeprecatedRecord record = new DeprecatedRecord();
        record.setAccession(accession);
        record.setDeprecated("Deprecated");
        record.setDeprecatedContent("content");
        return record;
    }

}







