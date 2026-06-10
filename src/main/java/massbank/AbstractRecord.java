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


import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

/**
 * Abstract base class for all MassBank records (standard and deprecated).
 * <p>
 * This class defines common properties of all records and is used as
 * mapped superclass for concrete entity tables.
 *
 * @author rmeier
 * @version 10-06-2026
 */
@MappedSuperclass
public abstract class AbstractRecord {
    @Id
    @Column(name = "accession", nullable = false, length = 105, unique = true)
    protected String accession;

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
