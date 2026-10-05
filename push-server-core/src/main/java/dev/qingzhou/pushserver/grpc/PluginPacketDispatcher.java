package dev.qingzhou.pushserver.grpc;

import dev.qingzhou.push.api.grpc.PacketHeader;
import dev.qingzhou.push.api.grpc.PlatformDownstreamPacket;
import dev.qingzhou.push.api.grpc.UserActionEvent;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class PluginPacketDispatcher {

    private final PluginConnectionManager connectionManager;

    public boolean sendUserAction(String pluginKey, UserActionEvent actionEvent) {
        StreamObserver<PlatformDownstreamPacket> observer = connectionManager.get(pluginKey);
        if (observer == null) {
            log.warn("Cannot send action to plugin {}: No active connection", pluginKey);
            return false;
        }

        PlatformDownstreamPacket packet = PlatformDownstreamPacket.newBuilder()
                .setHeader(PacketHeader.newBuilder()
                        .setTimestamp(System.currentTimeMillis())
                        .setTraceId(UUID.randomUUID().toString())
                        .setPluginKey(pluginKey)
                        .build())
                .setActionEvent(actionEvent)
                .build();

        try {
            observer.onNext(packet);
            return true;
        } catch (Exception e) {
            log.error("Failed to send packet to plugin {}", pluginKey, e);
            connectionManager.unregister(pluginKey);
            return false;
        }
    }
}
