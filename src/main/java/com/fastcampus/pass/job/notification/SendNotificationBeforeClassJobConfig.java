package com.fastcampus.pass.job.notification;

import com.fastcampus.pass.repository.booking.BookingEntity;
import com.fastcampus.pass.repository.booking.BookingStatus;
import com.fastcampus.pass.repository.notification.NotificationEntity;
import com.fastcampus.pass.repository.notification.NotificationEvent;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaCursorItemReader;
import org.springframework.batch.item.database.builder.JpaCursorItemReaderBuilder;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.batch.item.support.SynchronizedItemStreamReader;
import org.springframework.batch.item.support.builder.SynchronizedItemStreamReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDateTime;
import java.util.Map;

@RequiredArgsConstructor
@Configuration
public class SendNotificationBeforeClassJobConfig {
    private final int CHUNK_SIZE = 1000;

    private final EntityManagerFactory entityManagerFactory;

    private final SendNotificationItemWriter sendNotificationItemWriter;

    @Bean
    public Job jpaSendNotificationBeforeClassJob(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new JobBuilder("jpaSendNotificationBeforeClassJob", jobRepository)
                .start(jpaAddNotificationStep(jobRepository, transactionManager))
                .next(sendNotificationStep(jobRepository, transactionManager))
                .build();
    }

    @Bean
    public Step jpaAddNotificationStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("jpaAddNotificationStep", jobRepository)
                //BookingEntity은 reader가 읽는 타입, NotificationEntity은 processor, writer가 다루는 타입.
                .<BookingEntity, NotificationEntity>chunk(CHUNK_SIZE, transactionManager)
                .reader(jpaAddNotificationItemReader())
                .processor(jpaAddNotificationItemProcessor())
                .writer(jpaAddNotificationItemWriter())
                .build();
    }

    /**
     * JpaPagingItemReader: JPA에서 사용하는 페이징 기법입니다.
     * 쿼리 당 pageSize만큼 가져오며 다른 PagingItemReader와 마찬가지로 Thread-safe 합니다.
     */
    @Bean
    public ItemReader<BookingEntity> jpaAddNotificationItemReader() {
        final String addNotificationItemReaderQuery = """
                    select b
                    from BookingEntity b
                    join fetch b.userEntity
                    where b.status = :status and
                        b.startedAt <= :startedAt
                    order by b.bookingSeq
                """;
        return new JpaPagingItemReaderBuilder<BookingEntity>()
                .name("jpaAddNotificationItemReader")
                .entityManagerFactory(entityManagerFactory)
                .pageSize(CHUNK_SIZE) // 한 번에 조회할 ROW 수
                .queryString(addNotificationItemReaderQuery)
                .parameterValues(Map.of("status", BookingStatus.READY, "startedAt", LocalDateTime.now()))
                .build();
    }

    @Bean
    public ItemProcessor<BookingEntity, NotificationEntity> jpaAddNotificationItemProcessor() {
        return bookingEntity -> bookingEntity.toNotificationEntity(NotificationEvent.BEFORE_CLASS);
    }

    /**
     * Notification이 저장된다.
     * @return
     */
    @Bean
    public ItemWriter<NotificationEntity> jpaAddNotificationItemWriter() {
        return new JpaItemWriterBuilder<NotificationEntity>()
                .entityManagerFactory(entityManagerFactory)
                .build();
    }

    /**
     * Processor가 필요없으면 굳이 넣지 않아도됌.
     */
    @Bean
    public Step sendNotificationStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("sendNotificationStep", jobRepository)
                //BookingEntity은 reader가 읽는 타입, NotificationEntity은 processor, writer가 다루는 타입.
                .<NotificationEntity, NotificationEntity>chunk(CHUNK_SIZE, transactionManager)
                .reader(jpaSendNotificationItemReader())
                .writer(sendNotificationItemWriter)
                .taskExecutor(new SimpleAsyncTaskExecutor()) // 가장 간단한 멀티쓰레드 TaskExecutor를 선언하였습니다.
                .build();
    }

    /**
     * SynchronizedItemStreamReader: multi-thread 환경에서 reader와 writer는 thread-safe 해야합니다.
     * Cursor 기법의 ItemReader는 thread-safe하지 않아 Paging 기법을 사용하거나 synchronized 를 선언하여 순차적으로 수행해야합니다.
     */
    @Bean
    public SynchronizedItemStreamReader<NotificationEntity> jpaSendNotificationItemReader() {
        final String jpaSendNotificationItemReaderQuery = """
                    select n
                    from NotificationEntity n
                    where n.event = :event and
                    n.sent = :sent
                """;

        JpaCursorItemReader<NotificationEntity> itemReader = new JpaCursorItemReaderBuilder<NotificationEntity>()
                .name("jpaSendNotificationItemReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString(jpaSendNotificationItemReaderQuery)
                .parameterValues(Map.of("event", NotificationEvent.BEFORE_CLASS, "sent", false))
                .build();

        return new SynchronizedItemStreamReaderBuilder<NotificationEntity>()
                .delegate(itemReader)
                .build();
    }
}
