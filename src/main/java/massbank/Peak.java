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
import java.math.RoundingMode;

/**
 * This class represents a single peak in a mass spectrum, containing the measured mass-to-charge ratio (mz),
 * intensity, and relative intensity. The scale (number of decimal places) for mz and intensity is
 * managed internally to ensure scientific accuracy and correct persistence. This class is a JPA entity
 * and is used for database storage and retrieval of peak data. The public API does not expose scale fields;
 * scale is automatically tracked and restored on load.
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
    @Column(nullable = false, precision = 20, scale = 10)
    private BigDecimal mz;
    @Column(nullable = false, precision = 20, scale = 10)
    private BigDecimal intensity;
    @Column(name = "rel_intensity", nullable = false)
    private Integer relIntensity;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "record_id", nullable = false)
    private Record record;
    @Column(name = "mz_scale", nullable = false)
    private Integer mzScale;
    @Column(name = "intensity_scale", nullable = false)
    private Integer intensityScale;

    public Peak() {
    }

    public Peak(BigDecimal mz, BigDecimal intensity, Integer relIntensity) {
        setMz(mz);
        setIntensity(intensity);
        setRelIntensity(relIntensity);
    }

    @PostLoad
    private void restoreScaleAfterLoad() {
        if (mz != null && mzScale != null) {
            mz = mz.setScale(mzScale, RoundingMode.UNNECESSARY);
        }
        if (intensity != null && intensityScale != null) {
            intensity = intensity.setScale(intensityScale, RoundingMode.UNNECESSARY);
        }
    }

    protected Long getId() {
        return id;
    }

    public BigDecimal getMz() {
        return mz;
    }

    public void setMz(BigDecimal mz) {
        this.mz = mz;
        this.mzScale = mz != null ? mz.scale() : 0;
    }

    public BigDecimal getIntensity() {
        return intensity;
    }

    public void setIntensity(BigDecimal intensity) {
        this.intensity = intensity;
        this.intensityScale = intensity != null ? intensity.scale() : 0;
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

    public void setRecord(Record record) {
        this.record = record;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Peak other)) return false;
        if (id != null && other.id != null) {
            return id.equals(other.id);
        }
        return mz != null && mz.equals(other.mz)
                && intensity != null && intensity.equals(other.intensity)
                && relIntensity != null && relIntensity.equals(other.relIntensity);
    }

    @Override
    public int hashCode() {
        if (id != null) return id.hashCode();
        int result = mz != null ? mz.hashCode() : 0;
        result = 31 * result + (intensity != null ? intensity.hashCode() : 0);
        result = 31 * result + (relIntensity != null ? relIntensity.hashCode() : 0);
        return result;
    }

//    @Override
//    public boolean equals(Object o) {
//        if (this == o) return true;
//        if (!(o instanceof Peak)) return false;
//        return id != null && id.equals(((Peak) o).getId());
//    }
//
//    @Override
//    public int hashCode() {
//        return getClass().hashCode();
//    }
}
