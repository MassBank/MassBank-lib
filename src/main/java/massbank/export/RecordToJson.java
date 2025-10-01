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
import massbank.Record;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
		LinkedHashMap<String, String> CH$LINK; // optional
		String SP$SCIENTIFIC_NAME; // optional
		String SP$LINEAGE; // optional
		LinkedHashMap<String, String> SP$LINK; // optional
		List<String> SP$SAMPLE; // optional
		String AC$INSTRUMENT;
		String AC$INSTRUMENT_TYPE;
		String AC$MASS_SPECTROMETRY_MS_TYPE;
		String AC$MASS_SPECTROMETRY_ION_MODE;
        List<Pair<String, String>> AC$MASS_SPECTROMETRY; // optional
        List<Pair<String, String>> AC$CHROMATOGRAPHY; // optional
        List<Pair<String, String>> MS$FOCUSED_ION; // optional
        List<Pair<String, String>> MS$DATA_PROCESSING; // optional
		String PK$SPLASH;
		List<List<String>> PK$ANNOTATION; // optional
		Integer PK$NUM_PEAK;
		List<List<String>> PK$PEAK;

		RecordJsonSerializer(Record record) {
			ACCESSION = record.ACCESSION();
			if (record.isDeprecated()) {
                isDeprecated = Boolean.TRUE;
                DEPRECATED = record.DEPRECATED();
				DEPRECATED_CONTENT = record.DEPRECATED_CONTENT();
			} else {
				RECORD_TITLE = record.RECORD_TITLE();
				DATE = record.DATE();
				AUTHORS = record.AUTHORS();
				LICENSE = record.LICENSE();
				COPYRIGHT = "".equals(record.COPYRIGHT()) ? null : record.COPYRIGHT();
				PUBLICATION = "".equals(record.PUBLICATION()) ? null : record.PUBLICATION();
				PROJECT = "".equals(record.PROJECT()) ? null : record.PROJECT();
				COMMENT = record.COMMENT().isEmpty() ? null : record.COMMENT();
				CH$NAME = record.CH_NAME();
				CH$COMPOUND_CLASS = record.CH_COMPOUND_CLASS();
				CH$FORMULA = record.CH_FORMULA();
				CH$EXACT_MASS = record.CH_EXACT_MASS();
				CH$SMILES = record.CH_SMILES();
				CH$IUPAC = record.CH_IUPAC();
				CH$LINK = record.CH_LINK().isEmpty() ? null : record.CH_LINK();
				SP$SCIENTIFIC_NAME = "".equals(record.SP_SCIENTIFIC_NAME()) ? null : record.SP_SCIENTIFIC_NAME();
				SP$LINEAGE = "".equals(record.SP_LINEAGE()) ? null : record.SP_LINEAGE();
				SP$LINK = record.SP_LINK().isEmpty() ? null : record.SP_LINK();
				SP$SAMPLE = record.SP_SAMPLE().isEmpty() ? null : record.SP_SAMPLE();
				AC$INSTRUMENT = record.AC_INSTRUMENT();
				AC$INSTRUMENT_TYPE = record.AC_INSTRUMENT_TYPE();
				AC$MASS_SPECTROMETRY_MS_TYPE = record.AC_MASS_SPECTROMETRY_MS_TYPE();
				AC$MASS_SPECTROMETRY_ION_MODE = record.AC_MASS_SPECTROMETRY_ION_MODE();
				if (record.AC_MASS_SPECTROMETRY().isEmpty())
					AC$MASS_SPECTROMETRY = null;
				else {
					AC$MASS_SPECTROMETRY = record.AC_MASS_SPECTROMETRY();
				}
				if (record.AC_CHROMATOGRAPHY().isEmpty())
					AC$CHROMATOGRAPHY = null;
				else {
					AC$CHROMATOGRAPHY = record.AC_CHROMATOGRAPHY();
				}
				if (record.MS_FOCUSED_ION().isEmpty())
					MS$FOCUSED_ION = null;
				else {
					MS$FOCUSED_ION = record.MS_FOCUSED_ION();
				}
				if (record.MS_DATA_PROCESSING().isEmpty())
					MS$DATA_PROCESSING = null;
				else {
					MS$DATA_PROCESSING = record.MS_DATA_PROCESSING();
				}
				PK$SPLASH = record.PK_SPLASH();
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
				for (Triple<BigDecimal, BigDecimal, Integer> triple : record.PK_PEAK()) {
					List<String> peakLine = new ArrayList<String>();
					peakLine.add(triple.getLeft().toString());
					peakLine.add(triple.getMiddle().toString());
					peakLine.add(triple.getRight().toString());
					PK$PEAK.add(peakLine);
				}
			}
		}
	}

	public static String convert(Record record) {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(new RecordJsonSerializer(record));
	}

	public static String convert(List<Record> records) {
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
		// collect data
		String recordJson = convert(records);
		
		BufferedWriter writer;
		try {
			writer = new BufferedWriter(new FileWriter(file));
			writer.write(recordJson);
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
