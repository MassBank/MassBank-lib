package massbank;

import org.junit.jupiter.api.Test;
import org.petitparser.context.Result;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RecordTest {

    @Test
    void testRecordParser() throws IOException {
        Set<String> config = new HashSet<>();
        config.add("validate");
        RecordParser recordparser = new RecordParser(config);

        InputStream is = getClass().getClassLoader().getResourceAsStream("MSBNK-IPB_Halle-PB000122.txt");
        assertNotNull(is);
        String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);

        Result res = recordparser.parse(content);
        Record record = res.get();

    }

}