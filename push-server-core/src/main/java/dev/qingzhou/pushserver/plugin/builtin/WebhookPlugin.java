package dev.qingzhou.pushserver.plugin.builtin;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.qingzhou.push.api.model.ActionContext;
import dev.qingzhou.push.api.model.ConfigField;
import dev.qingzhou.push.api.model.ConfigType;
import dev.qingzhou.push.api.model.PluginMeta;
import dev.qingzhou.push.api.spi.PushPlugin;
import dev.qingzhou.push.api.spi.PushSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookPlugin implements PushPlugin {

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public PluginMeta getMeta() {
        return PluginMeta.builder()
                .id("builtin-webhook")
                .name("Webhook")
                .description("将消息事件以 JSON 格式推送到指定的 HTTP URL")
                .version("1.0.0")
                .maxConcurrency(100)
                .configFields(List.of(
                        ConfigField.builder()
                                .name("url")
                                .label("Webhook URL")
                                .type(ConfigType.TEXT)
                                .required(true)
                                .description("接收事件的接口地址")
                                .build(),
                        ConfigField.builder()
                                .name("headerName")
                                .label("Auth Header Name")
                                .type(ConfigType.TEXT)
                                .required(false)
                                .description("可选，例如 Authorization")
                                .build(),
                        ConfigField.builder()
                                .name("headerValue")
                                .label("Auth Header Value")
                                .type(ConfigType.PASSWORD)
                                .required(false)
                                .description("可选，例如 Bearer xxx")
                                .build()
                ))
                .build();
    }

    @Override
    public boolean supports(ActionContext context) {
        // 只有当运行时配置中包含了 'url' 时，才由本插件处理
        return context.getConfig("url") != null && !context.getConfig("url").isBlank();
    }

    @Override
    public void init(PushSender sender) {
        // Webhook 通常是单向通知，不需要回复用户，所以这里不需要保存 sender
    }

    @Override
    public void handle(ActionContext context) {
        String url = context.getConfig("url");
        String headerName = context.getConfig("headerName");
        String headerValue = context.getConfig("headerValue");

        // 异步执行，避免阻塞主事件循环
        CompletableFuture.runAsync(() -> {
            try {
                Map<String, Object> payload = new HashMap<>();
                payload.put("eventId", context.getEventId());
                payload.put("type", context.getType());
                payload.put("userId", context.getUserId());
                payload.put("content", context.getContent());
                payload.put("timestamp", System.currentTimeMillis());

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                if (StringUtils.hasText(headerName) && StringUtils.hasText(headerValue)) {
                    headers.set(headerName, headerValue);
                }

                log.debug("Sending webhook to {}", url);
                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
                String response = restTemplate.postForObject(url, entity, String.class);
                log.debug("Webhook response: {}", response);
                
            } catch (Exception e) {
                log.error("Failed to send webhook to {}: {}", url, e.getMessage());
            }
        });
    }

    @Override
    public void shutdown() {
        // No resources to release
    }
}
