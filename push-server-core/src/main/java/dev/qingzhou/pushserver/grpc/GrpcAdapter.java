package dev.qingzhou.pushserver.grpc;

import dev.qingzhou.push.api.grpc.*;
import dev.qingzhou.pushserver.model.dto.openapi.PushRequest;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

@Component
public class GrpcAdapter {

    public PushRequest toCorePushRequest(dev.qingzhou.push.api.grpc.PushRequest protoReq) {
        PushRequest coreReq = new PushRequest();
        coreReq.setTarget(protoReq.getTargetUserId());
        coreReq.setContent(protoReq.getContent());
        coreReq.setTitle(protoReq.getTitle());
        coreReq.setUrl(protoReq.getUrl());
        coreReq.setMediaId(protoReq.getMediaId());

        switch (protoReq.getType()) {
            case PUSH_CONTENT_TYPE_MARKDOWN:
                coreReq.setType("markdown");
                break;
            case PUSH_CONTENT_TYPE_IMAGE:
                coreReq.setType("image");
                break;
            case PUSH_CONTENT_TYPE_NEWS:
                coreReq.setType("news");
                break;
            case PUSH_CONTENT_TYPE_TEXT_CARD:
                coreReq.setType("textcard");
                break;
            case PUSH_CONTENT_TYPE_TEXT:
            default:
                coreReq.setType("text");
                break;
        }
        
        if (protoReq.getArticlesCount() > 0) {
            List<PushRequest.Article> articles = new ArrayList<>();
            for (PushArticle protoArticle : protoReq.getArticlesList()) {
                PushRequest.Article article = new PushRequest.Article();
                article.setTitle(protoArticle.getTitle());
                article.setDescription(protoArticle.getDescription());
                article.setUrl(protoArticle.getUrl());
                article.setPicUrl(protoArticle.getPicUrl());
                articles.add(article);
            }
            coreReq.setArticles(articles);
        }
        
        return coreReq;
    }
}
