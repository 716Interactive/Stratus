-- Add ownership to resources
ALTER TABLE templates ADD COLUMN IF NOT EXISTS owner_id INT DEFAULT 1;
ALTER TABLE server_groups ADD COLUMN IF NOT EXISTS owner_id INT DEFAULT 1;

-- Create user_permissions table for sharing
CREATE TABLE IF NOT EXISTS user_permissions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    resource_id CHAR(36) NOT NULL,
    resource_type VARCHAR(20) NOT NULL,
    permissions TEXT NOT NULL
);
