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
