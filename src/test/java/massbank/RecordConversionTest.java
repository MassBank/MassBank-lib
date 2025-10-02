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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static massbank.RecordParserTest.*;
import static org.junit.jupiter.api.Assertions.*;

class RecordConversionTest {

    @Test
    void testRecordToNIST_MSP() throws IOException {
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
    void testRecordToMultipleNIST_MSP() throws IOException {
        ParseResult res1 = parseRecord("MSBNK-IPB_Halle-PB000122.txt");
        ParseResult res2 = parseRecord("MSBNK-test-TST00001.txt");
        List<Record> records = new ArrayList<>();
        records.add(res1.result().get());
        records.add(res2.result().get());
        String nist = """
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

Name: Fiscalin C
DB#: MSBNK-test-TST00001
InChI: InChI=1S/C27H29N5O4/c1-14(2)20-21-28-17-11-7-5-9-15(17)23(34)31(21)19(22(33)29-20)13-27(36)16-10-6-8-12-18(16)32-24(27)30-26(3,4)25(32)35/h5-12,14,19-20,24,30,36H,13H2,1-4H3,(H,29,33)/t19-,20+,24-,27-/m1/s1
SMILES: CC(C)[C@H]1C2=NC3=CC=CC=C3C(=O)N2[C@@H](C(=O)N1)C[C@@]4([C@@H]5NC(C(=O)N5C6=CC=CC=C64)(C)C)O
Spectrum_type: MS2
Instrument_type: LC-ESI-ITFT
Instrument: Q-Exactive Orbitrap Thermo Scientific
Ion_mode: POSITIVE
Formula: C27H29N5O4
MW: 487
ExactMass: 487.22194
Comments: Parent=-1
Splash: splash10-03di-0290000000-8035e4fe85235c78b955
Num Peaks: 3
185.1073 312
213.1022 999
258.1237 222

""";
        assertEquals(nist, RecordToNIST_MSP.convertRecords(records));
    }

    @Test
    void testRecordToRIKEN_MSP() throws IOException {
        RecordParserTest.ParseResult res = parseRecord("MSBNK-IPB_Halle-PB000122.txt");
        assertTrue(res.result().isSuccess());
        String RIKENMspPB000122 = """
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
        assertEquals(RIKENMspPB000122, RecordToRIKEN_MSP.convert(res.result().get()));
    }

    @Test
    void testRecordToMultipleRIKEN_MSP() throws IOException {
        ParseResult res1 = parseRecord("MSBNK-IPB_Halle-PB000122.txt");
        ParseResult res2 = parseRecord("MSBNK-test-TST00001.txt");
        List<Record> records = new ArrayList<>();
        records.add(res1.result().get());
        records.add(res2.result().get());
        String nist = """
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

NAME: Fiscalin C
PRECURSORMZ:\s
PRECURSORTYPE: NA
FORMULA: C27H29N5O4
INCHIKEY: N/A
INCHI: InChI=1S/C27H29N5O4/c1-14(2)20-21-28-17-11-7-5-9-15(17)23(34)31(21)19(22(33)29-20)13-27(36)16-10-6-8-12-18(16)32-24(27)30-26(3,4)25(32)35/h5-12,14,19-20,24,30,36H,13H2,1-4H3,(H,29,33)/t19-,20+,24-,27-/m1/s1
SMILES: CC(C)[C@H]1C2=NC3=CC=CC=C3C(=O)N2[C@@H](C(=O)N1)C[C@@]4([C@@H]5NC(C(=O)N5C6=CC=CC=C64)(C)C)O
RETENTIONTIME: 0
INSTRUMENTTYPE: LC-ESI-ITFT
INSTRUMENT: Q-Exactive Orbitrap Thermo Scientific
IONMODE: Positive
LINKS:\s
Comment: DB#=MSBNK-test-TST00001; origin=MassBank
Splash: splash10-03di-0290000000-8035e4fe85235c78b955
Num Peaks: 3
185.1073	73653728.0
213.1022	235010720.0
258.1237	52446636.0

""";
        assertEquals(nist, RecordToRIKEN_MSP.convertRecords(records));
    }




}