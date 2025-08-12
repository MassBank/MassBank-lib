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

import massbank.export.RecordToNIST_MSP;
import massbank.export.RecordToRIKEN_MSP;
import org.junit.jupiter.api.Test;
import org.petitparser.context.Result;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RecordTest {

    @Test
    void testNormalRecord() throws IOException {
        ParseResult res = parseRecord("MSBNK-IPB_Halle-PB000122.txt");
        assertTrue(res.result().isSuccess());
        assertEquals("MSBNK-IPB_Halle-PB000122", ((Record) res.result().get()).ACCESSION());
        assertEquals("Naringenin; LC-ESI-QTOF; MS2; CE:15 eV; [M+H]+", ((Record) res.result().get()).RECORD_TITLE1());
        assertEquals(res.content(), res.result().get().toString());
    }

    @Test
    void testDeprecatedRecord() throws IOException {
        ParseResult res = parseRecord("MSBNK-LCSB-LU092805.txt");
        assertTrue(res.result().isSuccess());
        assertEquals("MSBNK-LCSB-LU092805", ((Record) res.result().get()).ACCESSION());
        assertEquals(res.content(), res.result().get().toString());
    }

    @Test
    void testRecordToNIST_MSPTest() throws IOException {
        ParseResult res = parseRecord("MSBNK-IPB_Halle-PB000122.txt");
        assertTrue(res.result().isSuccess());
        String nistMspPB000122 = """
Name: Naringenin
Synon: 5,7-dihydroxy-2-(4-hydroxyphenyl)chroman-4-one
DB#: MSBNK-IPB_Halle-PB000122
InChIKey: FTVWIRXFELQLPI-ZDUSSCGKSA-N
InChI: InChI=1S/C15H12O5/c16-9-3-1-8(2-4-9)13-7-12(19)15-11(18)5-10(17)6-14(15)20-13/h1-6,13,16-18H,7H2/t13-/m0/s1
SMILES: C1[C@H](OC2=CC(=CC(=C2C1=O)O)O)C3=CC=C(C=C3)O
Precursor_type: [M+H]+
Spectrum_type: MS2
Instrument_type: LC-ESI-QTOF
Instrument: API QSTAR Pulsar i
Ion_mode: POSITIVE
Collision_energy: 15 eV
Formula: C15H12O5
MW: 272
ExactMass: 272.06847
Comments: Parent=-1
Splash: splash10-00di-0090000000-ed08d01208992e5a7a9f
Num Peaks: 4
147.044 20
153.019 30
273.076 999
274.083 30

""";
        assertEquals(nistMspPB000122, RecordToNIST_MSP.convert(res.result().get()));
    }

    @Test
    void testRecordToRIKEN_MSPTest() throws IOException {
        ParseResult res = parseRecord("MSBNK-IPB_Halle-PB000122.txt");
        assertTrue(res.result().isSuccess());
        String nistMspPB000122 = """
NAME: Naringenin
PRECURSORMZ:\s
PRECURSORTYPE: [M+H]+
FORMULA: C15H12O5
Ontology: Flavans
INCHIKEY: FTVWIRXFELQLPI-ZDUSSCGKSA-N
INCHI: InChI=1S/C15H12O5/c16-9-3-1-8(2-4-9)13-7-12(19)15-11(18)5-10(17)6-14(15)20-13/h1-6,13,16-18H,7H2/t13-/m0/s1
SMILES: C1[C@H](OC2=CC(=CC(=C2C1=O)O)O)C3=CC=C(C=C3)O
RETENTIONTIME: 0
INSTRUMENTTYPE: LC-ESI-QTOF
INSTRUMENT: API QSTAR Pulsar i
IONMODE: Positive
LINKS: INCHIKEY:FTVWIRXFELQLPI-ZDUSSCGKSA-N; KEGG:C00509; PUBCHEM:CID:439246; COMPTOX:DTXSID1022392
Comment: DB#=MSBNK-IPB_Halle-PB000122; origin=MassBank; IPB_RECORD: 83; Annotation confident structure
Splash: splash10-00di-0090000000-ed08d01208992e5a7a9f
Num Peaks: 4
147.044	218.845
153.019	316.545
273.076	10000.000
274.083	318.003

""";
        assertEquals(nistMspPB000122, RecordToRIKEN_MSP.convert(res.result().get()));
    }



    // just a record
    public record ParseResult(Result result, String content) {}

    //parse a file from the resource folder
    ParseResult parseRecord(String filename) throws IOException {
        Set<String> config = new HashSet<>();
        config.add("validate");
        RecordParser recordparser = new RecordParser(config);
        InputStream is = getClass().getClassLoader().getResourceAsStream(filename);
        assertNotNull(is);
        String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        Result result = recordparser.parse(content);
        return new ParseResult(result, content);
    }
}