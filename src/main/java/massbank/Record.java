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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.inchi.InChIGeneratorFactory;
import org.openscience.cdk.inchi.InChIToStructure;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents an active MassBank record.
 * <p>
 * The entity stores curated record metadata, chemical/sample/acquisition
 * details, peak annotations and measured peaks. Parser-facing accessors and
 * {@link #toString()} intentionally preserve the canonical MassBank text
 * format used by round-trip tests and downstream exporters.
 *
 * @author rmeier
 * @version 03-07-2026
 */
@Entity
@Table(name = "massbank-records")
public class Record extends AbstractRecord {
	private static final Logger logger = LogManager.getLogger(Record.class);
	private static final Gson prettyGson = new GsonBuilder().setPrettyPrinting().create();
	private static final Pattern doiPattern = Pattern.compile(".*(10\\.\\d{3,9}/[\\-._;()/:a-zA-Z0-9]+[a-zA-Z0-9]).*");
	private static final Pattern pmidPattern = Pattern.compile(".*(PMID: ?\\d{8}).*");
	private static final IChemObjectBuilder chemObjectBuilder = SilentChemObjectBuilder.getInstance();

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "record_title", columnDefinition = "jsonb", nullable = false)
	private List<String> recordTitle = new ArrayList<>();

	@Column(name = "date", length = 100)
	private String date = "";

	@Column(name = "authors", length = 512)
	private String authors = "";

	@Column(name = "license", length = 64)
	private String license = "";

	@Column(name = "copyright", length = 2048)
	private String copyright; // optional

	@Column(name = "publication", length = 2048)
	private String publication; // optional

	@Column(name = "project", length = 512)
	private String project; // optional

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "comment", columnDefinition = "jsonb")
	private List<String> comment = new ArrayList<>(); // optional

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "ch_name", columnDefinition = "jsonb")
	private List<String> chName = new ArrayList<>();

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "ch_compound_class", columnDefinition = "jsonb")
	private List<String> chCompoundClass = new ArrayList<>(); // optional

	@Column(name = "ch_formula", length = 512)
	private String chFormula = "";

	@Column(name = "ch_exact_mass", columnDefinition = "numeric")
	private BigDecimal exactMass = BigDecimal.ZERO;

	@Column(name = "ch_smiles", length = 2048)
	private String chSMILES = "";

	@Column(name = "ch_iupac", length = 2048)
	private String chIUPAC = "";

	@ElementCollection
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "ch_link", columnDefinition = "jsonb")
	private List<KeyValue> chLink = new ArrayList<>(); // optional

	@Column(name = "sp_scientific_name", length = 512)
	private String spScientificName; // optional

	@Column(name = "sp_lineage", length = 2048)
	private String spLineage; // optional

	@ElementCollection
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "sp_link", columnDefinition = "jsonb")
	private List<KeyValue> spLink = new ArrayList<>(); // optional

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "sp_sample", columnDefinition = "jsonb")
	private List<String> spSample = new ArrayList<>(); // optional

	@Column(name = "ac_instrument", length = 2048)
	private String acInstrument = "";

	@Column(name = "ac_instrument_type", length = 512)
	private String acInstrumentType = "";

	@Column(name = "ac_mass_spectrometry_ms_type", length = 32)
	private String acMassSpectrometryMsType = "";

	@Column(name = "ac_mass_spectrometry_ion_mode", length = 32)
	private String acMassSpectrometryIonMode = "";

	@ElementCollection
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "ac_mass_spectrometry", columnDefinition = "jsonb")
	private List<KeyValue> acMassSpectrometry = new ArrayList<>(); // optional

	@ElementCollection
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "ac_chromatography", columnDefinition = "jsonb")
	private List<KeyValue> acChromatography = new ArrayList<>(); // optional

	@ElementCollection
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "ms_focused_ion", columnDefinition = "jsonb")
	private List<KeyValue> msFocusedIon = new ArrayList<>(); // optional

	@ElementCollection
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "ms_data_processing", columnDefinition = "jsonb")
	private List<KeyValue> msDataProcessing = new ArrayList<>(); // optional

	@Column(name = "pk_splash", length = 128)
	private String pkSplash = "";

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "pk_annotation", columnDefinition = "jsonb")
	private final PeakAnnotationTable pkAnnotationTable = new PeakAnnotationTable();

	@OneToMany(mappedBy = "record", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("mz ASC")
	private List<Peak> pkPeak = new ArrayList<>();


	public List<String> getRecordTitle() {
		return List.copyOf(recordTitle);
	}
	public String getRecordTitle1() {
		return String.join("; ", recordTitle);
	}
	public void setRecordTitle(List<String> recordTitle) {
		if (recordTitle == null || recordTitle.isEmpty()) {
			throw new IllegalStateException("Missing required field: recordTitle");
		}
		this.recordTitle = new ArrayList<>(recordTitle);
	}
	public void setRecordTitle1(String recordTitle) {
		if (recordTitle == null || recordTitle.isBlank()) {
			throw new IllegalStateException("Missing required field: recordTitle");
		}
		this.recordTitle = new ArrayList<>(Arrays.asList(recordTitle.split("; ")));
	}


	public String getDate() {
		return date;
	}
	public String[] getDate1() {
		// DATE: 2016.01.15
		// DATE: 2011.02.21 (Created 2007.07.07)
		// DATE: 2016.01.19 (Created 2006.12.21, modified 2011.05.06)
		return date.replace("(Created ", "").replace(", modified", "").replace(")", "").split(" ");
	}
	public void setDate(String date) {
		if (date == null || date.isEmpty()) {
			throw new IllegalStateException("Missing required field: date");
		}
		this.date = date;
	}


	public String getAuthors() {
		return authors;
	}
	public void setAuthors(String authors) {
		if (authors == null || authors.isEmpty()) {
			throw new IllegalStateException("Missing required field: authors");
		}
		this.authors = authors;
	}


	public String getLicense() {
		return license;
	}
	public void setLicense(String license) {
		if (license == null || license.isEmpty()) {
			throw new IllegalStateException("Missing required field: license");
		}
		this.license = license;
	}


	public String getCopyright() {
		return copyright == null ? "" : copyright;
	}
	public String getCopyrightNullable() {
		return copyright;
	}
	public void setCopyright(String value) {
		copyright = nullIfBlank(value);
	}


	public String getPublication() {
		return publication == null ? "" : publication;
	}
	public String getPublicationNullable() {
		return publication;
	}
	public void setPublication(String value) {
		publication = nullIfBlank(value);
	}


	public String getProject() {
		return project == null ? "" : project;
	}
	public String getProjectNullable() {
		return project;
	}
	public void setProject(String value) {
		project = nullIfBlank(value);
	}


	public List<String> getComment() {
		return List.copyOf(comment);
	}
	public void setComment(List<String> comment) {
		this.comment = comment == null ? new ArrayList<>() : new ArrayList<>(comment);
	}


	public List<String> getChName() {
		return List.copyOf(chName);
	}
	public void setChName(List<String> chName) {
		if (chName == null || chName.isEmpty()) {
			throw new IllegalStateException("Missing required field: chName");
		}
		this.chName = new ArrayList<>(chName);
	}


	public List<String> getChCompoundClass() {
		return List.copyOf(chCompoundClass);
	}
	public void setChCompoundClass(List<String> chCompoundClass) {
		this.chCompoundClass = chCompoundClass == null ? new ArrayList<>() : new ArrayList<>(chCompoundClass);
	}


	public String getChFormula() {
		return chFormula;
	}
	/**
	 * Returns the molecular formula as a String with HTML sup tags.
	 */
	public String getChFormula1() {
		IMolecularFormula m = MolecularFormulaManipulator.getMolecularFormula(getChFormula(), chemObjectBuilder);
		return MolecularFormulaManipulator.getHTML(m);
	}
	public void setChFormula(String chFormula) {
		if (chFormula == null || chFormula.isEmpty()) {
			throw new IllegalStateException("Missing required field: chFormula");
		}
		this.chFormula = chFormula;
	}


	public BigDecimal getChExactMass() {
		return exactMass;
	}
	public void setChExactMass(BigDecimal exactMass) {
		if (exactMass == null) {
			throw new IllegalStateException("Missing required field: exactMass");
		}
		this.exactMass = exactMass;
	}


	public String getChSMILES() {
		return chSMILES;
	}
	@SuppressWarnings("unused")
	public IAtomContainer getChSMILES_obj() {
		if ("N/A".equals(chSMILES)) return chemObjectBuilder.newAtomContainer();
		try {
			return new SmilesParser(chemObjectBuilder).parseSmiles(chSMILES);
		} catch (InvalidSmilesException e) {
			logger.error("Structure generation from SMILES failed. Error: {} for {}.", e.getMessage(), chSMILES);
			return chemObjectBuilder.newAtomContainer();
		}
	}
	public void setChSMILES(String chSMILES) {
		if (chSMILES == null|| chSMILES.isEmpty()) {
			throw new IllegalStateException("Missing required field: chSMILES");
		}
		this.chSMILES = chSMILES;
	}


	public String getChIUPAC() {
		return chIUPAC;
	}
	@SuppressWarnings("unused")
	public IAtomContainer getChIUPAC_obj() {
		if ("N/A".equals(chIUPAC)) return chemObjectBuilder.newAtomContainer();
		try {
			// Get InChIToStructure
			InChIToStructure intostruct = InChIGeneratorFactory.getInstance().getInChIToStructure(chIUPAC, chemObjectBuilder);
			InchiStatus ret = intostruct.getStatus();
			if (ret == InchiStatus.WARNING) {
				// Structure generated, but with warning message
				logger.warn("InChI warning: {} converting {}.", intostruct.getMessage(), chIUPAC);
			} else if (ret == InchiStatus.ERROR) {
				// Structure generation failed
				logger.error("Structure generation failed: {} converting {}.", intostruct.getMessage(), chIUPAC);
				return chemObjectBuilder.newAtomContainer();
			}
			return intostruct.getAtomContainer();
		} catch (CDKException e) {
			logger.error("Structure generation from InChI failed. Error: {} for {}.", e.getMessage(), chIUPAC);
			return chemObjectBuilder.newAtomContainer();
		}
	}
	public void setChIUPAC(String chIUPAC) {
		if (chIUPAC == null|| chIUPAC.isEmpty()) {
			throw new IllegalStateException("Missing required field: chIUPAC");
		}
		this.chIUPAC = chIUPAC;
	}


	public List<KeyValue> getChLink() {
		return List.copyOf(chLink);
	}
	public void setChLink(List<KeyValue> chLink) {
		this.chLink = chLink == null ? new ArrayList<>() : new ArrayList<>(chLink);
	}


	public String getSpScientificName() {
		return spScientificName == null ? "" : spScientificName;
	}
	public String getSpScientificNameNullable() {
		return spScientificName;
	}
	public void setSpScientificName(String value) {
		spScientificName = nullIfBlank(value);
	}


	public String getSpLineage() {
		return spLineage == null ? "" : spLineage;
	}
	public String getSpLineageNullable() {
		return spLineage;
	}
	public void setSpLineage(String value) {
		spLineage = nullIfBlank(value);
	}


	public List<KeyValue> getSpLink() {
		return List.copyOf(spLink);
	}
	public void setSpLink(List<KeyValue> spLink) {
		this.spLink = spLink == null ? new ArrayList<>() : new ArrayList<>(spLink);
	}


	public List<String> getSpSample() {
		return List.copyOf(spSample);
	}
	public void setSpSample(List<String> spSample) {
		this.spSample = spSample == null ? new ArrayList<>() : new ArrayList<>(spSample);
	}


	public String getAcInstrument() {
		return acInstrument;
	}
	public void setAcInstrument(String acInstrument) {
		if (acInstrument == null || acInstrument.isEmpty()) {
			throw new IllegalStateException("Missing required field: acInstrument");
		}
		this.acInstrument = acInstrument;
	}


	public String getAcInstrumentType() {
		return acInstrumentType;
	}
	public void setAcInstrumentType(String acInstrumentType) {
		if (acInstrumentType == null || acInstrumentType.isEmpty()) {
			throw new IllegalStateException("Missing required field: acInstrumentType");
		}
		this.acInstrumentType = acInstrumentType;
	}


	public String getAcMassSpectrometryMsType() {
		return acMassSpectrometryMsType;
	}
	public void setAcMassSpectrometryMsType(String acMassSpectrometryMsType) {
		if (acMassSpectrometryMsType == null || acMassSpectrometryMsType.isEmpty()) {
			throw new IllegalStateException("Missing required field: acMassSpectrometryMsType");
		}
		this.acMassSpectrometryMsType = acMassSpectrometryMsType;
	}


	public String getAcMassSpectrometryIonMode() {
		return acMassSpectrometryIonMode;
	}
	public void setAcMassSpectrometryIonMode(String acMassSpectrometryIonMode) {
		if (acMassSpectrometryIonMode == null || acMassSpectrometryIonMode.isEmpty()) {
			throw new IllegalStateException("Missing required field: acMassSpectrometryIonMode");
		}
		this.acMassSpectrometryIonMode = acMassSpectrometryIonMode;
	}


	public List<KeyValue> getAcMassSpectrometry() {
		return List.copyOf(acMassSpectrometry);
	}
	public void setAcMassSpectrometry(List<KeyValue> acMassSpectrometry) {
		this.acMassSpectrometry = acMassSpectrometry == null ? new ArrayList<>() : new ArrayList<>(acMassSpectrometry);
	}

	public List<KeyValue> getAcChromatography() {
		return List.copyOf(acChromatography);
	}
	public void setAcChromatography(List<KeyValue> acChromatography) {
		this.acChromatography = acChromatography == null ? new ArrayList<>() : new ArrayList<>(acChromatography);
	}

	public List<KeyValue> getMsFocusedIon() {
		return List.copyOf(msFocusedIon);
	}
	public void setMsFocusedIon(List<KeyValue> msFocusedIon) {
		this.msFocusedIon = msFocusedIon == null ? new ArrayList<>() : new ArrayList<>(msFocusedIon);
	}


	public List<KeyValue> getMsDataProcessing() {
		return List.copyOf(msDataProcessing);
	}
	public void setMsDataProcessing(List<KeyValue> msDataProcessing) {
		this.msDataProcessing = msDataProcessing == null ? new ArrayList<>() : new ArrayList<>(msDataProcessing);
	}


	public String getPkSPLASH() {
		return pkSplash;
	}
	public void setPkSPLASH(String pkSplash) {
		if (pkSplash == null|| pkSplash.isEmpty()) {
			throw new IllegalStateException("Missing required field: pkSplash");
		}
		this.pkSplash = pkSplash;
	}

	@SuppressWarnings("unused")
	public PeakAnnotationTable getPkAnnotationTable() {
		PeakAnnotationTable copy = new PeakAnnotationTable();
		copy.setHeader(pkAnnotationTable.getHeader());
		copy.setRows(pkAnnotationTable.getRows());
		return copy;
	}
	public List<String> getPkAnnotationHeader() { return pkAnnotationTable.getHeader(); }
	public void setPkAnnotationHeader(List<String> header) {
		pkAnnotationTable.setHeader(header);
	}
	public List<PeakAnnotationRow> getPkAnnotation() {
		return pkAnnotationTable.getRows();
	}
	public void setPkAnnotation(List<PeakAnnotationRow> annotation) { pkAnnotationTable.setRows(annotation); }


	public int getPkNumPeak() {
		return pkPeak.size();
	}


	public void addPeak(Peak peak) {
		if (peak == null) {
			throw new IllegalStateException("peak must not be null");
		}
		if (peak.getRecord() != null) {
			throw new IllegalStateException("addPeak only accepts detached peaks");
		}
		peak.setRecord(this);
		pkPeak.add(peak);
	}
	@SuppressWarnings("unused")
	public void removePeak(Peak peak) {
		if (peak == null) {
			throw new IllegalStateException("peak must not be null");
		}
		if (peak.getRecord() != this) {
			throw new IllegalStateException("peak back-reference does not match this record");
		}
		if (!pkPeak.remove(peak)) {
			throw new IllegalStateException("peak is not part of this record");
		}
		peak.setRecord(null);
	}
	public List<Peak> getPkPeak() {
		return List.copyOf(pkPeak);
	}
	@SuppressWarnings("unused")
	public void setPkPeak(List<Peak> peaks) {
		List<Peak> newPeaks = peaks == null ? new ArrayList<>() : new ArrayList<>(peaks);
		for (Peak peak : new ArrayList<>(pkPeak)) {
			removePeak(peak);
		}
		for (Peak peak : newPeaks) {
			addPeak(peak);
		}
	}

	//TODO
	@Override
	protected void validateState() {
		super.validateState();
		if (recordTitle == null || recordTitle.isEmpty() || !hasText(recordTitle.getFirst())) {
			throw new IllegalStateException("Missing required field: recordTitle");
		}
		requireText("date", date);
		requireText("chFormula", chFormula);
		requireText("chSMILES", chSMILES);
		requireText("chIUPAC", chIUPAC);
		if (exactMass == null) {
			throw new IllegalStateException("Missing required field: exactMass");
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(4096);

		sb.append("ACCESSION: ").append(accession).append("\n");
		sb.append("RECORD_TITLE: ").append(String.join("; ", recordTitle)).append("\n");
		sb.append("DATE: ").append(date).append("\n");
		sb.append("AUTHORS: ").append(authors).append("\n");
		sb.append("LICENSE: ").append(license).append("\n");
		if (hasText(copyright))
			sb.append("COPYRIGHT: ").append(copyright).append("\n");
		if (hasText(publication))
			sb.append("PUBLICATION: ").append(publication).append("\n");
		if (hasText(project))
			sb.append("PROJECT: ").append(project).append("\n");
		for (String commentItem : comment)
			sb.append("COMMENT: ").append(commentItem).append("\n");

		for (String chNameItem : chName)
			sb.append("CH$NAME: ").append(chNameItem).append("\n");
		if (!chCompoundClass.isEmpty()) {
			sb.append("CH$COMPOUND_CLASS: ").append(String.join("; ", chCompoundClass)).append("\n");
		}
		sb.append("CH$FORMULA: ").append(chFormula).append("\n");
		sb.append("CH$EXACT_MASS: ").append(exactMass).append("\n");
		sb.append("CH$SMILES: ").append(chSMILES).append("\n");
		sb.append("CH$IUPAC: ").append(chIUPAC).append("\n");
		for (KeyValue entry : chLink) {
			sb.append("CH$LINK: ")
					.append(entry.key())
					.append(" ")
					.append(entry.value())
					.append("\n");
		}

		if (hasText(spScientificName))
			sb.append("SP$SCIENTIFIC_NAME: ").append(spScientificName).append("\n");
		if (hasText(spLineage))
			sb.append("SP$LINEAGE: ").append(spLineage).append("\n");
		for (KeyValue entry : spLink) {
			sb.append("SP$LINK: ")
					.append(entry.key())
					.append(" ")
					.append(entry.value() != null ? entry.value() : "")
					.append("\n");
		}
		for (String sample : spSample)
			sb.append("SP$SAMPLE: ").append(sample).append("\n");

		sb.append("AC$INSTRUMENT: ").append(acInstrument).append("\n");
		sb.append("AC$INSTRUMENT_TYPE: ").append(acInstrumentType).append("\n");
		sb.append("AC$MASS_SPECTROMETRY: MS_TYPE ").append(acMassSpectrometryMsType).append("\n");
		sb.append("AC$MASS_SPECTROMETRY: ION_MODE ").append(acMassSpectrometryIonMode).append("\n");
		for (KeyValue entry : acMassSpectrometry) {
			sb.append("AC$MASS_SPECTROMETRY: ")
					.append(entry.key())
					.append(" ")
					.append(entry.value())
					.append("\n");
		}
		for (KeyValue entry : acChromatography) {
			sb.append("AC$CHROMATOGRAPHY: ")
					.append(entry.key())
					.append(" ")
					.append(entry.value())
					.append("\n");
		}
		for (KeyValue entry : msFocusedIon) {
			sb.append("MS$FOCUSED_ION: ")
					.append(entry.key())
					.append(" ")
					.append(entry.value())
					.append("\n");
		}
		for (KeyValue entry : msDataProcessing) {
			sb.append("MS$DATA_PROCESSING: ")
					.append(entry.key())
					.append(" ")
					.append(entry.value())
					.append("\n");
		}

		sb.append("PK$SPLASH: ").append(pkSplash).append("\n");
		if (!pkAnnotationTable.header.isEmpty()) {
			sb.append("PK$ANNOTATION:");
			for (String annotationHeaderItem : pkAnnotationTable.header)
				sb.append(" ").append(annotationHeaderItem);
			sb.append("\n");
			for (PeakAnnotationRow row : pkAnnotationTable.rows) {
				sb.append("  ").append(row.getMz()).append(" ").append(String.join(" ", row.getColumns())).append("\n");
			}
		}

		sb.append("PK$NUM_PEAK: ").append(pkPeak.size()).append("\n");
		sb.append("PK$PEAK: m/z int. rel.int.\n");
		for (Peak peak : pkPeak) {
			String intensity = String.valueOf(peak.getIntensity());
			sb.append("  ").append(peak.getMz()).append(" ").append(intensity).append(" ").append(peak.getRelIntensity()).append("\n");
		}
		sb.append("//\n");

		return sb.toString();
	}

	@SuppressWarnings("unused")
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
		sb.append("<b>CH$FORMULA:</b> <a href=\"https://pubchem.ncbi.nlm.nih.gov/#query=").append(getChFormula()).append("\" target=\"_blank\">").append(getChFormula1()).append("</a><br>\n");
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
					sb.append("<b>CH$LINK:</b> ").append(key).append(" <a href=\"https://www.hmdb.ca/metabolites/").append(value).append("\" target=\"_blank\">").append(value).append("</a><br>\n");
					break;
				case "KAPPAVIEW":
					sb.append("<b>CH$LINK:</b> ").append(key).append(" <a href=\"https://kpv.kazusa-db.jp/kpv4/compoundInformation/view.action?id=").append(value).append("\" target=\"_blank\">").append(value).append("</a><br>\n");
					break;
				case "KEGG":
					sb.append("<b>CH$LINK:</b> ").append(key).append(" <a href=\"https://www.genome.jp/dbget-bin/www_bget?cpd:").append(value).append("\" target=\"_blank\">").append(value).append("</a><br>\n");
					break;
				case "KNAPSACK":
					sb.append("<b>CH$LINK:</b> ").append(key).append(" <a href=\"https://www.knapsackfamily.com/knapsack_core/information.php?word=").append(value).append("\" target=\"_blank\">").append(value).append("</a><br>\n");
					break;
				case "LIPIDBANK":
					sb.append("<b>CH$LINK:</b> ").append(key).append(" <a href=\"https://www.google.com/search?q=lipidbank ").append(value).append("\" target=\"_blank\">").append(value).append("</a><br>\n");
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

		sb.append("<b>PK$SPLASH:</b> <a href=\"https://www.google.com/search?q=").append(getPkSPLASH()).append("\" target=\"_blank\">").append(getPkSPLASH()).append("</a><br>\n");
		if (!getPkAnnotationHeader().isEmpty()) {
			sb.append("<b>PK$ANNOTATION:</b>");
			for (String annotation_header_item : getPkAnnotationHeader())
				sb.append(" ").append(annotation_header_item);
			sb.append("<br>\n");
			for (PeakAnnotationRow row : getPkAnnotation()) {
				sb.append("&nbsp;&nbsp;").append(row.getMz()).append("&nbsp;").append(String.join("&nbsp;", row.getColumns())).append("<br>\n");
			}
		}
		sb.append("<b>PK$NUM_PEAK:</b> ").append(getPkNumPeak()).append("<br>\n");
		sb.append("<b>PK$PEAK:</b> m/z int. rel.int.<br>\n");
		for (Peak peak : getPkPeak()) {
			sb.append("&nbsp;&nbsp;").append(peak.getMz()).append("&nbsp;").append(peak.getIntensity()).append("&nbsp;").append(peak.getRelIntensity()).append("<br>\n");
		}

		sb.append("//");

		return sb.toString();
	}

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

	private String getPublicationLink() {
		String pub = getPublication();
		Matcher matcher_doi = doiPattern.matcher(pub);
		Matcher matcher_pmid = pmidPattern.matcher(pub);
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

	private static String nullIfBlank(String value) {
		if (value == null) return null;
		return value.isBlank() ? null : value;
	}


	private static boolean hasText(String value) {
		return value != null && !value.isBlank();
	}

	private static void requireText(String fieldName, String value) {
		if (!hasText(value)) {
			throw new IllegalStateException("Missing required field: " + fieldName);
		}
	}

	//https://github.com/BioSchemas/specifications/issues/198

	public JsonArray createStructuredDataJsonArray() {
		String accession = getAccession();
		String recordUrl = "https://massbank.eu/MassBank/RecordDisplay?id=" + accession;
		String primaryTitle = recordTitle.getFirst();
		String inChIKey = getChLink().stream().filter(e -> "INCHIKEY".equals(e.key())).map(KeyValue::value).findFirst().orElse(null);
		String description = "This MassBank record with Accession " + accession
				+ " contains the " + getAcMassSpectrometryMsType() + " mass spectrum of " + primaryTitle
				+ ((inChIKey == null) ? "." : " with the InChIkey " + inChIKey + ".");

		// dataset
		JsonObject dataset = new JsonObject();
		dataset.addProperty("@context", "https://schema.org");
		dataset.addProperty("@type", "Dataset");
		dataset.add("http://purl.org/dc/terms/conformsTo",
				prettyGson.fromJson("{ \"@type\": \"CreativeWork\", \"@id\": \"https://bioschemas.org/profiles/Dataset/1.0-RELEASE\" }", JsonObject.class));
		dataset.addProperty("@id", recordUrl + "#Dataset");
		dataset.addProperty("description", description);
		dataset.addProperty("identifier", accession);
		dataset.addProperty("name", getRecordTitle1());

		JsonArray keywords = new JsonArray();
		keywords.add(prettyGson.fromJson(
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
		about.addProperty("@id", recordUrl + "#ChemicalSubstance");
		dataset.add("about", about);

		dataset.addProperty("url", recordUrl);
		dataset.addProperty("datePublished", getDate1()[0].replace(".","-"));
		dataset.addProperty("citation", getPublication());

		JsonArray measurementTechnique = new JsonArray();
		measurementTechnique.add(prettyGson.fromJson(
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

		dataset.add("includedInDataCatalog", prettyGson.fromJson(
				"{\"@type\": \"DataCatalog\","
						+ "\"name\": \"MassBank\","
						+ "\"url\": \"https://massbank.eu\""
						+ "}", JsonObject.class));

		JsonObject chemicalSubstance = new JsonObject();
		chemicalSubstance.addProperty("@context", "https://schema.org");
		chemicalSubstance.addProperty("@type", "ChemicalSubstance");
		chemicalSubstance.add("http://purl.org/dc/terms/conformsTo",
				prettyGson.fromJson("{ \"@type\": \"CreativeWork\", \"@id\": \"https://bioschemas.org/profiles/ChemicalSubstance/0.4-RELEASE\" }", JsonObject.class));
		chemicalSubstance.addProperty("@id", recordUrl + "#ChemicalSubstance");
		chemicalSubstance.addProperty("identifier", accession);
		chemicalSubstance.addProperty("name", primaryTitle);
		chemicalSubstance.addProperty("url", recordUrl);
		chemicalSubstance.addProperty("chemicalComposition", getChFormula());
		if (getChName().size() == 1)  chemicalSubstance.addProperty("alternateName", getChName().getFirst());
		else if (!getChName().isEmpty()) chemicalSubstance.add("alternateName", prettyGson.toJsonTree(getChName()));

		JsonArray molecularEntities = new JsonArray();

		// create a loop in case of multiple MolecularEntity
		JsonObject molecularEntity = new JsonObject();
		molecularEntity.addProperty("@context", "https://schema.org");
		molecularEntity.addProperty("@type", "MolecularEntity");
		molecularEntity.add("http://purl.org/dc/terms/conformsTo",
				prettyGson.fromJson("{ \"@type\": \"CreativeWork\", \"@id\": \"https://bioschemas.org/profiles/MolecularEntity/0.5-RELEASE\" }", JsonObject.class));
		molecularEntity.addProperty("@id", recordUrl + "#" + (inChIKey != null ? inChIKey : "MolecularEntity"));
		molecularEntity.addProperty("identifier", accession);
		molecularEntity.addProperty("name", primaryTitle);
		molecularEntity.addProperty("url", recordUrl);
		if (!getChIUPAC().equals("N/A")) molecularEntity.addProperty("inChI", getChIUPAC());
		if (!getChSMILES().equals("N/A")) molecularEntity.addProperty("smiles", getChSMILES());
		molecularEntity.addProperty("molecularFormula", getChFormula());
		molecularEntity.addProperty("monoisotopicMolecularWeight", getChExactMass());
		if (inChIKey != null) molecularEntity.addProperty("inChIKey", inChIKey);

		molecularEntities.add(molecularEntity);
		chemicalSubstance.add("hasBioChemEntityPart", molecularEntities);

		JsonObject subjectOf = new JsonObject();
		subjectOf.addProperty("@type", "Dataset");
		subjectOf.addProperty("@id", recordUrl + "#Dataset");
		chemicalSubstance.add("subjectOf", subjectOf);

		// put MolecularEntity and Dataset together
		JsonArray structuredData = new JsonArray();
		structuredData.add(dataset);
		structuredData.add(chemicalSubstance);
		return structuredData;

	}

	@SuppressWarnings("unused")
	public String createStructuredData() {
		return prettyGson.toJson(createStructuredDataJsonArray());
	}


	public record KeyValue(String key, String value) {}

	@SuppressWarnings("unused")
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
		public List<String> getColumns() { return List.copyOf(columns); }
		public void setColumns(List<String> columns) {
			this.columns = columns == null ? new ArrayList<>() : new ArrayList<>(columns);
		}
	}

	@SuppressWarnings("unused")
	public static class PeakAnnotationTable {
		private List<String> header = new ArrayList<>();
		private List<PeakAnnotationRow> rows = new ArrayList<>();

		public PeakAnnotationTable() {}

		public List<String> getHeader() { return List.copyOf(header); }
		public void setHeader(List<String> header) {
			this.header = header == null ? new ArrayList<>() : new ArrayList<>(header);
		}
		public List<PeakAnnotationRow> getRows() {
			List<PeakAnnotationRow> copy = new ArrayList<>(rows.size());
			for (PeakAnnotationRow row : rows) {
				copy.add(new PeakAnnotationRow(row.getMz(), row.getColumns()));
			}
			return copy;
		}
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





