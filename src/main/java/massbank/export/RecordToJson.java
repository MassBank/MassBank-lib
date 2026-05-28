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
package massbank.export;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import massbank.AbstractRecord;
import massbank.Peak;
import massbank.Record;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Convert Record structure to json.
 * 
 * @author rmeier
 * @version 09-06-2022
 */
public class RecordToJson {
	private static final Logger logger = LogManager.getLogger(RecordToJson.class);

	public static class RecordJsonSerializer {
		String ACCESSION;
        Boolean isDeprecated;
        String DEPRECATED;
		String DEPRECATED_CONTENT;
		List<String> RECORD_TITLE;
		String DATE;
		String AUTHORS;
		String LICENSE;
		String COPYRIGHT; // optional
		String PUBLICATION; // optional
		String PROJECT; // optional
		List<String> COMMENT; // optional
		List<String> CH$NAME;
		List<String> CH$COMPOUND_CLASS;
		String CH$FORMULA;
		BigDecimal CH$EXACT_MASS;
		String CH$SMILES;
		String CH$IUPAC;
		List<Record.KeyValue> CH$LINK; // optional
		String SP$SCIENTIFIC_NAME; // optional
		String SP$LINEAGE; // optional
		List<Record.KeyValue> SP$LINK; // optional
		List<String> SP$SAMPLE; // optional
		String AC$INSTRUMENT;
		String AC$INSTRUMENT_TYPE;
		String AC$MASS_SPECTROMETRY_MS_TYPE;
		String AC$MASS_SPECTROMETRY_ION_MODE;
		List<Record.KeyValue> AC$MASS_SPECTROMETRY; // optional
		List<Record.KeyValue> AC$CHROMATOGRAPHY; // optional
		List<Record.KeyValue> MS$FOCUSED_ION; // optional
		List<Record.KeyValue> MS$DATA_PROCESSING; // optional
		String PK$SPLASH;
		List<List<String>> PK$ANNOTATION; // optional
		Integer PK$NUM_PEAK;
		List<List<String>> PK$PEAK;

		RecordJsonSerializer(AbstractRecord abstractRecord) {
			ACCESSION = abstractRecord.getAccession();
			if (abstractRecord instanceof massbank.DeprecatedRecord) {
				massbank.DeprecatedRecord dr = (massbank.DeprecatedRecord) abstractRecord;
				isDeprecated = Boolean.TRUE;
				DEPRECATED = dr.getDeprecated();
				DEPRECATED_CONTENT = dr.getDeprecatedContent();
			} else {
				Record record =  (Record) abstractRecord;
				RECORD_TITLE = record.getRecordTitle();
				DATE = record.getDate();
				AUTHORS = record.getAuthors();
				LICENSE = record.getLicense();
				COPYRIGHT = "".equals(record.getCopyright()) ? null : record.getCopyright();
				PUBLICATION = "".equals(record.getPublication()) ? null : record.getPublication();
				PROJECT = "".equals(record.getProject()) ? null : record.getProject();
				COMMENT = record.getComment().isEmpty() ? null : record.getComment();
				CH$NAME = record.getChName();
				CH$COMPOUND_CLASS = record.getChCompoundClass();
				CH$FORMULA = record.getChFormula();
				CH$EXACT_MASS = record.getChExactMass();
				CH$SMILES = record.getChSMILES();
				CH$IUPAC = record.getChIUPAC();
				CH$LINK = record.getChLink().isEmpty() ? null : record.getChLink();
				SP$SCIENTIFIC_NAME = "".equals(record.getSpScientificName()) ? null : record.getSpScientificName();
				SP$LINEAGE = "".equals(record.getSpLineage()) ? null : record.getSpLineage();
				SP$LINK = record.getSpLink().isEmpty() ? null : record.getSpLink();
				SP$SAMPLE = record.getSpSample().isEmpty() ? null : record.getSpSample();
				AC$INSTRUMENT = record.getAcInstrument();
				AC$INSTRUMENT_TYPE = record.getAcInstrumentType();
				AC$MASS_SPECTROMETRY_MS_TYPE = record.getAcMassSpectrometryMsType();
				AC$MASS_SPECTROMETRY_ION_MODE = record.getAcMassSpectrometryIonMode();
				AC$MASS_SPECTROMETRY = record.getAcMassSpectrometry();
				AC$CHROMATOGRAPHY = record.getAcChromatography();
				MS$FOCUSED_ION = record.getMsFocusedIon();
				MS$DATA_PROCESSING = record.getMsDataProcessing();
				PK$SPLASH = record.getPkSPLASH();
				if (record.PK_ANNOTATION().isEmpty())
					PK$ANNOTATION = null;
				else {
					PK$ANNOTATION = new ArrayList<List<String>>();
					PK$ANNOTATION.add(record.PK_ANNOTATION_HEADER());
					for (Pair<BigDecimal, List<String>> pair : record.PK_ANNOTATION()) {
						List<String> annotationLine = new ArrayList<String>();
						annotationLine.add(pair.getKey().toString());
						annotationLine.addAll(pair.getValue());
						PK$ANNOTATION.add(annotationLine);
					}
				}
				PK$NUM_PEAK = record.PK_NUM_PEAK();
				PK$PEAK = new ArrayList<List<String>>();
							   for (Peak peak : record.getPkPeak()) {
								   List<String> peakLine = new ArrayList<String>();
								   peakLine.add(peak.getMz().toString());
								   peakLine.add(peak.getIntensity().toString());
								   peakLine.add(peak.getRelIntensity().toString());
								   PK$PEAK.add(peakLine);
							   }
			}
		}
	}

	public static String convert(Record record) {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(new RecordJsonSerializer(record));
	}

	public static String convertRecords(List<Record> records) {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		List<RecordJsonSerializer> recordJsonSerializers = records.stream().map(RecordJsonSerializer::new).collect(Collectors.toList());
        return gson.toJson(recordJsonSerializers);
	}

	/**
	 * A wrapper to convert multiple Records and write to file.
	 * 
	 * @param file    to write
	 * @param records to convert
     */
	public static void recordsToJson(File file, List<Record> records) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(convertRecords(records));
        } catch (IOException e) {
            e.printStackTrace();
        }
	}

}
