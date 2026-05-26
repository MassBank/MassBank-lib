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

/**
 * Abstract base class for all MassBank records (standard and deprecated).
 * <p>
 * This class defines the common properties of all records and is configured
 * for Single Table Inheritance (STI) with a discriminator column for inheritance.
 * The concrete type (standard or deprecated) is implemented by the subclasses
 * Record and DeprecatedRecord.
 *
 * @author rmeier
 * @version 22-15-2026
 */
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "record_type")
@Entity
@Table(name = "massbank-records")
public abstract class AbstractRecord {
    @Id
    @Column(name = "accession", nullable = false, length = 105, unique = true)
    protected String accession;

    public String getAccession() {
        return accession;
    }

    public void setAccession(String accession) {
        this.accession = accession;
    }

}
