-- Proxy Groups Table
CREATE TABLE proxy_groups (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    metadata JSON
);

-- Proxies Table (Static and Dynamic)
CREATE TABLE proxies (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    host VARCHAR(255) NOT NULL,
    port INT NOT NULL,
    proxy_group_id VARCHAR(36),
    is_main BOOLEAN DEFAULT FALSE,
    is_static BOOLEAN DEFAULT TRUE,
    last_heartbeat DATETIME,
    FOREIGN KEY (proxy_group_id) REFERENCES proxy_groups(id) ON DELETE SET NULL
);

-- Add toggles to Groups and Servers
ALTER TABLE server_groups ADD COLUMN auto_proxy_add BOOLEAN DEFAULT TRUE;
ALTER TABLE servers ADD COLUMN auto_proxy_add BOOLEAN DEFAULT TRUE;

-- Add player count to nodes for capacity tracking
ALTER TABLE nodes ADD COLUMN current_players INT DEFAULT 0;
