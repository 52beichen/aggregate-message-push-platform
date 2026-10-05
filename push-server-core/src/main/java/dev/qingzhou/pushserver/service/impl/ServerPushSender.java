package dev.qingzhou.pushserver.service.impl;

import dev.qingzhou.push.api.model.PushMessage;
import dev.qingzhou.push.api.spi.PushSender;
import dev.qingzhou.pushserver.model.dto.openapi.PushRequest;
import dev.qingzhou.pushserver.service.PushService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ServerPushSender implements PushSender {

    private final PushService pushService;

    @Override
    public void send(PushMessage message) {
        PushRequest req = new PushRequest();
        req.setTarget(message.getTargetUserId());
        req.setType(message.getType() != null ? message.getType() : "text");
        req.setContent(message.getContent());
        req.setTitle(message.getTitle());
        req.setUrl(message.getUrl());
        // Map other fields...
        
        pushService.push(req);
    }
}
