package massbank.db;

import massbank.DeprecatedRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface DeprecatedRecordRepository extends JpaRepository<DeprecatedRecord, String> {
	@Query("select d.accession from DeprecatedRecord d")
	List<String> findAllAccessions();

	@Query("select d.accession from DeprecatedRecord d where d.accession in :accessions")
	List<String> findExistingAccessions(@Param("accessions") Set<String> accessions);
}

