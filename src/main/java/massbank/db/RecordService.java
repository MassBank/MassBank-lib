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
import org.springframework.stereotype.Service;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class RecordService {
    private final RecordEntityRepository repo;
    private final Gson gson;

    public RecordService(RecordEntityRepository repo) {
        this.repo = repo;
        this.gson = new GsonBuilder()
                .registerTypeAdapter(Pair.class, new PairTypeAdapter()) // reuse adapters if accessible; else reimplement
                .registerTypeAdapter(Triple.class, new TripleTypeAdapter())
                .create();
    }

    /**
     * Custom TypeAdapter for deserializing Triple objects.
     */
    static class TripleTypeAdapter implements JsonDeserializer<Triple<?, ?, ?>>, JsonSerializer<Triple<?, ?, ?>> {
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
     * Custom TypeAdapter for deserializing Pair objects.
     */
    static class PairTypeAdapter implements JsonDeserializer<Pair<?, ?>>, JsonSerializer<Pair<?, ?>> {
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

    public void save(Record record) {
        RecordEntity e = recordToEntity(record);
        repo.save(e);
    }

    public Record findByAccession(String accession) {
        Optional<RecordEntity> opt = repo.findById(accession);
        return opt.map(this::entityToRecord).orElse(null);
    }

    public List<Record> findAll() {
        return repo.findAll().stream().map(this::entityToRecord).collect(Collectors.toList());
    }

    public boolean delete(String accession) {
        if (repo.existsById(accession)) {
            repo.deleteById(accession);
            return true;
        }
        return false;
    }

    public boolean exists(String accession) {
        return repo.existsById(accession);
    }

    public Record entityToRecord(RecordEntity e) {
        Record r = new Record();
        r.ACCESSION(e.getAccession());
        r.isDeprecated(Boolean.TRUE.equals(e.getIsDeprecated()));
        r.DEPRECATED(e.getDeprecated());
        r.DEPRECATED_CONTENT(e.getDeprecatedContent());

        Type listString = new TypeToken<List<String>>() {}.getType();
        Type mapStringString = new TypeToken<LinkedHashMap<String, String>>() {}.getType();
        Type listPair = new TypeToken<List<Pair<String, String>>>() {}.getType();
        Type pkAnnotationType = new TypeToken<List<Pair<BigDecimal, List<String>>>>() {}.getType();
        Type pkPeakType = new TypeToken<List<Triple<BigDecimal, BigDecimal, Integer>>>() {}.getType();

        r.RECORD_TITLE(gson.fromJson(e.getRecordTitleJson(), listString));
        r.DATE(e.getDate());
        r.AUTHORS(e.getAuthors());
        r.LICENSE(e.getLicense());
        r.COPYRIGHT(e.getCopyright());
        r.PUBLICATION(e.getPublication());
        r.PROJECT(e.getProject());
        r.COMMENT(gson.fromJson(e.getCommentJson(), listString));

        r.CH_NAME(gson.fromJson(e.getChNameJson(), listString));
        r.CH_COMPOUND_CLASS(gson.fromJson(e.getChCompoundClassJson(), listString));
        r.CH_FORMULA(e.getChFormula());
        r.CH_EXACT_MASS(e.getChExactMass());
        r.CH_SMILES(e.getChSmiles());
        r.CH_IUPAC(e.getChIupac());
        r.CH_LINK(gson.fromJson(e.getChLinkJson(), mapStringString));

        r.SP_SCIENTIFIC_NAME(e.getSpScientificName());
        r.SP_LINEAGE(e.getSpLineage());
        r.SP_LINK(gson.fromJson(e.getSpLinkJson(), mapStringString));
        r.SP_SAMPLE(gson.fromJson(e.getSpSampleJson(), listString));

        r.AC_INSTRUMENT(e.getAcInstrument());
        r.AC_INSTRUMENT_TYPE(e.getAcInstrumentType());
        r.AC_MASS_SPECTROMETRY_MS_TYPE(e.getAcMassSpectrometryMsType());
        r.AC_MASS_SPECTROMETRY_ION_MODE(e.getAcMassSpectrometryIonMode());
        r.AC_MASS_SPECTROMETRY(gson.fromJson(e.getAcMassSpectrometryJson(), listPair));
        r.AC_CHROMATOGRAPHY(gson.fromJson(e.getAcChromatographyJson(), listPair));

        r.MS_FOCUSED_ION(gson.fromJson(e.getMsFocusedIonJson(), listPair));
        r.MS_DATA_PROCESSING(gson.fromJson(e.getMsDataProcessingJson(), listPair));

        r.PK_SPLASH(e.getPkSplash());
        r.PK_ANNOTATION_HEADER(gson.fromJson(e.getPkAnnotationHeaderJson(), listString));

        List<Pair<BigDecimal, List<String>>> pkAnnotation = gson.fromJson(e.getPkAnnotationJson(), pkAnnotationType);
        if (pkAnnotation != null) pkAnnotation.forEach(r::PK_ANNOTATION_ADD_LINE);

        List<Triple<BigDecimal, BigDecimal, Integer>> pkPeak = gson.fromJson(e.getPkPeakJson(), pkPeakType);
        if (pkPeak != null) pkPeak.forEach(r::PK_PEAK_ADD_LINE);

        return r;
    }

    public RecordEntity recordToEntity(Record r) {
        RecordEntity e = new RecordEntity();
        e.setAccession(r.ACCESSION());
        e.setIsDeprecated(r.isDeprecated());
        e.setDeprecated(r.DEPRECATED());
        e.setDeprecatedContent(r.DEPRECATED_CONTENT());

        e.setRecordTitleJson(gson.toJson(r.RECORD_TITLE()));
        e.setDate(r.DATE());
        e.setAuthors(r.AUTHORS());
        e.setLicense(r.LICENSE());
        e.setCopyright(r.COPYRIGHT());
        e.setPublication(r.PUBLICATION());
        e.setProject(r.PROJECT());
        e.setCommentJson(gson.toJson(r.COMMENT()));

        e.setChNameJson(gson.toJson(r.CH_NAME()));
        e.setChCompoundClassJson(gson.toJson(r.CH_COMPOUND_CLASS()));
        e.setChFormula(r.CH_FORMULA());
        e.setChExactMass(r.CH_EXACT_MASS());
        e.setChSmiles(r.CH_SMILES());
        e.setChIupac(r.CH_IUPAC());
        e.setChLinkJson(gson.toJson(r.CH_LINK()));

        e.setSpScientificName(r.SP_SCIENTIFIC_NAME());
        e.setSpLineage(r.SP_LINEAGE());
        e.setSpLinkJson(gson.toJson(r.SP_LINK()));
        e.setSpSampleJson(gson.toJson(r.SP_SAMPLE()));

        e.setAcInstrument(r.AC_INSTRUMENT());
        e.setAcInstrumentType(r.AC_INSTRUMENT_TYPE());
        e.setAcMassSpectrometryMsType(r.AC_MASS_SPECTROMETRY_MS_TYPE());
        e.setAcMassSpectrometryIonMode(r.AC_MASS_SPECTROMETRY_ION_MODE());
        e.setAcMassSpectrometryJson(gson.toJson(r.AC_MASS_SPECTROMETRY()));
        e.setAcChromatographyJson(gson.toJson(r.AC_CHROMATOGRAPHY()));

        e.setMsFocusedIonJson(gson.toJson(r.MS_FOCUSED_ION()));
        e.setMsDataProcessingJson(gson.toJson(r.MS_DATA_PROCESSING()));

        e.setPkSplash(r.PK_SPLASH());
        e.setPkAnnotationHeaderJson(gson.toJson(r.PK_ANNOTATION_HEADER()));
        e.setPkAnnotationJson(gson.toJson(r.PK_ANNOTATION()));
        e.setPkPeakJson(gson.toJson(r.PK_PEAK()));

        return e;
    }
}
