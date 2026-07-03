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

import massbank.DeprecatedRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;


/**
 * Spring Data repository for deprecated MassBank record entities.
 * <p>
 * Mirrors active-record accession queries so service-layer operations can
 * combine active and deprecated records consistently.
 *
 * @author rmeier
 * @version 03-07-2026
 */
public interface DeprecatedRecordRepository extends JpaRepository<DeprecatedRecord, String> {
	@Query("select d.accession from DeprecatedRecord d")
	List<String> findAllAccessions();
}

