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

import java.math.BigDecimal;

/**
 * This class represents a single peak in a mass spectrum, containing the measured mass-to-charge ratio (mz),
 * intensity, and relative intensity. This class is a JPA entity
 * and is used for database storage and retrieval of peak data.
 *
 * @author rmeier
 * @version 28-05-2026
 */
@Entity
@Table(name = "massbank-peaks")
public class Peak {
    @Id
    @GeneratedValue
    private Long id;
    @Column(nullable = false, columnDefinition = "numeric")
    private BigDecimal mz;
    @Column(nullable = false, columnDefinition = "numeric")
    private BigDecimal intensity;
    @Column(name = "rel_intensity", nullable = false)
    private Integer relIntensity;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "record_id", nullable = false)
    private Record record;
    public Peak() {
    }

    public Peak(BigDecimal mz, BigDecimal intensity, Integer relIntensity) {
        setMz(mz);
        setIntensity(intensity);
        setRelIntensity(relIntensity);
    }

    protected Long getId() {
        return id;
    }

    public BigDecimal getMz() {
        return mz;
    }

    public void setMz(BigDecimal mz) {
        this.mz = mz;
    }

    public BigDecimal getIntensity() {
        return intensity;
    }

    public void setIntensity(BigDecimal intensity) {
        this.intensity = intensity;
    }

    public Integer getRelIntensity() {
        return relIntensity;
    }

    public void setRelIntensity(Integer relIntensity) {
        this.relIntensity = relIntensity;
    }

    public Record getRecord() {
        return record;
    }

    void setRecord(Record record) {
        if (this.record != null && record != null && this.record != record) {
            throw new IllegalStateException("peak must be detached via setRecord(null) before assigning a different record");
        }
        this.record = record;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Peak other)) return false;
        Long peakId = getId();
        return peakId != null && peakId.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return Peak.class.hashCode();
    }

}
