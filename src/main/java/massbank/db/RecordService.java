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

import massbank.AbstractRecord;
import massbank.Record;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Transactional service facade for MassBank records.
 * <p>
 * Exposes read/write operations across both active {@link Record} instances
 * and deprecated records while keeping callers independent of the underlying
 * split-table persistence model.
 * @author rmeier
 * @version 03-07-2026
 */
@Transactional
public interface RecordService {
    AbstractRecord save(AbstractRecord record);

    /**
     * Inserts records with new accessions. The call joins an existing transaction
     * or starts one if needed. All internal chunks share that transaction and
     * commit or roll back together.
     * <p>
     * During batch persistence, the service flushes pending changes and clears
     * the entire persistence context. This detaches previously managed entities,
     * including those loaded by the caller. Later changes to detached entities
     * are no longer tracked automatically.
     */
    List<AbstractRecord> saveAll(List<AbstractRecord> records);

    /**
     * Creates the PostgreSQL peak lookup index and refreshes table statistics.
     * Call once after all import batches have committed, with no concurrent writers.
     * Existing indexes are retained on subsequent imports.
     */
    void ensurePeakIndex();

    void resetAll();

    /**
     * Deletes all MassBank records, peaks and accession claims in bulk.
     * Flushes pending changes and clears the persistence context before deleting.
     */
    void deleteAll();

    @Transactional(readOnly = true)
    long countAll();

    @Transactional(readOnly = true)
    long countActive();

    @Transactional(readOnly = true)
    long countDeprecated();

    @Transactional(readOnly = true)
    List<String> getAllAccessions();

    @Transactional(readOnly = true)
    List<Record> findAllActive();

    /** Returns an active record whose peaks and metadata are available after the transaction. */
    @Transactional(readOnly = true)
    Record findByIdAsRecord(String accession);

    /** Optional variant of {@link #findByIdAsRecord(String)} with the same loading guarantees. */
    @Transactional(readOnly = true)
    Optional<Record> findOptionalByIdAsRecord(String accession);

    @Transactional(readOnly = true)
    List<AbstractRecord> findAll();

    @Transactional(readOnly = true)
    AbstractRecord findById(String accession);
}
