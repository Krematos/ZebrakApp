-- Vložení výchozího administrátorského účtu (admin@zebrak.cz / heslo: admin123)
-- BCrypt hash pro 'admin123': $2a$10$GZL3s0oO04Q0uTz6zYvN5uXq.8vjR2F2n8oY9Wv8mX7Q4K5d4n0K6
INSERT INTO users (email, password, nickname, role, active, created_at, updated_at)
VALUES (
    'admin@zebrak.cz',
    '$2a$10$GZL3s0oO04Q0uTz6zYvN5uXq.8vjR2F2n8oY9Wv8mX7Q4K5d4n0K6',
    'Administrátor Žebrák',
    'ROLE_ADMIN',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (email) DO NOTHING;
