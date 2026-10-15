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

-- the demo_order and demo_order_history tables serve the orders demo
-- (DemoOrderEndpoint uses the PgRequest helper, DemoOrderEvents sends events to "postgres.service")
CREATE TABLE IF NOT EXISTS demo_order (
        id VARCHAR(40) PRIMARY KEY,
        profile_id VARCHAR(40) NOT NULL,
        item VARCHAR(100) NOT NULL,
        quantity INT NOT NULL,
        amount NUMERIC(12, 2) NOT NULL,
        status VARCHAR(20) NOT NULL,
        created TIMESTAMP NOT NULL,
        updated TIMESTAMP NOT NULL
    );

CREATE TABLE IF NOT EXISTS demo_order_history (
        id SERIAL PRIMARY KEY,
        order_id VARCHAR(40) NOT NULL,
        event VARCHAR(20) NOT NULL,
        created TIMESTAMP NOT NULL
    );
