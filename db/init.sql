-- Database initialization script for jobfinder
-- PostgreSQL 15+

-- Drop tables if exist (in reverse order due to foreign keys)
DROP TABLE IF EXISTS block_history CASCADE;
DROP TABLE IF EXISTS password_reset_tokens CASCADE;
DROP TABLE IF EXISTS complaints CASCADE;
DROP TABLE IF EXISTS notifications CASCADE;
DROP TABLE IF EXISTS favorites CASCADE;
DROP TABLE IF EXISTS applications CASCADE;
DROP TABLE IF EXISTS vacancy_tags CASCADE;
DROP TABLE IF EXISTS vacancies CASCADE;
DROP TABLE IF EXISTS user_tags CASCADE;
DROP TABLE IF EXISTS tags CASCADE;
DROP TABLE IF EXISTS companies CASCADE;
DROP TABLE IF EXISTS users CASCADE;

-- =============================================================================
-- USERS table
-- =============================================================================
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('USER', 'HR', 'ADMIN')),
    name VARCHAR(100),
    surname VARCHAR(100),
    city VARCHAR(100),
    phone VARCHAR(50),
    additional_info TEXT,
    is_blocked BOOLEAN DEFAULT FALSE,
    blocked_until TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_is_blocked ON users(is_blocked);

-- =============================================================================
-- COMPANIES table (for HR users)
-- =============================================================================
CREATE TABLE companies (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL UNIQUE REFERENCES users(id) ON DELETE RESTRICT,
    company_name VARCHAR(255) NOT NULL,
    website VARCHAR(500),
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_companies_user_id ON companies(user_id);

-- =============================================================================
-- TAGS table (skills/skills)
-- =============================================================================
CREATE TABLE tags (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_tags_name ON tags(name);
CREATE INDEX idx_tags_is_active ON tags(is_active);

-- =============================================================================
-- USER_TAGS table (user skills junction)
-- =============================================================================
CREATE TABLE user_tags (
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    tag_id INTEGER NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, tag_id)
);

CREATE INDEX idx_user_tags_user_id ON user_tags(user_id);
CREATE INDEX idx_user_tags_tag_id ON user_tags(tag_id);

-- =============================================================================
-- VACANCIES table
-- =============================================================================
CREATE TABLE vacancies (
    id SERIAL PRIMARY KEY,
    company_id INTEGER NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    title VARCHAR(500) NOT NULL,
    description TEXT NOT NULL,
    city VARCHAR(100),
    salary_min INTEGER,
    salary_max INTEGER,
    employment_type VARCHAR(50) CHECK (employment_type IN ('FULL_TIME', 'PART_TIME', 'INTERNSHIP', 'REMOTE', 'CONTRACT')),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    closed_at TIMESTAMP
);

CREATE INDEX idx_vacancies_company_id ON vacancies(company_id);
CREATE INDEX idx_vacancies_created_at ON vacancies(created_at);
CREATE INDEX idx_vacancies_is_active ON vacancies(is_active);
CREATE INDEX idx_vacancies_city ON vacancies(city);
CREATE INDEX idx_vacancies_salary ON vacancies(salary_min, salary_max);
CREATE INDEX idx_vacancies_employment_type ON vacancies(employment_type);
CREATE INDEX idx_vacancies_title_trgm ON vacancies USING gin (title gin_trgm_ops);
CREATE INDEX idx_vacancies_description_trgm ON vacancies USING gin (description gin_trgm_ops);

-- =============================================================================
-- VACANCY_TAGS table (vacancy skills junction)
-- =============================================================================
CREATE TABLE vacancy_tags (
    vacancy_id INTEGER NOT NULL REFERENCES vacancies(id) ON DELETE CASCADE,
    tag_id INTEGER NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
    PRIMARY KEY (vacancy_id, tag_id)
);

CREATE INDEX idx_vacancy_tags_vacancy_id ON vacancy_tags(vacancy_id);
CREATE INDEX idx_vacancy_tags_tag_id ON vacancy_tags(tag_id);

-- =============================================================================
-- APPLICATIONS table
-- =============================================================================
CREATE TABLE applications (
    id SERIAL PRIMARY KEY,
    vacancy_id INTEGER NOT NULL REFERENCES vacancies(id) ON DELETE CASCADE,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'INVITED', 'ACCEPTED', 'REJECTED', 'WITHDRAWN')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (vacancy_id, user_id)
);

