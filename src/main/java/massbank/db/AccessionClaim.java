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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Registry entity for all claimed MassBank accessions.
 * <p>
 * Active and deprecated records reference this table to enforce accession
 * uniqueness across both record tables.
 *
 * @author rmeier
 * @version 03-07-2026
 */
@Entity
@Table(name = "massbank-accession-registry")
public class AccessionClaim {

    @Id
    @Column(name = "accession", nullable = false, length = 105, unique = true)
    @SuppressWarnings("unused")
    private String accession;

    protected AccessionClaim() {
    }

    public String getAccession() {
        return accession;
    }
}
