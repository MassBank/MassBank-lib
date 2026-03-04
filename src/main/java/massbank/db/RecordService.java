package massbank.db;

import massbank.Record;

import java.util.List;

public class RecordService {
    private final RecordRepository recordRepository;

    public RecordService(RecordRepository recordRepository) {
        this.recordRepository = recordRepository;
    }

    public Record save(Record record) {
        return recordRepository.save(record);
    }

    public List<Record> findAll() {
        return recordRepository.findAll();
    }

    public Record findById(String accession) {
        return recordRepository.findById(accession)
                .orElseThrow(() -> new RuntimeException("Record not found"));
    }
}
