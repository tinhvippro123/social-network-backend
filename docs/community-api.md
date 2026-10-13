# Community groups and chat API

All REST routes require `Authorization: Bearer <access token>` and use the standard `ApiResponse` envelope. Lists include `meta` with one-based `page`, `limit`, `total` and `totalPages`; limits must be 1–100. User objects contain public ID, name and avatar only. Timestamps are ISO local date-time strings, matching the existing backend.

## Groups

- `GET /api/groups?page=1&limit=20&q=&joined=false`: discover groups or list joined groups. Private groups expose only directory metadata.
- `POST /api/groups`: create a group using `{name, description?, avatar?, coverImage?, isPublic?}`. The creator becomes its owner.
- `GET /api/groups/{id}`: directory metadata, counts and the viewer's membership role.
- `PUT /api/groups/{id}`: replace metadata; owner and group administrators only.
- `DELETE /api/groups/{id}`: owner or site administrator deletes the group, its memberships and group posts.
- `POST /api/groups/{id}/join`: idempotently join a public group. Private groups require an owner/administrator invitation.
- `DELETE /api/groups/{id}/members/me`: leave. The owner must keep ownership or delete the group.
- `GET /api/groups/{id}/members`: paginated membership list; private groups require membership.
- `POST /api/groups/{id}/members` with `{userId}`: owner/administrator adds an active user.
- `DELETE /api/groups/{id}/members/{userId}`: owner/administrator removes a member. Administrators cannot remove other administrators or the owner.
- `PATCH /api/groups/{id}/members/{userId}/role` with `{role: "ADMIN" | "MODERATOR" | "MEMBER"}`: owner only. Ownership cannot be assigned through this endpoint.
- `GET /api/groups/{id}/posts` and `GET /api/groups/{id}/posts/{postId}`: public group content or private content for members.
- `POST /api/groups/{id}/posts` with `{title, content, excerpt?, coverImage?}`: members only.
- `PUT /api/groups/{id}/posts/{postId}`: author who is still a member updates a post with the same request shape.
- `DELETE /api/groups/{id}/posts/{postId}`: author, owner, administrator or moderator removes a post.

Group posts use their own persistence table and API. They never enter public post feeds, public search, maps, or `/api/posts/{id}`; this prevents private group content from leaking through those endpoints. Group posts do not currently include the reactions/comments attached to ordinary public posts.

## Chat

- `GET /api/chat/conversations`: paginated viewer conversations, pinned first then most recently updated.
- `POST /api/chat/conversations` with `{participantIds: ["other-user-id"], isGroup: false}`: create or reuse a direct conversation. Include `isGroup: true, name: "Team"` for a group chat. The creator is automatically included; maximum 50 participants.
- `GET /api/chat/conversations/{id}/messages?page=1&limit=50&q=`: persisted history or text search, newest first. Reverse each page for chronological rendering.
- `POST /api/chat/conversations/{id}/messages` with `{content, type?: "text" | "image" | "file", replyToId?, imageUrl?, fileUrl?, fileName?, fileSize?}`: send. Image/file messages require an HTTP(S) attachment URL; upload files through the existing file API first. Replies must refer to the same conversation.
- `POST /api/chat/conversations/{id}/read`: mark messages currently persisted as read; update unread count and receipts.
- `PATCH /api/chat/conversations/{id}/pin` and `/mute` with `{value: true}`: save per-user preferences.
- `DELETE /api/chat/conversations/{id}`: hide the conversation for the current user. History remains, and a new message makes it visible again.
- `DELETE /api/chat/conversations/{id}/messages/{messageId}`: sender revokes content and attachments while preserving the history entry.
- `PATCH /api/chat/conversations/{id}/messages/{messageId}/pin` with `{value: true}`: members change a message's shared pin.
- `POST /api/chat/blocks/{userId}` and `DELETE /api/chat/blocks/{userId}`: block/unblock direct messages. Blocking applies in both directions to direct conversations; group chats remain shared spaces.

Every conversation operation checks membership. `isOwn` is viewer-specific; `status` is `sent`, `read` when all other participants have read, or `revoked`. Read receipts contain public user information and `readAt`. Online presence is not tracked and `isOnline` remains false.

## Real-time WebSocket

Connect to `ws://localhost:8080/ws/chat` (use `wss://` in deployment). The origin must appear in `cors.allowed-origins`. Send the first frame within 10 seconds:

```json
{"type":"auth","token":"<access token>"}
```

The server returns `{"type":"authenticated","userId":"..."}`. It checks token expiry and active account status during delivery and periodically closes expired sessions. Tokens are never passed in URLs. There are no client-controlled topic subscriptions; only the authenticated user's current conversation events are delivered.

REST writes automatically deliver events after their database transaction commits. A connected client can also send:

```json
{"type":"message.send","conversationId":"...","message":{"content":"Hello","type":"text"}}
```

```json
{"type":"conversation.read","conversationId":"..."}
```

`{"type":"ping"}` receives `{"type":"pong"}`. Accepted messages receive a `message.accepted` acknowledgement with `messageId`. Events have `{type, conversationId, recipientId, data}` with types `conversation.created`, `message.created`, `conversation.read`, `message.revoked`, and `message.updated`. Message events include the same viewer-specific DTO as REST. Clients should deduplicate by message ID and fetch history after reconnecting; WebSocket delivery is transient, while message history is durable.

Single-instance delivery works without Redis. For multiple instances set `CHAT_REDIS_ENABLED=true` (Spring property `chat.redis.enabled`) and configure `spring.data.redis.*`. All instances publish/subscribe through `vietblog.chat.events`; each validates the recipient locally. If publishing fails, the committed message remains in the database and local sockets still receive it. Remote clients recover using history. Redis must be available during startup when this mode is enabled.
