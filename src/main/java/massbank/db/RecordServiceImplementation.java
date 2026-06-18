package massbank.db;

import massbank.AbstractRecord;
import massbank.DeprecatedRecord;
import massbank.Record;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class RecordServiceImplementation implements RecordService {
    private final RecordRepository recordRepository;
    private final DeprecatedRecordRepository deprecatedRecordRepository;

    public RecordServiceImplementation(RecordRepository recordRepository, DeprecatedRecordRepository deprecatedRecordRepository) {
        this.recordRepository = recordRepository;
        this.deprecatedRecordRepository = deprecatedRecordRepository;
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
            ensureGlobalAccessionUniqueness(accession, false);
            return recordRepository.save(typedRecord);
        }
        if (record instanceof DeprecatedRecord typedDeprecatedRecord) {
            ensureGlobalAccessionUniqueness(accession, true);
            return deprecatedRecordRepository.save(typedDeprecatedRecord);
        }

        throw new IllegalArgumentException("Unsupported record type: " + record.getClass().getName());
    }

    @Override
    public void deleteAll() {
        // Keep operation atomic and flush in-between to make DB state transitions explicit.
        deprecatedRecordRepository.deleteAllInBatch();
        deprecatedRecordRepository.flush();
        // deleteAll triggers entity removal and therefore honors cascades to dependent peak rows.
        recordRepository.deleteAll();
        recordRepository.flush();
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

    private void ensureGlobalAccessionUniqueness(String accession, boolean savingDeprecatedRecord) {
        if (savingDeprecatedRecord) {
            if (recordRepository.existsById(accession)) {
                throw new IllegalStateException("Duplicate accession across record tables: " + accession);
            }
            return;
        }
        if (deprecatedRecordRepository.existsById(accession)) {
            throw new IllegalStateException("Duplicate accession across record tables: " + accession);
        }
    }
}

