package massbank.db;

import massbank.AbstractRecord;
import massbank.AccessionRegistry;
import massbank.DeprecatedRecord;
import massbank.Record;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.springframework.beans.factory.annotation.Value;
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
    private static final int MAX_SAFE_CHUNK_SIZE = 65535;

    public record SaveAllMetrics(
            long totalNanos,
            long partitionNanos,
            long activeLookupNanos,
            long deprecatedLookupNanos,
            long reserveAccessionsNanos,
            long persistActiveNanos,
            long persistDeprecatedNanos,
            int activeRecords,
            int deprecatedRecords,
            int reservedAccessions,
            int activeChunks,
            int deprecatedChunks,
            long activeFirstChunkNanos,
            long activeLastChunkNanos,
            long deprecatedFirstChunkNanos,
            long deprecatedLastChunkNanos
    ) {
        static SaveAllMetrics empty() {
            return new SaveAllMetrics(0L, 0L, 0L, 0L, 0L, 0L, 0L,
                    0, 0, 0, 0, 0, 0L, 0L, 0L, 0L);
        }
    }

    private static final class SaveAllMetricsAccumulator {
        private long partitionNanos;
        private long activeLookupNanos;
        private long deprecatedLookupNanos;
        private long reserveAccessionsNanos;
        private long persistActiveNanos;
        private long persistDeprecatedNanos;
        private int activeRecords;
        private int deprecatedRecords;
        private int reservedAccessions;
        private int activeChunks;
        private int deprecatedChunks;
        private long activeFirstChunkNanos;
        private long activeLastChunkNanos;
        private long deprecatedFirstChunkNanos;
        private long deprecatedLastChunkNanos;

        void observeActiveChunk(long nanos) {
            activeChunks++;
            if (activeChunks == 1) {
                activeFirstChunkNanos = nanos;
            }
            activeLastChunkNanos = nanos;
        }

        void observeDeprecatedChunk(long nanos) {
            deprecatedChunks++;
            if (deprecatedChunks == 1) {
                deprecatedFirstChunkNanos = nanos;
            }
            deprecatedLastChunkNanos = nanos;
        }

        SaveAllMetrics toSnapshot(long totalNanos) {
            return new SaveAllMetrics(
                    totalNanos,
                    partitionNanos,
                    activeLookupNanos,
                    deprecatedLookupNanos,
                    reserveAccessionsNanos,
                    persistActiveNanos,
                    persistDeprecatedNanos,
                    activeRecords,
                    deprecatedRecords,
                    reservedAccessions,
                    activeChunks,
                    deprecatedChunks,
                    activeFirstChunkNanos,
                    activeLastChunkNanos,
                    deprecatedFirstChunkNanos,
                    deprecatedLastChunkNanos
            );
        }
    }

    private final RecordRepository recordRepository;
    private final DeprecatedRecordRepository deprecatedRecordRepository;
    private final AccessionRegistryRepository accessionRegistryRepository;
    private final EntityManager entityManager;
    private volatile SaveAllMetrics lastSaveAllMetrics = SaveAllMetrics.empty();

    @Value("${massbank.persistence.chunk-size:2000}")
    private int chunkSize;

    public RecordServiceImplementation(RecordRepository recordRepository,
                                       DeprecatedRecordRepository deprecatedRecordRepository,
                                       AccessionRegistryRepository accessionRegistryRepository,
                                       EntityManager entityManager) {
        this.recordRepository = recordRepository;
        this.deprecatedRecordRepository = deprecatedRecordRepository;
        this.accessionRegistryRepository = accessionRegistryRepository;
        this.entityManager = entityManager;
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
            lastSaveAllMetrics = SaveAllMetrics.empty();
            return List.of();
        }
        SaveAllMetricsAccumulator metrics = new SaveAllMetricsAccumulator();
        long saveAllStarted = System.nanoTime();
        try {
            return saveAllInternal(records, metrics, false);
        } finally {
            lastSaveAllMetrics = metrics.toSnapshot(System.nanoTime() - saveAllStarted);
        }
    }

    @Override
    @Transactional
    public List<AbstractRecord> importAllReplacingData(List<AbstractRecord> records) {
        if (records == null) {
            throw new IllegalArgumentException("records must not be null");
        }
        // Import path initializes tables from scratch.
        deleteAll();
        if (records.isEmpty()) {
            lastSaveAllMetrics = SaveAllMetrics.empty();
            return List.of();
        }
        SaveAllMetricsAccumulator metrics = new SaveAllMetricsAccumulator();
        long saveAllStarted = System.nanoTime();
        try {
            return saveAllInternal(records, metrics, true);
        } finally {
            lastSaveAllMetrics = metrics.toSnapshot(System.nanoTime() - saveAllStarted);
        }
    }

    public SaveAllMetrics getLastSaveAllMetrics() {
        return lastSaveAllMetrics;
    }

    private List<AbstractRecord> saveAllInternal(List<AbstractRecord> records,
                                                 SaveAllMetricsAccumulator metrics,
                                                 boolean importFastPath) {
        int chunk = effectiveChunkSize();
        if (records.size() > chunk) {
            List<AbstractRecord> saved = new ArrayList<>(records.size());
            for (int from = 0; from < records.size(); from += chunk) {
                int to = Math.min(from + chunk, records.size());
                saved.addAll(saveAllInternal(records.subList(from, to), metrics, importFastPath));
            }
            return saved;
        }

        List<Record> activeRecords = new ArrayList<>();
        List<Integer> activeIndexes = new ArrayList<>();
        List<DeprecatedRecord> deprecatedRecords = new ArrayList<>();
        List<Integer> deprecatedIndexes = new ArrayList<>();
        Set<String> activeAccessions = new HashSet<>();
        Set<String> deprecatedAccessions = new HashSet<>();

        long partitionStarted = System.nanoTime();
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
        metrics.partitionNanos += System.nanoTime() - partitionStarted;
        metrics.activeRecords += activeRecords.size();
        metrics.deprecatedRecords += deprecatedRecords.size();

        Set<String> overlaps = new HashSet<>(activeAccessions);
        overlaps.retainAll(deprecatedAccessions);
        if (!overlaps.isEmpty()) {
            throw new IllegalStateException("Duplicate accession across record tables: " + overlaps.iterator().next());
        }

        Set<String> accessionsToReserve = new HashSet<>();
        Set<String> existingActiveAccessions = Set.of();
        Set<String> existingDeprecatedAccessions = Set.of();
        if (importFastPath) {
            accessionsToReserve.addAll(activeAccessions);
            accessionsToReserve.addAll(deprecatedAccessions);
        } else {
            if (!activeAccessions.isEmpty()) {
                long activeLookupStarted = System.nanoTime();
                existingActiveAccessions = new HashSet<>(recordRepository.findExistingAccessions(activeAccessions));
                metrics.activeLookupNanos += System.nanoTime() - activeLookupStarted;
                Set<String> newActiveAccessions = new HashSet<>(activeAccessions);
                newActiveAccessions.removeAll(existingActiveAccessions);
                accessionsToReserve.addAll(newActiveAccessions);
            }
            if (!deprecatedAccessions.isEmpty()) {
                long deprecatedLookupStarted = System.nanoTime();
                existingDeprecatedAccessions = new HashSet<>(deprecatedRecordRepository.findExistingAccessions(deprecatedAccessions));
                metrics.deprecatedLookupNanos += System.nanoTime() - deprecatedLookupStarted;
                Set<String> newDeprecatedAccessions = new HashSet<>(deprecatedAccessions);
                newDeprecatedAccessions.removeAll(existingDeprecatedAccessions);
                accessionsToReserve.addAll(newDeprecatedAccessions);
            }
        }
        reserveAccessions(accessionsToReserve, metrics);

        List<Record> savedActive = saveRecordsInChunksWithEntityManager(activeRecords, existingActiveAccessions, metrics);
        List<DeprecatedRecord> savedDeprecated = saveDeprecatedRecordsInChunksWithEntityManager(deprecatedRecords, existingDeprecatedAccessions, metrics);

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
        reserveAccessions(accessions, null);
    }

    private void reserveAccessions(Set<String> accessions, SaveAllMetricsAccumulator metrics) {
        if (accessions.isEmpty()) {
            return;
        }
        long started = System.nanoTime();
        List<AccessionRegistry> toPersist = accessions.stream()
                .map(AccessionRegistry::new)
                .toList();
        try {
            for (int from = 0; from < toPersist.size(); from += effectiveChunkSize()) {
                int to = Math.min(from + effectiveChunkSize(), toPersist.size());
                List<AccessionRegistry> chunk = toPersist.subList(from, to);
                for (AccessionRegistry accessionRegistry : chunk) {
                    entityManager.persist(accessionRegistry);
                }
                entityManager.flush();
                entityManager.clear();
            }
        } catch (DataIntegrityViolationException | PersistenceException e) {
            throw new IllegalStateException("Duplicate accession across record tables", e);
        } finally {
            if (metrics != null) {
                metrics.reserveAccessionsNanos += System.nanoTime() - started;
                metrics.reservedAccessions += toPersist.size();
            }
        }
    }

    private List<Record> saveRecordsInChunksWithEntityManager(List<Record> records,
                                                               Set<String> existingAccessions,
                                                               SaveAllMetricsAccumulator metrics) {
        if (records.isEmpty()) {
            return List.of();
        }
        long started = System.nanoTime();
        List<Record> saved = new ArrayList<>(records.size());
        for (int from = 0; from < records.size(); from += effectiveChunkSize()) {
            int to = Math.min(from + effectiveChunkSize(), records.size());
            long chunkStarted = System.nanoTime();
            for (Record record : records.subList(from, to)) {
                if (existingAccessions.contains(record.getAccession())) {
                    saved.add(entityManager.merge(record));
                } else {
                    entityManager.persist(record);
                    saved.add(record);
                }
            }
            entityManager.flush();
            entityManager.clear();
            if (metrics != null) {
                metrics.observeActiveChunk(System.nanoTime() - chunkStarted);
            }
        }
        if (metrics != null) {
            metrics.persistActiveNanos += System.nanoTime() - started;
        }
        return saved;
    }

    private List<DeprecatedRecord> saveDeprecatedRecordsInChunksWithEntityManager(List<DeprecatedRecord> records,
                                                                                   Set<String> existingAccessions,
                                                                                   SaveAllMetricsAccumulator metrics) {
        if (records.isEmpty()) {
            return List.of();
        }
        long started = System.nanoTime();
        List<DeprecatedRecord> saved = new ArrayList<>(records.size());
        for (int from = 0; from < records.size(); from += effectiveChunkSize()) {
            int to = Math.min(from + effectiveChunkSize(), records.size());
            long chunkStarted = System.nanoTime();
            for (DeprecatedRecord record : records.subList(from, to)) {
                if (existingAccessions.contains(record.getAccession())) {
                    saved.add(entityManager.merge(record));
                } else {
                    entityManager.persist(record);
                    saved.add(record);
                }
            }
            entityManager.flush();
            entityManager.clear();
            if (metrics != null) {
                metrics.observeDeprecatedChunk(System.nanoTime() - chunkStarted);
            }
        }
        if (metrics != null) {
            metrics.persistDeprecatedNanos += System.nanoTime() - started;
        }
        return saved;
    }

    private int effectiveChunkSize() {
        if (chunkSize <= 0) {
            return 1000;
        }
        return Math.min(chunkSize, MAX_SAFE_CHUNK_SIZE);
    }
}
