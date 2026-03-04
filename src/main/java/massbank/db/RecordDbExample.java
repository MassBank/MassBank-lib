package massbank.db;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import massbank.Record;

import java.math.BigDecimal;
import java.util.List;


@SpringBootApplication
@EntityScan("massbank")
public class RecordDbExample {

    public static void main(String[] args) {
        SpringApplication.run(RecordDbExample.class, args);
    }

    @Bean
    CommandLineRunner demo(RecordRepository repository) {
        return args -> {
            System.out.println("Starting Record DB example...");

            // create a simple Record and save it
            Record r = new Record();
            r.setAccession("EXAMPLE-001");
            r.RECORD_TITLE(List.of("Example Compound", "Demo"));
            r.CH_FORMULA("C2H6O");
            r.CH_EXACT_MASS(new BigDecimal("46.04186"));
            r.CH_SMILES("CCO");

            repository.save(r);
            System.out.println("Saved record: " + r.getAccession());

            // read it back
            Record loaded = repository.findById("EXAMPLE-001").orElseThrow();
            System.out.println("Loaded record: " + loaded.getAccession());
            System.out.println(loaded.toString());
        };
    }
}
