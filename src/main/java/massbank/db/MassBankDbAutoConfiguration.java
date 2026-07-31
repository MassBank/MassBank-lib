/*******************************************************************************
 * Copyright (C) 2025 MassBank consortium
 *
 * This file is part of MassBank.
 *
 * MassBank is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *
 ******************************************************************************/
package massbank.db;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
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

/**
 * Spring Boot autoconfiguration for MassBank database persistence.
 * <p>
 * Registers MassBank entities, repositories and the default {@link RecordService}
 * when a JPA {@link EntityManagerFactory} is available.
 *
 * @author rmeier
 * @version 03-07-2026
 */
@AutoConfiguration(after = HibernateJpaAutoConfiguration.class)
public class MassBankDbAutoConfiguration {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnBean(EntityManagerFactory.class)
    @EnableJpaRepositories(basePackageClasses = {RecordRepository.class, DeprecatedRecordRepository.class, AccessionClaimRepository.class})
    @EntityScan(basePackageClasses = {Record.class, DeprecatedRecord.class, Peak.class, AccessionClaim.class})
    @SuppressWarnings("unused")
    static class MassBankJpaRepositoriesConfiguration {
    }

    @Bean
    @ConditionalOnBean({RecordRepository.class, DeprecatedRecordRepository.class, AccessionClaimRepository.class})
    @ConditionalOnMissingBean(RecordService.class)
    @SuppressWarnings("unused")
    RecordService recordService(RecordRepository recordRepository,
                                DeprecatedRecordRepository deprecatedRecordRepository,
                                AccessionClaimRepository accessionClaimRepository,
                                EntityManager entityManager) {
        return new RecordServiceImplementation(recordRepository, deprecatedRecordRepository, accessionClaimRepository, entityManager);
    }
}


