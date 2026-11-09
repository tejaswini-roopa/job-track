# JobTrack REST API

Log in at `POST /api/auth/login` and send `Authorization: Bearer <token>` on every other call.
`/api/auth/**` and `/api/health` are public. `/api/admin/**` needs role ADMIN; all other `/api/**` needs role STUDENT.
Errors always have the shape `{timestamp, status, error, message, path, fieldErrors?}`.
Lists are paginated: `?page=0&size=10&sort=field,asc|desc` (max size 50).

| Method | Path | Description | Success |
|---|---|---|---|
| POST | /api/auth/register | Register a student | 201 |
| POST | /api/auth/login | Log in, returns a JWT | 200 |
| GET / PUT | /api/student/profile | View / update profile | 200 |
| GET | /api/student/dashboard | Stats, charts data, upcoming interviews | 200 |
| GET | /api/student/saved-jobs | Wishlist | 200 |
| POST / DELETE | /api/student/saved-jobs/{jobId} | Save / remove a job | 201 / 204 |
| GET | /api/jobs | Search: company, role, location, skills, jobType, eligibleOnly | 200 |
| GET | /api/jobs/{id} | Job details | 200 |
| POST | /api/applications | Apply `{jobId}` | 201 |
| GET | /api/applications | My applications, filter by `status` | 200 |
| GET | /api/applications/{id} | One application | 200 |
| PATCH | /api/applications/{id}/notes | Update personal notes | 200 |
| GET | /api/applications/{id}/history | Status history | 200 |
| GET / POST | /api/admin/companies | List / create companies | 200 / 201 |
| GET / PUT / DELETE | /api/admin/companies/{id} | Read / update / delete | 200 / 200 / 204 |
| GET / POST | /api/admin/jobs | List all / create job | 200 / 201 |
| PUT / DELETE | /api/admin/jobs/{id} | Update / delete job | 200 / 204 |
| PATCH | /api/admin/jobs/{id}/status | Open or close a job | 200 |
| GET | /api/admin/applications | All applications, filter by `status` | 200 |
| PATCH | /api/admin/applications/{id}/status | Change status (state machine) | 200 |
| POST | /api/admin/applications/{id}/interviews | Schedule an interview | 201 |
| GET | /api/admin/applications/{id}/history | Audit trail | 200 |
| GET | /api/admin/students | List students | 200 |
| PATCH | /api/admin/students/{id}/enabled | Enable / disable account | 200 |
| DELETE | /api/admin/students/{id} | Delete student | 204 |
| GET | /api/admin/analytics | Success rate, company stats, monthly trend | 200 |

Status codes: 400 validation, 401 unauthenticated, 403 forbidden, 404 not found,
409 duplicate, 422 business rule violated.
