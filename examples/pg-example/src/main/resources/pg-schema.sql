-- health_check table is required
CREATE TABLE IF NOT EXISTS health_check (
        id VARCHAR(40) PRIMARY KEY,
        app_name VARCHAR(100) NOT NULL,
        app_instance VARCHAR(256) NOT NULL,
        created TIMESTAMP NOT NULL,
        updated TIMESTAMP NOT NULL
    );

-- create a temp table for the unit tests to run
CREATE TABLE IF NOT EXISTS demo_profile (
        id VARCHAR(40) PRIMARY KEY,
        name VARCHAR(100) NOT NULL,
        address VARCHAR(256) NOT NULL,
        created TIMESTAMP NOT NULL
    );
