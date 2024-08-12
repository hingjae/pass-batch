package com.fastcampus.pass.repository.pass;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "bulk_pass")
public class BulkPassEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer bulkPassSeq;
    private Integer packageSeq;
    private String userGroupId;

    @Enumerated(EnumType.STRING)
    private BulkPassStatus status;
    private Integer count;

    private LocalDateTime startedAt;
    private LocalDateTime endedAt;

    public PassEntity toPassEntity(String userId) {
        PassEntity passEntity = new PassEntity();
        passEntity.setPackageSeq(packageSeq);
        passEntity.setUserId(userId);
        passEntity.setStatus(PassStatus.READY);
        passEntity.setRemainingCount(count);
        passEntity.setStartedAt(startedAt);
        passEntity.setEndedAt(endedAt);
        return passEntity;
    }
}
