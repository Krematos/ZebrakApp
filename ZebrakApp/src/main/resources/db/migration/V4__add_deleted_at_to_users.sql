-- Tabulka uživatelů: přidání sloupce pro soft delete
ALTER TABLE users ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP NULL;

-- Index pro vyhledávání soft-deleted uživatelů a plánované čištění
CREATE INDEX IF NOT EXISTS idx_users_deleted_at ON users(deleted_at);
