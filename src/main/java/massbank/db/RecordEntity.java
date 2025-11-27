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

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "records")
public class RecordEntity {
    @Id
    @Column(name = "accession", nullable = false)
    private String accession;

    @Column(name = "is_deprecated")
    private Boolean isDeprecated;

    @Column(name = "deprecated")
    private String deprecated;

    @Column(name = "deprecated_content")
    private String deprecatedContent;

    // JSON/complex fields stored as JSON strings
    @Column(name = "record_title", columnDefinition = "jsonb")
    private String recordTitleJson;

    @Column(name = "date")
    private String date;

    @Column(name = "authors")
    private String authors;

    @Column(name = "license")
    private String license;

    @Column(name = "copyright")
    private String copyright;

    @Column(name = "publication")
    private String publication;

    @Column(name = "project")
    private String project;

    @Column(name = "comment", columnDefinition = "jsonb")
    private String commentJson;

    @Column(name = "ch_name", columnDefinition = "jsonb")
    private String chNameJson;

    @Column(name = "ch_compound_class", columnDefinition = "jsonb")
    private String chCompoundClassJson;

    @Column(name = "ch_formula")
    private String chFormula;

    @Column(name = "ch_exact_mass")
    private BigDecimal chExactMass;

    @Column(name = "ch_smiles")
    private String chSmiles;

    @Column(name = "ch_iupac")
    private String chIupac;

    @Column(name = "ch_link", columnDefinition = "jsonb")
    private String chLinkJson;

    @Column(name = "sp_scientific_name")
    private String spScientificName;

    @Column(name = "sp_lineage")
    private String spLineage;

    @Column(name = "sp_link", columnDefinition = "jsonb")
    private String spLinkJson;

    @Column(name = "sp_sample", columnDefinition = "jsonb")
    private String spSampleJson;

    @Column(name = "ac_instrument")
    private String acInstrument;

    @Column(name = "ac_instrument_type")
    private String acInstrumentType;

    @Column(name = "ac_mass_spectrometry_ms_type")
    private String acMassSpectrometryMsType;

    @Column(name = "ac_mass_spectrometry_ion_mode")
    private String acMassSpectrometryIonMode;

    @Column(name = "ac_mass_spectrometry", columnDefinition = "jsonb")
    private String acMassSpectrometryJson;

    @Column(name = "ac_chromatography", columnDefinition = "jsonb")
    private String acChromatographyJson;

    @Column(name = "ms_focused_ion", columnDefinition = "jsonb")
    private String msFocusedIonJson;

    @Column(name = "ms_data_processing", columnDefinition = "jsonb")
    private String msDataProcessingJson;

    @Column(name = "pk_splash")
    private String pkSplash;

    @Column(name = "pk_annotation_header", columnDefinition = "jsonb")
    private String pkAnnotationHeaderJson;

    @Column(name = "pk_annotation", columnDefinition = "jsonb")
    private String pkAnnotationJson;

    @Column(name = "pk_peak", columnDefinition = "jsonb")
    private String pkPeakJson;

    // standardkonstruktor, getter und setter

    public RecordEntity() {}

