-- DROP TABLE IF EXISTS health_check;

CREATE TABLE IF NOT EXISTS health_check (
        id VARCHAR(40) PRIMARY KEY,
        app_name VARCHAR(100) NOT NULL,
        app_instance VARCHAR(256) NOT NULL,
        created TIMESTAMP NOT NULL,
        updated TIMESTAMP NOT NULL
    );

-- INSERT INTO health_check (id, app_name, app_instance, created, updated) VALUES ('001', 'Mary', 'A', '2024-12-22 10:10:30', '2024-12-22 10:10:30');
-- INSERT INTO health_check (id, app_name, app_instance, created, updated) VALUES ('002', 'Peter', 'B', '2024-12-23 20:20:30', '2024-12-22 10:10:30');

-- DELETE FROM health_check WHERE updated < '2025-12-25T00:00:00Z';
