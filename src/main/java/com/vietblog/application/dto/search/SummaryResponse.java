package com.vietblog.application.dto.search;

import java.util.List;

public record SummaryResponse(String postId, String summary, List<String> sentences, String algorithm) {}
