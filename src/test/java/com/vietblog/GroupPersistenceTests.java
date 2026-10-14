package com.vietblog;

import com.vietblog.application.dto.community.CommunityDtos.*;
import com.vietblog.application.service.GroupService;
import com.vietblog.domain.entity.*;
import com.vietblog.domain.exception.*;
import com.vietblog.infrastructure.repository.*;
import com.vietblog.infrastructure.security.HtmlSanitizer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import static org.assertj.core.api.Assertions.*;

@DataJpaTest(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
    "spring.datasource.url=jdbc:h2:mem:community_test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=PostgreSQL",
    "spring.datasource.driver-class-name=org.h2.Driver"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({GroupService.class, HtmlSanitizer.class})
class GroupPersistenceTests {
    @Autowired GroupService groups;
    @Autowired UserRepository users;
    @Autowired GroupMemberRepository groupMembers;
    @Autowired GroupPostRepository groupPosts;
    @Autowired PostRepository publicPosts;
    private User owner;
    private User member;
    private User outsider;

    @BeforeEach void setup() {
        owner = users.save(User.builder().name("Owner").email("owner@test.local").password("unused").build());
        member = users.save(User.builder().name("Member").email("member@test.local").password("unused").build());
        outsider = users.save(User.builder().name("Outsider").email("outsider@test.local").password("unused").build());
    }

    @Test void privateGroupContentRequiresMembershipAndNeverEntersPublicPosts() {
        var group = groups.create(owner.getId(), new GroupRequest("Private", "Description", null, null, false));
        long publicCount = publicPosts.count();
        var post = groups.createPost(group.id(), owner.getId(), new GroupPostRequest("Private content", "Secret", null, null));
        assertThatThrownBy(() -> groups.join(group.id(), outsider.getId())).isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> groups.posts(group.id(), outsider.getId(), 1, 20)).isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> groups.post(group.id(), post.id(), outsider.getId())).isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> groups.members(group.id(), outsider.getId(), 1, 20)).isInstanceOf(ForbiddenException.class);
        groups.addMember(group.id(), owner.getId(), member.getId());
        assertThat(groups.posts(group.id(), member.getId(), 1, 20).getContent()).extracting(GroupPostResponse::content).containsExactly("Secret");
        assertThat(groups.members(group.id(), member.getId(), 1, 20).getTotalElements()).isEqualTo(2);
        assertThat(publicPosts.count()).isEqualTo(publicCount);
    }

    @Test void groupRoleEscalationAndOwnerRemovalAreRejected() {
        var group = groups.create(owner.getId(), new GroupRequest("Public", null, null, null, true));
        groups.join(group.id(), member.getId());
        groups.join(group.id(), member.getId());
        assertThat(groupMembers.countByGroupId(group.id())).isEqualTo(2);
        assertThatThrownBy(() -> groups.setRole(group.id(), member.getId(), member.getId(), GroupMember.Role.ADMIN)).isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> groups.removeMember(group.id(), owner.getId(), owner.getId())).isInstanceOf(BusinessRuleException.class);
        groups.setRole(group.id(), owner.getId(), member.getId(), GroupMember.Role.MODERATOR);
        var post = groups.createPost(group.id(), owner.getId(), new GroupPostRequest("Title", "Content", null, null));
        groups.deletePost(group.id(), post.id(), member.getId());
        assertThat(groupPosts.findById(post.id())).isEmpty();
        groups.delete(group.id(), owner.getId());
        assertThat(groupMembers.countByGroupId(group.id())).isZero();
    }

}
