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

    List<AbstractRecord> saveAll(List<AbstractRecord> records);

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

    @Transactional(readOnly = true)
    Record findByIdAsRecord(String accession);

    @Transactional(readOnly = true)
    List<AbstractRecord> findAll();

    @Transactional(readOnly = true)
    AbstractRecord findById(String accession);
}
