package massbank.db;

import org.springframework.data.jpa.repository.JpaRepository;
import massbank.Record;

public interface RecordRepository extends JpaRepository<Record, String> {
}