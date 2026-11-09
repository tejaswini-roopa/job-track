USE jobtrack;
-- The admin account is created by the app on startup from ADMIN_EMAIL / ADMIN_PASSWORD (Phase 7).

INSERT INTO companies (name, industry, website, location) VALUES
 ('Infosys', 'IT Services', 'https://www.infosys.com', 'Bengaluru'),
 ('Zoho', 'Software Products', 'https://www.zoho.com', 'Chennai'),
 ('Accenture', 'Consulting', 'https://www.accenture.com', 'Pune');

INSERT INTO jobs (company_id, title, description, location, job_type, skills_required, min_cgpa, salary_lpa, application_deadline, drive_date, status)
SELECT id, 'Java Backend Developer', 'Build REST services with Spring Boot and MySQL.', 'Bengaluru', 'FULL_TIME',
       'Java, Spring Boot, MySQL', 7.00, 6.50, DATE_ADD(CURDATE(), INTERVAL 30 DAY), DATE_ADD(CURDATE(), INTERVAL 40 DAY), 'OPEN'
FROM companies WHERE name = 'Infosys';

INSERT INTO jobs (company_id, title, description, location, job_type, skills_required, min_cgpa, salary_lpa, application_deadline, drive_date, status)
SELECT id, 'Software Engineer Intern', 'Six month internship on product engineering.', 'Chennai', 'INTERNSHIP',
       'Java, SQL, Problem Solving', 6.00, 3.00, DATE_ADD(CURDATE(), INTERVAL 20 DAY), DATE_ADD(CURDATE(), INTERVAL 25 DAY), 'OPEN'
FROM companies WHERE name = 'Zoho';

INSERT INTO jobs (company_id, title, description, location, job_type, skills_required, min_cgpa, salary_lpa, application_deadline, drive_date, status)
SELECT id, 'Application Development Analyst', 'Develop and maintain enterprise Java applications.', 'Pune', 'FULL_TIME',
       'Java, Spring, REST', 8.00, 4.50, DATE_ADD(CURDATE(), INTERVAL 15 DAY), DATE_ADD(CURDATE(), INTERVAL 22 DAY), 'OPEN'
FROM companies WHERE name = 'Accenture';
