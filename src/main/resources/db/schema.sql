-- Database schema for storing MassBank Record objects
-- This schema uses JSONB for complex data structures to maintain flexibility

CREATE TABLE IF NOT EXISTS records (
    -- Primary identifier
    accession VARCHAR(255) PRIMARY KEY,
    
    -- Deprecation information
    is_deprecated BOOLEAN NOT NULL DEFAULT FALSE,
    deprecated TEXT,
    deprecated_content TEXT,
    
    -- Basic metadata
    record_title JSONB NOT NULL,
    date VARCHAR(255) NOT NULL,
    authors TEXT NOT NULL,
    license VARCHAR(255) NOT NULL,
    copyright TEXT,
    publication TEXT,
    project TEXT,
    comment JSONB,
    
    -- Chemical compound information
    ch_name JSONB NOT NULL,
    ch_compound_class JSONB,
    ch_formula VARCHAR(255) NOT NULL,
    ch_exact_mass NUMERIC NOT NULL,
    ch_smiles TEXT NOT NULL,
    ch_iupac TEXT NOT NULL,
    ch_link JSONB,
    
    -- Species/sample information
    sp_scientific_name TEXT,
    sp_lineage TEXT,
    sp_link JSONB,
    sp_sample JSONB,
    
    -- Acquisition conditions
    ac_instrument TEXT NOT NULL,
    ac_instrument_type VARCHAR(255) NOT NULL,
    ac_mass_spectrometry_ms_type VARCHAR(255) NOT NULL,
    ac_mass_spectrometry_ion_mode VARCHAR(255) NOT NULL,
    ac_mass_spectrometry JSONB,
    ac_chromatography JSONB,
    
    -- Mass spectrometry data
    ms_focused_ion JSONB,
    ms_data_processing JSONB,
    
    -- Peak data
    pk_splash VARCHAR(255) NOT NULL,
    pk_annotation_header JSONB,
    pk_annotation JSONB,
    pk_peak JSONB NOT NULL,
    
    -- Metadata
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create index on accession for faster lookups
CREATE INDEX IF NOT EXISTS idx_records_accession ON records(accession);

-- Create index on is_deprecated for filtering
CREATE INDEX IF NOT EXISTS idx_records_is_deprecated ON records(is_deprecated);

-- Create GIN index on JSONB columns for better query performance
CREATE INDEX IF NOT EXISTS idx_records_ch_link ON records USING GIN (ch_link);
CREATE INDEX IF NOT EXISTS idx_records_ch_name ON records USING GIN (ch_name);
CREATE INDEX IF NOT EXISTS idx_records_pk_peak ON records USING GIN (pk_peak);
