# Post interactions

Requires the search PR for the image index deleted with each post and location response fields.

## Additional post APIs

- `GET /api/posts?category=<id-or-slug>&search=<text>&page=1&limit=20` filters published posts. Pagination accepts positive pages and limits of 1–100.
- `GET /api/posts/trending?limit=10` ranks published posts by current age, net votes, views and comments, using gravity decay weighted by the Wilson lower confidence bound. This is a project-specific score rather than an exact reproduction of Reddit Hot. Scores are recomputed during reads; the current implementation scans all published posts and needs a scheduled/indexed ranking strategy at larger scale.
- `POST /api/posts/{id}/upvote` and `/downvote` require authentication. Repeating a vote removes it; changing direction removes the old vote. These aliases share the existing single-reaction-per-user model.
- `GET /api/bookmarks?page=1&limit=20` returns the authenticated user's saved published posts. Toggle remains `POST /api/posts/{id}/bookmarks`.

Hidden/draft post detail is only available to its author. Public search, comments, reaction summaries, voting and bookmarks do not expose hidden content. Authors cannot republish moderator-hidden posts through ordinary edits. Banning an account blocks login and previously issued JWTs; chat sessions also recheck active status. Deleting a post removes image features, threaded comments, reactions and bookmarks in one transaction.
