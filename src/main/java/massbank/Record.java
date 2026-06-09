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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.dan2097.jnainchi.InchiStatus;
import jakarta.persistence.*;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.inchi.InChIGeneratorFactory;
import org.openscience.cdk.inchi.InChIToStructure;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class keeps all data of a record.
 *
 * @author rmeier
 * @version 01-12-2022
 */

@Entity
@DiscriminatorValue("RECORD")
public class Record extends AbstractRecord {
	private static final Logger logger = LogManager.getLogger(Record.class);

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "record_title", columnDefinition = "jsonb")
	private List<String> recordTitle;

	@Column(name = "date", length = 100)
	private String date;

	@Column(name = "authors", length = 512)
	private String authors;

	@Column(name = "license", length = 64)
	private String license;

	@Column(name = "copyright", length = 2048)
	private String copyright; // optional

	@Column(name = "publication", length = 2048)
	private String publication; // optional

	@Column(name = "project", length = 512)
	private String project; // optional

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "comment", columnDefinition = "jsonb")
	private List<String> comment; // optional

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "ch_name", columnDefinition = "jsonb")
	private List<String> chName;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "ch_compound_class", columnDefinition = "jsonb")
	private List<String> chCompoundClass; // optional

	@Column(name = "ch_formula", length = 512)
	private String chFormula;

	@Column(name = "ch_exact_mass", precision = 20, scale = 10)
	private BigDecimal exactMass;

	@Column(name = "ch_smiles", length = 2048)
	private String chSMILES;

	@Column(name = "ch_iupac", length = 2048)
	private String chIUPAC;

	@ElementCollection
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "ch_link", columnDefinition = "jsonb")
	private List<KeyValue> chLink; // optional

	@Column(name = "sp_scientific_name", length = 512)
	private String spScientificName; // optional

	@Column(name = "sp_lineage", length = 2048)
	private String spLineage; // optional

	@ElementCollection
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "sp_link", columnDefinition = "jsonb")
	private List<KeyValue> spLink; // optional

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "sp_sample", columnDefinition = "jsonb")
	private List<String> spSample; // optional

	@Column(name = "ac_instrument", length = 2048)
	private String acInstrument;

	@Column(name = "ac_instrument_type", length = 512)
	private String acInstrumentType;

	@Column(name = "ac_mass_spectrometry_ms_type", length = 32)
	private String acMassSpectrometryMsType;

	@Column(name = "ac_mass_spectrometry_ion_mode", length = 32)
	private String acMassSpectrometryIonMode;

	@ElementCollection
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "ac_mass_spectrometry", columnDefinition = "jsonb")
	private List<KeyValue> acMassSpectrometry; // optional

	@ElementCollection
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "ac_chromatography", columnDefinition = "jsonb")
	private List<KeyValue> acChromatography; // optional

	@ElementCollection
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "ms_focused_ion", columnDefinition = "jsonb")
	private List<KeyValue> msFocusedIon; // optional

	@ElementCollection
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "ms_data_processing", columnDefinition = "jsonb")
	private List<KeyValue> msDataProcessing; // optional

	@Column(name = "pk_splash", length = 128)
	private String pkSplash;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "pk_annotation", columnDefinition = "jsonb")
	private PeakAnnotationTable pkAnnotationTable = new PeakAnnotationTable();

	@OneToMany(mappedBy = "record", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("mz ASC")
	private List<Peak> pkPeak = new ArrayList<>();


	public Record() {
		super();
		recordTitle = new ArrayList<>();
		date = "";
		authors = "";
		license = "";
		copyright = null; // optional
		publication = null; // optional
		project = null; // optional
		comment = new ArrayList<>(); // optional
		chName = new ArrayList<>();
		chCompoundClass = new ArrayList<>();
		chFormula = "";
		exactMass = new BigDecimal(0);
		chSMILES = "";
		chIUPAC = "";
		chLink = new ArrayList<>(); // optional
		spScientificName = null; // optional
		spLineage = null; // optional
		spLink = new ArrayList<>(); // optional
		spSample = new ArrayList<>(); // optional
		acInstrument = "";
		acInstrumentType = "";
		acMassSpectrometryMsType = "";
		acMassSpectrometryIonMode = "";
		acMassSpectrometry = new ArrayList<>(); // optional
		acChromatography = new ArrayList<>(); // optional
		msFocusedIon = new ArrayList<>(); // optional
		msDataProcessing = new ArrayList<>(); // optional
		pkSplash = "";
		pkAnnotationTable = new PeakAnnotationTable();
	}


	public List<String> getRecordTitle() {
		return recordTitle;
	}
	public void setRecordTitle(List<String> recordTitle) {
		this.recordTitle = recordTitle;
	}

	public String getRecordTitle1() {
		return recordTitle == null ? "" : String.join("; ", recordTitle);
	}
	public void setRecordTitle1(String value) {
		recordTitle = value == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(value.split("; ")));
	}


	public String getDate() {
		return date;
	}
	public void setDate(String date) {
		this.date = date;
	}
	public String[] DATE1() {
		// DATE: 2016.01.15
		// DATE: 2011.02.21 (Created 2007.07.07)
		// DATE: 2016.01.19 (Created 2006.12.21, modified 2011.05.06)
		return date.replace("(Created ", "").replace(", modified", "").replace(")", "").split(" ");
	}


	public String getAuthors() {
		return authors;
	}
	public void setAuthors(String authors) {
		this.authors = authors;
	}


	public String getLicense() {
		return license;
	}
	public void setLicense(String value) {
		license = value;
	}


	public String getCopyright() {
		return orEmpty(copyright);
	}
	public String getCopyrightNullable() {
		return copyright;
	}
	public void setCopyright(String value) {
		copyright = nullIfBlank(value);
	}


	public String getPublication() {
		return orEmpty(publication);
	}
	public String getPublicationNullable() {
		return publication;
	}
	public void setPublication(String value) {
		publication = nullIfBlank(value);
	}


	public String getProject() {
		return orEmpty(project);
	}
	public String getProjectNullable() {
		return project;
	}
	public void setProject(String value) {
		project = nullIfBlank(value);
	}


	public List<String> getComment() {
		return comment;
	}
	public void setComment(List<String> value) {
		comment = value == null ? new ArrayList<>() : new ArrayList<>(value);
	}


	public List<String> getChName() {
		return chName;
	}
	public void setChName(List<String> value) {
		chName = value == null ? new ArrayList<>() : new ArrayList<>(value);
	}


	public List<String> getChCompoundClass() {
		return chCompoundClass;
	}
	public void setChCompoundClass(List<String> value) {
		chCompoundClass = value == null ? new ArrayList<>() : new ArrayList<>(value);
	}


	public String getChFormula() {
		return chFormula;
	}
	public void setChFormula(String value) {
		chFormula = value;
	}
	/**
	 * Returns the molecular formula as a String with HTML sup tags.
	 */
	@Transient
	public String getChFormula1() {
		IMolecularFormula m = MolecularFormulaManipulator.getMolecularFormula(getChFormula(), SilentChemObjectBuilder.getInstance());
		return MolecularFormulaManipulator.getHTML(m);
	}


	public BigDecimal getChExactMass() {
		return exactMass;
	}
	public void setChExactMass(BigDecimal value) {
		exactMass = value;
	}


	public String getChSMILES() {
		return chSMILES;
	}
	public IAtomContainer CH_SMILES_obj() {
		if ("N/A".equals(chSMILES)) return SilentChemObjectBuilder.getInstance().newAtomContainer();
		try {
			return new SmilesParser(SilentChemObjectBuilder.getInstance()).parseSmiles(chSMILES);
		} catch (InvalidSmilesException e) {
			logger.error("Structure generation from SMILES failed. Error: {} for {}.", e.getMessage(), chSMILES);
			return SilentChemObjectBuilder.getInstance().newAtomContainer();
		}
	}
	public void setChSMILES(String value) {
		chSMILES = value;
	}


	public String getChIUPAC() {
		return chIUPAC;
	}
	public IAtomContainer CH_IUPAC_obj() {
		if ("N/A".equals(chIUPAC)) return SilentChemObjectBuilder.getInstance().newAtomContainer();
		try {
			// Get InChIToStructure
			InChIToStructure intostruct = InChIGeneratorFactory.getInstance().getInChIToStructure(chIUPAC, SilentChemObjectBuilder.getInstance());
			InchiStatus ret = intostruct.getStatus();
			if (ret == InchiStatus.WARNING) {
				// Structure generated, but with warning message
				logger.warn("InChI warning: {} converting {}.", intostruct.getMessage(), chIUPAC);
			} else if (ret == InchiStatus.ERROR) {
				// Structure generation failed
				logger.error("Structure generation failed: {} converting {}.", intostruct.getMessage(), chIUPAC);
				return SilentChemObjectBuilder.getInstance().newAtomContainer();
			}
			return intostruct.getAtomContainer();
		} catch (CDKException e) {
			logger.error("Structure generation from InChI failed. Error: {} for {}.", e.getMessage(), chIUPAC);
			return SilentChemObjectBuilder.getInstance().newAtomContainer();
		}
	}
	public void setChIUPAC(String value) {
		chIUPAC = value;
	}


	public List<KeyValue> getChLink() {
		return chLink;
	}
	public void setChLink(List<KeyValue> value) {
		chLink = new ArrayList<>();
		if (value == null) return;
		for (KeyValue entry : value) {
			chLink.add(new KeyValue(entry.key(), entry.value()));
		}
	}


	public String getSpScientificName() {
		return orEmpty(spScientificName);
	}
	public String getSpScientificNameNullable() {
		return spScientificName;
	}
	public void setSpScientificName(String value) {
		spScientificName = nullIfBlank(value);
	}


	public String getSpLineage() {
		return orEmpty(spLineage);
	}
	public String getSpLineageNullable() {
		return spLineage;
	}
	public void setSpLineage(String value) {
		spLineage = nullIfBlank(value);
	}


	public List<KeyValue> getSpLink() {
		return spLink;
	}
	public void setSpLink(List<KeyValue> value) {
		spLink = new ArrayList<>();
		if (value == null) return;
		for (KeyValue entry : value) {
			spLink.add(new KeyValue(entry.key(), entry.value()));
		}
	}


	public List<String> getSpSample() {
		return spSample;
	}
	public void setSpSample(List<String> value) {
		spSample = value == null ? new ArrayList<>() : new ArrayList<>(value);
	}


	public String getAcInstrument() {
		return acInstrument;
	}
	public void setAcInstrument(String value) {
		acInstrument = value;
	}


	public String getAcInstrumentType() {
		return acInstrumentType;
	}
	public void setAcInstrumentType(String value) {
		this.acInstrumentType = value;
	}


	public String getAcMassSpectrometryMsType() {
		return acMassSpectrometryMsType;
	}
	public void setAcMassSpectrometryMsType(String value) {
		acMassSpectrometryMsType = value;
	}


	public String getAcMassSpectrometryIonMode() {
		return acMassSpectrometryIonMode;
	}
	public void setAcMassSpectrometryIonMode(String value) {
		acMassSpectrometryIonMode = value;
	}


	public List<KeyValue> getAcMassSpectrometry() {
		return acMassSpectrometry;
	}
	public void setAcMassSpectrometry(List<KeyValue> value) {
		acMassSpectrometry = new ArrayList<>();
		if (value == null) return;
		for (KeyValue entry : value) {
			if (entry != null) {
				acMassSpectrometry.add(new KeyValue(entry.key(), entry.value()));
			}
		}
	}

	public List<KeyValue> getAcChromatography() {
		return acChromatography;
	}
	public void setAcChromatography(List<KeyValue> value) {
		acChromatography = new ArrayList<>();
		if (value == null) return;
		for (KeyValue entry : value) {
			acChromatography.add(new KeyValue(entry.key(), entry.value()));
		}
	}

	public List<KeyValue> getMsFocusedIon() {
		return msFocusedIon;
	}
	public void setMsFocusedIon(List<KeyValue> value) {
		msFocusedIon = new ArrayList<>();
		if (value == null) return;
		for (KeyValue entry : value) {
			msFocusedIon.add(new KeyValue(entry.key(), entry.value()));
		}
	}

	public List<KeyValue> getMsDataProcessing() {
		return msDataProcessing;
	}
	public void setMsDataProcessing(List<KeyValue> value) {
		msDataProcessing = new ArrayList<>();
		if (value == null) return;
		for (KeyValue entry : value) {
			msDataProcessing.add(new KeyValue(entry.key(), entry.value()));
		}
	}

	public String getPkSPLASH() {
		return pkSplash;
	}
	public void setPkSPLASH(String value) {
		pkSplash = value;
	}

	public PeakAnnotationTable getPkAnnotationTable() {
		if (pkAnnotationTable == null) pkAnnotationTable = new PeakAnnotationTable();
		return pkAnnotationTable;
	}
	public void setPkAnnotationTable(PeakAnnotationTable table) {
		pkAnnotationTable = table == null
				? new PeakAnnotationTable()
				: new PeakAnnotationTable(table.getHeader(), table.getRows());
	}
	public List<String> getPkAnnotationHeader() { return getPkAnnotationTable().getHeader(); }
	public void setPkAnnotationHeader(List<String> header) { getPkAnnotationTable().setHeader(header); }
	public List<PeakAnnotationRow> getPkAnnotation() { return getPkAnnotationTable().getRows(); }
	public void setPkAnnotation(List<PeakAnnotationRow> annotation) { getPkAnnotationTable().setRows(annotation); }

	// PK_ANNOTATION is a two-dimensional List
	public List<Pair<BigDecimal, List<String>>> PK_ANNOTATION() {
		List<Pair<BigDecimal, List<String>>> result = new ArrayList<>();
		for (PeakAnnotationRow row : getPkAnnotation()) {
			result.add(Pair.of(row.getMz(), new ArrayList<>(row.getColumns())));
		}
		return result;
	}
	public void PK_ANNOTATION_ADD_LINE(Pair<BigDecimal, List<String>> annotation) {
		if (annotation == null) return;
		getPkAnnotation().add(new PeakAnnotationRow(annotation.getLeft(), annotation.getRight()));
	}
	public void setPK_ANNOTATION(List<Pair<BigDecimal, List<String>>> annotationList) {
		List<PeakAnnotationRow> rows = new ArrayList<>();
		if (annotationList == null) {
			setPkAnnotation(rows);
			return;
		}
		for (Pair<BigDecimal, List<String>> pair : annotationList) {
			if (pair != null) {
				rows.add(new PeakAnnotationRow(pair.getLeft(), pair.getRight()));
			}
		}
		setPkAnnotation(rows);
	}

	public int PK_NUM_PEAK() {
		return pkPeak.size();
	}


	public void addPeak(Peak peak) {
		Objects.requireNonNull(peak, "peak must not be null");
		if (peak.getRecord() != null) {
			throw new IllegalStateException("addPeak only accepts detached peaks");
		}
		peak.setRecord(this);
		pkPeak.add(peak);
	}
	public void removePeak(Peak peak) {
		Objects.requireNonNull(peak, "peak must not be null");
		if (!pkPeak.contains(peak)) {
			throw new IllegalStateException("peak is not part of this record");
		}
		if (peak.getRecord() != this) {
			throw new IllegalStateException("peak back-reference does not match this record");
		}
		pkPeak.remove(peak);
		peak.setRecord(null);
	}
	public List<Peak> getPkPeak() {
		return List.copyOf(pkPeak);
	}
	public void setPkPeak(List<Peak> peaks) {
		List<Peak> newPeaks = peaks == null ? new ArrayList<>() : new ArrayList<>(peaks);
		for (Peak peak : new ArrayList<>(pkPeak)) {
			removePeak(peak);
		}
		for (Peak peak : newPeaks) {
			addPeak(peak);
		}
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append("ACCESSION: ").append(getAccession()).append("\n");
		sb.append("RECORD_TITLE: ").append(getRecordTitle1()).append("\n");
		sb.append("DATE: ").append(getDate()).append("\n");
		sb.append("AUTHORS: ").append(getAuthors()).append("\n");
		sb.append("LICENSE: ").append(getLicense()).append("\n");
		if (hasText(copyright))
			sb.append("COPYRIGHT: ").append(getCopyright()).append("\n");
		if (hasText(publication))
			sb.append("PUBLICATION: ").append(getPublication()).append("\n");
		if (hasText(project))
			sb.append("PROJECT: ").append(getProject()).append("\n");
		for (String comment : getComment())
			sb.append("COMMENT: ").append(comment).append("\n");
		
		for (String ch_name : getChName())
			sb.append("CH$NAME: ").append(ch_name).append("\n");
		if (!getChCompoundClass().isEmpty()) {
			sb.append("CH$COMPOUND_CLASS: ").append(String.join("; ", getChCompoundClass())).append("\n");
		}
		sb.append("CH$FORMULA: ").append(getChFormula()).append("\n");
		sb.append("CH$EXACT_MASS: ").append(getChExactMass()).append("\n");
		sb.append("CH$SMILES: ").append(getChSMILES()).append("\n");
		sb.append("CH$IUPAC: ").append(getChIUPAC()).append("\n");
		for (KeyValue entry : getChLink()) {
			sb.append("CH$LINK: ")
				.append(entry.key())
				.append(" ")
				.append(entry.value() != null ? entry.value() : "")
				.append("\n");
		}
		
		if (hasText(spScientificName))
			sb.append("SP$SCIENTIFIC_NAME: ").append(getSpScientificName()).append("\n");
		if (hasText(spLineage))
			sb.append("SP$LINEAGE: ").append(getSpLineage()).append("\n");
		for (KeyValue entry : getSpLink()) {
			sb.append("SP$LINK: ")
				.append(entry.key())
				.append(" ")
				.append(entry.value() != null ? entry.value() : "")
				.append("\n");
		}
		for (String sample : getSpSample())
			sb.append("SP$SAMPLE: ").append(sample).append("\n");
		
		sb.append("AC$INSTRUMENT: ").append(getAcInstrument()).append("\n");
		sb.append("AC$INSTRUMENT_TYPE: ").append(getAcInstrumentType()).append("\n");
		sb.append("AC$MASS_SPECTROMETRY: MS_TYPE ").append(getAcMassSpectrometryMsType()).append("\n");
		sb.append("AC$MASS_SPECTROMETRY: ION_MODE ").append(getAcMassSpectrometryIonMode()).append("\n");
		for (KeyValue entry : getAcMassSpectrometry()) {
			sb.append("AC$MASS_SPECTROMETRY: ")
					.append(entry.key())
					.append(" ")
					.append(entry.value() != null ? entry.value() : "")
					.append("\n");
		}
		for (KeyValue entry : getAcChromatography()) {
			sb.append("AC$CHROMATOGRAPHY: ")
				.append(entry.key())
				.append(" ")
				.append(entry.value() != null ? entry.value() : "")
				.append("\n");
		}
		for (KeyValue entry : getMsFocusedIon()) {
			sb.append("MS$FOCUSED_ION: ")
				.append(entry.key())
				.append(" ")
				.append(entry.value() != null ? entry.value() : "")
				.append("\n");
		}
		for (KeyValue entry: getMsDataProcessing()) {
			sb.append("MS$DATA_PROCESSING: ")
				.append(entry.key())
				.append(" ")
				.append(entry.value())
				.append("\n");
		}

		sb.append("PK$SPLASH: ").append(getPkSPLASH()).append("\n");
		if (!getPkAnnotationHeader().isEmpty()) {
			sb.append("PK$ANNOTATION:");
			for (String annotation_header_item : getPkAnnotationHeader())
				sb.append(" ").append(annotation_header_item);
			sb.append("\n");
			for (Pair<BigDecimal, List<String>> annotation_line :  PK_ANNOTATION()) {
				sb.append("  ").append(annotation_line.getLeft()).append(" ").append(String.join(" ", annotation_line.getRight())).append("\n");
			}
		}

		sb.append("PK$NUM_PEAK: ").append(PK_NUM_PEAK()).append("\n");
		sb.append("PK$PEAK: m/z int. rel.int.\n");
		for (Peak peak : getPkPeak()) {
			String intensity1 = String.valueOf(peak.getIntensity());
			String intensity2 = String.valueOf(peak.getIntensity());
			String intensity = (intensity1.length() <  intensity2.length() ) ? intensity1 : intensity2;
			sb.append("  ").append(peak.getMz()).append(" ").append(intensity).append(" ").append(peak.getRelIntensity()).append("\n");
		}
		sb.append("//\n");

		return sb.toString();
	}
	
	public String createRecordString() {
		StringBuilder sb = new StringBuilder();

		sb.append("<b>ACCESSION:</b> ").append(getAccession()).append("<br>\n")
            .append("<b>RECORD_TITLE:</b> ").append(getRecordTitle1()).append("<br>\n")
			.append("<b>DATE:</b> ").append(getDate()).append("<br>\n")
			.append("<b>AUTHORS:</b> ").append(getAuthors()).append("<br>\n")
			.append("<b>LICENSE:</b> ").append(getLicenseLink()).append("<br>\n");
		if (hasText(copyright))
			sb.append("<b>COPYRIGHT:</b> ").append(getCopyright()).append("<br>\n");
		if (hasText(publication))
        	sb.append("<b>PUBLICATION:</b> ").append(getPublicationLink()).append("<br>\n");
		if (hasText(project))
			sb.append("<b>PROJECT:</b> ").append(getProject()).append("<br>\n");
		for (String comment : getComment())
			sb.append("<b>COMMENT:</b> ").append(comment).append("<br>\n");
		sb.append("<hr>\n");
		
		for (String ch_name : getChName())
			sb.append("<b>CH$NAME:</b> ").append(ch_name).append("<br>\n");
		sb.append("<b>CH$COMPOUND_CLASS:</b> ").append(String.join("; ", getChCompoundClass())).append("<br>\n");
		sb.append("<b>CH$FORMULA:</b> <a href=\"http://www.chemspider.com/Search.aspx?q=").append(getChFormula()).append("\" target=\"_blank\">").append(getChFormula1()).append("</a><br>\n");
		sb.append("<b>CH$EXACT_MASS:</b> ").append(getChExactMass()).append("<br>\n");
		sb.append("<b>CH$SMILES:</b> ").append(getChSMILES()).append("<br>\n");
		sb.append("<b>CH$IUPAC:</b> ").append(getChIUPAC()).append("<br>\n");
		for (KeyValue entry : getChLink()) {
			String key = entry.key();
			String value = entry.value();
			switch(key){
				case "CAS", "INCHIKEY":
					sb.append("<b>CH$LINK:</b> ").append(key).append(" <a href=\"https://www.google.com/search?q=&quot;").append(value).append("&quot;\" target=\"_blank\">").append(value).append("</a><br>\n");
					break;
				case "CAYMAN":
					sb.append("<b>CH$LINK:</b> ").append(key).append(" <a href=\"https://www.caymanchem.com/product/").append(value).append("\" target=\"_blank\">").append(value).append("</a><br>\n");
					break;
				case "CHEBI":
					sb.append("<b>CH$LINK:</b> ").append(key).append(" <a href=\"https://www.ebi.ac.uk/chebi/searchId.do?chebiId=CHEBI:").append(value).append("\" target=\"_blank\">").append(value).append("</a><br>\n");
					break;
				case "CHEMSPIDER":
					sb.append("<b>CH$LINK:</b> ").append(key).append(" <a href=\"https://www.chemspider.com/").append(value).append("\" target=\"_blank\">").append(value).append("</a><br>\n");
					break;
				case "COMPTOX":
					sb.append("<b>CH$LINK:</b> ").append(key).append(" <a href=\"https://comptox.epa.gov/dashboard/dsstoxdb/results?search=").append(value).append("\" target=\"_blank\">").append(value).append("</a><br>\n");
					break;
				case "HMDB":
					sb.append("<b>CH$LINK:</b> ").append(key).append(" <a href=\"http://www.hmdb.ca/metabolites/").append(value).append("\" target=\"_blank\">").append(value).append("</a><br>\n");
					break;
				case "KAPPAVIEW":
					sb.append("<b>CH$LINK:</b> ").append(key).append(" <a href=\"http://kpv.kazusa.or.jp/kpv4/compoundInformation/view.action?id=").append(value).append("\" target=\"_blank\">").append(value).append("</a><br>\n");
					break;
				case "KEGG":
					sb.append("<b>CH$LINK:</b> ").append(key).append(" <a href=\"https://www.genome.jp/dbget-bin/www_bget?cpd:").append(value).append("\" target=\"_blank\">").append(value).append("</a><br>\n");
					break;
				case "KNAPSACK":
					sb.append("<b>CH$LINK:</b> ").append(key).append(" <a href=\"http://www.knapsackfamily.com/knapsack_jsp/information.jsp?sname=C_ID&word=").append(value).append("\" target=\"_blank\">").append(value).append("</a><br>\n");
					break;
				case "LIPIDBANK":
					sb.append("<b>CH$LINK:</b> ").append(key).append(" <a href=\"http://lipidbank.jp/cgi-bin/detail.cgi?id=").append(value).append("\" target=\"_blank\">").append(value).append("</a><br>\n");
					break;
				case "LIPIDMAPS":
					sb.append("<b>CH$LINK:</b> ").append(key).append(" <a href=\"https://www.lipidmaps.org/data/LMSDRecord.php?LMID=").append(value).append("\" target=\"_blank\">").append(value).append("</a><br>\n");
					break;
				case "NIKKAJI":
					sb.append("<b>CH$LINK:</b> ").append(key).append(" <a href=\"https://jglobal.jst.go.jp/en/redirect?Nikkaji_No=").append(value).append("\" target=\"_blank\">").append(value).append("</a><br>\n");
					break;
				case "PUBCHEM":{
					if(value.startsWith("CID:")) sb.append("<b>CH$LINK:</b> ").append(key).append(" <a href=\"https://pubchem.ncbi.nlm.nih.gov/compound/").append(value.substring("CID:".length())).append("\" target=\"_blank\">").append(value).append("</a><br>\n");
					else if(value.startsWith("SID:")) sb.append("<b>CH$LINK:</b> ").append(key).append(" <a href=\"https://pubchem.ncbi.nlm.nih.gov/substance/").append(value.substring("SID:".length())).append("\" target=\"_blank\">").append(value).append("</a><br>\n");
					else sb.append("<b>CH$LINK:</b> ").append(key).append(" ").append(value).append("<br>\n");
					break;
				}
				default:
					sb.append("<b>CH$LINK:</b> ").append(key).append(" ").append(value).append("<br>\n");
			}
		}
		
		if (hasText(spScientificName))
			sb.append("<b>SP$SCIENTIFIC_NAME:</b> ").append(getSpScientificName()).append("<br>\n");
		if (hasText(spLineage))
			sb.append("<b>SP$LINEAGE:</b> ").append(getSpLineage()).append("<br>\n");
		for (KeyValue entry : getSpLink()) {
			sb.append("<b>SP$LINK:</b> ")
				.append(entry.key())
				.append(" ")
				.append(entry.value() != null ? entry.value() : "")
				.append("<br>\n");
		}
		for (String sample : getSpSample())
				sb.append("<b>SP$SAMPLE:</b> ").append(sample).append("<br>\n");
		sb.append("<hr>\n");
		
		sb.append("<b>AC$INSTRUMENT:</b> ").append(getAcInstrument()).append("<br>\n");
		sb.append("<b>AC$INSTRUMENT_TYPE:</b> ").append(getAcInstrumentType()).append("<br>\n");
		sb.append("<b>AC$MASS_SPECTROMETRY:</b> MS_TYPE ").append(getAcMassSpectrometryMsType()).append("<br>\n");
		sb.append("<b>AC$MASS_SPECTROMETRY:</b> ION_MODE ").append(getAcMassSpectrometryIonMode()).append("<br>\n");
		for (KeyValue entry: getAcMassSpectrometry()) {
			sb.append("<b>AC$MASS_SPECTROMETRY:</b> ")
					.append(entry.key())
					.append(" ")
					.append(entry.value() != null ? entry.value() : "")
					.append("<br>\n");
		}
		for (KeyValue entry : getAcChromatography()) {
			sb.append("<b>AC$CHROMATOGRAPHY:</b> ")
				.append(entry.key())
				.append(" ")
				.append(entry.value() != null ? entry.value() : "")
				.append("<br>\n");
		}
		sb.append("<hr>\n");

		for (KeyValue entry : getMsFocusedIon()) {
			sb.append("<b>MS$FOCUSED_ION:</b> ")
				.append(entry.key())
				.append(" ")
				.append(entry.value() != null ? entry.value() : "")
				.append("<br>\n");
		}
		for (KeyValue entry: getMsDataProcessing()) {
			sb.append("<b>MS$DATA_PROCESSING:</b> ")
				.append(entry.key())
				.append(" ")
				.append(entry.value())
				.append("<br>\n");
		}
		if (!getMsFocusedIon().isEmpty() || !getMsDataProcessing().isEmpty()) sb.append("<hr>\n");
		
		sb.append("<b>PK$SPLASH:</b> <a href=\"http://www.google.com/search?q=").append(getPkSPLASH()).append("\" target=\"_blank\">").append(getPkSPLASH()).append("</a><br>\n");
		if (!getPkAnnotationHeader().isEmpty()) {
			sb.append("<b>PK$ANNOTATION:</b>");
			for (String annotation_header_item : getPkAnnotationHeader())
				sb.append(" ").append(annotation_header_item);
			sb.append("<br>\n");
			for (Pair<BigDecimal, List<String>> annotation_line :  PK_ANNOTATION()) {
				sb.append("&nbsp;&nbsp;").append(annotation_line.getLeft()).append("&nbsp;").append(String.join("&nbsp;", annotation_line.getRight())).append("<br>\n");
	  }
		}
		sb.append("<b>PK$NUM_PEAK:</b> ").append(PK_NUM_PEAK()).append("<br>\n");
		sb.append("<b>PK$PEAK:</b> m/z int. rel.int.<br>\n");
		for (Peak peak : getPkPeak()) {
			sb.append("&nbsp;&nbsp;").append(peak.getMz()).append("&nbsp;").append(peak.getIntensity()).append("&nbsp;").append(peak.getRelIntensity()).append("<br>\n");
		}
		
		sb.append("//");

		return sb.toString();
	}

	@Transient
	private String getLicenseLink() {
		return switch (getLicense()) {
			case "CC0" -> "<a href=\"https://creativecommons.org/publicdomain/zero/1.0/\" target=\"_blank\">CC0</a>";
			case "CC BY" -> "<a href=\"https://creativecommons.org/licenses/by/4.0/\" target=\"_blank\">CC BY</a>";
			case "CC BY-SA" ->
				"<a href=\"https://creativecommons.org/licenses/by-sa/4.0/\" target=\"_blank\">CC BY-SA</a>";
			case "CC BY-NC" ->
				"<a href=\"https://creativecommons.org/licenses/by-nc/4.0/\" target=\"_blank\">CC BY-NC</a>";
			case "CC BY-NC-SA" ->
				"<a href=\"https://creativecommons.org/licenses/by-nc-sa/4.0/\" target=\"_blank\">CC BY-NC-SA</a>";
			case "dl-de/by-2-0" -> "<a href=\"https://www.govdata.de/dl-de/by-2-0\" target=\"_blank\">dl-de/by-2-0</a>";
			default -> getLicense();
		};
	}

	@Transient
	private String getPublicationLink() {
		String pub = getPublication();
		String regex_doi = "10\\.\\d{3,9}/[\\-._;()/:a-zA-Z0-9]+[a-zA-Z0-9]";
		String regex_pmid = "PMID: ?\\d{8}";
		Pattern pattern_doi = Pattern.compile(".*(" + regex_doi + ").*");
		Pattern pattern_pmid = Pattern.compile(".*(" + regex_pmid + ").*");
		Matcher matcher_doi = pattern_doi.matcher(pub);
		Matcher matcher_pmid = pattern_pmid.matcher(pub);
		if (matcher_doi.matches()) {
			String doi = pub.substring(matcher_doi.start(1), matcher_doi.end(1));
			pub = pub.replace(doi, "<a href=\"https://doi.org/" + doi + "\" target=\"_blank\">" + doi + "</a>");
		} else if (matcher_pmid.matches()) {
			String PMID = pub.substring(matcher_pmid.start(1), matcher_pmid.end(1));
			String id = PMID.substring("PMID:".length()).trim();
			pub = pub.replace(PMID, "<a href=\"https://pubmed.ncbi.nlm.nih.gov/" + id +"\" target=\"_blank\">" + PMID + "</a>");
		}
		return pub;
	}

	@Transient
	private static String nullIfBlank(String value) {
		if (value == null) return null;
		return value.isBlank() ? null : value;
	}

	@Transient
	private static String orEmpty(String value) {
		return value == null ? "" : value;
	}

	@Transient
	private static boolean hasText(String value) {
		return value != null && !value.isBlank();
	}

