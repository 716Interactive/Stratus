ALTER TABLE servers ADD COLUMN state_changed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER last_heartbeat;
UPDATE servers SET state_changed_at = created_at;
