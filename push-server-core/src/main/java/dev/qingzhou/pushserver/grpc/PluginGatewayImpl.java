package dev.qingzhou.pushserver.grpc;

import dev.qingzhou.push.api.grpc.*;
import dev.qingzhou.push.api.grpc.PluginGatewayGrpc.PluginGatewayImplBase;
import dev.qingzhou.pushserver.model.entity.portal.PortalPlugin;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import dev.qingzhou.pushserver.service.PushService;
import dev.qingzhou.pushserver.service.PluginManagerService;
import dev.qingzhou.push.api.model.PluginMeta;
import dev.qingzhou.push.api.model.ConfigField;
import dev.qingzhou.push.api.model.ConfigType;
import dev.qingzhou.push.api.model.SelectOption;
import dev.qingzhou.pushserver.mapper.portal.PortalPluginActionLogMapper;
import dev.qingzhou.pushserver.mapper.portal.PortalPluginHeartbeatLogMapper;
import dev.qingzhou.pushserver.model.entity.portal.PortalPluginActionLog;
import dev.qingzhou.pushserver.model.entity.portal.PortalPluginHeartbeatLog;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PluginGatewayImpl extends PluginGatewayImplBase {

    private final PluginConnectionManager connectionManager;
    private final GrpcAdapter grpcAdapter;
    private final PushService pushService;
    private final PluginManagerService pluginManagerService;
    private final PortalPluginActionLogMapper actionLogMapper;
    private final PortalPluginHeartbeatLogMapper heartbeatLogMapper;

    @Override
    public StreamObserver<PluginUpstreamPacket> connect(StreamObserver<PlatformDownstreamPacket> responseObserver) {
        
        PortalPlugin plugin = PluginAuthInterceptor.PLUGIN_CONTEXT_KEY.get();
        if (plugin == null) {
            // Should be caught by interceptor, but safety check
            responseObserver.onError(io.grpc.Status.UNAUTHENTICATED.asRuntimeException());
            return new StreamObserver<>() {
                @Override public void onNext(PluginUpstreamPacket value) {}
                @Override public void onError(Throwable t) {}
                @Override public void onCompleted() {}
            };
        }

        final String pluginKey = plugin.getPluginKey();
        log.info("New connection stream from plugin: {}", pluginKey);

        return new StreamObserver<PluginUpstreamPacket>() {
            @Override
            public void onNext(PluginUpstreamPacket packet) {
                try {
                    handlePacket(pluginKey, packet, responseObserver);
                } catch (Exception e) {
                    log.error("Error handling packet from {}", pluginKey, e);
                }
            }

            @Override
            public void onError(Throwable t) {
                log.warn("Stream error for plugin {}: {}", pluginKey, t.getMessage());
                cleanup();
            }

            @Override
            public void onCompleted() {
                log.info("Stream completed for plugin {}", pluginKey);
                cleanup();
                responseObserver.onCompleted();
            }
            
            private void cleanup() {
                connectionManager.unregister(pluginKey);
                pluginManagerService.unregisterRemotePlugin(pluginKey);
            }
        };
    }
    
    private void handlePacket(String pluginKey, PluginUpstreamPacket packet, StreamObserver<PlatformDownstreamPacket> responseObserver) {
        if (packet.hasRegister()) {
             // Register logic
             RegisterRequest req = packet.getRegister();
             if (!req.getPluginKey().equals(pluginKey)) {
                 log.warn("Plugin Key mismatch in register packet. Token says {}, packet says {}", pluginKey, req.getPluginKey());
             }
             
             // Convert RegisterRequest to PluginMeta
             PluginMeta meta = PluginMeta.builder()
                     .id(pluginKey)
                     .version(req.getPluginVersion())
                     .maxConcurrency(req.getMaxConcurrency())
                     .build();
             
             // Convert ConfigDefinitions if needed... (simplified here)
             
             connectionManager.register(pluginKey, responseObserver);
             pluginManagerService.registerRemotePlugin(pluginKey, meta);
             
             responseObserver.onNext(PlatformDownstreamPacket.newBuilder()
                     .setHeader(PacketHeader.newBuilder()
                             .setTraceId(packet.getHeader().getTraceId())
                             .setTimestamp(System.currentTimeMillis())
                             .build())
                     .setRegisterAck(RegisterResponse.newBuilder().setSuccess(true).build())
                     .build());
                     
        } else if (packet.hasHeartbeat()) {
            Heartbeat hb = packet.getHeartbeat();
            connectionManager.updateHeartbeat(pluginKey);
            log.debug("Heartbeat from {} inflight={} uptime={}", pluginKey, hb.getCurrentInflight(), hb.getUptimeSeconds());
            saveHeartbeat(pluginKey, hb);
            // Currently protocol has no downstream heartbeat ack; tracking timestamp is enough.
        } else if (packet.hasActionAck()) {
            ActionAck ack = packet.getActionAck();
            log.info("ActionAck from {} eventId={} status={} msg={}",
                    pluginKey, ack.getEventId(), ack.getStatus(), ack.getMessage());
            saveActionAck(pluginKey, ack);
        } else if (packet.hasPushRequest()) {
            dev.qingzhou.pushserver.model.dto.openapi.PushRequest coreReq = grpcAdapter.toCorePushRequest(packet.getPushRequest());
            
            try {
                pushService.push(coreReq);
                
                responseObserver.onNext(PlatformDownstreamPacket.newBuilder()
                     .setHeader(PacketHeader.newBuilder()
                             .setTraceId(packet.getHeader().getTraceId())
                             .setTimestamp(System.currentTimeMillis())
                             .build())
                     .setPushResponse(PushResponse.newBuilder().setSuccess(true).build())
                     .build());
            } catch (Exception e) {
                log.error("Push failed for plugin {}", pluginKey, e);
                responseObserver.onNext(PlatformDownstreamPacket.newBuilder()
                     .setHeader(PacketHeader.newBuilder()
                             .setTraceId(packet.getHeader().getTraceId())
                             .setTimestamp(System.currentTimeMillis())
                             .build())
                     .setPushResponse(PushResponse.newBuilder()
                             .setSuccess(false)
                             .setErrorMsg(e.getMessage())
                             .build())
                     .build());
            }
        }
    }

    private void saveActionAck(String pluginKey, ActionAck ack) {
        PortalPluginActionLog logEntity = new PortalPluginActionLog();
        logEntity.setPluginKey(pluginKey);
        logEntity.setEventId(ack.getEventId());
        logEntity.setStatus(ack.getStatusValue());
        logEntity.setMessage(ack.getMessage());
        logEntity.setCreatedAt(System.currentTimeMillis());
        try {
            int updated = actionLogMapper.update(logEntity,
                    new LambdaQueryWrapper<PortalPluginActionLog>()
                            .eq(PortalPluginActionLog::getPluginKey, pluginKey)
                            .eq(PortalPluginActionLog::getEventId, ack.getEventId()));
            if (updated == 0) {
                actionLogMapper.insert(logEntity);
            }
        } catch (Exception e) {
            log.warn("Failed to persist ActionAck for plugin {} event {}", pluginKey, ack.getEventId(), e);
        }
    }

    private void saveHeartbeat(String pluginKey, Heartbeat hb) {
        PortalPluginHeartbeatLog logEntity = new PortalPluginHeartbeatLog();
        logEntity.setPluginKey(pluginKey);
        logEntity.setCurrentInflight(hb.getCurrentInflight());
        logEntity.setUptimeSeconds((int) hb.getUptimeSeconds());
        logEntity.setCreatedAt(System.currentTimeMillis());
        heartbeatLogMapper.insert(logEntity);
    }
}
