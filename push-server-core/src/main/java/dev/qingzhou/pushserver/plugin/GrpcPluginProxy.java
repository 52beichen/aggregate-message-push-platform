package dev.qingzhou.pushserver.plugin;

import dev.qingzhou.push.api.model.ActionContext;
import dev.qingzhou.push.api.model.PluginMeta;
import dev.qingzhou.push.api.spi.PushPlugin;
import dev.qingzhou.push.api.spi.PushSender;
import dev.qingzhou.pushserver.grpc.PluginPacketDispatcher;
import dev.qingzhou.push.api.grpc.UserActionEvent;
import dev.qingzhou.push.api.grpc.UserActionType;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@RequiredArgsConstructor
public class GrpcPluginProxy implements PushPlugin {

    private final String pluginKey;
    private final PluginMeta meta;
    private final PluginPacketDispatcher dispatcher;

    @Override
    public PluginMeta getMeta() {
        return meta;
    }

    @Override
    public boolean supports(ActionContext context) {
        // 简单路由：真正的逻辑应在 Manager 层根据 commands 前缀匹配
        return true; 
    }

    @Override
    public void init(PushSender sender) {
        // Remote plugins don't need local sender injection
    }

    @Override
    public void handle(ActionContext context) {
        UserActionEvent.Builder eventBuilder = UserActionEvent.newBuilder()
                .setEventId(context.getEventId() == null ? UUID.randomUUID().toString() : context.getEventId())
                .setAppId(context.getAppId() == null ? "" : context.getAppId())
                .setUserId(context.getUserId() == null ? "" : context.getUserId())
                .setUserName(context.getUserName() == null ? "" : context.getUserName())
                .setContent(context.getContent() == null ? "" : context.getContent());

        if ("CLICK".equalsIgnoreCase(context.getType())) {
            eventBuilder.setType(UserActionType.USER_ACTION_TYPE_CLICK);
        } else if ("IMAGE".equalsIgnoreCase(context.getType())) {
            eventBuilder.setType(UserActionType.USER_ACTION_TYPE_IMAGE);
        } else {
            eventBuilder.setType(UserActionType.USER_ACTION_TYPE_TEXT);
        }
        
        if (context.getPluginConfig() != null) {
            eventBuilder.putAllPluginConfig(context.getPluginConfig());
        }

        dispatcher.sendUserAction(pluginKey, eventBuilder.build());
    }
}
