DROP TABLE IF EXISTS temp_unit_test_table;

-- health_check table is required
CREATE TABLE IF NOT EXISTS health_check (
        id VARCHAR(40) PRIMARY KEY,
        app_name VARCHAR(100) NOT NULL,
        app_instance VARCHAR(256) NOT NULL,
        created TIMESTAMP NOT NULL,
        updated TIMESTAMP NOT NULL
    );

-- create a temp table for the unit tests to run
CREATE TABLE IF NOT EXISTS temp_unit_test_table (
        id VARCHAR(40) PRIMARY KEY,
        name VARCHAR(100) NOT NULL,
        address VARCHAR(256) NOT NULL,
        created TIMESTAMP NOT NULL
    );

INSERT INTO temp_unit_test_table (id, name, address, created) VALUES ('001', 'Mary', '100 World Blvd', '2024-12-22 10:10:30');
INSERT INTO temp_unit_test_table (id, name, address, created) VALUES ('002', 'Peter', '200 World Blvd', '2025-01-02 10:20:30');