//	[
//	{
//	"identifier": "LQB00001",
//	"url": "https://massbank.eu/MassBank/RecordDisplay?id=LQB00001",
//	"name": "Cer[AP] t34:0",
//	"alternateName": "Cer[AP] t34:0",
//	"inchikey": "RHIXBFQKTNYVCX-UHFFFAOYSA-N",
//	"description": "This MassBank record with Accession LQB00001 contains the MS2 mass spectrum of 'Cer[AP] t34:0'.",
//	"molecularFormula": "C34H69NO5",
//	"monoisotopicMolecularWeight": "571.928",
//	"inChI": "InChI=1S/C34H69NO5/c1-3-5-7-9-11-13-15-17-19-21-23-25-27-31(37)33(39)30(29-36)35-34(40)32(38)28-26-24-22-20-18-16-14-12-10-8-6-4-2/h30-33,36-39H,3-29H2,1-2H3,(H,35,40)",
//	"smiles": "CCCCCCCCCCCCCCC(O)C(O)C(CO)NC(=O)C(O)CCCCCCCCCCCCCC",
//	"@context": "http://schema.org",
//	"@type": "MolecularEntity"
//	},
//	{
//	"identifier": "LQB00001",
//	"url": "https://massbank.eu/MassBank/RecordDisplay?id=LQB00001",
//	"headline": "Cer[AP] t34:0; LC-ESI-QTOF; MS2",
//	"name": "Cer[AP] t34:0",
//	"description": "This MassBank record with Accession LQB00001 contains the MS2 mass spectrum of 'Cer[AP] t34:0'.",
//	"datePublished": "2016-10-03",
//	"license": "https://creativecommons.org/licenses",
//	"citation": "null",
//	"comment": "Found in mouse small intestine; TwoDicalId=238; MgfFile=160907_Small_Intestine_normal_Neg_01_never; MgfId=1081",
//	"alternateName": "Cer[AP] t34:0",
//	"@context": "http://schema.org",
//	"@type": "Dataset"
//	}
//	]

