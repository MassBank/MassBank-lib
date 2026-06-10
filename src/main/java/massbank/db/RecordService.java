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
public class RecordService {
    private final RecordRepository recordRepository;
    private final DeprecatedRecordRepository deprecatedRecordRepository;

    public RecordService(RecordRepository recordRepository, DeprecatedRecordRepository deprecatedRecordRepository) {
        this.recordRepository = recordRepository;
        this.deprecatedRecordRepository = deprecatedRecordRepository;
    }

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

    @Transactional(readOnly = true)
    public List<AbstractRecord> findAll() {
        List<AbstractRecord> all = new ArrayList<>();
        all.addAll(recordRepository.findAll());
        all.addAll(deprecatedRecordRepository.findAll());
        return all;
    }

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
