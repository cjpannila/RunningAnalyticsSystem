
CREATE DATABASE RunAnalytics;

CREATE TABLE clubs (
    club_id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT
);

CREATE TABLE users (
    user_id BIGINT PRIMARY KEY,  -- Strava athlete ID
    firstname VARCHAR(50),
    lastname VARCHAR(50),
    access_token TEXT,
    refresh_token TEXT,
    token_expires_at BIGINT,
    city VARCHAR(100),
    country VARCHAR(100),
    sex CHAR(1),
    club_id INT REFERENCES clubs(club_id)
);

CREATE TABLE activities (
    activity_id BIGINT PRIMARY KEY,  -- Strava activity ID
    user_id BIGINT REFERENCES users(user_id),

    activity_name VARCHAR(255),
    activity_type VARCHAR(50),

    start_time TIMESTAMP,

    distance_m FLOAT,
    moving_time_s INT,
    elapsed_time_s INT,

    elevation_gain_m FLOAT,

    avg_speed_mps FLOAT,
    max_speed_mps FLOAT,

    avg_cadence FLOAT,

    avg_heartrate_bpm FLOAT,
    max_heartrate_bpm FLOAT,

    device_name VARCHAR(100),
    gear_id VARCHAR(50),

    map_polyline TEXT
);

CREATE TABLE weekly_summary (
    id SERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(user_id),

    week_start_date DATE,

    total_distance_m FLOAT,
    total_runs INT,
    avg_pace FLOAT,
    avg_heartrate FLOAT
);

CREATE INDEX idx_activities_user ON activities(user_id);
CREATE INDEX idx_activities_date ON activities(start_time);

INSERT INTO clubs (name, description)
VALUES ('Seabrook Runners', 'Local recreational running club');

INSERT INTO users (user_id, firstname, lastname, club_id)
VALUES (23177662, 'Chinthana', 'Pannila', 1);