-- Vložení výchozího administrátorského účtu (admin@zebrak.cz / heslo: admin123)
-- Platný BCrypt hash pro 'admin123': $2a$10$W6CYKtDkzF/Mro5mEmLpmuXzVfevci0rNO35OeTa9ywhJtf5YOLMC
INSERT INTO users (email, password, nickname, role, active, created_at, updated_at)
VALUES (
    'admin@zebrak.cz',
    '$2a$10$W6CYKtDkzF/Mro5mEmLpmuXzVfevci0rNO35OeTa9ywhJtf5YOLMC',
    'Administrátor Žebrák',
    'ROLE_ADMIN',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (email) DO UPDATE SET password = EXCLUDED.password, active = TRUE, role = 'ROLE_ADMIN';
