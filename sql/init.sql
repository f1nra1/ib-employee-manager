-- База данных: Система управления сотрудниками ИБ

CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100),
    role VARCHAR(20) NOT NULL DEFAULT 'user',
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS positions (
    id SERIAL PRIMARY KEY,
    title VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    security_level INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS departments (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    code VARCHAR(20) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS employees (
    id SERIAL PRIMARY KEY,
    employee_number VARCHAR(20) NOT NULL UNIQUE,
    last_name VARCHAR(50) NOT NULL,
    first_name VARCHAR(50) NOT NULL,
    middle_name VARCHAR(50),
    birth_date DATE NOT NULL,
    hire_date DATE NOT NULL,
    position_id INT NOT NULL REFERENCES positions(id),
    department_id INT NOT NULL REFERENCES departments(id),
    email VARCHAR(100) UNIQUE,
    phone VARCHAR(20),
    clearance_level INT NOT NULL DEFAULT 1,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS certifications (
    id SERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL UNIQUE,
    issuing_authority VARCHAR(100) NOT NULL,
    description TEXT,
    validity_years INT DEFAULT 3,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS employee_certifications (
    id SERIAL PRIMARY KEY,
    employee_id INT NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    certification_id INT NOT NULL REFERENCES certifications(id) ON DELETE CASCADE,
    issue_date DATE NOT NULL,
    expiry_date DATE NOT NULL,
    certificate_number VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(employee_id, certification_id, issue_date)
);

CREATE TABLE IF NOT EXISTS access_log (
    id SERIAL PRIMARY KEY,
    employee_id INT REFERENCES employees(id) ON DELETE SET NULL,
    action_type VARCHAR(50) NOT NULL,
    action_description TEXT,
    ip_address VARCHAR(45),
    action_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO positions (title, description, security_level) VALUES
('Специалист по ИБ', 'Младший специалист', 2),
('Ведущий специалист по ИБ', 'Ведущий специалист', 3),
('Аналитик SOC', 'Аналитик мониторинга', 3),
('Инженер по безопасности', 'Инженер', 3),
('Пентестер', 'Тестировщик', 4),
('Руководитель отдела ИБ', 'Начальник отдела', 5),
('CISO', 'Директор по ИБ', 5)
ON CONFLICT (title) DO NOTHING;

INSERT INTO departments (name, code, description) VALUES
('Отдел мониторинга', 'SOC', 'Security Operations Center'),
('Отдел аудита', 'AUDIT', 'Внутренний аудит'),
('Отдел инфраструктурной безопасности', 'INFRA', 'Защита инфраструктуры'),
('Отдел прикладной безопасности', 'APPSEC', 'Безопасность приложений'),
('Отдел реагирования на инциденты', 'IR', 'Incident Response')
ON CONFLICT (name) DO NOTHING;

INSERT INTO certifications (name, issuing_authority, description, validity_years) VALUES
('CISSP', 'ISC2', 'Certified Information Systems Security Professional', 3),
('CEH', 'EC-Council', 'Certified Ethical Hacker', 3),
('CISM', 'ISACA', 'Certified Information Security Manager', 3),
('CompTIA Security+', 'CompTIA', 'Базовая сертификация', 3),
('OSCP', 'Offensive Security', 'Offensive Security Certified Professional', 0)
ON CONFLICT (name) DO NOTHING;