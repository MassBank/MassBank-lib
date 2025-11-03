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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;

/**
 * Utility class for managing database connections.
 */
public class DatabaseConnection {
    private static final Logger logger = LogManager.getLogger(DatabaseConnection.class);

    /**
     * Creates a connection to a PostgreSQL database.
     * 
     * @param url The JDBC URL
     * @param username The database username
     * @param password The database password
     * @return A Connection object
     * @throws SQLException if a database access error occurs
     */
    public static Connection getConnection(String url, String username, String password) throws SQLException {
        logger.info("Connecting to database: {}", url);
        return DriverManager.getConnection(url, username, password);
    }

    /**
     * Initializes the database schema from the schema.sql file.
     * 
     * @param connection The database connection
     * @throws SQLException if a database error occurs
     * @throws IOException if the schema file cannot be read
     */
    public static void initializeSchema(Connection connection) throws SQLException, IOException {
        logger.info("Initializing database schema");
        
        String schemaScript = loadResourceAsString("/db/schema.sql");
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(schemaScript);
        }
        
        logger.info("Database schema initialized successfully");
    }

    /**
     * Loads a resource file as a String.
     * 
     * @param resourcePath The path to the resource
     * @return The contents of the resource as a String
     * @throws IOException if the resource cannot be read
     */
    private static String loadResourceAsString(String resourcePath) throws IOException {
        try (InputStream is = DatabaseConnection.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IOException("Resource not found: " + resourcePath);
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        }
    }
}
