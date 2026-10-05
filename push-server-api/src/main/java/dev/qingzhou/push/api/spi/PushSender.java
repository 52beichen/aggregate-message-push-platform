package dev.qingzhou.push.api.spi;

import dev.qingzhou.push.api.model.PushMessage;

public interface PushSender {
    /**
     * 发送消息
     * @param message 消息对象
     */
    void send(PushMessage message);
}