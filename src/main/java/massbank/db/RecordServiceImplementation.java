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
import massbank.AbstractRecord;
import massbank.DeprecatedRecord;
import massbank.Record;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Default transactional implementation of {@link RecordService}.
 * <p>
 * Saves active and deprecated records into separate tables while claiming each
 * accession in a shared registry first, ensuring cross-table accession
 * uniqueness even during bulk imports.
 * @author rmeier
 * @version 03-07-2026
 */
@Service
@Transactional
public class RecordServiceImplementation implements RecordService {
    private static final int MAX_SAFE_CHUNK_SIZE = 65535;

    private final RecordRepository recordRepository;
    private final DeprecatedRecordRepository deprecatedRecordRepository;
    private final AccessionClaimRepository accessionClaimRepository;
    private final EntityManager entityManager;

    @Value("${massbank.persistence.chunk-size:2000}")
    private int chunkSize;

    public RecordServiceImplementation(RecordRepository recordRepository,
                                       DeprecatedRecordRepository deprecatedRecordRepository,
                                       AccessionClaimRepository accessionClaimRepository,
                                       EntityManager entityManager) {
        this.recordRepository = recordRepository;
        this.deprecatedRecordRepository = deprecatedRecordRepository;
        this.accessionClaimRepository = accessionClaimRepository;
        this.entityManager = entityManager;
    }

    @Override
    public AbstractRecord save(AbstractRecord record) {
        if (record == null) {
            throw new IllegalArgumentException("record must not be null");
        }
        String accession = record.getAccession();
        if (accession == null || accession.isBlank()) {
            throw new IllegalArgumentException("accession must not be blank");
        }

        if (record instanceof Record typedRecord) {
            if (accessionAlreadyClaimed(accession) && !recordRepository.existsById(accession)) {
                throw new IllegalStateException("Duplicate accession across record tables: " + accession);
            }
            return recordRepository.save(typedRecord);
        }
        if (record instanceof DeprecatedRecord typedDeprecatedRecord) {
            if (accessionAlreadyClaimed(accession) && !deprecatedRecordRepository.existsById(accession)) {
                throw new IllegalStateException("Duplicate accession across record tables: " + accession);
            }
            return deprecatedRecordRepository.save(typedDeprecatedRecord);
        }

        throw new IllegalArgumentException("Unsupported record type: " + record.getClass().getName());
    }

    @Override
    @Transactional
    public List<AbstractRecord> saveAll(List<AbstractRecord> records) {
        if (records == null) {
            throw new IllegalArgumentException("records must not be null");
        }
        if (records.isEmpty()) {
            return List.of();
        }
        int chunk = effectiveChunkSize();
        if (records.size() > chunk) {
            List<AbstractRecord> saved = new ArrayList<>(records.size());
            for (int from = 0; from < records.size(); from += chunk) {
                int to = Math.min(from + chunk, records.size());
                saved.addAll(saveAll(records.subList(from, to)));
            }
            return saved;
        }

        Set<String> accessionsToReserve = new HashSet<>();
        List<Record> activeRecords = new ArrayList<>();
        List<DeprecatedRecord> deprecatedRecords = new ArrayList<>();
        for (int index = 0; index < records.size(); index++) {
            AbstractRecord record = records.get(index);
            if (record == null) {
                throw new IllegalArgumentException("records must not contain null elements (index " + index + ")");
            }
            String accession = record.getAccession();
            if (accession == null || accession.isBlank()) {
                throw new IllegalArgumentException("accession must not be blank");
            }

            if (record instanceof Record typedRecord) {
                activeRecords.add(typedRecord);
            } else if (record instanceof DeprecatedRecord typedDeprecatedRecord) {
                deprecatedRecords.add(typedDeprecatedRecord);
            } else {
                throw new IllegalArgumentException("Unsupported record type: " + record.getClass().getName());
            }
            if (!accessionsToReserve.add(accession)) {
                throw new IllegalStateException("Duplicate accession across record tables: " + accession);
            }
        }
        reserveAccessions(accessionsToReserve);

        // Persist grouped by entity type to maximize statement batch reuse per table.
        saveRecordsInChunksWithEntityManager(activeRecords);
        saveRecordsInChunksWithEntityManager(deprecatedRecords);
        return new ArrayList<>(records);
    }

    @Override
    public void deleteAll() {
        // Keep operation atomic and flush in-between to make DB state transitions explicit.
        deprecatedRecordRepository.deleteAllInBatch();
        deprecatedRecordRepository.flush();
        // deleteAll triggers entity removal and therefore honors cascades to dependent peak rows.
        recordRepository.deleteAll();
        recordRepository.flush();
        accessionClaimRepository.deleteAllInBatch();
        accessionClaimRepository.flush();
    }

    @Override
    @Transactional(readOnly = true)
    public long countAll() {
        return countActive() + countDeprecated();
    }

    @Override
    @Transactional(readOnly = true)
    public long countActive() {
        return recordRepository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public long countDeprecated() {
        return deprecatedRecordRepository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getAllAccessions() {
        List<String> accessions = new ArrayList<>();
        accessions.addAll(recordRepository.findAllAccessions());
        accessions.addAll(deprecatedRecordRepository.findAllAccessions());
        return accessions;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Record> findAllActive() {
        return recordRepository.findAll();
    }


    @Override
    @Transactional(readOnly = true)
    public Record findByIdAsRecord(String accession) {
        return recordRepository.findById(accession)
                .orElseThrow(() -> new RuntimeException("Active record not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Record> findOptionalByIdAsRecord(String accession) {
        return recordRepository.findById(accession);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AbstractRecord> findAll() {
        List<AbstractRecord> all = new ArrayList<>();
        all.addAll(recordRepository.findAll());
        all.addAll(deprecatedRecordRepository.findAll());
        return all;
    }

    @Override
    @Transactional(readOnly = true)
    public AbstractRecord findById(String accession) {
        return recordRepository.findById(accession)
                .map(AbstractRecord.class::cast)
                .or(() -> deprecatedRecordRepository.findById(accession).map(AbstractRecord.class::cast))
                .orElseThrow(() -> new RuntimeException("Record not found"));
    }

    private void reserveAccessions(Set<String> accessions) {
        if (accessions.isEmpty()) {
            return;
        }
        int claimed = accessionClaimRepository.claimAccessions(accessions.toArray(String[]::new));
        if (claimed != accessions.size()) {
            throw new IllegalStateException("Duplicate accession across record tables");
        }
    }

    private boolean accessionAlreadyClaimed(String accession) {
        return accessionClaimRepository.claimAccession(accession) != 1;
    }

    private <T extends AbstractRecord> List<T> saveRecordsInChunksWithEntityManager(List<T> records) {
        if (records.isEmpty()) {
            return List.of();
        }
        List<T> saved = new ArrayList<>(records.size());
        for (int from = 0; from < records.size(); from += effectiveChunkSize()) {
            int to = Math.min(from + effectiveChunkSize(), records.size());
            for (T record : records.subList(from, to)) {
                entityManager.persist(record);
                saved.add(record);
            }
            entityManager.flush();
            entityManager.clear();
        }
        return saved;
    }

    private int effectiveChunkSize() {
        if (chunkSize <= 0) {
            return 1000;
        }
        return Math.min(chunkSize, MAX_SAFE_CHUNK_SIZE);
    }
}
