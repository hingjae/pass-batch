package com.fastcampus.pass.job.notification;

import com.fastcampus.pass.job.RollbackBatchMetaDataService;
import com.fastcampus.pass.repository.booking.BookingEntity;
import com.fastcampus.pass.repository.booking.BookingRepository;
import com.fastcampus.pass.repository.booking.BookingStatus;
import com.fastcampus.pass.repository.notification.NotificationRepository;
import com.fastcampus.pass.repository.pass.PassEntity;
import com.fastcampus.pass.repository.pass.PassRepository;
import com.fastcampus.pass.repository.pass.PassStatus;
import com.fastcampus.pass.repository.user.UserEntity;
import com.fastcampus.pass.repository.user.UserRepository;
import com.fastcampus.pass.repository.user.UserStatus;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@Import(RollbackBatchMetaDataService.class)
@SpringBatchTest
@ActiveProfiles("test")
@SpringBootTest
class SendNotificationBeforeClassJobConfigTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;
    @Autowired
    private PassRepository passRepository;
    @Autowired
    private RollbackBatchMetaDataService rollbackBatchMetaDataService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    @Qualifier("jpaSendNotificationBeforeClassJob")
    private Job sendNotificationBeforeClassJob;
    @Autowired
    private NotificationRepository notificationRepository;

    @AfterEach
    public void rollback() {
        rollbackBatchMetaDataService.clearBatchMetadata();
        bookingRepository.deleteAllInBatch();
        notificationRepository.deleteAllInBatch();
        passRepository.deleteAllInBatch();
    }

    @Test
    public void test_addNotificationStep() throws Exception {
        jobLauncherTestUtils.setJob(sendNotificationBeforeClassJob);
        // given
        addBookingEntity();

        JobExecution jobExecution = jobLauncherTestUtils.launchJob();
//        assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());
        assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
    }

    private void addBookingEntity() {
        final LocalDateTime now = LocalDateTime.now();
        final String userId = "A100" + RandomStringUtils.randomNumeric(4);

        UserEntity userEntity = new UserEntity();
        userEntity.setUserId(userId);
        userEntity.setUserName("김영희");
        userEntity.setStatus(UserStatus.ACTIVE);
        userEntity.setPhone("01033334444");
        userEntity.setMeta(Map.of("uuid", "abcd1234"));
        userRepository.save(userEntity);

        PassEntity passEntity = new PassEntity();
        passEntity.setPackageSeq(1);
        passEntity.setUserId(userId);
        passEntity.setStatus(PassStatus.PROGRESSED);
        passEntity.setRemainingCount(10);
        passEntity.setStartedAt(now.minusDays(60));
        passEntity.setEndedAt(now.minusDays(1));
        passRepository.save(passEntity);

        BookingEntity bookingEntity = new BookingEntity();
        bookingEntity.setPassSeq(passEntity.getPassSeq());
        bookingEntity.setUserId(userId);
        bookingEntity.setStatus(BookingStatus.READY);
        bookingEntity.setStartedAt(now.plusMinutes(10));
        bookingEntity.setEndedAt(bookingEntity.getStartedAt().plusMinutes(50));
        bookingRepository.save(bookingEntity);
    }

}
