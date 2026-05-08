-- ============================================================
-- Stratus — V1 Initial Schema
-- MariaDB / MySQL compatible
-- ============================================================

-- ── Nodes ────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS nodes
(
    id             CHAR(36)     NOT NULL,
    pterodactyl_id INT          NOT NULL,
    name           VARCHAR(100) NOT NULL,
    total_memory   INT          NOT NULL COMMENT 'MB',
    total_disk     INT          NOT NULL COMMENT 'MB',
    PRIMARY KEY (id),
    UNIQUE KEY uq_nodes_pterodactyl_id (pterodactyl_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- ── Templates ────────────────────────────────────────────────
-- current_version_id added as FK after template_versions is created
CREATE TABLE IF NOT EXISTS templates
(
    id                 CHAR(36)     NOT NULL,
    name               VARCHAR(100) NOT NULL,
    current_version_id CHAR(36)     NULL DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_templates_name (name)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- ── Template Versions ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS template_versions
(
    id              CHAR(36) NOT NULL,
    template_id     CHAR(36) NOT NULL,
    version_number  INT      NOT NULL,
    egg_id          INT      NOT NULL,
    config_json     JSON     NOT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_tv_template_version (template_id, version_number),
    CONSTRAINT fk_tv_template
        FOREIGN KEY (template_id) REFERENCES templates (id)
            ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- Back-fill FK on templates once template_versions exists
ALTER TABLE templates
    ADD CONSTRAINT fk_templates_current_version
        FOREIGN KEY (current_version_id) REFERENCES template_versions (id)
            ON DELETE SET NULL;

-- ── Server Groups ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS server_groups
(
    id                          CHAR(36)     NOT NULL,
    name                        VARCHAR(100) NOT NULL,
    template_id                 CHAR(36)     NOT NULL,
    min_servers                 INT          NOT NULL DEFAULT 0,
    max_servers                 INT          NOT NULL DEFAULT 10,
    target_free_slots           INT          NOT NULL DEFAULT 1,
    scale_down_cooldown_seconds INT          NOT NULL DEFAULT 120,
    metadata                    JSON         NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_server_groups_name (name),
    CONSTRAINT fk_sg_template
        FOREIGN KEY (template_id) REFERENCES templates (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- ── Servers ───────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS servers
(
    id                  CHAR(36)                                                                       NOT NULL,
    pterodactyl_id      INT                                                                            NULL,
    node_id             CHAR(36)                                                                       NOT NULL,
    group_id            CHAR(36)                                                                       NOT NULL,
    template_version_id CHAR(36)                                                                       NOT NULL,
    host                VARCHAR(45)                                                                    NOT NULL,
    port                INT                                                                            NOT NULL,
    state               ENUM ('STARTING','READY','IN_GAME','ENDING','EMPTY','DRAINING','TERMINATED')   NOT NULL DEFAULT 'STARTING',
    players             INT                                                                            NOT NULL DEFAULT 0,
    last_heartbeat      DATETIME                                                                       NULL,
    created_at          DATETIME                                                                       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_servers_group_state (group_id, state),
    KEY idx_servers_state (state),
    KEY idx_servers_node (node_id),
    CONSTRAINT fk_servers_node
        FOREIGN KEY (node_id) REFERENCES nodes (id),
    CONSTRAINT fk_servers_group
        FOREIGN KEY (group_id) REFERENCES server_groups (id),
    CONSTRAINT fk_servers_template_version
        FOREIGN KEY (template_version_id) REFERENCES template_versions (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
