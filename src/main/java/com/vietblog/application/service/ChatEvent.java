package com.vietblog.application.service;

/** A committed chat change addressed to exactly one recipient. */
public record ChatEvent(String type, String conversationId, String recipientId, Object data) {}
