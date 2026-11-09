CREATE DATABASE IF NOT EXISTS jobtrack
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE jobtrack;

CREATE TABLE users (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    email         VARCHAR(150) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    full_name     VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT chk_users_role CHECK (role IN ('STUDENT', 'ADMIN'))
) ENGINE=InnoDB;

CREATE TABLE student_profiles (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    phone           VARCHAR(20),
    college         VARCHAR(150),
    degree          VARCHAR(80),
    branch          VARCHAR(80),
    graduation_year INT,
    cgpa            DECIMAL(4,2),
    skills          VARCHAR(500),
    resume_url      VARCHAR(255),
    CONSTRAINT uk_profile_user UNIQUE (user_id),
    CONSTRAINT fk_profile_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE companies (
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    name     VARCHAR(120) NOT NULL,
    industry VARCHAR(80),
    website  VARCHAR(200),
    location VARCHAR(100),
    CONSTRAINT uk_company_name UNIQUE (name)
) ENGINE=InnoDB;

CREATE TABLE jobs (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    company_id           BIGINT NOT NULL,
    title                VARCHAR(150) NOT NULL,
    description          TEXT NOT NULL,
    location             VARCHAR(100),
    job_type             VARCHAR(20) NOT NULL DEFAULT 'FULL_TIME',
    skills_required      VARCHAR(500),
    min_cgpa             DECIMAL(4,2) DEFAULT 0,
    salary_lpa           DECIMAL(6,2),
    application_deadline DATE,
    drive_date           DATE,
    status               VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_at           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_job_company FOREIGN KEY (company_id)
        REFERENCES companies (id) ON DELETE RESTRICT,
    INDEX idx_job_company (company_id),
    INDEX idx_job_status_deadline (status, application_deadline),
    INDEX idx_job_title (title),
    INDEX idx_job_location (location)
) ENGINE=InnoDB;

CREATE TABLE job_applications (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    job_id     BIGINT NOT NULL,
    status     VARCHAR(30) NOT NULL DEFAULT 'APPLIED',
    notes      TEXT,
    applied_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
               ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_application_student_job UNIQUE (student_id, job_id),
    CONSTRAINT fk_app_student FOREIGN KEY (student_id)
        REFERENCES student_profiles (id) ON DELETE CASCADE,
    CONSTRAINT fk_app_job FOREIGN KEY (job_id)
        REFERENCES jobs (id) ON DELETE CASCADE,
    INDEX idx_app_status (status),
    INDEX idx_app_applied_at (applied_at)
) ENGINE=InnoDB;

CREATE TABLE interviews (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    application_id   BIGINT NOT NULL,
    round_name       VARCHAR(80) NOT NULL,
    scheduled_at     DATETIME NOT NULL,
    mode             VARCHAR(20) NOT NULL DEFAULT 'ONLINE',
    location_or_link VARCHAR(255),
    CONSTRAINT fk_interview_app FOREIGN KEY (application_id)
        REFERENCES job_applications (id) ON DELETE CASCADE,
    INDEX idx_interview_scheduled (scheduled_at)
) ENGINE=InnoDB;

CREATE TABLE saved_jobs (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    job_id     BIGINT NOT NULL,
    saved_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_saved_student_job UNIQUE (student_id, job_id),
    CONSTRAINT fk_saved_student FOREIGN KEY (student_id)
        REFERENCES student_profiles (id) ON DELETE CASCADE,
    CONSTRAINT fk_saved_job FOREIGN KEY (job_id)
        REFERENCES jobs (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE application_history (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    application_id BIGINT NOT NULL,
    old_status     VARCHAR(30),
    new_status     VARCHAR(30) NOT NULL,
    changed_by     BIGINT,
    remarks        VARCHAR(255),
    changed_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_hist_app FOREIGN KEY (application_id)
        REFERENCES job_applications (id) ON DELETE CASCADE,
    CONSTRAINT fk_hist_user FOREIGN KEY (changed_by)
        REFERENCES users (id) ON DELETE SET NULL,
    INDEX idx_hist_application (application_id)
) ENGINE=InnoDB;
