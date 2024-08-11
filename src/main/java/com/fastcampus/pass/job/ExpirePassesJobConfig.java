package com.fastcampus.pass.job;

import com.fastcampus.pass.repository.pass.PassEntity;
import com.fastcampus.pass.repository.pass.PassStatus;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.builder.JpaCursorItemReaderBuilder;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Configuration
public class ExpirePassesJobConfig {
    private final int CHUNK_SIZE = 100;

    private final EntityManagerFactory entityManagerFactory;

    @Bean
    public Job expirePassesJob(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new JobBuilder("expirePassesJob", jobRepository)
                .start(expirePassesStep(jobRepository, transactionManager))
                .build();
    }

    @Bean
    public Step expirePassesStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("expirePassesStep", jobRepository)
                .<PassEntity, PassEntity>chunk(CHUNK_SIZE, transactionManager)
                .reader(expirePassesItemReader())
                .processor(expirePassesItemProcessor())
                .writer(expirePassesItemWriter())
                .build();
    }

    /**
     * JpaCursorItemReader: JpaPagingItemReader만 지원하다가 Spring 4.3에서 추가되었습니다.
     * 페이징 기법보다 보다 높은 성능으로, 데이터 변경에 무관한 무결성 조회가 가능합니다.
     */
    @Bean
    public ItemReader<PassEntity> expirePassesItemReader() {
        String expirePassesItemReaderQuery = """
                select p
                from PassEntity p
                where p.status = :status and
                        p.endedAt <= :endedAt
                """;
        return new JpaCursorItemReaderBuilder<PassEntity>()
                .entityManagerFactory(entityManagerFactory)
                .name("expirePassesItemReader")
                .queryString(expirePassesItemReaderQuery)
                .parameterValues(Map.of("status", PassStatus.PROGRESSED, "endedAt", LocalDateTime.now()))
                .build();
    }

    @Bean
    public ItemProcessor<PassEntity, PassEntity> expirePassesItemProcessor() {
        return passEntity -> {
            passEntity.setStatus(PassStatus.EXPIRED);
            passEntity.setEndedAt(LocalDateTime.now());
            return passEntity;
        };
    }

    /**
     * JpaItemWriter: JPA의 영속성 관리를 위해 EntityManager를 필수로 설정해줘야 합니다.
     */
    @Bean
    public ItemWriter<PassEntity> expirePassesItemWriter() {
        return new JpaItemWriterBuilder<PassEntity>()
                .entityManagerFactory(entityManagerFactory)
                .build();
    }
}
