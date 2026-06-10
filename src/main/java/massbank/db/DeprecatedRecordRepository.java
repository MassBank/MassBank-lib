package massbank.db;

import massbank.DeprecatedRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeprecatedRecordRepository extends JpaRepository<DeprecatedRecord, String> {
}

