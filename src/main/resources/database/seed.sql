USE almohtadine_paydesk_db;

-- Temporary value used only before Java initializes password security.
-- SchemaInitializer upgrades this admin password to PBKDF2 hash and salt.
INSERT INTO users (username, password_hash, password_salt, full_name, role, active)
VALUES ('admin', 'admin123', NULL, 'Administrateur', 'ADMIN', TRUE)
ON DUPLICATE KEY UPDATE
    role = 'ADMIN',
    active = TRUE;

INSERT INTO school_years (name, start_date, end_date, active)
VALUES ('2025/2026', '2025-09-01', '2026-06-30', TRUE)
ON DUPLICATE KEY UPDATE
    start_date = '2025-09-01',
    end_date = '2026-06-30',
    active = TRUE;

INSERT INTO school_year_months (school_year_id, month_name, month_number, display_order, active)
SELECT id, 'Septembre', 9, 1, TRUE FROM school_years WHERE name = '2025/2026'
ON DUPLICATE KEY UPDATE month_number = 9, display_order = 1, active = TRUE;

INSERT INTO school_year_months (school_year_id, month_name, month_number, display_order, active)
SELECT id, 'Octobre', 10, 2, TRUE FROM school_years WHERE name = '2025/2026'
ON DUPLICATE KEY UPDATE month_number = 10, display_order = 2, active = TRUE;

INSERT INTO school_year_months (school_year_id, month_name, month_number, display_order, active)
SELECT id, 'Novembre', 11, 3, TRUE FROM school_years WHERE name = '2025/2026'
ON DUPLICATE KEY UPDATE month_number = 11, display_order = 3, active = TRUE;

INSERT INTO school_year_months (school_year_id, month_name, month_number, display_order, active)
SELECT id, 'Décembre', 12, 4, TRUE FROM school_years WHERE name = '2025/2026'
ON DUPLICATE KEY UPDATE month_number = 12, display_order = 4, active = TRUE;

INSERT INTO school_year_months (school_year_id, month_name, month_number, display_order, active)
SELECT id, 'Janvier', 1, 5, TRUE FROM school_years WHERE name = '2025/2026'
ON DUPLICATE KEY UPDATE month_number = 1, display_order = 5, active = TRUE;

INSERT INTO school_year_months (school_year_id, month_name, month_number, display_order, active)
SELECT id, 'Février', 2, 6, TRUE FROM school_years WHERE name = '2025/2026'
ON DUPLICATE KEY UPDATE month_number = 2, display_order = 6, active = TRUE;

INSERT INTO school_year_months (school_year_id, month_name, month_number, display_order, active)
SELECT id, 'Mars', 3, 7, TRUE FROM school_years WHERE name = '2025/2026'
ON DUPLICATE KEY UPDATE month_number = 3, display_order = 7, active = TRUE;

INSERT INTO school_year_months (school_year_id, month_name, month_number, display_order, active)
SELECT id, 'Avril', 4, 8, TRUE FROM school_years WHERE name = '2025/2026'
ON DUPLICATE KEY UPDATE month_number = 4, display_order = 8, active = TRUE;

INSERT INTO school_year_months (school_year_id, month_name, month_number, display_order, active)
SELECT id, 'Mai', 5, 9, TRUE FROM school_years WHERE name = '2025/2026'
ON DUPLICATE KEY UPDATE month_number = 5, display_order = 9, active = TRUE;

INSERT INTO school_year_months (school_year_id, month_name, month_number, display_order, active)
SELECT id, 'Juin', 6, 10, TRUE FROM school_years WHERE name = '2025/2026'
ON DUPLICATE KEY UPDATE month_number = 6, display_order = 10, active = TRUE;

INSERT INTO settings (setting_key, setting_value, description)
VALUES ('garderie_name', 'Garderie Almohtadine', 'Nom de la garderie')
ON DUPLICATE KEY UPDATE
    setting_value = 'Garderie Almohtadine',
    description = 'Nom de la garderie';

INSERT INTO settings (setting_key, setting_value, description)
VALUES ('payment_deadline_day', '10', 'Jour limite de paiement')
ON DUPLICATE KEY UPDATE
    setting_value = '10',
    description = 'Jour limite de paiement';

INSERT INTO settings (setting_key, setting_value, description)
VALUES ('backup_folder', '', 'Dossier de sauvegarde')
ON DUPLICATE KEY UPDATE
    setting_value = '',
    description = 'Dossier de sauvegarde';
