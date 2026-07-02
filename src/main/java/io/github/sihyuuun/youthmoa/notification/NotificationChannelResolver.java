package io.github.sihyuuun.youthmoa.notification;

import io.github.sihyuuun.youthmoa.user.User;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * D1b: User 의 boolean 3필드를 활성 채널 리스트로 변환.
 * <p>
 * enum 선언 순서(KAKAO → SMS → EMAIL)를 보존해 반환하므로,
 * 뷰에서 "결과는 {A}·{B}로 안내드려요" 조립 시 그대로 사용 가능.
 */
@Component
public class NotificationChannelResolver {

    public List<NotificationChannel> activeChannelsFor(User user) {
        List<NotificationChannel> channels = new ArrayList<>(3);
        if (user.isNotifyKakao()) channels.add(NotificationChannel.KAKAO);
        if (user.isNotifySms()) channels.add(NotificationChannel.SMS);
        if (user.isNotifyEmail()) channels.add(NotificationChannel.EMAIL);
        return channels;
    }
}
