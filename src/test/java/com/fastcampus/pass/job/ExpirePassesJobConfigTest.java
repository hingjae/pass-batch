package com.fastcampus.pass.job;

import com.fastcampus.pass.repository.pass.PassEntity;
import com.fastcampus.pass.repository.pass.PassRepository;
import com.fastcampus.pass.repository.pass.PassStatus;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@Import(RollbackBatchMetaDataService.class)
@SpringBatchTest
@ActiveProfiles("test")
@SpringBootTest
class ExpirePassesJobConfigTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private PassRepository passRepository;

    @Autowired
    private RollbackBatchMetaDataService rollbackBatchMetaDataService;

    @Autowired
    @Qualifier("expirePassesJob")
    private Job expirePassesJob;

    @BeforeEach
    public void setJob() {
        jobLauncherTestUtils.setJob(expirePassesJob);
    }

    @AfterEach
    public void rollBack() {
        rollbackBatchMetaDataService.clearBatchMetadata();
        passRepository.deleteAllInBatch();
    }

    /**
     * Spring Batch Job은 외부 트랜잭션을 허용하지 않는다.
     */
    @DisplayName("endedAt이 지난 pass는 EXPIRED상태가 된다.")
    @Test
    public void expirePassesJob() throws Exception {
        int size = 500;
        addPassEntities(size);

        JobExecution jobExecution = jobLauncherTestUtils.launchJob();
        JobInstance jobInstance = jobExecution.getJobInstance();
        List<PassEntity> passes = passRepository.findAll();

        assertThat(ExitStatus.COMPLETED).isEqualTo(jobExecution.getExitStatus());
        assertThat("expirePassesJob").isEqualTo(jobInstance.getJobName());
        assertThat(passes).hasSize(size)
                .extracting(PassEntity::getStatus)
                .containsOnly(PassStatus.EXPIRED);
    }

    @DisplayName("endedAt이 지나지 않은 pass는 상태가 그대로다.")
    @Test
    public void notEndedPass() throws Exception {
        int size = 500;
        addPassEntities(size);
        LocalDateTime tommorowLocalDateTime = LocalDateTime.now().plusDays(1);
        PassEntity passEntity = getPassEntity(500, 10, tommorowLocalDateTime);
        passRepository.save(passEntity);

        JobExecution jobExecution = jobLauncherTestUtils.launchJob();
        JobInstance jobInstance = jobExecution.getJobInstance();
        List<PassEntity> notEndedPass = passRepository.findAll().stream()
                .filter(item -> item.getStatus() == PassStatus.PROGRESSED)
                .toList();

        assertThat(ExitStatus.COMPLETED).isEqualTo(jobExecution.getExitStatus());
        assertThat("expirePassesJob").isEqualTo(jobInstance.getJobName());
        assertThat(notEndedPass).hasSize(1)
                .extracting(PassEntity::getStatus)
                .containsOnly(PassStatus.PROGRESSED);
    }

    private void addPassEntities(int size) {
        LocalDateTime now = LocalDateTime.of(2024, 8, 10, 0, 0, 0);
        Random random = new Random();

        List<PassEntity> passEntities = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            PassEntity passEntity = getPassEntity(i, random.nextInt(11), now);
            passEntities.add(passEntity);
        }
        passRepository.saveAll(passEntities);
    }

    private PassEntity getPassEntity(int id, int remainingCount, LocalDateTime now) {
        PassEntity passEntity = new PassEntity();
        passEntity.setPackageSeq(1);
        passEntity.setUserId("A" + id);
        passEntity.setStatus(PassStatus.PROGRESSED);
        passEntity.setRemainingCount(remainingCount);
        passEntity.setStartedAt(now.minusDays(60));
        passEntity.setEndedAt(now.minusDays(1));
        return passEntity;
    }
}
