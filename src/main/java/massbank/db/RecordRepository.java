package massbank.db;

import massbank.Record;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface RecordRepository extends JpaRepository<Record, String> {
	@Query("select r.accession from Record r")
	List<String> findAllAccessions();

	@Query("select r.accession from Record r where r.accession in :accessions")
	List<String> findExistingAccessions(@Param("accessions") Set<String> accessions);
}