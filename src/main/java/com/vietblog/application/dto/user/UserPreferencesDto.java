package com.vietblog.application.dto.user;

import com.vietblog.domain.entity.User;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPreferencesDto {
    @Pattern(regexp = "public|followers|private", message = "Chế độ hiển thị hồ sơ không hợp lệ")
    private String profileVisibility;

    @Pattern(regexp = "everyone|followers|nobody", message = "Chế độ nhận tin nhắn không hợp lệ")
    private String allowMessages;

    private Boolean showOnlineStatus;
    private Boolean showReadReceipts;

    private Boolean emailNewFollower;
    private Boolean emailNewComment;
    private Boolean emailNewMessage;

    private Boolean pushNewFollower;
    private Boolean pushNewComment;
    private Boolean pushNewMessage;
    private Boolean pushTrending;

    public static UserPreferencesDto fromEntity(User user) {
        return UserPreferencesDto.builder()
                .profileVisibility(user.effectiveProfileVisibility())
                .allowMessages(user.effectiveAllowMessages())
                .showOnlineStatus(user.isShowOnlineStatusEffective())
                .showReadReceipts(user.isShowReadReceiptsEffective())
                .emailNewFollower(user.isEmailNewFollowerEffective())
                .emailNewComment(user.isEmailNewCommentEffective())
                .emailNewMessage(user.isEmailNewMessageEffective())
                .pushNewFollower(user.isNotifyNewFollowerEffective())
                .pushNewComment(user.isNotifyNewCommentEffective())
                .pushNewMessage(user.isNotifyNewMessageEffective())
                .pushTrending(user.isNotifyTrendingEffective())
                .build();
    }
}

