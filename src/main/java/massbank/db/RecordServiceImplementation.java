package massbank.db;

import massbank.AbstractRecord;
import massbank.AccessionRegistry;
import massbank.DeprecatedRecord;
import massbank.Record;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class RecordServiceImplementation implements RecordService {
    private final RecordRepository recordRepository;
    private final DeprecatedRecordRepository deprecatedRecordRepository;
    private final AccessionRegistryRepository accessionRegistryRepository;

    public RecordServiceImplementation(RecordRepository recordRepository,
                                       DeprecatedRecordRepository deprecatedRecordRepository,
                                       AccessionRegistryRepository accessionRegistryRepository) {
        this.recordRepository = recordRepository;
        this.deprecatedRecordRepository = deprecatedRecordRepository;
        this.accessionRegistryRepository = accessionRegistryRepository;
    }

    @Override
    public AbstractRecord save(AbstractRecord record) {
        if (record == null) {
            throw new IllegalArgumentException("record must not be null");
        }
        String accession = record.getAccession();
        if (accession == null || accession.isBlank()) {
            throw new IllegalArgumentException("accession must not be blank");
        }

        if (record instanceof Record typedRecord) {
            if (!recordRepository.existsById(accession)) {
                reserveAccessions(Set.of(accession));
            }
            return recordRepository.save(typedRecord);
        }
        if (record instanceof DeprecatedRecord typedDeprecatedRecord) {
            if (!deprecatedRecordRepository.existsById(accession)) {
                reserveAccessions(Set.of(accession));
            }
            return deprecatedRecordRepository.save(typedDeprecatedRecord);
        }

        throw new IllegalArgumentException("Unsupported record type: " + record.getClass().getName());
    }

    @Override
    @Transactional
    public List<AbstractRecord> saveAll(List<AbstractRecord> records) {
        if (records == null) {
            throw new IllegalArgumentException("records must not be null");
        }
        if (records.isEmpty()) {
            return List.of();
        }

        List<Record> activeRecords = new ArrayList<>();
        List<Integer> activeIndexes = new ArrayList<>();
        List<DeprecatedRecord> deprecatedRecords = new ArrayList<>();
        List<Integer> deprecatedIndexes = new ArrayList<>();
        Set<String> activeAccessions = new HashSet<>();
        Set<String> deprecatedAccessions = new HashSet<>();

        for (int index = 0; index < records.size(); index++) {
            AbstractRecord record = records.get(index);
            if (record == null) {
                throw new IllegalArgumentException("records must not contain null elements (index " + index + ")");
            }
            String accession = record.getAccession();
            if (accession == null || accession.isBlank()) {
                throw new IllegalArgumentException("accession must not be blank");
            }

            if (record instanceof Record typedRecord) {
                activeRecords.add(typedRecord);
                activeIndexes.add(index);
                activeAccessions.add(accession);
                continue;
            }
            if (record instanceof DeprecatedRecord typedDeprecatedRecord) {
                deprecatedRecords.add(typedDeprecatedRecord);
                deprecatedIndexes.add(index);
                deprecatedAccessions.add(accession);
                continue;
            }
            throw new IllegalArgumentException("Unsupported record type: " + record.getClass().getName());
        }

        Set<String> overlaps = new HashSet<>(activeAccessions);
        overlaps.retainAll(deprecatedAccessions);
        if (!overlaps.isEmpty()) {
            throw new IllegalStateException("Duplicate accession across record tables: " + overlaps.iterator().next());
        }

        Set<String> accessionsToReserve = new HashSet<>();
        if (!activeAccessions.isEmpty()) {
            Set<String> existingActiveAccessions = new HashSet<>(recordRepository.findExistingAccessions(activeAccessions));
            Set<String> newActiveAccessions = new HashSet<>(activeAccessions);
            newActiveAccessions.removeAll(existingActiveAccessions);
            accessionsToReserve.addAll(newActiveAccessions);
        }
        if (!deprecatedAccessions.isEmpty()) {
            Set<String> existingDeprecatedAccessions = new HashSet<>(deprecatedRecordRepository.findExistingAccessions(deprecatedAccessions));
            Set<String> newDeprecatedAccessions = new HashSet<>(deprecatedAccessions);
            newDeprecatedAccessions.removeAll(existingDeprecatedAccessions);
            accessionsToReserve.addAll(newDeprecatedAccessions);
        }
        reserveAccessions(accessionsToReserve);

        List<Record> savedActive = activeRecords.isEmpty() ? List.of() : recordRepository.saveAll(activeRecords);
        List<DeprecatedRecord> savedDeprecated = deprecatedRecords.isEmpty() ? List.of() : deprecatedRecordRepository.saveAll(deprecatedRecords);

        if (savedActive.size() != activeRecords.size()) {
            throw new IllegalStateException("Active saveAll result size mismatch");
        }
        if (savedDeprecated.size() != deprecatedRecords.size()) {
            throw new IllegalStateException("Deprecated saveAll result size mismatch");
        }

        List<AbstractRecord> result = new ArrayList<>(records);
        for (int i = 0; i < savedActive.size(); i++) {
            result.set(activeIndexes.get(i), savedActive.get(i));
        }
        for (int i = 0; i < savedDeprecated.size(); i++) {
            result.set(deprecatedIndexes.get(i), savedDeprecated.get(i));
        }
        return result;
    }

    @Override
    public void deleteAll() {
        // Keep operation atomic and flush in-between to make DB state transitions explicit.
        deprecatedRecordRepository.deleteAllInBatch();
        deprecatedRecordRepository.flush();
        // deleteAll triggers entity removal and therefore honors cascades to dependent peak rows.
        recordRepository.deleteAll();
        recordRepository.flush();
        accessionRegistryRepository.deleteAllInBatch();
        accessionRegistryRepository.flush();
    }

    @Override
    @Transactional(readOnly = true)
    public long countAll() {
        return countActive() + countDeprecated();
    }

    @Override
    @Transactional(readOnly = true)
    public long countActive() {
        return recordRepository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public long countDeprecated() {
        return deprecatedRecordRepository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getAllAccessions() {
        List<String> accessions = new ArrayList<>();
        accessions.addAll(recordRepository.findAllAccessions());
        accessions.addAll(deprecatedRecordRepository.findAllAccessions());
        return accessions;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Record> findAllActive() {
        return recordRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Record findByIdAsRecord(String accession) {
        return recordRepository.findById(accession)
                .orElseThrow(() -> new RuntimeException("Active record not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AbstractRecord> findAll() {
        List<AbstractRecord> all = new ArrayList<>();
        all.addAll(recordRepository.findAll());
        all.addAll(deprecatedRecordRepository.findAll());
        return all;
    }

    @Override
    @Transactional(readOnly = true)
    public AbstractRecord findById(String accession) {
        return recordRepository.findById(accession)
                .map(AbstractRecord.class::cast)
                .or(() -> deprecatedRecordRepository.findById(accession).map(AbstractRecord.class::cast))
                .orElseThrow(() -> new RuntimeException("Record not found"));
    }

    private void reserveAccessions(Set<String> accessions) {
        if (accessions.isEmpty()) {
            return;
        }
        List<AccessionRegistry> toPersist = accessions.stream()
                .map(AccessionRegistry::new)
                .toList();
        try {
            accessionRegistryRepository.saveAll(toPersist);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException("Duplicate accession across record tables", e);
        }
    }
}