//	Thanks for the contribution of markup within MassBank. As discussed in PR 274 there are some refinements that should be made.
//
//	Add DataCatalog and Dataset markup to the landing page https://massbank.eu/MassBank/
//	Use DataRecord instead of Dataset on MassBank massbank.Record pages such as LQB00001
//
//	    Replace the value in the @type property so that it is DataRecord instead of Dataset
//	    Include the comment text with the schema:description property
//
//	Include the comment text with the schema:description property
//
//	    Include the chemical image with the schema:image property
//
//	You should ensure that there are different identifiers used for the DataRecord (currently Dataset) and the MolecularEntity.
//
//	Once you've made these refinements, we'll be able to add you to the DataRecord, Dataset, and DataCatalog list of live deploys.

	//https://github.com/BioSchemas/specifications/issues/198

	public JsonArray createStructuredDataJsonArray() {
		String InChiKey = getChLink().stream().filter(e -> "INCHIKEY".equals(e.key())).map(KeyValue::value).findFirst().orElse(null);
		String description = "This MassBank record with Accession " + getAccession()
			+ " contains the " + getAcMassSpectrometryMsType() + " mass spectrum of " + getRecordTitle().getFirst()
			+ ((InChiKey==null) ? "." : " with the InChIkey " + InChiKey + ".");
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		
		// dataset
		JsonObject dataset = new JsonObject();
		dataset.addProperty("@context", "https://schema.org");
		dataset.addProperty("@type", "Dataset");
		dataset.add("http://purl.org/dc/terms/conformsTo",
				gson.fromJson("{ \"@type\": \"CreativeWork\", \"@id\": \"https://bioschemas.org/profiles/Dataset/1.0-RELEASE\" }", JsonObject.class));
		dataset.addProperty("@id", "https://massbank.eu/MassBank/RecordDisplay?id=" + getAccession() + "#Dataset");
		dataset.addProperty("description", description);
		dataset.addProperty("identifier", getAccession());
		dataset.addProperty("name", getRecordTitle1());

		JsonArray keywords = new JsonArray();
		keywords.add(gson.fromJson(
			"""
				{ "@type": "DefinedTerm",\
				"name": "Mass spectrometry data",\
				"url": "http://edamontology.org/data_2536",\
				"termCode": "data_2536",\
				"inDefinedTermSet": {\
				"@type": "DefinedTermSet",
				"name": "Bioinformatics operations, data types, formats, identifiers and topics",
				"url": "http://edamontology.org"
				} }""", JsonObject.class));
		dataset.add("keywords", keywords);

	        switch (getLicense()) {
            case "CC0" -> dataset.addProperty("license", "https://creativecommons.org/publicdomain/zero/1.0/");
            case "CC BY" -> dataset.addProperty("license", "https://creativecommons.org/licenses/by/4.0/");
            case "CC BY-SA" -> dataset.addProperty("license", "https://creativecommons.org/licenses/by-sa/4.0");
            case "CC BY-NC" -> dataset.addProperty("license", "https://creativecommons.org/licenses/by-nc/4.0");
            case "CC BY-NC-SA" -> dataset.addProperty("license", "https://creativecommons.org/licenses/by-nc-sa/4.0");
            case "dl-de/by-2-0" -> dataset.addProperty("license", "https://www.govdata.de/dl-de/by-2-0");
        }

		JsonObject about = new JsonObject();
		about.addProperty("@type", "ChemicalSubstance");
		about.addProperty("@id", "https://massbank.eu/MassBank/RecordDisplay?id=" + getAccession() + "#ChemicalSubstance");
		dataset.add("about", about);

		dataset.addProperty("url", "https://massbank.eu/MassBank/RecordDisplay?id=" + getAccession());
		dataset.addProperty("datePublished", DATE1()[0].replace(".","-"));
		dataset.addProperty("citation", getPublication());

		JsonArray measurementTechnique = new JsonArray();
		measurementTechnique.add(gson.fromJson(
				"{\"@type\": \"DefinedTerm\","
				+ "\"name\": \"liquid chromatography-mass spectrometry\","
				+ "\"url\": \"http://purl.obolibrary.org/obo/CHMO_0000524\","
				+ "\"termCode\": \"CHMO_0000524\","
				+ "\"inDefinedTermSet\": {"
				+ "\"@type\": \"DefinedTermSet\","
				+ "\"name\": \"Chemical Methods Ontology\","
				+ "\"url\": \"http://purl.obolibrary.org/obo/chmo.owl\""
				+ "} }", JsonObject.class));
		dataset.add("measurementTechnique", measurementTechnique);
		
		dataset.add("includedInDataCatalog", gson.fromJson(
				"{\"@type\": \"DataCatalog\","
				+ "\"name\": \"MassBank\","
				+ "\"url\": \"https://massbank.eu\""
				+ "}", JsonObject.class));
		
		JsonObject chemicalSubstance = new JsonObject();
		chemicalSubstance.addProperty("@context", "https://schema.org");
		chemicalSubstance.addProperty("@type", "ChemicalSubstance");
		chemicalSubstance.add("http://purl.org/dc/terms/conformsTo",
				gson.fromJson("{ \"@type\": \"CreativeWork\", \"@id\": \"https://bioschemas.org/profiles/ChemicalSubstance/0.4-RELEASE\" }", JsonObject.class));
		chemicalSubstance.addProperty("@id", "https://massbank.eu/MassBank/RecordDisplay?id=" + getAccession() + "#ChemicalSubstance");
		chemicalSubstance.addProperty("identifier", getAccession());
		chemicalSubstance.addProperty("name", getRecordTitle().getFirst());
		chemicalSubstance.addProperty("url", "https://massbank.eu/MassBank/RecordDisplay?id=" + getAccession());
		chemicalSubstance.addProperty("chemicalComposition", getChFormula());
		if (getChName().size() == 1)  chemicalSubstance.addProperty("alternateName", getChName().getFirst());
		else if (!getChName().isEmpty()) chemicalSubstance.add("alternateName", gson.toJsonTree(getChName()));

		JsonArray molecularEntitys = new JsonArray();
		
		// create a loop in case of multiple MolecularEntity
		JsonObject molecularEntity = new JsonObject();
		molecularEntity.addProperty("@context", "https://schema.org");
		molecularEntity.addProperty("@type", "MolecularEntity");
		molecularEntity.add("http://purl.org/dc/terms/conformsTo",
				gson.fromJson("{ \"@type\": \"CreativeWork\", \"@id\": \"https://bioschemas.org/profiles/MolecularEntity/0.5-RELEASE\" }", JsonObject.class));
		molecularEntity.addProperty("@id", "https://massbank.eu/MassBank/RecordDisplay?id=" + getAccession()
				+ "#" + (InChiKey!=null ? InChiKey : "MolecularEntity"));
		molecularEntity.addProperty("identifier", getAccession());
		molecularEntity.addProperty("name", getRecordTitle().getFirst());
		molecularEntity.addProperty("url", "https://massbank.eu/MassBank/RecordDisplay?id=" + getAccession());
		if (!getChIUPAC().equals("N/A")) molecularEntity.addProperty("inChI", getChIUPAC());
		if (!getChSMILES().equals("N/A")) molecularEntity.addProperty("smiles", getChSMILES());
		molecularEntity.addProperty("molecularFormula", getChFormula());
		molecularEntity.addProperty("monoisotopicMolecularWeight", getChExactMass());
		if (InChiKey!=null) molecularEntity.addProperty("inChIKey", InChiKey);
		
		molecularEntitys.add(molecularEntity);
		chemicalSubstance.add("hasBioChemEntityPart", molecularEntitys);

		JsonObject subjectOf = new JsonObject();
		subjectOf.addProperty("@type", "Dataset");
		subjectOf.addProperty("@id", "https://massbank.eu/MassBank/RecordDisplay?id=" + getAccession() + "#Dataset");
		chemicalSubstance.add("subjectOf", subjectOf);

		// put MolecularEntity and Dataset together
		JsonArray structuredData = new JsonArray();
		structuredData.add(dataset);
		structuredData.add(chemicalSubstance);
		return structuredData;

	}
	
	public String createStructuredData() {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		return gson.toJson(createStructuredDataJsonArray());
	}
	

	public String createPeakListForSpectrumViewer() {
        // convert a list of lists [[mz, int, rel.int], [...], ...]
        // to String "mz,rel.int@mz,rel.int@..."
		List<String> peaks = new ArrayList<>();
		for (Peak peak : getPkPeak()) {
			peaks.add(peak.getMz()+","+peak.getRelIntensity());
		}
		return String.join("@", peaks);
	}

	public JsonObject createPeakListData() {
		JsonObject result = new JsonObject();
		JsonArray peaklist = new JsonArray();
		for (Peak peak : getPkPeak()) {
			JsonObject jsonPeak = new JsonObject();
			jsonPeak.addProperty("intensity",peak.getRelIntensity());
			jsonPeak.addProperty("mz", peak.getMz());
			peaklist.add(jsonPeak);
		}
		result.add("peaks", peaklist);
		return result;
	}

	public record Structure(String CH_SMILES, String CH_IUPAC) {
	}

	public record Contributor(String ACRONYM, String SHORT_NAME, String FULL_NAME) {
	}


	public record KeyValue(String key, String value) {}

	public static class PeakAnnotationRow {
		private BigDecimal mz;
		private List<String> columns = new ArrayList<>();

		public PeakAnnotationRow() {}
		public PeakAnnotationRow(BigDecimal mz, List<String> columns) {
			this.mz = mz;
			setColumns(columns);
		}
		public BigDecimal getMz() { return mz; }
		public void setMz(BigDecimal mz) { this.mz = mz; }
		public List<String> getColumns() { return columns; }
		public void setColumns(List<String> columns) {
			this.columns = columns == null ? new ArrayList<>() : new ArrayList<>(columns);
		}
	}

	public static class PeakAnnotationTable {
		private List<String> header = new ArrayList<>();
		private List<PeakAnnotationRow> rows = new ArrayList<>();

		public PeakAnnotationTable() {}
		public PeakAnnotationTable(List<String> header, List<PeakAnnotationRow> rows) {
			setHeader(header);
			setRows(rows);
		}
		public List<String> getHeader() { return header; }
		public void setHeader(List<String> header) {
			this.header = header == null ? new ArrayList<>() : new ArrayList<>(header);
		}
		public List<PeakAnnotationRow> getRows() { return rows; }
		public void setRows(List<PeakAnnotationRow> rows) {
			this.rows = new ArrayList<>();
			if (rows == null) return;
			for (PeakAnnotationRow row : rows) {
				if (row != null) {
					this.rows.add(new PeakAnnotationRow(row.getMz(), row.getColumns()));
				}
			}
		}
	}
}



