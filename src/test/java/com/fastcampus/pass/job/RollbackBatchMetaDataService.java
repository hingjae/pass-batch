package com.fastcampus.pass.job;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@TestComponent
public class RollbackBatchMetaDataService {
    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public void clearBatchMetadata() {
        jdbcTemplate.update("DELETE FROM BATCH_STEP_EXECUTION_CONTEXT");
        jdbcTemplate.update("DELETE FROM BATCH_STEP_EXECUTION_SEQ");
        jdbcTemplate.update("DELETE FROM BATCH_STEP_EXECUTION");
        jdbcTemplate.update("DELETE FROM BATCH_JOB_EXECUTION_CONTEXT");
        jdbcTemplate.update("DELETE FROM BATCH_JOB_EXECUTION_PARAMS");
        jdbcTemplate.update("DELETE FROM BATCH_JOB_EXECUTION");
        jdbcTemplate.update("DELETE FROM BATCH_JOB_EXECUTION_SEQ");
        jdbcTemplate.update("DELETE FROM BATCH_JOB_INSTANCE");
        jdbcTemplate.update("DELETE FROM BATCH_JOB_SEQ");
    }
}
