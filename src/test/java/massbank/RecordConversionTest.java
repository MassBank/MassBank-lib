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

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import massbank.export.RecordToJson;
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
        ParseResult res3 = parseRecord("MSBNK-IPB_Halle-PB001341.txt");
        List<Record> records = new ArrayList<>();
        records.add(res1.result().get());
        records.add(res2.result().get());
        records.add(res3.result().get());
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

Name: Rutin
Synon: 2-(3,4-dihydroxyphenyl)-5,7-dihydroxy-3-[(2S,3R,4S,5S,6R)-3,4,5-trihydroxy-6-[[(2R,3R,4R,5R,6S)-3,4,5-trihydroxy-6-methyloxan-2-yl]oxymethyl]oxan-2-yl]oxychromen-4-one
DB#: MSBNK-IPB_Halle-PB001341
InChIKey: IKGXIBQEEMLURG-NVPNHPEKSA-N
InChI: InChI=1S/C27H30O16/c1-8-17(32)20(35)22(37)26(40-8)39-7-15-18(33)21(36)23(38)27(42-15)43-25-19(34)16-13(31)5-10(28)6-14(16)41-24(25)9-2-3-11(29)12(30)4-9/h2-6,8,15,17-18,20-23,26-33,35-38H,7H2,1H3/t8-,15+,17-,18+,20+,21-,22+,23+,26+,27-/m0/s1
SMILES: C[C@H]1[C@@H]([C@H]([C@H]([C@@H](O1)OC[C@@H]2[C@H]([C@@H]([C@H]([C@@H](O2)OC3=C(OC4=CC(=CC(=C4C3=O)O)O)C5=CC(=C(C=C5)O)O)O)O)O)O)O)O
Precursor_type: [M+H]+
Spectrum_type: MS2
Instrument_type: LC-ESI-QTOF
Instrument: API QSTAR Pulsar i
Ion_mode: POSITIVE
Collision_energy: 10 eV
Formula: C27H30O16
MW: 610
ExactMass: 610.15338
Comments: Parent=-1
Splash: splash10-0wmi-0009506000-98ca7f7c8f3072af4481
Num Peaks: 5
147.063 11
303.050 999
449.108 64
465.102 587
611.161 669

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
        ParseResult res3 = parseRecord("MSBNK-test-TST00002.txt");
        assertTrue(res3.result().isSuccess());
        List<Record> records = new ArrayList<>();
        records.add(res1.result().get());
        records.add(res2.result().get());
        records.add(res3.result().get());
        String expectedMsp = """
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

NAME: Disialoganglioside GD1a
PRECURSORMZ: 324.207
PRECURSORTYPE: [M+H]+
FORMULA: C86H152N4O39
INCHIKEY: UPMLUBZFFWELOX-IOFMCULOSA-N
INCHI: InChI=1S/C86H152N4O39/c1-6-8-10-12-14-16-18-20-21-22-23-25-27-29-31-33-35-37-60(105)90-50(51(100)36-34-32-30-28-26-24-19-17-15-13-11-9-7-2)46-118-80-69(111)68(110)72(58(44-95)121-80)123-82-71(113)78(129-86(84(116)117)39-53(102)62(88-48(4)98)76(127-86)65(107)55(104)41-92)73(59(45-96)122-82)124-79-63(89-49(5)99)74(66(108)56(42-93)119-79)125-81-70(112)77(67(109)57(43-94)120-81)128-85(83(114)115)38-52(101)61(87-47(3)97)75(126-85)64(106)54(103)40-91/h34,36,50-59,61-82,91-96,100-104,106-113H,6-33,35,37-46H2,1-5H3,(H,87,97)(H,88,98)(H,89,99)(H,90,105)(H,114,115)(H,116,117)/b36-34+/t50-,51+,52-,53-,54+,55+,56+,57+,58+,59+,61+,62+,63+,64?,65?,66-,67-,68+,69+,70+,71+,72+,73-,74+,75+,76+,77-,78+,79-,80+,81-,82-,85-,86-/m0/s1
SMILES: CCCCCCCCCCCCCCCCCCCC(=O)N[C@@H](CO[C@H]1[C@@H]([C@H]([C@@H]([C@H](O1)CO)O[C@H]2[C@@H]([C@H]([C@H]([C@H](O2)CO)O[C@H]3[C@@H]([C@H]([C@H]([C@H](O3)CO)O)O[C@H]4[C@@H]([C@H]([C@H]([C@H](O4)CO)O)O[C@@]5(C[C@@H]([C@H]([C@@H](O5)C([C@@H](CO)O)O)NC(=O)C)O)C(=O)O)O)NC(=O)C)O[C@@]6(C[C@@H]([C@H]([C@@H](O6)C([C@@H](CO)O)O)NC(=O)C)O)C(=O)O)O)O)O)[C@@H](/C=C/CCCCCCCCCCCCC)O
RETENTIONTIME: 5.680 min
INSTRUMENTTYPE: MALDI-TOF
INSTRUMENT: Voyager DE-PRO, Applied Biosystems
IONMODE: Positive
LINKS: INCHIKEY:UPMLUBZFFWELOX-IOFMCULOSA-N; PUBCHEM:CID:137628549
Comment: DB#=MSBNK-test-TST00002; origin=MassBank; [Chemical] The ceramide part has 38 carbons and 1 double bond (38:1), but its structure cannot be decided.  The displaying structure is GD1a d18:1-20:0.; Profile spectrum of this record is given as a JPEG file.; [Profile] MCH00001.jpg
Splash: splash10-03dr-0000010098-a1f70871442a6d6662d3
Num Peaks: 84
1278.12	205
1279.11	177
1279.18	186
1306.21	242
1309.13	168
1366.03	162
1375.23	291
1527.60	181
1569.08	187
1591.14	198
1595.21	177
1597.12	261
1598.11	228
1599.07	185
1620.30	272
1620.80	266
1621.18	367
1843.21	198
1860.16	905
1861.17	822
1862.18	531
1863.19	284
1864.22	219
1866.26	183
1867.23	162
1871.18	168
1876.14	297
1877.14	280
1878.13	192
1879.19	165
1882.18	497
1883.18	545
1884.18	349
1885.20	242
1886.19	181
1888.20	1311
1889.21	1127
1890.21	844
1891.22	413
1892.24	240
1893.23	185
1894.24	175
1895.21	146
1897.14	165
1898.15	320
1899.16	260
1900.17	218
1902.16	191
1903.20	188
1904.19	574
1905.20	509
1906.20	383
1907.23	222
1908.19	186
1910.21	795
1911.21	674
1912.21	478
1913.22	264
1914.21	194
1915.20	187
1920.18	284
1921.21	215
1922.19	179
1926.20	368
1927.19	363
1928.20	254
1929.21	207
1930.20	187
1931.32	170
1932.22	532
1933.23	495
1934.23	352
1935.22	261
1943.20	194
1948.24	274
1949.22	255
1950.22	226
1960.26	151
1965.23	185
1971.32	168
2064.29	176
2091.32	180
2092.30	206
2136.33	162

""";
        assertEquals(expectedMsp, RecordToRIKEN_MSP.convertRecords(records));
    }

    @Test
    void testRecordToJSON() throws IOException {
        RecordParserTest.ParseResult res = parseRecord("MSBNK-IPB_Halle-PB000122.txt");
        assertTrue(res.result().isSuccess());
        String JsonPB000122 = """
{
  "ACCESSION": "MSBNK-IPB_Halle-PB000122",
  "RECORD_TITLE": [
    "Naringenin",
    "LC-ESI-QTOF",
    "MS2",
    "CE:15 eV",
    "[M+H]+"
  ],
  "DATE": "2016.01.19 (Created 2008.01.02, modified 2013.06.04)",
  "AUTHORS": "Boettcher C, Institute of Plant Biochemistry, Halle, Germany",
  "LICENSE": "CC BY-SA",
  "COMMENT": [
    "IPB_RECORD: 83",
    "CONFIDENCE confident structure"
  ],
  "CH$NAME": [
    "Naringenin",
    "5,7-dihydroxy-2-(4-hydroxyphenyl)chroman-4-one"
  ],
  "CH$COMPOUND_CLASS": [
    "Natural Product",
    "Flavanone"
  ],
  "CH$FORMULA": "C15H12O5",
  "CH$EXACT_MASS": 272.06847,
  "CH$SMILES": "C1[C@H](OC2\\u003dCC(\\u003dCC(\\u003dC2C1\\u003dO)O)O)C3\\u003dCC\\u003dC(C\\u003dC3)O",
  "CH$IUPAC": "InChI\\u003d1S/C15H12O5/c16-9-3-1-8(2-4-9)13-7-12(19)15-11(18)5-10(17)6-14(15)20-13/h1-6,13,16-18H,7H2/t13-/m0/s1",
  "CH$LINK": [
    {"key":"INCHIKEY","value":"FTVWIRXFELQLPI-ZDUSSCGKSA-N"},
    {"key":"KEGG","value":"C00509"},
    {"key":"PUBCHEM","value":"CID:439246"},
    {"key":"COMPTOX","value":"DTXSID1022392"},
    {"key":"ChemOnt","value":"CHEMONTID:0000337; Organic compounds; Phenylpropanoids and polyketides; Flavonoids; Flavans"}
   ],
  "AC$INSTRUMENT": "API QSTAR Pulsar i",
  "AC$INSTRUMENT_TYPE": "LC-ESI-QTOF",
  "AC$MASS_SPECTROMETRY_MS_TYPE": "MS2",
  "AC$MASS_SPECTROMETRY_ION_MODE": "POSITIVE",
  "AC$MASS_SPECTROMETRY": [{"key":"COLLISION_ENERGY","value":"15 eV"},{"key":"IONIZATION","value":"ESI"}],
  "AC$CHROMATOGRAPHY":[],
  "MS$FOCUSED_ION":[
    {"key":"PRECURSOR_TYPE","value":"[M+H]+"}
  ],
  "MS$DATA_PROCESSING":[],
  "PK$SPLASH": "splash10-00di-0090000000-ed08d01208992e5a7a9f",
  "PK$NUM_PEAK": 4,
  "PK$PEAK": [
    [
      "147.044",
      "218.845",
      "20"
    ],
    [
      "153.019",
      "316.545",
      "30"
    ],
    [
      "273.076",
      "10000.000",
      "999"
    ],
    [
      "274.083",
      "318.003",
      "30"
    ]
  ]
}
""";
        JsonElement expected = JsonParser.parseString(JsonPB000122);
        JsonElement actual = JsonParser.parseString(RecordToJson.convert(res.result().get()));
        assertEquals(expected, actual);
    }



}