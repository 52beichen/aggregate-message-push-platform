package dev.qingzhou.pushserver.grpc;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import dev.qingzhou.pushserver.mapper.portal.PortalPluginMapper;
import dev.qingzhou.pushserver.model.entity.portal.PortalPlugin;
import io.grpc.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PluginAuthInterceptor implements ServerInterceptor {

    public static final Context.Key<PortalPlugin> PLUGIN_CONTEXT_KEY = Context.key("plugin");

    private final PortalPluginMapper pluginMapper;

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {

        String authHeader = headers.get(Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER));
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            call.close(Status.UNAUTHENTICATED.withDescription("Missing or invalid Authorization header"), headers);
            return new ServerCall.Listener<>() {};
        }

        String token = authHeader.substring(7);

        // TODO: Add Caching here to avoid DB hit on every connect
        PortalPlugin plugin = pluginMapper.selectOne(new LambdaQueryWrapper<PortalPlugin>()
                .eq(PortalPlugin::getToken, token)
                .eq(PortalPlugin::getStatus, 1)); // Must be enabled

        if (plugin == null) {
            call.close(Status.UNAUTHENTICATED.withDescription("Invalid Token"), headers);
            return new ServerCall.Listener<>() {};
        }

        Context ctx = Context.current().withValue(PLUGIN_CONTEXT_KEY, plugin);
        return Contexts.interceptCall(ctx, call, headers, next);
    }
}
