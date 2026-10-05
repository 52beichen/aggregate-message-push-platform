package dev.qingzhou.push.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PushMessage {
    private String appId;
    private String targetUserId;
    private String requestId;
    
    private String type;      // 消息类型: text, markdown, image, news 等
    private String content;   // 文本内容 / Markdown 内容
    
    private String title;     // 标题
    private String url;       // 跳转链接
    private String mediaId;   // 媒体ID (如图片/视频)
    
    private List<Article> articles; // 图文列表

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Article {
        private String title;
        private String description;
        private String url;
        private String picUrl;
    }
}