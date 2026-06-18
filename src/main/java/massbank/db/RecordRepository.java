package massbank.db;

import massbank.Record;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RecordRepository extends JpaRepository<Record, String> {
	@Query("select r.accession from Record r")
	List<String> findAllAccessions();
}