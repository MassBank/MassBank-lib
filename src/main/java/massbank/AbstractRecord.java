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
package massbank;

import jakarta.persistence.*;
import massbank.db.AccessionClaim;

/**
 * Abstract base class for all MassBank records.
 * <p>
 * Concrete record entities share the accession primary key and are linked to
 * the accession registry, which enforces uniqueness across active and
 * deprecated record tables.
 *
 * @author rmeier
 * @version 03-07-2026
 */
@MappedSuperclass
public abstract class AbstractRecord {
    @Id
    @Column(name = "accession", nullable = false, length = 105, unique = true)
    protected String accession;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "accession", referencedColumnName = "accession", insertable = false, updatable = false)
    @SuppressWarnings("unused")
    protected AccessionClaim accessionClaim;

    public String getAccession() {
        return accession;
    }
    public void setAccession(String accession) {
        if (accession == null || accession.isBlank()) {
            throw new IllegalStateException("Missing required field: accession");
        }
        this.accession = accession;
    }

    @PrePersist
    @PreUpdate
    protected void validateState() {
        if (accession == null || accession.isBlank()) {
            throw new IllegalStateException("Missing required field: accession");
        }
    }
}
