CREATE DATABASE IF NOT EXISTS almohtadine_paydesk_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE almohtadine_paydesk_db;

CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    password_salt VARCHAR(255) NULL,
    full_name VARCHAR(100) NULL,
    role ENUM('ADMIN', 'PERSONNEL') NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS children (
    id INT AUTO_INCREMENT PRIMARY KEY,
    first_name VARCHAR(80) NOT NULL,
    last_name VARCHAR(80) NOT NULL,
    class_group VARCHAR(80) NULL,
    birth_date DATE NULL,
    parent_full_name VARCHAR(120) NULL,
    parent_phone VARCHAR(30) NULL,
    registration_date DATE NULL,
    monthly_fee DECIMAL(10,3) NOT NULL DEFAULT 0.000,
    notes TEXT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS school_years (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(20) NOT NULL UNIQUE,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    active BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS school_year_months (
    id INT AUTO_INCREMENT PRIMARY KEY,
    school_year_id INT NOT NULL,
    month_name VARCHAR(30) NOT NULL,
    month_number TINYINT NOT NULL,
    display_order TINYINT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE (school_year_id, month_name),
    INDEX idx_school_year_months_year (school_year_id),
    CONSTRAINT fk_school_year_months_year
        FOREIGN KEY (school_year_id)
        REFERENCES school_years(id)
        ON DELETE RESTRICT
        ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS monthly_records (
    id INT AUTO_INCREMENT PRIMARY KEY,
    child_id INT NOT NULL,
    school_year_month_id INT NOT NULL,
    expected_amount DECIMAL(10,3) NOT NULL DEFAULT 0.000,
    total_paid DECIMAL(10,3) NOT NULL DEFAULT 0.000,
    payment_status ENUM('Payé', 'Non payé', 'Partiel', 'En retard') NOT NULL DEFAULT 'Non payé',
    due_date DATE NULL,
    notes TEXT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE (child_id, school_year_month_id),
    INDEX idx_monthly_records_child (child_id),
    INDEX idx_monthly_records_month (school_year_month_id),
    CONSTRAINT fk_monthly_records_child
        FOREIGN KEY (child_id)
        REFERENCES children(id)
        ON DELETE RESTRICT
        ON UPDATE RESTRICT,
    CONSTRAINT fk_monthly_records_month
        FOREIGN KEY (school_year_month_id)
        REFERENCES school_year_months(id)
        ON DELETE RESTRICT
        ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS extras (
    id INT AUTO_INCREMENT PRIMARY KEY,
    child_id INT NOT NULL,
    school_year_month_id INT NULL,
    start_month_id INT NULL,
    end_month_id INT NULL,
    label VARCHAR(120) NOT NULL,
    extra_type VARCHAR(20) NOT NULL DEFAULT 'PONCTUEL',
    amount DECIMAL(10,3) NOT NULL DEFAULT 0.000,
    extra_date DATE NOT NULL,
    paid BOOLEAN NOT NULL DEFAULT FALSE,
    notes TEXT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_extras_child (child_id),
    INDEX idx_extras_month (school_year_month_id),
    INDEX idx_extras_start_month (start_month_id),
    INDEX idx_extras_end_month (end_month_id),
    CONSTRAINT fk_extras_child
        FOREIGN KEY (child_id)
        REFERENCES children(id)
        ON DELETE RESTRICT
        ON UPDATE RESTRICT,
    CONSTRAINT fk_extras_month
        FOREIGN KEY (school_year_month_id)
        REFERENCES school_year_months(id)
        ON DELETE RESTRICT
        ON UPDATE RESTRICT,
    CONSTRAINT fk_extras_start_month
        FOREIGN KEY (start_month_id)
        REFERENCES school_year_months(id)
        ON DELETE RESTRICT
        ON UPDATE RESTRICT,
    CONSTRAINT fk_extras_end_month
        FOREIGN KEY (end_month_id)
        REFERENCES school_year_months(id)
        ON DELETE RESTRICT
        ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS payment_entries (
    id INT AUTO_INCREMENT PRIMARY KEY,
    monthly_record_id INT NULL,
    extra_id INT NULL,
    amount DECIMAL(10,3) NOT NULL,
    payment_date DATE NOT NULL,
    payment_method VARCHAR(40) NULL,
    notes TEXT NULL,
    created_by_user_id INT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_payment_entries_monthly_record (monthly_record_id),
    INDEX idx_payment_entries_extra (extra_id),
    INDEX idx_payment_entries_user (created_by_user_id),
    CONSTRAINT fk_payment_entries_monthly_record
        FOREIGN KEY (monthly_record_id)
        REFERENCES monthly_records(id)
        ON DELETE RESTRICT
        ON UPDATE RESTRICT,
    CONSTRAINT fk_payment_entries_extra
        FOREIGN KEY (extra_id)
        REFERENCES extras(id)
        ON DELETE RESTRICT
        ON UPDATE RESTRICT,
    CONSTRAINT fk_payment_entries_user
        FOREIGN KEY (created_by_user_id)
        REFERENCES users(id)
        ON DELETE RESTRICT
        ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS receipts (
    id INT AUTO_INCREMENT PRIMARY KEY,
    receipt_number VARCHAR(40) NOT NULL UNIQUE,
    child_id INT NOT NULL,
    payment_entry_id INT NULL UNIQUE,
    receipt_date DATE NOT NULL,
    total_amount DECIMAL(10,3) NOT NULL,
    payer_name VARCHAR(120) NULL,
    notes TEXT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_receipts_child (child_id),
    INDEX idx_receipts_payment_entry (payment_entry_id),
    CONSTRAINT fk_receipts_child
        FOREIGN KEY (child_id)
        REFERENCES children(id)
        ON DELETE RESTRICT
        ON UPDATE RESTRICT,
    CONSTRAINT fk_receipts_payment_entry
        FOREIGN KEY (payment_entry_id)
        REFERENCES payment_entries(id)
        ON DELETE RESTRICT
        ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS settings (
    id INT AUTO_INCREMENT PRIMARY KEY,
    setting_key VARCHAR(80) NOT NULL UNIQUE,
    setting_value VARCHAR(255) NOT NULL,
    description VARCHAR(255) NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS backup_logs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    folder_path VARCHAR(255) NULL,
    backup_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status ENUM('SUCCESS', 'FAILED') NOT NULL,
    message TEXT NULL,
    created_by_user_id INT NULL,
    INDEX idx_backup_logs_user (created_by_user_id),
    CONSTRAINT fk_backup_logs_user
        FOREIGN KEY (created_by_user_id)
        REFERENCES users(id)
        ON DELETE RESTRICT
        ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS activity_logs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NULL,
    action VARCHAR(120) NOT NULL,
    table_name VARCHAR(80) NULL,
    record_id INT NULL,
    details TEXT NULL,
    activity_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_activity_logs_user (user_id),
    CONSTRAINT fk_activity_logs_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE RESTRICT
        ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
