# Search and text analysis APIs

All responses use the existing `ApiResponse` envelope. GET routes below are public unless marked otherwise. `limit` defaults to 20 and accepts 1–100. Only published posts are searchable; drafts and hidden posts return 404 from public analysis routes.

## Endpoints

- `GET /api/search?q=java&type=keyword&limit=20`: all normalized query terms must occur in the title, excerpt, content or tags.
- `GET /api/search/semantic?q=java&limit=20`: TF-IDF cosine ranking. Also available through `GET /api/search?type=semantic&q=java`.
- `POST /api/search/image`: authenticated multipart request with field `image` containing a JPEG or PNG.
- `POST /api/search/document`: authenticated multipart request with field `file` containing PDF or DOCX. Extracted text becomes a TF-IDF query.
- `POST /api/posts/{id}/search-image`: authenticated author or administrator uploads multipart field `image` to create or replace the post's image feature index. This is separate from Cloudinary upload; it never fetches remote URLs. Existing cover images need this indexing step before they can appear in image results.
- `GET /api/posts/nearby?lat=10.8&lng=106.7&radius=5&limit=20`: Haversine distance ordering; radius is in kilometers, greater than zero and at most 500. Responses include latitude, longitude and locationAddress.
- `GET /api/posts/{id}/summary?sentences=3`: returns `postId`, `summary`, selected `sentences` and `algorithm`. Sentence count accepts 1–10.
- `POST /api/nlp/sentiment`: authenticated JSON body `{"text":"Bài viết rất hay"}`. Returns label, score, positive/negative lexicon matches and algorithm identifier.
- `GET /api/posts/{postId}/comments/{commentId}/sentiment`: analyzes a comment belonging to that published post.
- `GET /api/moderation/sentiment`: administrator or moderator only; positive, negative and neutral counts across stored comments, for moderation charts.

## Algorithms and limits

Algorithms are implemented in plain Java. Text search uses Unicode NFC normalization, stop words, logarithmic term frequency, smoothed IDF and cosine similarity. TextRank uses sentence similarity edges with damping, dangling-node redistribution and original sentence order in the result. This is lexical relevance and extractive summarization, not pretrained language understanding.

Image search combines a 32-bin HSV histogram, 16-bin Sobel orientation histogram and three horizontal GLCM statistics into a 51-dimensional feature vector. Vectors are persisted per post and compared by cosine similarity. This measures color/texture similarity, not object recognition.

Sentiment currently uses a small Vietnamese/English dictionary and a two-token negation window. It has no learned word embeddings or UIT-VSFC training/evaluation. Neutral can mean no dictionary evidence; results should assist human moderation, never automatically ban accounts.

The reference implementation searches the latest 2,000 published posts, with at most 20,000 characters per post. Nearby and image candidate selection share this bound. Older posts outside that window are not searched. There are no pgvector or PostGIS indexes yet. These are explicit remaining research/scaling tasks from the proposal, not prerequisites for invoking these APIs.

Files are limited to 5 MiB. Images are JPEG/PNG only and at most 16 million source pixels. PDF extraction accepts at most 100 pages and 100,000 text characters, rejecting encrypted PDFs. Scanned PDFs without a text layer need OCR, which is not implemented. DOCX extraction bounds archive entry count and decompressed size and disables DTD/external entities. Summary analysis uses at most 100,000 characters and 200 sentences. Multipart server limits can impose a stricter cap.

The PDF parser dependency follows the [Apache PDFBox release page](https://pdfbox.apache.org/download.cgi). No AI/ML dependency is used.
