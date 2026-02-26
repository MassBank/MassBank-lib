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
// java
package massbank.db;

import massbank.Record;
import massbank.RecordParserTest;
import org.junit.jupiter.api.Test;

import java.util.*;

import static massbank.RecordParserTest.*;
import static org.junit.jupiter.api.Assertions.*;

class RecordServiceSerializationTest {

    @Test
    void roundtripSerializationRecord1() throws Exception {
        RecordParserTest.ParseResult res = parseRecord("MSBNK-test-TST00002.txt");
        Record record = res.result().get();

        RecordService svc = new RecordService(null);

        RecordEntity entity = svc.recordToEntity(record);
        Record record2 = svc.entityToRecord(entity);

        assertNotNull(record2, "Roundtrip Record is null");
        assertEquals(record.toString(), record2.toString(), "Roundtrip Record does not match original Record");
    }
}