CREATE INDEX idx_applications_vacancy_id ON applications(vacancy_id);
CREATE INDEX idx_applications_user_id ON applications(user_id);
CREATE INDEX idx_applications_status ON applications(status);
CREATE INDEX idx_applications_user_vacancy ON applications(user_id, vacancy_id);

-- =============================================================================
-- FAVORITES table
-- =============================================================================
CREATE TABLE favorites (
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    vacancy_id INTEGER NOT NULL REFERENCES vacancies(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, vacancy_id)
);

CREATE INDEX idx_favorites_user_id ON favorites(user_id);
CREATE INDEX idx_favorites_vacancy_id ON favorites(vacancy_id);

-- =============================================================================
-- NOTIFICATIONS table
-- =============================================================================
CREATE TABLE notifications (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT,
    link VARCHAR(500),
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_is_read ON notifications(is_read);
CREATE INDEX idx_notifications_created_at ON notifications(created_at);

-- =============================================================================
-- COMPLAINTS table
-- =============================================================================
CREATE TABLE complaints (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    target_type VARCHAR(20) NOT NULL CHECK (target_type IN ('VACANCY', 'USER', 'COMPANY')),
    target_id INTEGER NOT NULL,
    reason TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'RESOLVED', 'REJECTED')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_by INTEGER REFERENCES users(id) ON DELETE SET NULL,
    resolved_at TIMESTAMP
);

CREATE INDEX idx_complaints_user_id ON complaints(user_id);
CREATE INDEX idx_complaints_target ON complaints(target_type, target_id);
CREATE INDEX idx_complaints_status ON complaints(status);

-- =============================================================================
-- PASSWORD_RESET_TOKENS table
-- =============================================================================
CREATE TABLE password_reset_tokens (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_password_reset_tokens_user_id ON password_reset_tokens(user_id);
CREATE INDEX idx_password_reset_tokens_token ON password_reset_tokens(token);
CREATE INDEX idx_password_reset_tokens_expires_at ON password_reset_tokens(expires_at);

-- =============================================================================
-- BLOCK_HISTORY table
-- =============================================================================
CREATE TABLE block_history (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    blocked_by INTEGER NOT NULL REFERENCES users(id),
    reason TEXT,
    blocked_until TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_block_history_user_id ON block_history(user_id);
CREATE INDEX idx_block_history_created_at ON block_history(created_at);

-- =============================================================================
-- VIEW_HISTORY table (for tracking vacancy views)
-- =============================================================================
CREATE TABLE view_history (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    vacancy_id INTEGER NOT NULL REFERENCES vacancies(id) ON DELETE CASCADE,
    viewed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (user_id, vacancy_id)
);

CREATE INDEX idx_view_history_user_id ON view_history(user_id);
CREATE INDEX idx_view_history_vacancy_id ON view_history(vacancy_id);
CREATE INDEX idx_view_history_viewed_at ON view_history(viewed_at);

-- Insert default tags (skills catalog)
INSERT INTO tags (name) VALUES
    ('Java'),
    ('Python'),
    ('JavaScript'),
    ('TypeScript'),
    ('C++'),
    ('C#'),
    ('Go'),
    ('Rust'),
    ('Kotlin'),
    ('Swift'),
    ('SQL'),
    ('PostgreSQL'),
    ('MySQL'),
    ('MongoDB'),
    ('Redis'),
    ('Docker'),
    ('Kubernetes'),
    ('Git'),
    ('Linux'),
    ('Windows Server'),
    ('Agile'),
    ('Scrum'),
    ('Kanban'),
    ('JIRA'),
    ('CI/CD'),
    ('REST API'),
    ('GraphQL'),
    ('Microservices'),
    ('Machine Learning'),
    ('Data Analysis'),
    ('Excel'),
    ('Power BI'),
    ('Tableau'),
    ('Project Management'),
    ('Communication'),
    ('Teamwork'),
    ('Problem Solving'),
    ('English'),
    ('German'),
    ('French'),
    ('Spanish')
ON CONFLICT (name) DO NOTHING;