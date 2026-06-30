package massbank.db;

import jakarta.persistence.EntityManager;
import massbank.AbstractRecord;
import massbank.AccessionRegistry;
import massbank.DeprecatedRecord;
import massbank.Record;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
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
    private AccessionRegistryRepository accessionRegistryRepository;

    @Mock
    private EntityManager entityManager;

    private RecordServiceImplementation service;

    @BeforeEach
    void setUp() {
        service = new RecordServiceImplementation(recordRepository, deprecatedRecordRepository, accessionRegistryRepository, entityManager);
    }

    @Test
    void saveAll_savesActiveRecords() {
        Record first = createRecord("ACT-001");
        Record second = createRecord("ACT-002");
        List<AbstractRecord> input = List.of(first, second);

        when(recordRepository.findExistingAccessions(any(Set.class))).thenReturn(List.of());

        List<AbstractRecord> saved = service.saveAll(input);

        assertEquals(2, saved.size());
        assertEquals("ACT-001", saved.get(0).getAccession());
        assertEquals("ACT-002", saved.get(1).getAccession());
        verify(entityManager).persist(first);
        verify(entityManager).persist(second);
        verify(entityManager, times(4)).persist(any());
        verify(entityManager, never()).merge(any());
        verify(accessionRegistryRepository, never()).saveAll(any(List.class));
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

        when(deprecatedRecordRepository.findExistingAccessions(any(Set.class))).thenReturn(List.of());

        List<AbstractRecord> saved = service.saveAll(input);

        assertEquals(2, saved.size());
        assertEquals("DEP-001", saved.get(0).getAccession());
        assertEquals("DEP-002", saved.get(1).getAccession());
        verify(entityManager).persist(first);
        verify(entityManager).persist(second);
        verify(entityManager, times(4)).persist(any());
        verify(entityManager, never()).merge(any());
        verify(accessionRegistryRepository, never()).saveAll(any(List.class));
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

        when(deprecatedRecordRepository.findExistingAccessions(any(Set.class))).thenReturn(List.of());
        when(recordRepository.findExistingAccessions(any(Set.class))).thenReturn(List.of());

        List<AbstractRecord> saved = service.saveAll(input);

        assertEquals(3, saved.size());
        assertEquals("MIX-ACT-001", saved.get(0).getAccession());
        assertEquals("MIX-DEP-001", saved.get(1).getAccession());
        assertEquals("MIX-ACT-002", saved.get(2).getAccession());
        verify(entityManager).persist(active);
        verify(entityManager).persist(deprecated);
        verify(entityManager).persist(activeSecond);
        verify(entityManager, times(6)).persist(any());
        verify(entityManager, never()).merge(any());
        verify(recordRepository, never()).saveAll(any(List.class));
        verify(deprecatedRecordRepository, never()).saveAll(any(List.class));
        verify(accessionRegistryRepository, never()).saveAll(any(List.class));

        ArgumentCaptor<Set<String>> activeIdsCaptor = ArgumentCaptor.forClass(Set.class);
        verify(recordRepository).findExistingAccessions(activeIdsCaptor.capture());
        assertTrue(toList(activeIdsCaptor.getValue()).contains("MIX-ACT-001"));
        assertTrue(toList(activeIdsCaptor.getValue()).contains("MIX-ACT-002"));

        ArgumentCaptor<Set<String>> deprecatedIdsCaptor = ArgumentCaptor.forClass(Set.class);
        verify(deprecatedRecordRepository).findExistingAccessions(deprecatedIdsCaptor.capture());
        assertEquals(List.of("MIX-DEP-001"), toList(deprecatedIdsCaptor.getValue()));
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
    void importAllReplacingData_clearsTablesAndSkipsLookups() {
        Record active = createRecord("IMP-ACT-001");
        DeprecatedRecord deprecated = createDeprecatedRecord("IMP-DEP-001");

        List<AbstractRecord> saved = service.importAllReplacingData(List.of(active, deprecated));

        assertEquals(2, saved.size());
        verify(deprecatedRecordRepository).deleteAllInBatch();
        verify(deprecatedRecordRepository).flush();
        verify(recordRepository).deleteAll();
        verify(recordRepository).flush();
        verify(accessionRegistryRepository).deleteAllInBatch();
        verify(accessionRegistryRepository).flush();
        verify(recordRepository, never()).findExistingAccessions(any(Set.class));
        verify(deprecatedRecordRepository, never()).findExistingAccessions(any(Set.class));
        verify(entityManager).persist(active);
        verify(entityManager).persist(deprecated);
    }

    @Test
    void saveAll_mergesExistingRecords() {
        Record active = createRecord("EXISTING-ACT-001");
        DeprecatedRecord deprecated = createDeprecatedRecord("EXISTING-DEP-001");
        Record mergedActive = createRecord("EXISTING-ACT-001");
        DeprecatedRecord mergedDeprecated = createDeprecatedRecord("EXISTING-DEP-001");

        when(recordRepository.findExistingAccessions(any(Set.class))).thenReturn(List.of("EXISTING-ACT-001"));
        when(deprecatedRecordRepository.findExistingAccessions(any(Set.class))).thenReturn(List.of("EXISTING-DEP-001"));
        when(entityManager.merge(active)).thenReturn(mergedActive);
        when(entityManager.merge(deprecated)).thenReturn(mergedDeprecated);

        List<AbstractRecord> saved = service.saveAll(List.of(active, deprecated));

        assertEquals(mergedActive, saved.get(0));
        assertEquals(mergedDeprecated, saved.get(1));
        verify(entityManager).merge(active);
        verify(entityManager).merge(deprecated);
        verify(entityManager, never()).persist(any(AccessionRegistry.class));
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

    private static <T> List<T> toList(Iterable<T> values) {
        if (values instanceof List<T> list) {
            return list;
        }
        List<T> result = new ArrayList<>();
        for (T value : values) {
            result.add(value);
        }
        return result;
    }
}







