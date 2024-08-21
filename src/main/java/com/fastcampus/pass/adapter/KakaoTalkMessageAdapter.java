package com.fastcampus.pass.adapter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class KakaoTalkMessageAdapter {
    public boolean mockMessage(final String uuid, final String text) {
        log.info("[{}] - {}", uuid, text);
        return true;
    }
}
