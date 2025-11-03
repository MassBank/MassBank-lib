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

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import massbank.Record;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Repository class for storing and retrieving Record objects in PostgreSQL database.
 * Uses JSONB for complex data structures.
 */
public class RecordRepository {
    private static final Logger logger = LogManager.getLogger(RecordRepository.class);
    private final Connection connection;
    private final Gson gson;

    public RecordRepository(Connection connection) {
        this.connection = connection;
        this.gson = createGsonWithTypeAdapters();
    }
    
    /**
     * Creates a Gson instance with custom type adapters for Apache Commons Lang3 Pair and Triple.
     * These types are abstract and cannot be directly deserialized by Gson.
     */
    private Gson createGsonWithTypeAdapters() {
        return new GsonBuilder()
            .registerTypeAdapter(Pair.class, new PairTypeAdapter())
            .registerTypeAdapter(Triple.class, new TripleTypeAdapter())
            .create();
    }
    
    /**
     * Custom TypeAdapter for deserializing Pair objects.
     */
    private static class PairTypeAdapter implements JsonDeserializer<Pair<?, ?>>, JsonSerializer<Pair<?, ?>> {
        @Override
        public Pair<?, ?> deserialize(JsonElement json, java.lang.reflect.Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject obj = json.getAsJsonObject();
            Object left = deserializeValue(obj.get("left"));
            Object right = deserializeValue(obj.get("right"));
            return ImmutablePair.of(left, right);
        }
        
        @Override
        public JsonElement serialize(Pair<?, ?> src, java.lang.reflect.Type typeOfSrc, JsonSerializationContext context) {
            JsonObject obj = new JsonObject();
            obj.add("left", context.serialize(src.getLeft()));
            obj.add("right", context.serialize(src.getRight()));
            return obj;
        }
    }
    
    /**
     * Custom TypeAdapter for deserializing Triple objects.
     */
    private static class TripleTypeAdapter implements JsonDeserializer<Triple<?, ?, ?>>, JsonSerializer<Triple<?, ?, ?>> {
        @Override
        public Triple<?, ?, ?> deserialize(JsonElement json, java.lang.reflect.Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject obj = json.getAsJsonObject();
            Object left = deserializeValue(obj.get("left"));
            Object middle = deserializeValue(obj.get("middle"));
            Object right = deserializeValue(obj.get("right"));
            return ImmutableTriple.of(left, middle, right);
        }
        
        @Override
        public JsonElement serialize(Triple<?, ?, ?> src, java.lang.reflect.Type typeOfSrc, JsonSerializationContext context) {
            JsonObject obj = new JsonObject();
            obj.add("left", context.serialize(src.getLeft()));
            obj.add("middle", context.serialize(src.getMiddle()));
            obj.add("right", context.serialize(src.getRight()));
            return obj;
        }
    }
    
