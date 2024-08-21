package com.fastcampus.pass.repository.pass;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@Service
public class PassService {

    private final PassRepository passRepository;

    @Transactional
    public void expiredPasses() {
        List<PassEntity> passes = passRepository.findAllByStatusAAndEndedAt(PassStatus.PROGRESSED, LocalDateTime.now());
        LocalDateTime now = LocalDateTime.now();
        for (PassEntity pass : passes) {
            pass.setStatus(PassStatus.EXPIRED);
            pass.setExpiredAt(now);
        }
    }
}
