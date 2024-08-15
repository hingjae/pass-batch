package com.fastcampus.pass.job;


import com.fastcampus.pass.repository.pass.PassEntity;
import com.fastcampus.pass.repository.pass.PassStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.time.LocalDateTime;

@RequiredArgsConstructor
@Configuration
public class JdbcExpirePassesJobConfig {
    private final int CHUNK_SIZE = 1000;
    private final DataSource dataSource;

    @Bean
    public Job jdbcExpirePassesJob(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new JobBuilder("jdbcExpirePassesJob", jobRepository)
                .start(jdbcExpirePassesStep(jobRepository, transactionManager))
                .build();
    }

    @Bean
    public Step jdbcExpirePassesStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("jdbcExpirePassesStep", jobRepository)
                .<PassEntity, PassEntity>chunk(CHUNK_SIZE, transactionManager)
                .reader(jdbcExpirePassesItemReader())
                .processor(jdbcExpirePassesItemProcessor())
                .writer(jdbcExpirePassesItemWriter())
                .build();
    }

    @Bean
    public ItemReader<PassEntity> jdbcExpirePassesItemReader() {
        final String expirePassesItemReaderQuery = """
                        select pass_seq, status, ended_at
                        from pass
                        where status = ? and ended_at <= ?
                    """;
        return new JdbcCursorItemReaderBuilder<PassEntity>()
                .dataSource(dataSource)
                .name("expirePassesItemReader")
                .sql(expirePassesItemReaderQuery)
                .preparedStatementSetter((ps) -> {
                    ps.setString(1, PassStatus.PROGRESSED.name());
                    ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
                })
                .rowMapper((rs, rowNum) -> {
                    PassEntity passEntity = new PassEntity();
                    passEntity.setPassSeq(rs.getInt("pass_seq"));
                    passEntity.setStatus(PassStatus.valueOf(rs.getString("status")));
                    passEntity.setEndedAt(rs.getTimestamp("ended_at").toLocalDateTime());
                    return passEntity;
                })
                .build();
    }

    @Bean
    public ItemProcessor<PassEntity, PassEntity> jdbcExpirePassesItemProcessor() {
        LocalDateTime now = LocalDateTime.now();
        return passEntity -> {
            passEntity.setStatus(PassStatus.EXPIRED);
            passEntity.setEndedAt(now);
            return passEntity;
        };
    }


    @Bean
    public ItemWriter<PassEntity> jdbcExpirePassesItemWriter() {
        final String updateQuery = """
                    update pass
                    set status = ?, ended_at = ?
                    where pass_seq = ?
                """;
        return new JdbcBatchItemWriterBuilder<PassEntity>()
                .dataSource(dataSource)
                .sql(updateQuery)
                .itemPreparedStatementSetter((passEntity, ps) -> {
                    ps.setString(1, passEntity.getStatus().name());
                    ps.setTimestamp(2, Timestamp.valueOf(passEntity.getEndedAt()));
                    ps.setInt(3, passEntity.getPassSeq());
                })
                .build();
    }
}
