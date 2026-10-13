# Administration, reports, notifications and tags

All responses use `ApiResponse`: `success`, `message`, `data`, and, for lists, `meta` (`page`, `limit`, `total`, `totalPages`). Pagination is one-based, defaults to 20 results, and accepts limits from 1 to 100. Enum request values use uppercase; response status/type/role values use lowercase. Authentication uses the existing bearer JWT.

## Administration

These endpoints require `ADMIN` at both the HTTP and service layers:

- `GET /api/admin/stats`: persisted user/post/comment/group/report totals, pending report count, cumulative post views and seven daily user/post growth counts. Dates use the Vietnam calendar. Cumulative views are not presented as daily views because the current data model does not retain view timestamps.
- `GET /api/admin/users?role=USER&search=...&page=1&limit=20`: paginated user directory, including email and account active state. `role` is optional (`USER`, `MODERATOR`, `ADMIN`).
- `PATCH /api/admin/users/{id}/role`, body `{"role":"MODERATOR"}`: change a user's role. Administrators cannot demote themselves or other administrator accounts, preventing concurrent requests from removing the last administrator.
- `PATCH /api/admin/users/{id}/ban`, body `{"banned":true}`: deactivate an account; `false` restores access. Self-bans and administrator bans are rejected. JWT authentication checks the persisted active flag so deactivated accounts cannot continue using an existing token.
- `GET /api/admin/posts?status=PUBLISHED&search=...&page=1&limit=20`: list posts across all statuses; `status` is optional (`DRAFT`, `PUBLISHED`, `HIDDEN`).
- `PATCH /api/admin/posts/{id}/status`, body `{"status":"HIDDEN"}`: change publication status. Hidden posts are retained for moderation/review.

User accounts are deactivated rather than physically deleted so authored content and report audit records remain consistent.

## Reports

- `POST /api/reports` (authenticated), body `{"targetId":"...","type":"POST","reason":"Spam","description":"Optional details"}`. Supported types: `POST`, `COMMENT`, `USER`, `GROUP`. Returns HTTP 201.
- `GET /api/admin/reports?status=PENDING&page=1&limit=20` (`ADMIN` or `MODERATOR`); statuses are `PENDING`, `RESOLVED`, `DISMISSED`.
- `PATCH /api/admin/reports/{id}/resolve`, body `{"action":"DELETE"}` (`ADMIN` or `MODERATOR`). `KEEP` retains the target, `DELETE` hides a post or deletes a comment and its replies, `BAN` deactivates the target user or content author/group owner. Group deletion requires `ADMIN` and removes its posts and memberships. User deletion through a report is rejected; use `BAN`.
- `PATCH /api/admin/reports/{id}/dismiss` (`ADMIN` or `MODERATOR`): dismiss without changing the target.

The server derives the target title from persisted content. Only published posts/comments can be reported; reports about private groups require membership. Reasons must contain 1–255 characters, descriptions at most 2,000. A reporter may have one pending report per target/type. Submissions lock the reporter row and moderation locks the report row, preventing duplicate pending reports and repeated resolution. Resolutions record action, time and actor and create a notification for the reporter in the same transaction. Moderators cannot ban moderators or administrators; nobody can ban themselves or an administrator.

## Notifications

All routes require authentication and operate exclusively on the current recipient:

- `GET /api/notifications?unreadOnly=false&page=1&limit=20`
- `GET /api/notifications/unread-count` returns `data: {"count":3}`.
- `PATCH /api/notifications/{id}/read` marks one notification read. Missing and other users' IDs both return 404.
- `PATCH /api/notifications/read-all` marks the current user's unread notifications read and returns `data: {"updated":3}`.

Notification creation is internal through `NotificationService.notify`; there is no endpoint that lets clients send arbitrary notifications to other users.

## Tags

`GET /api/tags?search=java&limit=30` is public and returns `[{"name":"java","postsCount":2}]`, ordered by published-post usage then name. `limit` accepts 1–100; search is at most 100 characters. Draft/hidden post tags never appear. Tags remain free-form post metadata: authors create/update/remove assignments through the existing post create/update APIs, avoiding a second unrelated tag catalog.

## Validation

Additional administration endpoints:

- `PATCH /api/admin/tags/{name}` with `{"name":"replacement"}` renames assignments across posts and removes duplicates. `DELETE /api/admin/tags/{name}` removes assignments. Both require `ADMIN` and return the affected post count.
- `PATCH /api/moderation/posts/{id}/status` accepts `PUBLISHED` or `HIDDEN` for `ADMIN`/`MODERATOR`, excluding author-private drafts.
- `GET /api/moderation/sentiment` returns positive/negative/neutral counts for all comments using the current lexicon analyzer; see [search limitations](search-api.md).

Comment creation also creates a notification for the post author when someone else comments. Account banning now blocks previously issued JWTs as well as new logins.

Service tests cover administrator protection, role authorization, pagination limits, duplicate report rejection, private/hidden target access, moderation effects and recipient ownership. Repository tests verify persisted filtering and notification ownership against an isolated test database. No production services or seeded accounts are required.
