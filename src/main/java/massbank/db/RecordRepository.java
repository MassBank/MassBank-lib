package massbank.db;

import org.springframework.data.jpa.repository.JpaRepository;
import massbank.AbstractRecord;

public interface RecordRepository extends JpaRepository<AbstractRecord, String> {
}