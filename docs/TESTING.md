# Testing JobTrack

## Layers (the test pyramid)

| Layer | Tools | What it proves | Needs |
|---|---|---|---|
| Unit | JUnit 5, Mockito, AssertJ | Business rules in services and enums | nothing |
| Repository | `@DataJpaTest`, H2 | Mappings, unique keys, JPQL and Specification queries | nothing |
| Web slice | `@WebMvcTest`, MockMvc | Controllers, validation, error JSON, role rules, Thymeleaf pages | nothing |
| Integration | `@SpringBootTest`, MockMvc, H2 | Whole app incl. real JWT security | nothing |
| API regression | Postman / Newman | The running app against MySQL | running app |

## Run the automated tests
```
mvn test                     # all JUnit tests
mvn test -Dtest=ApplicationServiceTest
mvn verify                   # also builds the jar
```
Coverage report (JaCoCo): open `target/site/jacoco/index.html` after `mvn test`.

## Run the Postman regression suite
1. Start the app against MySQL with `ADMIN_EMAIL` / `ADMIN_PASSWORD` set.
2. Import `postman/JobTrack.regression.postman_collection.json` and
   `postman/JobTrack.local.postman_environment.json`, select the environment and fill in `adminPassword`.
3. Run the collection with the Collection Runner. All 33 requests should pass, and the last three clean up the data.

Command line (Newman):
```
npm install -g newman
newman run postman/JobTrack.regression.postman_collection.json \
       -e postman/JobTrack.local.postman_environment.json
```

## Manual UI checklist
- Register with a weak password: errors appear under the field.
- Log in with a wrong password: a clear message, button re-enabled.
- Student: profile, search, filters, wishlist, apply, notes, history, dashboard charts.
- Student opens `/admin`: redirected to the dashboard. Logged out visit to `/dashboard`: redirected to login.
- Admin: add company and job, change a status, schedule an interview, disable a student.
- Disabled student's open tab is sent to login on the next action.
- Resize to phone width: navbar collapses, cards stack, tables scroll.
