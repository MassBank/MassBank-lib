package massbank;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "massbank-accession-registry")
public class AccessionRegistry {

    @Id
    @Column(name = "accession", nullable = false, length = 105, unique = true)
    private String accession;

    protected AccessionRegistry() {
    }

    public AccessionRegistry(String accession) {
        this.accession = accession;
    }

    public String getAccession() {
        return accession;
    }
}

