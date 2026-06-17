package massbank.db;

import massbank.AbstractRecord;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional
public interface RecordService {
    AbstractRecord save(AbstractRecord record);

    @Transactional(readOnly = true)
    List<AbstractRecord> findAll();

    @Transactional(readOnly = true)
    AbstractRecord findById(String accession);
}