    /**
     * Helper method to deserialize JSON values, preserving numeric types appropriately.
     */
    private static Object deserializeValue(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return null;
        }
        if (element.isJsonPrimitive()) {
            JsonPrimitive primitive = element.getAsJsonPrimitive();
            if (primitive.isNumber()) {
                // Check if the number is an integer value
                BigDecimal bd = primitive.getAsBigDecimal();
                try {
                    // If it's an exact integer, return as Integer
                    if (bd.scale() <= 0 && bd.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) <= 0 
                        && bd.compareTo(BigDecimal.valueOf(Integer.MIN_VALUE)) >= 0) {
                        return bd.intValueExact();
                    }
                } catch (ArithmeticException e) {
                    // Not an exact integer, continue to return as BigDecimal
                }
                // Otherwise preserve as BigDecimal for decimal values
                return bd;
            } else if (primitive.isBoolean()) {
                return primitive.getAsBoolean();
            } else if (primitive.isString()) {
                return primitive.getAsString();
            }
        }
        // For complex types, return the element as-is
        return element;
    }

    /**
     * Stores a Record in the database.
     * @param record The record to store
     * @throws SQLException if a database error occurs
     */
    public void store(Record record) throws SQLException {
        String sql = """
            INSERT INTO records (
                accession, is_deprecated, deprecated, deprecated_content,
                record_title, date, authors, license, copyright, publication,
                project, comment, ch_name, ch_compound_class, ch_formula,
                ch_exact_mass, ch_smiles, ch_iupac, ch_link, sp_scientific_name,
                sp_lineage, sp_link, sp_sample, ac_instrument, ac_instrument_type,
                ac_mass_spectrometry_ms_type, ac_mass_spectrometry_ion_mode,
                ac_mass_spectrometry, ac_chromatography, ms_focused_ion,
                ms_data_processing, pk_splash, pk_annotation_header, pk_annotation, pk_peak
            ) VALUES (?, ?, ?, ?, CAST(? AS jsonb), ?, ?, ?, ?, ?, ?, CAST(? AS jsonb), CAST(? AS jsonb), CAST(? AS jsonb), ?, 
                     ?, ?, ?, CAST(? AS jsonb), ?, ?, CAST(? AS jsonb), CAST(? AS jsonb), ?, ?, ?, ?, CAST(? AS jsonb),
                     CAST(? AS jsonb), CAST(? AS jsonb), CAST(? AS jsonb), ?, CAST(? AS jsonb), CAST(? AS jsonb), CAST(? AS jsonb))
            ON CONFLICT (accession) DO UPDATE SET
                is_deprecated = EXCLUDED.is_deprecated,
                deprecated = EXCLUDED.deprecated,
                deprecated_content = EXCLUDED.deprecated_content,
                record_title = EXCLUDED.record_title,
                date = EXCLUDED.date,
                authors = EXCLUDED.authors,
                license = EXCLUDED.license,
                copyright = EXCLUDED.copyright,
                publication = EXCLUDED.publication,
                project = EXCLUDED.project,
                comment = EXCLUDED.comment,
                ch_name = EXCLUDED.ch_name,
                ch_compound_class = EXCLUDED.ch_compound_class,
                ch_formula = EXCLUDED.ch_formula,
                ch_exact_mass = EXCLUDED.ch_exact_mass,
                ch_smiles = EXCLUDED.ch_smiles,
                ch_iupac = EXCLUDED.ch_iupac,
                ch_link = EXCLUDED.ch_link,
                sp_scientific_name = EXCLUDED.sp_scientific_name,
                sp_lineage = EXCLUDED.sp_lineage,
                sp_link = EXCLUDED.sp_link,
                sp_sample = EXCLUDED.sp_sample,
                ac_instrument = EXCLUDED.ac_instrument,
                ac_instrument_type = EXCLUDED.ac_instrument_type,
                ac_mass_spectrometry_ms_type = EXCLUDED.ac_mass_spectrometry_ms_type,
                ac_mass_spectrometry_ion_mode = EXCLUDED.ac_mass_spectrometry_ion_mode,
                ac_mass_spectrometry = EXCLUDED.ac_mass_spectrometry,
                ac_chromatography = EXCLUDED.ac_chromatography,
                ms_focused_ion = EXCLUDED.ms_focused_ion,
                ms_data_processing = EXCLUDED.ms_data_processing,
                pk_splash = EXCLUDED.pk_splash,
                pk_annotation_header = EXCLUDED.pk_annotation_header,
                pk_annotation = EXCLUDED.pk_annotation,
                pk_peak = EXCLUDED.pk_peak
            """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, record.ACCESSION());
            stmt.setBoolean(2, record.isDeprecated());
            stmt.setString(3, record.DEPRECATED());
            stmt.setString(4, record.DEPRECATED_CONTENT());
            stmt.setString(5, gson.toJson(record.RECORD_TITLE()));
            stmt.setString(6, record.DATE());
            stmt.setString(7, record.AUTHORS());
            stmt.setString(8, record.LICENSE());
            stmt.setString(9, record.COPYRIGHT());
            stmt.setString(10, record.PUBLICATION());
            stmt.setString(11, record.PROJECT());
            stmt.setString(12, gson.toJson(record.COMMENT()));
            stmt.setString(13, gson.toJson(record.CH_NAME()));
            stmt.setString(14, gson.toJson(record.CH_COMPOUND_CLASS()));
            stmt.setString(15, record.CH_FORMULA());
            stmt.setBigDecimal(16, record.CH_EXACT_MASS());
            stmt.setString(17, record.CH_SMILES());
            stmt.setString(18, record.CH_IUPAC());
            stmt.setString(19, gson.toJson(record.CH_LINK()));
            stmt.setString(20, record.SP_SCIENTIFIC_NAME());
            stmt.setString(21, record.SP_LINEAGE());
            stmt.setString(22, gson.toJson(record.SP_LINK()));
            stmt.setString(23, gson.toJson(record.SP_SAMPLE()));
            stmt.setString(24, record.AC_INSTRUMENT());
            stmt.setString(25, record.AC_INSTRUMENT_TYPE());
            stmt.setString(26, record.AC_MASS_SPECTROMETRY_MS_TYPE());
            stmt.setString(27, record.AC_MASS_SPECTROMETRY_ION_MODE());
            stmt.setString(28, gson.toJson(record.AC_MASS_SPECTROMETRY()));
            stmt.setString(29, gson.toJson(record.AC_CHROMATOGRAPHY()));
            stmt.setString(30, gson.toJson(record.MS_FOCUSED_ION()));
            stmt.setString(31, gson.toJson(record.MS_DATA_PROCESSING()));
            stmt.setString(32, record.PK_SPLASH());
            stmt.setString(33, gson.toJson(record.PK_ANNOTATION_HEADER()));
            stmt.setString(34, gson.toJson(record.PK_ANNOTATION()));
            stmt.setString(35, gson.toJson(record.PK_PEAK()));

            stmt.executeUpdate();
            logger.info("Stored record with accession: {}", record.ACCESSION());
        }
    }

    /**
     * Retrieves a Record from the database by accession ID.
     * @param accession The accession ID of the record
     * @return The Record object, or null if not found
     * @throws SQLException if a database error occurs
     */
    public Record retrieve(String accession) throws SQLException {
        String sql = """
            SELECT accession, is_deprecated, deprecated, deprecated_content,
                   record_title, date, authors, license, copyright, publication,
                   project, comment, ch_name, ch_compound_class, ch_formula,
                   ch_exact_mass, ch_smiles, ch_iupac, ch_link, sp_scientific_name,
                   sp_lineage, sp_link, sp_sample, ac_instrument, ac_instrument_type,
                   ac_mass_spectrometry_ms_type, ac_mass_spectrometry_ion_mode,
                   ac_mass_spectrometry, ac_chromatography, ms_focused_ion,
                   ms_data_processing, pk_splash, pk_annotation_header, pk_annotation, pk_peak
            FROM records
            WHERE accession = ?
            """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, accession);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return buildRecordFromResultSet(rs);
                }
            }
        }
        
        logger.warn("Record not found with accession: {}", accession);
        return null;
    }

    /**
     * Retrieves all Records from the database.
     * @return List of all Record objects
     * @throws SQLException if a database error occurs
     */
    public List<Record> retrieveAll() throws SQLException {
        List<Record> records = new ArrayList<>();
        String sql = """
            SELECT accession, is_deprecated, deprecated, deprecated_content,
                   record_title, date, authors, license, copyright, publication,
                   project, comment, ch_name, ch_compound_class, ch_formula,
                   ch_exact_mass, ch_smiles, ch_iupac, ch_link, sp_scientific_name,
                   sp_lineage, sp_link, sp_sample, ac_instrument, ac_instrument_type,
                   ac_mass_spectrometry_ms_type, ac_mass_spectrometry_ion_mode,
                   ac_mass_spectrometry, ac_chromatography, ms_focused_ion,
                   ms_data_processing, pk_splash, pk_annotation_header, pk_annotation, pk_peak
            FROM records
            """;

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                records.add(buildRecordFromResultSet(rs));
            }
        }
        
        logger.info("Retrieved {} records from database", records.size());
        return records;
    }

    /**
     * Deletes a Record from the database by accession ID.
     * @param accession The accession ID of the record to delete
     * @return true if a record was deleted, false otherwise
     * @throws SQLException if a database error occurs
     */
    public boolean delete(String accession) throws SQLException {
        String sql = "DELETE FROM records WHERE accession = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, accession);
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0) {
                logger.info("Deleted record with accession: {}", accession);
                return true;
            } else {
                logger.warn("No record found to delete with accession: {}", accession);
                return false;
            }
        }
    }

    /**
     * Checks if a Record exists in the database.
     * @param accession The accession ID to check
     * @return true if the record exists, false otherwise
     * @throws SQLException if a database error occurs
     */
    public boolean exists(String accession) throws SQLException {
        String sql = "SELECT 1 FROM records WHERE accession = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, accession);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Builds a Record object from a ResultSet.
     * @param rs The ResultSet positioned at a record row
     * @return The constructed Record object
     * @throws SQLException if a database error occurs
     */
    private Record buildRecordFromResultSet(ResultSet rs) throws SQLException {
        Record record = new Record();
        
        record.ACCESSION(rs.getString("accession"));
        record.isDeprecated(rs.getBoolean("is_deprecated"));
        record.DEPRECATED(rs.getString("deprecated"));
        record.DEPRECATED_CONTENT(rs.getString("deprecated_content"));
        
        record.RECORD_TITLE(gson.fromJson(rs.getString("record_title"), new TypeToken<List<String>>(){}.getType()));
        record.DATE(rs.getString("date"));
        record.AUTHORS(rs.getString("authors"));
        record.LICENSE(rs.getString("license"));
        record.COPYRIGHT(rs.getString("copyright"));
        record.PUBLICATION(rs.getString("publication"));
        record.PROJECT(rs.getString("project"));
        record.COMMENT(gson.fromJson(rs.getString("comment"), new TypeToken<List<String>>(){}.getType()));
        
        record.CH_NAME(gson.fromJson(rs.getString("ch_name"), new TypeToken<List<String>>(){}.getType()));
        record.CH_COMPOUND_CLASS(gson.fromJson(rs.getString("ch_compound_class"), new TypeToken<List<String>>(){}.getType()));
        record.CH_FORMULA(rs.getString("ch_formula"));
        record.CH_EXACT_MASS(rs.getBigDecimal("ch_exact_mass"));
        record.CH_SMILES(rs.getString("ch_smiles"));
        record.CH_IUPAC(rs.getString("ch_iupac"));
        record.CH_LINK(gson.fromJson(rs.getString("ch_link"), new TypeToken<LinkedHashMap<String, String>>(){}.getType()));
        
        record.SP_SCIENTIFIC_NAME(rs.getString("sp_scientific_name"));
        record.SP_LINEAGE(rs.getString("sp_lineage"));
        record.SP_LINK(gson.fromJson(rs.getString("sp_link"), new TypeToken<LinkedHashMap<String, String>>(){}.getType()));
        record.SP_SAMPLE(gson.fromJson(rs.getString("sp_sample"), new TypeToken<List<String>>(){}.getType()));
        
        record.AC_INSTRUMENT(rs.getString("ac_instrument"));
        record.AC_INSTRUMENT_TYPE(rs.getString("ac_instrument_type"));
        record.AC_MASS_SPECTROMETRY_MS_TYPE(rs.getString("ac_mass_spectrometry_ms_type"));
        record.AC_MASS_SPECTROMETRY_ION_MODE(rs.getString("ac_mass_spectrometry_ion_mode"));
        record.AC_MASS_SPECTROMETRY(gson.fromJson(rs.getString("ac_mass_spectrometry"), new TypeToken<List<Pair<String, String>>>(){}.getType()));
        record.AC_CHROMATOGRAPHY(gson.fromJson(rs.getString("ac_chromatography"), new TypeToken<List<Pair<String, String>>>(){}.getType()));
        
        record.MS_FOCUSED_ION(gson.fromJson(rs.getString("ms_focused_ion"), new TypeToken<List<Pair<String, String>>>(){}.getType()));
        record.MS_DATA_PROCESSING(gson.fromJson(rs.getString("ms_data_processing"), new TypeToken<List<Pair<String, String>>>(){}.getType()));
        
        record.PK_SPLASH(rs.getString("pk_splash"));
        record.PK_ANNOTATION_HEADER(gson.fromJson(rs.getString("pk_annotation_header"), new TypeToken<List<String>>(){}.getType()));
        
        List<Pair<BigDecimal, List<String>>> pkAnnotation = gson.fromJson(
            rs.getString("pk_annotation"), 
            new TypeToken<List<Pair<BigDecimal, List<String>>>>(){}.getType()
        );
        if (pkAnnotation != null) {
            for (Pair<BigDecimal, List<String>> annotation : pkAnnotation) {
                record.PK_ANNOTATION_ADD_LINE(annotation);
            }
        }
        
        List<Triple<BigDecimal, BigDecimal, Integer>> pkPeak = gson.fromJson(
            rs.getString("pk_peak"),
            new TypeToken<List<Triple<BigDecimal, BigDecimal, Integer>>>(){}.getType()
        );
        if (pkPeak != null) {
            for (Triple<BigDecimal, BigDecimal, Integer> peak : pkPeak) {
                record.PK_PEAK_ADD_LINE(peak);
            }
        }
        
        return record;
    }
}
