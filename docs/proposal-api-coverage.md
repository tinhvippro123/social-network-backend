# Proposal API coverage

This implementation follows the functional requirements in `Mentor/1250080199_LeThanhTinh_12_DH_CNPM3_DeCuongDACN.docx` in the parent repository. The backend now exposes persisted group/chat, moderation/report, notification, tag, search and text-analysis operations in addition to existing authentication, profiles, categories, posts and comments.

## Module documentation

- [Groups and chat](community-api.md): group membership and roles, private group posts, direct/group conversations, durable history, read receipts, blocking and authenticated WebSocket events. Redis pub/sub is optional for multiple application instances.
- [Administration](admin-api.md): dashboard totals and seven-day growth, account roles and banning, post moderation, reports, notification ownership and tag usage.
- [Search and analysis](search-api.md): keyword, TF-IDF, image, document and nearby search; TextRank summaries and lexicon sentiment, including input limits and research gaps.

## Additional post APIs

- `GET /api/posts?category=<id-or-slug>&search=<text>&page=1&limit=20` filters published posts. Pagination accepts positive pages and limits of 1–100.
- `GET /api/posts/trending?limit=10` ranks published posts by current age, net votes, views and comments, using gravity decay weighted by the Wilson lower confidence bound. This is a project-specific score rather than an exact reproduction of Reddit Hot. Scores are recomputed during reads; the current implementation scans all published posts and needs a scheduled/indexed ranking strategy at larger scale.
- `POST /api/posts/{id}/upvote` and `/downvote` require authentication. Repeating a vote removes it; changing direction removes the old vote. These aliases share the existing single-reaction-per-user model.
- `GET /api/bookmarks?page=1&limit=20` returns the authenticated user's saved published posts. Toggle remains `POST /api/posts/{id}/bookmarks`.
- `PATCH /api/moderation/posts/{id}/status` accepts `{"status":"HIDDEN"}` or `PUBLISHED` for administrators/moderators. Drafts remain private to their author.
- `PATCH /api/admin/tags/{name}` accepts `{"name":"replacement"}` and deduplicates assignments; `DELETE` removes that tag from posts. Both return the affected post count. New tags are created when assigned to a post.

Hidden/draft post detail is only available to its author. Public search, comments, reaction summaries, voting and bookmarks do not expose hidden content. Authors cannot republish moderator-hidden posts through ordinary edits. Banning an account blocks login and previously issued JWTs; chat sessions also recheck active status. Deleting a post removes image features, threaded comments, reactions and bookmarks in one transaction.

## Validation and remaining work

Run `gradlew.bat build` on Windows or `./gradlew build` elsewhere with JDK 17. The test suite uses H2 for persistence and application integration. The legacy local database creation helper is opt-in through `VIETBLOG_CREATE_LOCAL_DATABASE=true` and is skipped by default. The user's local `application.yml` is not part of these changes.

Tests cover algorithm behavior, malformed uploads, repository queries, group and conversation membership privacy, WebSocket recipient checks, moderation permissions, recipient-only notifications, JWT revocation on ban, public post visibility, vote transitions, tag renaming and dependent-row deletion. H2 tests do not verify PostgreSQL-specific behavior or a running multi-instance Redis deployment; those environments still need deployment smoke tests.

The existing frontend still has mocked features and must be connected to these contracts. No claim is made that every UI flow is integrated. Proposal research milestones still include pgvector/PostGIS acceleration, learned sentiment embeddings, a larger curated sentiment lexicon, evaluation datasets/metrics, and algorithm comparison experiments. Online presence, OCR and historical daily page-view tracking are not implemented.
