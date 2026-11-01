package com.vietblog.application.service;

import com.vietblog.application.dto.chat.ChatDtos.*;
import com.vietblog.application.dto.community.CommunityDtos.PublicUser;
import com.vietblog.domain.entity.*;
import com.vietblog.domain.exception.*;
import com.vietblog.infrastructure.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service @RequiredArgsConstructor @Transactional
public class ChatService {
    private final ConversationRepository conversations;
    private final ConversationMemberRepository members;
    private final ChatMessageRepository messages;
    private final ChatBlockRepository blocks;
    private final UserRepository users;
    private final ApplicationEventPublisher events;
    @PersistenceContext private EntityManager entityManager;

    @Transactional(readOnly = true)
    public Page<ConversationResponse> conversations(String userId, int page, int limit) {
        GroupService.page(page, limit);
        return members.findByUserIdAndHiddenFalse(userId, PageRequest.of(page - 1, limit,
                Sort.by(Sort.Order.desc("pinned"), Sort.Order.desc("conversation.updatedAt"), Sort.Order.desc("id"))))
                .map(m -> conversation(m.getConversation(), m));
    }

    public ConversationResponse create(String userId, ConversationRequest request) {
        User creator = user(userId);
        Set<String> ids = new TreeSet<>(request.participantIds());
        ids.add(userId);
        if (ids.size() < 2 || ids.size() > 50 || (!request.isGroup() && ids.size() != 2)) invalid();
        if (request.isGroup() && (request.name() == null || request.name().isBlank())) invalid();
        List<User> participants = ids.stream().map(this::user).toList();
        String directKey = request.isGroup() ? null : String.join(":", ids);
        if (!request.isGroup()) {
            requireUnblocked(participants.get(0).getId(), participants.get(1).getId());
            // Every request for the same pair takes the same user lock before checking uniqueness.
            entityManager.lock(participants.get(0), LockModeType.PESSIMISTIC_WRITE);
            Optional<Conversation> existing = conversations.findByDirectKey(directKey);
            if (existing.isPresent()) {
                ConversationMember membership = member(existing.get().getId(), userId);
                membership.setHidden(false);
                return conversation(existing.get(), membership);
            }
        }
        Conversation conversation = conversations.save(Conversation.builder().creator(creator).name(request.name())
                .groupChat(request.isGroup()).directKey(directKey).build());
        for (User participant : participants) members.save(ConversationMember.builder().conversation(conversation).user(participant).build());
        for (ConversationMember recipient : members.findByConversationId(conversation.getId())) {
            events.publishEvent(new ChatEvent("conversation.created", conversation.getId(), recipient.getUser().getId(), conversation(conversation, recipient)));
        }
        return conversation(conversation, member(conversation.getId(), userId));
    }

    @Transactional(readOnly = true)
    public Page<MessageResponse> history(String id, String userId, String query, int page, int limit) {
        member(id, userId);
        PageRequest pagination = GroupService.page(page, limit);
        List<ConversationMember> participants = members.findByConversationId(id);
        Page<ChatMessage> result = query == null || query.isBlank() ? messages.findByConversationId(id, pagination)
                : messages.findByConversationIdAndRevokedFalseAndContentContainingIgnoreCase(id, query.trim(), pagination);
        return result.map(message -> message(message, userId, participants));
    }

    public MessageResponse send(String id, String userId, SendRequest request) {
        Conversation conversation = lock(id);
        ConversationMember sender = member(id, userId);
        if (!sender.getUser().isActive()) forbidden();
        List<ConversationMember> participants = members.findByConversationId(id);
        if (!conversation.isGroupChat()) {
            participants.stream().filter(m -> !m.getUser().getId().equals(userId))
                    .forEach(m -> requireUnblocked(userId, m.getUser().getId()));
        }
        String type = request.type() == null ? "text" : request.type();
        if (!Set.of("text", "image", "file").contains(type) || request.content() == null || request.content().length() > 10000) invalid();
        if (type.equals("text") && request.content().isBlank()) invalid();
        if (type.equals("image")) requireUrl(request.imageUrl());
        if (type.equals("file")) requireUrl(request.fileUrl());
        ChatMessage reply = request.replyToId() == null ? null : findMessage(id, request.replyToId());
        ChatMessage message = ChatMessage.builder().conversation(conversation).sender(sender.getUser()).content(request.content())
                .type(type).replyTo(reply).fileName(request.fileName()).fileSize(request.fileSize()).fileUrl(request.fileUrl()).imageUrl(request.imageUrl()).build();
        messages.save(message);
        conversation.setUpdatedAt(message.getCreatedAt());
        participants.forEach(m -> m.setHidden(false));
        for (ConversationMember recipient : participants) {
            events.publishEvent(new ChatEvent("message.created", id, recipient.getUser().getId(), message(message, recipient.getUser().getId(), participants)));
        }
        return message(message, userId, participants);
    }

