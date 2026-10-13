# Notification APIs

## Notifications

All routes require authentication and operate exclusively on the current recipient:

- `GET /api/notifications?unreadOnly=false&page=1&limit=20`
- `GET /api/notifications/unread-count` returns `data: {"count":3}`.
- `PATCH /api/notifications/{id}/read` marks one notification read. Missing and other users' IDs both return 404.
- `PATCH /api/notifications/read-all` marks the current user's unread notifications read and returns `data: {"updated":3}`.

Notification creation is internal through `NotificationService.notify`; there is no endpoint that lets clients send arbitrary notifications to other users.


Tests cover recipient ownership in both service and persistence layers.
