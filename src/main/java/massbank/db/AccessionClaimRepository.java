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

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repository for claiming accessions in the shared accession registry.
 * <p>
 * Native PostgreSQL upserts are used so single and bulk imports can reserve
 * accessions atomically before persisting active or deprecated records.
 *
 * @author rmeier
 * @version 03-07-2026
 */
public interface AccessionClaimRepository extends JpaRepository<AccessionClaim, String> {
	/**
	 * Attempts to reserve one accession in the shared claim table.
	 *
	 * @param accession accession identifier to reserve
	 * @return {@code 1} if a new claim row was inserted, {@code 0} if the
	 * accession was already claimed
	 */
	@Modifying
	@Query(value = "INSERT INTO \"massbank-accession-registry\" (accession) VALUES (:accession) ON CONFLICT (accession) DO NOTHING", nativeQuery = true)
	int claimAccession(@Param("accession") String accession);

	/**
	 * Attempts to reserve multiple accessions in one statement.
	 *
	 * @param accessions accession identifiers to reserve
	 * @return number of newly inserted claim rows (between {@code 0} and
	 * {@code accessions.length})
	 */
	@Modifying
	@Query(value = "INSERT INTO \"massbank-accession-registry\" (accession) SELECT UNNEST(CAST(:accessions AS text[])) ON CONFLICT (accession) DO NOTHING", nativeQuery = true)
	int claimAccessions(@Param("accessions") String[] accessions);
}

