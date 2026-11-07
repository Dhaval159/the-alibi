-- The Alibi Production PostgreSQL Database Schema
-- Provides persistent storage for rooms, players, cases, assignments, messages, evidence, votes, and game progress.

CREATE TABLE IF NOT EXISTS cases (
    case_id VARCHAR(64) PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    subtitle TEXT,
    classification VARCHAR(64),
    setting TEXT NOT NULL,
    date_time VARCHAR(128),
    difficulty VARCHAR(32),
    estimated_duration_minutes VARCHAR(64),
    cinematic_intro TEXT,
    incident_summary TEXT,
    investigation_objective TEXT,
    poster_asset_id VARCHAR(128),
    case_data_json JSONB NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS rooms (
    room_id VARCHAR(64) PRIMARY KEY,
    room_code VARCHAR(6) UNIQUE NOT NULL,
    host_player_id VARCHAR(64) NOT NULL,
    state VARCHAR(32) NOT NULL DEFAULT 'WAITING_FOR_PLAYERS',
    match_duration_minutes INT DEFAULT 18,
    frozen_player_count INT,
    case_id VARCHAR(64) REFERENCES cases(case_id) ON DELETE SET NULL,
    phase_start_time_millis BIGINT,
    phase_duration_seconds INT,
    phase_end_time_millis BIGINT,
    generation_job_id VARCHAR(64),
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS players (
    player_id VARCHAR(64) PRIMARY KEY,
    room_code VARCHAR(6) REFERENCES rooms(room_code) ON DELETE CASCADE,
    player_name VARCHAR(128) NOT NULL,
    is_host BOOLEAN DEFAULT FALSE,
    is_ready BOOLEAN DEFAULT TRUE,
    is_connected BOOLEAN DEFAULT TRUE,
    session_token VARCHAR(128) NOT NULL,
    assigned_character_id VARCHAR(64),
    joined_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS personal_objectives (
    objective_id VARCHAR(64) PRIMARY KEY,
    room_code VARCHAR(6) REFERENCES rooms(room_code) ON DELETE CASCADE,
    player_id VARCHAR(64) REFERENCES players(player_id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    category VARCHAR(64) NOT NULL,
    is_primary BOOLEAN NOT NULL,
    is_verified BOOLEAN DEFAULT FALSE,
    progress INT DEFAULT 0,
    is_completed BOOLEAN DEFAULT FALSE,
    points INT DEFAULT 100
);

CREATE TABLE IF NOT EXISTS private_messages (
    message_id VARCHAR(64) PRIMARY KEY,
    room_code VARCHAR(6) REFERENCES rooms(room_code) ON DELETE CASCADE,
    sender_player_id VARCHAR(64) REFERENCES players(player_id) ON DELETE CASCADE,
    sender_character_name VARCHAR(128),
    recipient_player_id VARCHAR(64) REFERENCES players(player_id) ON DELETE CASCADE,
    recipient_character_name VARCHAR(128),
    message_text TEXT NOT NULL,
    message_type VARCHAR(32) DEFAULT 'CHAT',
    attached_evidence_id VARCHAR(64),
    deal_details TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS deal_proposals (
    deal_id VARCHAR(64) PRIMARY KEY,
    room_code VARCHAR(6) REFERENCES rooms(room_code) ON DELETE CASCADE,
    proposer_player_id VARCHAR(64) REFERENCES players(player_id) ON DELETE CASCADE,
    recipient_player_id VARCHAR(64) REFERENCES players(player_id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    terms TEXT NOT NULL,
    status VARCHAR(32) DEFAULT 'PENDING',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS discovered_evidence (
    room_code VARCHAR(6) REFERENCES rooms(room_code) ON DELETE CASCADE,
    evidence_id VARCHAR(64) NOT NULL,
    discovered_by_player_id VARCHAR(64),
    is_public BOOLEAN DEFAULT TRUE,
    discovered_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (room_code, evidence_id)
);

CREATE TABLE IF NOT EXISTS ballot_votes (
    room_code VARCHAR(6) REFERENCES rooms(room_code) ON DELETE CASCADE,
    voter_player_id VARCHAR(64) REFERENCES players(player_id) ON DELETE CASCADE,
    accused_culprit_id VARCHAR(64) NOT NULL,
    method_accusation TEXT NOT NULL,
    motive_accusation TEXT NOT NULL,
    smoking_gun_evidence_id VARCHAR(64),
    submitted_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (room_code, voter_player_id)
);

CREATE TABLE IF NOT EXISTS case_resolutions (
    room_code VARCHAR(6) PRIMARY KEY REFERENCES rooms(room_code) ON DELETE CASCADE,
    true_culprit_id VARCHAR(64) NOT NULL,
    true_culprit_name VARCHAR(128) NOT NULL,
    true_motive TEXT NOT NULL,
    true_method TEXT NOT NULL,
    culprit_confession TEXT NOT NULL,
    is_culprit_convicted BOOLEAN NOT NULL,
    player_scores_json JSONB NOT NULL,
    resolved_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_rooms_code ON rooms(room_code);
CREATE INDEX IF NOT EXISTS idx_players_room ON players(room_code);
CREATE INDEX IF NOT EXISTS idx_messages_conversation ON private_messages(room_code, sender_player_id, recipient_player_id);
