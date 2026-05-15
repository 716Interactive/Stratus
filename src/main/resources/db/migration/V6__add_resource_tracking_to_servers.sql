-- Migration V6: Add missing resource and connection columns to Servers and Nodes

-- Add resource tracking to servers for intelligent scheduling
ALTER TABLE servers ADD COLUMN memory INT NOT NULL DEFAULT 0 AFTER port;
ALTER TABLE servers ADD COLUMN disk INT NOT NULL DEFAULT 0 AFTER memory;

-- Add host and token to nodes for Template File Manager (Wings connectivity)
ALTER TABLE nodes ADD COLUMN host VARCHAR(255) NOT NULL DEFAULT 'localhost' AFTER name;
ALTER TABLE nodes ADD COLUMN token TEXT NULL AFTER host;