    public void read(String id, String userId) {
        lock(id);
        ConversationMember reader = member(id, userId);
        // Send and read share the conversation lock: newly arriving messages cannot be marked read accidentally.
        LocalDateTime boundary = messages.findFirstByConversationIdOrderByCreatedAtDescIdDesc(id)
                .map(m -> m.getCreatedAt().truncatedTo(ChronoUnit.MICROS))
                .orElse(LocalDateTime.now().truncatedTo(ChronoUnit.MICROS));
        reader.setLastReadAt(boundary);
        members.saveAndFlush(reader);
        for (ConversationMember recipient : members.findByConversationId(id)) {
            events.publishEvent(new ChatEvent("conversation.read", id, recipient.getUser().getId(),
                    Map.of("userId", userId, "readAt", boundary.toString())));
        }
    }

    public void preference(String id, String userId, String preference, boolean value) {
        ConversationMember member = member(id, userId);
        switch (preference) {
            case "pin" -> member.setPinned(value);
            case "mute" -> member.setMuted(value);
            case "hide" -> member.setHidden(value);
            default -> invalid();
        }
    }

    public void setNickname(String id, String userId, String targetUserId, String nickname) {
        member(id, userId);
        List<ConversationMember> participants = members.findByConversationId(id);
        String target = (targetUserId != null && !targetUserId.isBlank()) ? targetUserId
                : participants.stream().filter(m -> !m.getUser().getId().equals(userId)).map(m -> m.getUser().getId()).findFirst().orElse(userId);
        ConversationMember targetMember = members.findByConversationIdAndUserId(id, target)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));
        String cleaned = (nickname == null || nickname.isBlank()) ? null : nickname.trim();
        targetMember.setNickname(cleaned);
        members.save(targetMember);
        for (ConversationMember recipient : participants) {
            events.publishEvent(new ChatEvent("conversation.nickname", id, recipient.getUser().getId(),
                    Map.of("targetUserId", target, "nickname", cleaned != null ? cleaned : "")));
        }
    }

    public void block(String actorId, String targetId, boolean blocked) {
        if (actorId.equals(targetId)) invalid();
        User actor = user(actorId);
        User target = user(targetId);
        entityManager.lock(actor, LockModeType.PESSIMISTIC_WRITE);
        if (blocked) {
            if (!blocks.existsByBlockerIdAndBlockedId(actorId, targetId)) blocks.save(ChatBlock.builder().blocker(actor).blocked(target).build());
        } else blocks.deleteByBlockerIdAndBlockedId(actorId, targetId);
    }

    public MessageResponse revoke(String id, String messageId, String userId) {
        lock(id);
        member(id, userId);
        ChatMessage message = findMessage(id, messageId);
        if (!message.getSender().getId().equals(userId)) forbidden();
        message.setRevoked(true);
        message.setContent("");
        message.setFileName(null); message.setFileSize(null); message.setFileUrl(null); message.setImageUrl(null);
        List<ConversationMember> participants = members.findByConversationId(id);
        for (ConversationMember recipient : participants) {
            events.publishEvent(new ChatEvent("message.revoked", id, recipient.getUser().getId(), message(message, recipient.getUser().getId(), participants)));
        }
        return message(message, userId, participants);
    }

    public MessageResponse pinMessage(String id, String messageId, String userId, boolean pinned) {
        lock(id);
        member(id, userId);
        ChatMessage message = findMessage(id, messageId);
        message.setPinned(pinned);
        List<ConversationMember> participants = members.findByConversationId(id);
        for (ConversationMember recipient : participants) {
            events.publishEvent(new ChatEvent("message.updated", id, recipient.getUser().getId(), message(message, recipient.getUser().getId(), participants)));
        }
        return message(message, userId, participants);
    }

    @Transactional(readOnly = true)
    public boolean canReceive(String conversationId, String userId) {
        return users.findById(userId).map(User::isActive).orElse(false)
                && members.existsByConversationIdAndUserId(conversationId, userId);
    }

    private ConversationResponse conversation(Conversation conversation, ConversationMember viewer) {
        List<ConversationMember> allMembers = members.findByConversationId(conversation.getId());
        List<PublicUser> participants = allMembers.stream().map(m -> PublicUser.from(m.getUser())).toList();
        PublicUser other = participants.stream().filter(u -> !u.id().equals(viewer.getUser().getId())).findFirst().orElse(PublicUser.from(viewer.getUser()));
        ConversationMember otherMember = allMembers.stream().filter(m -> !m.getUser().getId().equals(viewer.getUser().getId())).findFirst().orElse(viewer);
        String nickname = conversation.isGroupChat() ? null : otherMember.getNickname();
        String displayName = conversation.isGroupChat() ? conversation.getName()
                : (nickname != null && !nickname.isBlank() ? nickname : other.name());
        ChatMessage latest = messages.findFirstByConversationIdOrderByCreatedAtDescIdDesc(conversation.getId()).orElse(null);
        return new ConversationResponse(conversation.getId(), displayName,
                conversation.isGroupChat() ? null : other.avatar(), latest == null || latest.isRevoked() ? "" : latest.getContent(),
                latest == null ? conversation.getCreatedAt() : latest.getCreatedAt(),
                messages.countUnread(conversation.getId(), viewer.getUser().getId(), viewer.getLastReadAt()), conversation.isGroupChat(), false,
                viewer.isPinned(), viewer.isMuted(), participants, nickname);
    }

    private MessageResponse message(ChatMessage message, String userId, List<ConversationMember> participants) {
        List<ReadReceipt> receipts = participants.stream().filter(m -> !m.getUser().getId().equals(message.getSender().getId()))
                .filter(m -> m.getLastReadAt() != null && !m.getLastReadAt().isBefore(message.getCreatedAt()))
                .map(m -> new ReadReceipt(PublicUser.from(m.getUser()), m.getLastReadAt())).toList();
        ChatMessage reply = message.getReplyTo();
        ReplyResponse replyResponse = reply == null ? null : new ReplyResponse(reply.getId(), reply.isRevoked() ? "" : reply.getContent(), reply.getSender().getName());
        String status = message.isRevoked() ? "revoked" : receipts.size() == participants.size() - 1 ? "read" : "sent";
        return new MessageResponse(message.getId(), message.getConversation().getId(), message.isRevoked() ? "" : message.getContent(),
                PublicUser.from(message.getSender()), message.getCreatedAt(), message.getSender().getId().equals(userId), message.isRevoked(),
                message.getType(), status, receipts, replyResponse, message.getFileName(), message.getFileSize(), message.getFileUrl(), message.getImageUrl(), message.isPinned());
    }
    private Conversation lock(String id) {
        return conversations.findLockedById(id).orElseThrow(() -> new ResourceNotFoundException(ErrorCode.CONVERSATION_NOT_FOUND));
    }
    private ConversationMember member(String id, String userId) {
        return members.findByConversationIdAndUserId(id, userId).orElseThrow(() -> new ForbiddenException(ErrorCode.FORBIDDEN));
    }
    private ChatMessage findMessage(String id, String messageId) {
        return messages.findByIdAndConversationId(messageId, id).orElseThrow(() -> new ResourceNotFoundException(ErrorCode.MESSAGE_NOT_FOUND));
    }
    private User user(String id) {
        User user = users.findById(id).orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));
        if (!user.isActive()) forbidden();
        return user;
    }
    private void requireUnblocked(String first, String second) {
        if (blocks.existsByBlockerIdAndBlockedId(first, second) || blocks.existsByBlockerIdAndBlockedId(second, first)) forbidden();
    }
    private static void requireUrl(String value) {
        try {
            URI uri = URI.create(value == null ? "" : value);
            if (!("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme())) || uri.getHost() == null) invalid();
        } catch (IllegalArgumentException ex) { invalid(); }
    }
    private static void invalid() { throw new BusinessRuleException(ErrorCode.INVALID_INPUT); }
    private static void forbidden() { throw new ForbiddenException(ErrorCode.FORBIDDEN); }
}
