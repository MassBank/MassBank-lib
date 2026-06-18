package massbank.db;

import massbank.DeprecatedRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DeprecatedRecordRepository extends JpaRepository<DeprecatedRecord, String> {
	@Query("select d.accession from DeprecatedRecord d")
	List<String> findAllAccessions();
}

