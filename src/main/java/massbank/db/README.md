# PostgreSQL Database Support for MassBank Records

This package provides PostgreSQL database support for storing and retrieving MassBank `Record` objects.

## Features

- Store and retrieve MassBank records in PostgreSQL database
- JSONB support for complex data structures
- Full CRUD operations (Create, Read, Update, Delete)
- Upsert support (automatic update on conflict)
- Comprehensive unit tests with TestContainers

## Usage

### 1. Initialize Database Connection

```java
import massbank.db.DatabaseConnection;
import massbank.db.RecordRepository;
import java.sql.Connection;

// Create connection
Connection connection = DatabaseConnection.getConnection(
    "jdbc:postgresql://localhost:5432/massbank",
    "username",
    "password"
);

// Initialize schema (first time only)
DatabaseConnection.initializeSchema(connection);

// Create repository
RecordRepository repository = new RecordRepository(connection);
```

### 2. Store a Record

```java
import massbank.Record;

Record record = new Record();
// ... populate record fields ...

repository.store(record);
```

### 3. Retrieve a Record

```java
// Retrieve by accession ID
Record record = repository.retrieve("MSBNK-TEST-00001");

// Retrieve all records
List<Record> allRecords = repository.retrieveAll();
```

### 4. Update a Record

```java
// Store with same accession ID to update
record.AUTHORS("Updated Author");
repository.store(record);
```

### 5. Delete a Record

```java
boolean deleted = repository.delete("MSBNK-TEST-00001");
```

### 6. Check if Record Exists

```java
boolean exists = repository.exists("MSBNK-TEST-00001");
```

## Database Schema

The schema is automatically created by the `DatabaseConnection.initializeSchema()` method.

Key features:
- Primary key on `accession` field
- JSONB columns for complex structures (lists, maps)
- GIN indexes on JSONB columns for efficient queries
- Timestamps for created_at and updated_at

## Dependencies

Add these dependencies to your `pom.xml`:

```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <version>42.7.4</version>
</dependency>
```

## Testing

The package includes comprehensive unit tests using TestContainers:

```bash
mvn test
```

Tests cover:
- Basic CRUD operations
- Complex record with all fields
- Deprecated records
- Peak data storage
- Annotation data storage
- Update operations

## Notes

- JSONB is used for complex data structures to maintain flexibility
- The repository uses prepared statements to prevent SQL injection
- All operations are logged using log4j
- Connection management is left to the caller (not auto-closed)
