-- Audit Logs Table
CREATE TABLE audit_logs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    level VARCHAR(20) NOT NULL, -- INFO, WARNING, ERROR, ACTION
    category VARCHAR(50) NOT NULL, -- AUTOSCALING, ALLOCATION, TEMPLATE, PROXY
    message TEXT NOT NULL,
    metadata JSON,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- Advanced Scheduling Toggles for Groups
ALTER TABLE server_groups ADD COLUMN preferred_node_id CHAR(36) NULL;
ALTER TABLE server_groups ADD COLUMN scheduling_strategy VARCHAR(20) DEFAULT 'SPREAD'; -- SPREAD, BIN_PACKING

-- Template Local Storage Path
ALTER TABLE templates ADD COLUMN local_path VARCHAR(255) DEFAULT '/var/lib/pterodactyl/templates';

-- Google Drive Integration
CREATE TABLE google_drive_config (
    id INT PRIMARY KEY DEFAULT 1,
    client_id TEXT NOT NULL,
    client_secret TEXT NOT NULL,
    access_token TEXT NULL,
    refresh_token TEXT NULL,
    backup_interval_minutes INT DEFAULT 1440, -- Default daily
    last_backup_at DATETIME NULL
);
