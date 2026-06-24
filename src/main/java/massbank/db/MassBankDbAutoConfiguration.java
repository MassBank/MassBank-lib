package massbank.db;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import massbank.AccessionRegistry;
import massbank.DeprecatedRecord;
import massbank.Peak;
import massbank.Record;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@AutoConfiguration(after = HibernateJpaAutoConfiguration.class)
public class MassBankDbAutoConfiguration {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnBean(EntityManagerFactory.class)
    @EnableJpaRepositories(basePackageClasses = {RecordRepository.class, DeprecatedRecordRepository.class, AccessionRegistryRepository.class})
    @EntityScan(basePackageClasses = {Record.class, DeprecatedRecord.class, Peak.class, AccessionRegistry.class})
    static class MassBankJpaRepositoriesConfiguration {
    }

    @Bean
    @ConditionalOnBean({RecordRepository.class, DeprecatedRecordRepository.class, AccessionRegistryRepository.class})
    @ConditionalOnMissingBean(RecordService.class)
    RecordService recordService(RecordRepository recordRepository,
                                DeprecatedRecordRepository deprecatedRecordRepository,
                                 AccessionRegistryRepository accessionRegistryRepository,
                                 EntityManager entityManager) {
        return new RecordServiceImplementation(recordRepository, deprecatedRecordRepository, accessionRegistryRepository, entityManager);
    }
}


