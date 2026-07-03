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

import com.google.gson.JsonArray;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

/**
 * Represents a deprecated MassBank record.
 * <p>
 * Deprecated records are resolved by accession and can be rendered as plain
 * text via {@link #toString()}. They intentionally store only deprecation message
 * metadata and the original textual payload.
 *
 * @author rmeier
 * @version 03-07-2026
 */
@Entity
@Table(name = "massbank-deprecated-records")
public class DeprecatedRecord extends AbstractRecord {

    @Column(name = "deprecated", nullable = false, length = 200)
    private String deprecated;
    @Lob
    @Column(name = "deprecated_content", nullable = false)
    private String deprecatedContent;

    public String getDeprecated() {
        return deprecated;
    }
    public void setDeprecated(String deprecated) {
        if (deprecated == null || deprecated.isBlank()) {
            throw new IllegalStateException("Missing required field: deprecated");
        }
        this.deprecated = deprecated;
    }

    public String getDeprecatedContent() {
        return deprecatedContent;
    }
    public void setDeprecatedContent(String deprecatedContent) {
        if (deprecatedContent == null || deprecatedContent.isBlank()) {
            throw new IllegalStateException("Missing required field: deprecatedContent");
        }
        this.deprecatedContent = deprecatedContent;
    }

    @Override
    protected void validateState() {
        super.validateState();
        if (deprecated == null || deprecated.isBlank()) {
            throw new IllegalStateException("Missing required field: deprecated");
        }
        if (deprecatedContent == null || deprecatedContent.isBlank()) {
            throw new IllegalStateException("Missing required field: deprecatedContent");
        }
    }

    @Override
    public String toString() {
        return "ACCESSION: " + getAccession() + "\n"
            + "DEPRECATED: " + getDeprecated() + "\n"
            + getDeprecatedContent();
    }

    @SuppressWarnings("unused")
    public JsonArray createStructuredDataJsonArray() {
        return new JsonArray();
    }
}
