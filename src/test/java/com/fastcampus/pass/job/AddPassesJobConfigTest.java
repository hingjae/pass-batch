package com.fastcampus.pass.job;

import com.fastcampus.pass.repository.pass.*;
import com.fastcampus.pass.repository.user.UserGroupMappingEntity;
import com.fastcampus.pass.repository.user.UserGroupMappingRepository;
import org.assertj.core.api.Assertions;
import org.assertj.core.groups.Tuple;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@Import(RollbackBatchMetaDataService.class)
@SpringBatchTest
@ActiveProfiles("test")
@SpringBootTest
class AddPassesJobConfigTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    @Qualifier("addPassesJob")
    private Job addPassesJob;

    @Autowired
    private RollbackBatchMetaDataService rollbackBatchMetaDataService;

    @Autowired
    private BulkPassRepository bulkPassRepository;

    @Autowired
    private UserGroupMappingRepository userGroupMappingRepository;
    @Autowired
    private PassRepository passRepository;

    @BeforeEach
    public void setJob() {
        jobLauncherTestUtils.setJob(addPassesJob);
    }

    @AfterEach
    public void rollBack() {
        rollbackBatchMetaDataService.clearBatchMetadata();
        bulkPassRepository.deleteAllInBatch();
        userGroupMappingRepository.deleteAllInBatch();
        passRepository.deleteAllInBatch();
    }

    @DisplayName("유저 그룹에 일괄로 pass를 추가한다.")
    @Test
    public void addPassesJob() throws Exception {
        final LocalDateTime startedAt = LocalDateTime.now();
        final LocalDateTime endedAt = LocalDateTime.now().plusMonths(1L);
        initData(startedAt, endedAt);

        JobExecution jobExecution = jobLauncherTestUtils.launchJob();

        JobInstance jobInstance = jobExecution.getJobInstance();
        List<PassEntity> passes = passRepository.findAll();
        List<BulkPassEntity> bulkPasses = bulkPassRepository.findAll();

        assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
        assertThat(jobInstance.getJobName()).isEqualTo("addPassesJob");
        assertThat(passes).hasSize(5)
                .extracting(PassEntity::getPackageSeq, PassEntity::getUserId, PassEntity::getStatus)
                .containsOnly(
                        Tuple.tuple(1, "user1", PassStatus.READY),
                        Tuple.tuple(1, "user2", PassStatus.READY),
                        Tuple.tuple(1, "user3", PassStatus.READY),
                        Tuple.tuple(2, "user4", PassStatus.READY),
                        Tuple.tuple(2, "user5", PassStatus.READY)
                );
        assertThat(bulkPasses).hasSize(2)
                .extracting(BulkPassEntity::getStatus)
                .containsOnly(BulkPassStatus.COMPLETED);
    }

    private void initData(LocalDateTime startedAt, LocalDateTime endedAt) {
        Integer bulkPassSeq1 = initBulkPass(1, "group1", startedAt, endedAt);
        Integer bulkPassSeq2 = initBulkPass(2, "group2", startedAt, endedAt);
        initUserGroup("group1", "user1");
        initUserGroup("group1", "user2");
        initUserGroup("group1", "user3");
        initUserGroup("group2", "user4");
        initUserGroup("group2", "user5");
    }

    private void initUserGroup(String userGroupId, String userId) {
        UserGroupMappingEntity userGroupMappingEntity = new UserGroupMappingEntity();

        userGroupMappingEntity.setUserGroupId(userGroupId);
        userGroupMappingEntity.setUserId(userId);
        userGroupMappingEntity.setUserGroupName("foo");
        userGroupMappingEntity.setDescription("description");

        userGroupMappingRepository.save(userGroupMappingEntity);
    }

    private Integer initBulkPass(Integer packageSeq, String userGroupId, LocalDateTime startedAt, LocalDateTime endedAt) {
        BulkPassEntity bulkPassEntity = new BulkPassEntity();

        bulkPassEntity.setPackageSeq(packageSeq);
        bulkPassEntity.setUserGroupId(userGroupId);
        bulkPassEntity.setStatus(BulkPassStatus.READY);
        bulkPassEntity.setCount(10);
        bulkPassEntity.setStartedAt(startedAt);
        bulkPassEntity.setEndedAt(endedAt);

        return bulkPassRepository.save(bulkPassEntity)
                .getBulkPassSeq();
    }
}