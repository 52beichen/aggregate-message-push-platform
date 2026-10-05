package dev.qingzhou.push.api.spi;

import dev.qingzhou.push.api.model.ActionContext;
import dev.qingzhou.push.api.model.PluginMeta;

public interface PushPlugin {

    // 获取插件元数据 (ID, Version, ConfigDefinition)
    PluginMeta getMeta();

    // 路由判断：决定是否处理该消息
    boolean supports(ActionContext context);

    // 初始化：注入发送器能力
    void init(PushSender sender);

    // 核心业务逻辑
    // 约定：无返回值，处理结果必须通过 sender 异步发送
    void handle(ActionContext context);

    default void shutdown() {}
}