    // getters und setters für alle Felder (omitted hier aus Platzgründen)
    // erzeuge diese via IDE / Lombok falls gewünscht
    public String getAccession() { return accession; }
    public void setAccession(String accession) { this.accession = accession; }
    public Boolean getIsDeprecated() { return isDeprecated; }
    public void setIsDeprecated(Boolean isDeprecated) { this.isDeprecated = isDeprecated; }
    public String getDeprecated() { return deprecated; }
    public void setDeprecated(String deprecated) { this.deprecated = deprecated; }
    public String getDeprecatedContent() { return deprecatedContent; }
    public void setDeprecatedContent(String deprecatedContent) { this.deprecatedContent = deprecatedContent; }
    public String getRecordTitleJson() { return recordTitleJson; }
    public void setRecordTitleJson(String recordTitleJson) { this.recordTitleJson = recordTitleJson; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getAuthors() { return authors; }
    public void setAuthors(String authors) { this.authors = authors; }
    public String getLicense() { return license; }
    public void setLicense(String license) { this.license = license; }
    public String getCopyright() { return copyright; }
    public void setCopyright(String copyright) { this.copyright = copyright; }
    public String getPublication() { return publication; }
    public void setPublication(String publication) { this.publication = publication; }
    public String getProject() { return project; }
    public void setProject(String project) { this.project = project; }
    public String getCommentJson() { return commentJson; }
    public void setCommentJson(String commentJson) { this.commentJson = commentJson; }
    public String getChNameJson() { return chNameJson; }
    public void setChNameJson(String chNameJson) { this.chNameJson = chNameJson; }
    public String getChCompoundClassJson() { return chCompoundClassJson; }
    public void setChCompoundClassJson(String chCompoundClassJson) { this.chCompoundClassJson = chCompoundClassJson; }
    public String getChFormula() { return chFormula; }
    public void setChFormula(String chFormula) { this.chFormula = chFormula; }
    public BigDecimal getChExactMass() { return chExactMass; }
    public void setChExactMass(BigDecimal chExactMass) { this.chExactMass = chExactMass; }
    public String getChSmiles() { return chSmiles; }
    public void setChSmiles(String chSmiles) { this.chSmiles = chSmiles; }
    public String getChIupac() { return chIupac; }
    public void setChIupac(String chIupac) { this.chIupac = chIupac; }
    public String getChLinkJson() { return chLinkJson; }
    public void setChLinkJson(String chLinkJson) { this.chLinkJson = chLinkJson; }
    public String getSpScientificName() { return spScientificName; }
    public void setSpScientificName(String spScientificName) { this.spScientificName = spScientificName; }
    public String getSpLineage() { return spLineage; }
    public void setSpLineage(String spLineage) { this.spLineage = spLineage; }
    public String getSpLinkJson() { return spLinkJson; }
    public void setSpLinkJson(String spLinkJson) { this.spLinkJson = spLinkJson; }
    public String getSpSampleJson() { return spSampleJson; }
    public void setSpSampleJson(String spSampleJson) { this.spSampleJson = spSampleJson; }
    public String getAcInstrument() { return acInstrument; }
    public void setAcInstrument(String acInstrument) { this.acInstrument = acInstrument; }
    public String getAcInstrumentType() { return acInstrumentType; }
    public void setAcInstrumentType(String acInstrumentType) { this.acInstrumentType = acInstrumentType; }
    public String getAcMassSpectrometryMsType() { return acMassSpectrometryMsType; }
    public void setAcMassSpectrometryMsType(String acMassSpectrometryMsType) { this.acMassSpectrometryMsType = acMassSpectrometryMsType; }
    public String getAcMassSpectrometryIonMode() { return acMassSpectrometryIonMode; }
    public void setAcMassSpectrometryIonMode(String acMassSpectrometryIonMode) { this.acMassSpectrometryIonMode = acMassSpectrometryIonMode; }
    public String getAcMassSpectrometryJson() { return acMassSpectrometryJson; }
    public void setAcMassSpectrometryJson(String acMassSpectrometryJson) { this.acMassSpectrometryJson = acMassSpectrometryJson; }
    public String getAcChromatographyJson() { return acChromatographyJson; }
    public void setAcChromatographyJson(String acChromatographyJson) { this.acChromatographyJson = acChromatographyJson; }
    public String getMsFocusedIonJson() { return msFocusedIonJson; }
    public void setMsFocusedIonJson(String msFocusedIonJson) { this.msFocusedIonJson = msFocusedIonJson; }
    public String getMsDataProcessingJson() { return msDataProcessingJson; }
    public void setMsDataProcessingJson(String msDataProcessingJson) { this.msDataProcessingJson = msDataProcessingJson; }
    public String getPkSplash() { return pkSplash; }
    public void setPkSplash(String pkSplash) { this.pkSplash = pkSplash; }
    public String getPkAnnotationHeaderJson() { return pkAnnotationHeaderJson; }
    public void setPkAnnotationHeaderJson(String pkAnnotationHeaderJson) { this.pkAnnotationHeaderJson = pkAnnotationHeaderJson; }
    public String getPkAnnotationJson() { return pkAnnotationJson; }
    public void setPkAnnotationJson(String pkAnnotationJson) { this.pkAnnotationJson = pkAnnotationJson; }
    public String getPkPeakJson() { return pkPeakJson; }
    public void setPkPeakJson(String pkPeakJson) { this.pkPeakJson = pkPeakJson; }
}
