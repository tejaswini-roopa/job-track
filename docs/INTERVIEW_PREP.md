# JobTrack: Interview Preparation

## 60-second pitch
"JobTrack is a placement management platform I built with Spring Boot, Spring Security, JPA and MySQL. Students search jobs, apply and track each application; admins manage jobs and move applications through a validated hiring pipeline. I used JWT authentication with BCrypt, a layered architecture with DTOs and a global exception handler, and I tested it at every level: Mockito unit tests, JPA slice tests, MockMvc web tests, a full-context integration test and a Postman regression suite. The front end is Thymeleaf with vanilla JavaScript on purpose, to stay Java-focused."

## Resume bullets (pick 3-4)
- Built a full-stack job application and placement tracker with Spring Boot 3, Spring Security (JWT + BCrypt), Spring Data JPA and MySQL, serving student and admin roles
- Designed a normalized 8-table schema with unique constraints, indexes and an audit trail; modelled the application lifecycle as an enforced state machine with transactional history logging
- Implemented dynamic job search with JPA Specifications (company, role, skills, CGPA eligibility), pagination and sorting, and removed N+1 queries with entity graphs
- Created a layered REST API with DTO validation, centralized `@RestControllerAdvice` error handling and consistent HTTP semantics (201/204/404/409/422)
- Wrote about 90 automated tests (JUnit 5, Mockito, MockMvc, H2 integration) plus a 33-request Postman/Newman suite; CI via GitHub Actions
- Built a responsive Bootstrap 5 + vanilla JS UI with Chart.js dashboards and XSS-safe rendering

## Questions you should be ready for

**Spring and architecture**
1. *Why constructor injection?* Dependencies are final, the class is easy to unit test without Spring, and missing dependencies fail at startup.
2. *Why DTOs instead of returning entities?* They decouple the API from the schema, prevent leaking fields such as password hashes, and avoid lazy-loading errors outside a transaction.
3. *What does `@SpringBootApplication` do?* It combines `@Configuration`, `@EnableAutoConfiguration` and `@ComponentScan`.
4. *Why `open-in-view: false`?* Open Session In View keeps a DB session open during view rendering and hides N+1 problems. Disabling it forces all data access into the service layer.
5. *How does `@Transactional` work?* Spring wraps the bean in a proxy that opens a transaction, commits on return and rolls back on a RuntimeException. Self-invocation bypasses the proxy.

**JPA and database**
6. *What is the N+1 problem and how did you fix it?* Loading a list and then touching a lazy association per row issues N extra queries. I used `@EntityGraph` to fetch job and company in one join.
7. *Why LAZY on `@ManyToOne`?* EAGER is the default for `@ManyToOne` and causes surprise queries.
8. *Why is `SavedJob` an entity rather than `@ManyToMany`?* The link has its own attribute (`saved_at`), and explicit entities give control over the join table.
9. *How do you stop a student applying twice?* A service check for a friendly message, and a unique constraint on `(student_id, job_id)` as the real guarantee under concurrency. A `DataIntegrityViolationException` is translated to 409.
10. *Why `EnumType.STRING`?* Ordinals break when someone reorders the enum.
11. *What is dirty checking?* A managed entity's changes are flushed on commit without calling `save()`. Profile and notes updates rely on it.

**Security**
12. *Walk me through login.* `AuthenticationManager` loads the user and compares the BCrypt hash. On success we issue a signed JWT. Each request passes through `JwtAuthenticationFilter`, which validates the token and sets the `SecurityContext`.
13. *Why disable CSRF?* There is no session cookie. The token is sent in a header, so a forged cross-site request cannot carry it.
14. *Where do you store the JWT and what is the risk?* `localStorage`. It is exposed to XSS, which is why all dynamic UI text is escaped. A hardened version would use an HttpOnly cookie plus CSRF protection.
15. *Why reload the user from the DB on each request?* So a disabled account or changed role takes effect immediately, at the cost of one indexed query.
16. *401 vs 403?* 401: not authenticated. 403: authenticated but not allowed.
17. *Why does a student get 404 for another student's application?* Returning 403 would confirm the record exists.

**Design and testing**
18. *How is the status flow enforced?* `ApplicationStatus.canTransitionTo` holds the rules; the service checks it and writes an `ApplicationHistory` row in the same transaction.
19. *How did you test?* Unit tests with Mockito for rules, `@DataJpaTest` for queries, `@WebMvcTest` for controllers and security rules, `@SpringBootTest` for the real flow, and Postman against MySQL.
20. *H2 vs MySQL in tests?* H2 is fast and needs no setup but can differ from MySQL, so the Postman suite runs against the real database. Testcontainers would be the next step.
21. *What would you improve?* Refresh tokens and cookie auth, Flyway migrations, email notifications, caching analytics, Testcontainers, OpenAPI docs.

## Demo script (3 minutes)
1. Register a student, fill the profile (CGPA 8.4).
2. Search jobs by skill, save one, apply. Show the duplicate and eligibility errors.
3. Switch to admin: move the application to Under Review, schedule an interview, show the history.
4. Back to the student: dashboard charts, tracker stepper.
5. Disable the student as admin and show the immediate lockout.
6. Show `mvn test` and the Postman run.
