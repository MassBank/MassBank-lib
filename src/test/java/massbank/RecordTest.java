package massbank;

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
        assertEquals(res.content(), ((Record) res.result().get()).toString());
    }

    @Test
    void testDeprecatedRecord() throws IOException {
        ParseResult res = parseRecord("MSBNK-LCSB-LU092805.txt");
        assertTrue(res.result().isSuccess());
        assertEquals("MSBNK-LCSB-LU092805", ((Record) res.result().get()).ACCESSION());
        assertEquals(res.content(), ((Record) res.result().get()).toString());
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