# Chat APIs

Requires the community PR for the shared public user DTO and pagination rules.

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
