package massbank.db;

import massbank.AccessionRegistry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccessionRegistryRepository extends JpaRepository<AccessionRegistry, String> {
}

