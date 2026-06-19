package massbank.db;

import massbank.AbstractRecord;
import massbank.Record;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional
public interface RecordService {
    AbstractRecord save(AbstractRecord record);

    List<AbstractRecord> saveAll(List<AbstractRecord> records);

    void deleteAll();

    @Transactional(readOnly = true)
    long countAll();

    @Transactional(readOnly = true)
    long countActive();

    @Transactional(readOnly = true)
    long countDeprecated();

    @Transactional(readOnly = true)
    List<String> getAllAccessions();

    @Transactional(readOnly = true)
    List<Record> findAllActive();

    @Transactional(readOnly = true)
    Record findByIdAsRecord(String accession);

    @Transactional(readOnly = true)
    List<AbstractRecord> findAll();

    @Transactional(readOnly = true)
    AbstractRecord findById(String accession);
}
