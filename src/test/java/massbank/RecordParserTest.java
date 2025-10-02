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

import org.junit.jupiter.api.Test;
import org.petitparser.context.Result;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RecordParserTest {

    @Test
    void testRecord1() throws IOException {
        ParseResult res = parseRecord("MSBNK-IPB_Halle-PB000122.txt");
        assertTrue(res.result().isSuccess());
        assertEquals("MSBNK-IPB_Halle-PB000122", ((Record) res.result().get()).ACCESSION());
        assertEquals("Naringenin; LC-ESI-QTOF; MS2; CE:15 eV; [M+H]+", ((Record) res.result().get()).RECORD_TITLE1());
        assertEquals(res.content(), res.result().get().toString());
    }

    @Test
    void testRecord2() throws IOException {
        ParseResult res = parseRecord("MSBNK-LCSB-LU092805.txt");
        assertTrue(res.result().isSuccess());
        assertEquals("MSBNK-LCSB-LU092805", ((Record) res.result().get()).ACCESSION());
        assertTrue(((Record) res.result().get()).isDeprecated());
        assertEquals("2022-02-08 possible mixed spectra", ((Record) res.result().get()).DEPRECATED());
        assertEquals(res.content(), res.result().get().toString());
    }

    @Test
    void testRecord3() throws IOException {
        ParseResult res = parseRecord("MSBNK-test-TST00001.txt");
        assertTrue(res.result().isSuccess());
        assertEquals("MSBNK-test-TST00001", ((Record) res.result().get()).ACCESSION());
        assertEquals("Fiscalin C; LC-ESI-ITFT; MS2; CE: 30; R=17500; [M+H]+", ((Record) res.result().get()).RECORD_TITLE1());
        assertEquals(res.content(), res.result().get().toString());
    }

    @Test
    void testRecord4() throws IOException {
        ParseResult res = parseRecord("MSBNK-test-TST00002.txt");
        assertTrue(res.result().isSuccess());
        assertEquals("MSBNK-test-TST00002", ((Record) res.result().get()).ACCESSION());
        assertEquals("Disialoganglioside GD1a; MALDI-TOF; MS; Pos", ((Record) res.result().get()).RECORD_TITLE1());
        assertEquals(res.content(), res.result().get().toString());
    }

    @Test
    void testRecord5() throws IOException {
        ParseResult res = parseRecord("MSBNK-test-TST00003.txt");
        assertTrue(res.result().isSuccess());
        assertEquals("MSBNK-test-TST00003", ((Record) res.result().get()).ACCESSION());
        assertTrue(((Record) res.result().get()).isDeprecated());
        assertEquals("2019-11-25 Wrong MS measurement assigned", ((Record) res.result().get()).DEPRECATED());
        assertEquals(res.content(), res.result().get().toString());
    }

    // just a record
    public record ParseResult(Result result, String content) {}

    //parse a file from the resource folder
    public static ParseResult parseRecord(String filename) throws IOException {
        Set<String> config = new HashSet<>();
        config.add("validate");
        RecordParser recordparser = new RecordParser(config);
        InputStream is = RecordParserTest.class.getClassLoader().getResourceAsStream(filename);
        assertNotNull(is);
        String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        Result result = recordparser.parse(content);
        return new ParseResult(result, content);
    }
}