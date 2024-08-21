package com.fastcampus.pass.job;

import com.fastcampus.pass.job.pass.AddPassesTasklet;
import com.fastcampus.pass.repository.pass.*;
import com.fastcampus.pass.repository.user.UserGroupMappingEntity;
import com.fastcampus.pass.repository.user.UserGroupMappingRepository;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
class AddPassesTaskletTest {

    @Autowired
    private AddPassesTasklet addPassesTasklet;

    @Autowired
    private BulkPassRepository bulkPassRepository;
    @Autowired
    private UserGroupMappingRepository userGroupMappingRepository;

    @MockBean
    private StepContribution stepContribution;
    @MockBean
    private ChunkContext chunkContext;
    @Autowired
    private PassRepository passRepository;

    @Transactional
    @Test
    public void test() throws Exception {
        Integer bulkPassSeq = initBulkPass();
        initUserGroup();

        RepeatStatus repeatStatus = addPassesTasklet.execute(stepContribution, chunkContext);

        BulkPassEntity bulkPass = bulkPassRepository.findById(bulkPassSeq).get();
        List<PassEntity> passes = passRepository.findAll();

        assertThat(bulkPass.getStatus()).isEqualTo(BulkPassStatus.COMPLETED);
        assertThat(passes).hasSize(1);
        assertThat(repeatStatus).isEqualTo(RepeatStatus.FINISHED);
    }

    private void initUserGroup() {
        UserGroupMappingEntity userGroupMappingEntity = new UserGroupMappingEntity();

        userGroupMappingEntity.setUserGroupId("group1");
        userGroupMappingEntity.setUserId("user1");
        userGroupMappingEntity.setUserGroupName("foo");
        userGroupMappingEntity.setDescription("description");

        userGroupMappingRepository.save(userGroupMappingEntity);
    }

    private Integer initBulkPass() {
        BulkPassEntity bulkPassEntity = new BulkPassEntity();
        final LocalDateTime startedAt = LocalDateTime.now();
        final LocalDateTime endedAt = LocalDateTime.now().plusMonths(1L);

        bulkPassEntity.setPackageSeq(1);
        bulkPassEntity.setUserGroupId("group1");
        bulkPassEntity.setStatus(BulkPassStatus.READY);
        bulkPassEntity.setCount(10);
        bulkPassEntity.setStartedAt(startedAt);
        bulkPassEntity.setEndedAt(endedAt);

        return bulkPassRepository.save(bulkPassEntity)
                .getBulkPassSeq();
    }
}