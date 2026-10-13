package com.vietblog;

import com.vietblog.application.dto.chat.ChatDtos.*;
import com.vietblog.application.service.ChatService;
import com.vietblog.domain.entity.*;
import com.vietblog.domain.exception.*;
import com.vietblog.infrastructure.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

@DataJpaTest(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
    "spring.datasource.url=jdbc:h2:mem:community_test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=PostgreSQL",
    "spring.datasource.driver-class-name=org.h2.Driver"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(ChatService.class)
class ChatPersistenceTests {

    @Autowired ChatService chat;
    @Autowired UserRepository users;
    @Autowired ChatMessageRepository messages;
    @Autowired ConversationRepository conversations;
    private User owner;
    private User member;
    private User outsider;

    @BeforeEach void setup() {
        owner = users.save(User.builder().name("Owner").email("owner@test.local").password("unused").build());
        member = users.save(User.builder().name("Member").email("member@test.local").password("unused").build());
        outsider = users.save(User.builder().name("Outsider").email("outsider@test.local").password("unused").build());
    }

    @Test void directConversationsAreReusedAndUnreadCountsAndReadReceiptsPersist() {
        var conversation = chat.create(owner.getId(), new ConversationRequest(List.of(member.getId()), null, false));
        var same = chat.create(member.getId(), new ConversationRequest(List.of(owner.getId()), null, false));
        assertThat(same.id()).isEqualTo(conversation.id());
        assertThat(conversations.count()).isEqualTo(1);
        var sent = chat.send(conversation.id(), owner.getId(), text("Hello"));
        assertThat(sent.status()).isEqualTo("sent");
        assertThat(chat.conversations(member.getId(), 1, 20).getContent().get(0).unreadCount()).isEqualTo(1);
        chat.read(conversation.id(), member.getId());
        assertThat(chat.conversations(member.getId(), 1, 20).getContent().get(0).unreadCount()).isZero();
        var stored = chat.history(conversation.id(), owner.getId(), null, 1, 20).getContent().get(0);
        assertThat(stored.status()).isEqualTo("read");
        assertThat(stored.readBy()).extracting(r -> r.user().id()).containsExactly(member.getId());
    }

    @Test void outsidersCannotReadSendMarkReadOrChangeConversationPreferences() {
        var conversation = chat.create(owner.getId(), new ConversationRequest(List.of(member.getId()), null, false));
        assertThatThrownBy(() -> chat.history(conversation.id(), outsider.getId(), null, 1, 20)).isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> chat.send(conversation.id(), outsider.getId(), text("Intrusion"))).isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> chat.read(conversation.id(), outsider.getId())).isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> chat.preference(conversation.id(), outsider.getId(), "pin", true)).isInstanceOf(ForbiddenException.class);
        assertThat(chat.canReceive(conversation.id(), outsider.getId())).isFalse();
        assertThat(messages.count()).isZero();
    }

    @Test void repliesCannotReferenceAnotherConversationAndOnlySenderCanRevoke() {
        var first = chat.create(owner.getId(), new ConversationRequest(List.of(member.getId()), null, false));
        var second = chat.create(owner.getId(), new ConversationRequest(List.of(outsider.getId()), null, false));
        var sent = chat.send(first.id(), owner.getId(), text("Secret"));
        assertThatThrownBy(() -> chat.send(second.id(), owner.getId(), new SendRequest("Reply", "text", sent.id(), null, null, null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> chat.revoke(first.id(), sent.id(), member.getId())).isInstanceOf(ForbiddenException.class);
        chat.revoke(first.id(), sent.id(), owner.getId());
        assertThat(chat.history(first.id(), member.getId(), null, 1, 20).getContent().get(0).content()).isEmpty();
        assertThat(chat.history(first.id(), member.getId(), "Secret", 1, 20).getTotalElements()).isZero();
    }

    @Test void blockingPreventsDirectMessagesInEitherDirection() {
        var conversation = chat.create(owner.getId(), new ConversationRequest(List.of(member.getId()), null, false));
        chat.block(member.getId(), owner.getId(), true);
        assertThatThrownBy(() -> chat.send(conversation.id(), owner.getId(), text("Blocked"))).isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> chat.send(conversation.id(), member.getId(), text("Blocked"))).isInstanceOf(ForbiddenException.class);
        chat.block(member.getId(), owner.getId(), false);
        assertThat(chat.send(conversation.id(), owner.getId(), text("Allowed")).content()).isEqualTo("Allowed");
    }

    private static SendRequest text(String content) { return new SendRequest(content, "text", null, null, null, null, null); }
}
