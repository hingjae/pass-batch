package com.fastcampus.pass.job.notification;

import com.fastcampus.pass.adapter.KakaoTalkMessageAdapter;
import com.fastcampus.pass.repository.notification.NotificationEntity;
import com.fastcampus.pass.repository.notification.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@RequiredArgsConstructor
@Component
public class SendNotificationItemWriter implements ItemWriter<NotificationEntity> {

    private final NotificationRepository notificationRepository;
    private final KakaoTalkMessageAdapter kakaoTalkMessageAdapter;

    @Override
    public void write(Chunk<? extends NotificationEntity> notificationEntities) throws Exception {
        int count = 0;
        LocalDateTime now = LocalDateTime.now();

        for (NotificationEntity notificationEntity : notificationEntities) {
            boolean successful = kakaoTalkMessageAdapter.mockMessage(notificationEntity.getUuid(), notificationEntity.getText());

            if (successful) {
                notificationEntity.setSent(true);
                notificationEntity.setSentAt(now);
                notificationRepository.save(notificationEntity);
                count++;
            }
        }

        log.info("SendNotificationItemWriter - write: 수업 전 알람 {}/{}건 전송 성공", count, notificationEntities.size());
    }
}
