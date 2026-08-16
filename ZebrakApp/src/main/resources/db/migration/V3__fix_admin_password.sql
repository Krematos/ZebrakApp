-- Aktualizace hesla administrátora na platný BCrypt hash (heslo: admin123)
UPDATE users 
SET password = '$2a$10$W6CYKtDkzF/Mro5mEmLpmuXzVfevci0rNO35OeTa9ywhJtf5YOLMC',
    active = TRUE,
    role = 'ROLE_ADMIN'
WHERE email = 'admin@zebrak.cz';
