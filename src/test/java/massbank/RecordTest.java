package massbank;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class RecordTest {

    @Test
    void testAccessionGetterSetter() {
        Record record = new Record();
        record.ACCESSION("ABC123");
        assertEquals("ABC123", record.ACCESSION());
    }

    @Test
    void testDeprecatedFlag() {
        Record record = new Record();
        record.DEPRECATED(true);
        assertTrue(record.DEPRECATED());
    }

    @Test
    void testRecordTitle() {
        Record record = new Record();
        record.RECORD_TITLE(List.of("Title1", "Title2"));
        assertEquals(List.of("Title1", "Title2"), record.RECORD_TITLE());
        assertEquals("Title1; Title2", record.RECORD_TITLE1());
    }

    @Test
    void testExactMass() {
        Record record = new Record();
        record.CH_EXACT_MASS(new BigDecimal("123.456"));
        assertEquals(new BigDecimal("123.456"), record.CH_EXACT_MASS());
    }
}