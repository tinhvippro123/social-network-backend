package com.vietblog.application.dto.search;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TextRequest(@NotBlank @Size(max = 10000) String text) {}
