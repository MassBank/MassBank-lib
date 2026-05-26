package massbank.db;

import massbank.AbstractRecord;

import java.util.List;

public class RecordService {
    private final RecordRepository recordRepository;

    public RecordService(RecordRepository recordRepository) {
        this.recordRepository = recordRepository;
    }

    public AbstractRecord save(AbstractRecord record) {
        return recordRepository.save(record);
    }

    public List<AbstractRecord> findAll() {
        return recordRepository.findAll();
    }

    public AbstractRecord findById(String accession) {
        return recordRepository.findById(accession)
                .orElseThrow(() -> new RuntimeException("Record not found"));
    }
}
