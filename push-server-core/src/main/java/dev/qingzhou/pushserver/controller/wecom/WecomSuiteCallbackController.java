package dev.qingzhou.pushserver.controller.wecom;

import dev.qingzhou.pushserver.manager.wecom.AesException;
import dev.qingzhou.pushserver.manager.wecom.WXBizMsgCrypt;
import dev.qingzhou.pushserver.model.entity.portal.PortalWecomSuiteApp;
import dev.qingzhou.pushserver.service.PortalWecomSuiteAppService;
import java.io.StringReader;
import java.util.concurrent.Executor;
import javax.xml.parsers.DocumentBuilderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

@RestController
@RequestMapping("/v2/wecom/suite-callback/{suiteAppId}")
public class WecomSuiteCallbackController {
    private static final Logger log = LoggerFactory.getLogger(WecomSuiteCallbackController.class);
    private final PortalWecomSuiteAppService service;
    private final Executor callbackExecutor;

    public WecomSuiteCallbackController(PortalWecomSuiteAppService service,
                                        @Qualifier("wecomCallbackExecutor") Executor callbackExecutor) {
        this.service = service;
        this.callbackExecutor = callbackExecutor;
    }

    @GetMapping
    public String verify(@PathVariable Long suiteAppId,
                         @RequestParam(name = "msg_signature", required = false) String signature,
                         @RequestParam(name = "timestamp", required = false) String timestamp,
                         @RequestParam(name = "nonce", required = false) String nonce,
                         @RequestParam(name = "echostr", required = false) String echostr) {
        if (!StringUtils.hasText(signature) || !StringUtils.hasText(timestamp)
                || !StringUtils.hasText(nonce) || !StringUtils.hasText(echostr)) {
            return "WeCom suite callback endpoint is ready";
        }
        try {
            PortalWecomSuiteApp app = service.getById(suiteAppId);
            if (app == null) return "FAILED";
            // Data callback validation may use the provider CorpID while instruction callbacks use SuiteID.
            // Signature and AES validation still authenticate the request; POST callbacks remain SuiteID-strict.
            return crypt(app).VerifyURL(signature, timestamp, nonce, echostr, false);
        } catch (Exception ex) {
            log.warn("Failed to verify WeCom suite callback URL for app {}", suiteAppId, ex);
            return "FAILED";
        }
    }

    @PostMapping
    public String receive(@PathVariable Long suiteAppId,
                          @RequestParam("msg_signature") String signature,
                          @RequestParam("timestamp") String timestamp,
                          @RequestParam("nonce") String nonce,
                          @RequestBody String body) {
        try {
            PortalWecomSuiteApp app = service.getById(suiteAppId);
            if (app == null) return "FAILED";
            String xml = crypt(app).DecryptMsg(signature, timestamp, nonce, body);
            callbackExecutor.execute(() -> {
                try {
                    processDecrypted(suiteAppId, app, xml);
                } catch (Exception ex) {
                    log.warn("Failed to process decrypted WeCom suite callback for app {}", suiteAppId, ex);
                }
            });
            return "success";
        } catch (Exception ex) {
            log.warn("Failed to process WeCom suite callback for app {}", suiteAppId, ex);
            return "FAILED";
        }
    }

    void processDecrypted(Long suiteAppId, PortalWecomSuiteApp app, String xml) throws Exception {
        String suiteId = readTag(xml, "SuiteId");
        if (StringUtils.hasText(suiteId) && !app.getSuiteId().equals(suiteId)) {
            throw new IllegalArgumentException("SuiteID does not match callback");
        }
        String infoType = readTag(xml, "InfoType");
        if ("suite_ticket".equalsIgnoreCase(infoType)) {
            service.saveSuiteTicket(suiteAppId, suiteId, readTag(xml, "SuiteTicket"));
            try {
                service.refreshSuiteAccessToken(suiteAppId);
            } catch (RuntimeException ex) {
                log.warn("Stored SuiteTicket but failed to refresh suite token for app {}", suiteAppId, ex);
            }
        } else if ("create_auth".equalsIgnoreCase(infoType)) {
            service.completeAuthorization(suiteAppId, readTag(xml, "AuthCode"));
        } else if ("change_auth".equalsIgnoreCase(infoType)) {
            service.refreshAuthorization(suiteAppId, readTag(xml, "AuthCorpId"));
        } else if ("cancel_auth".equalsIgnoreCase(infoType)) {
            service.cancelAuthorization(suiteAppId, readTag(xml, "AuthCorpId"));
        }
    }

    private WXBizMsgCrypt crypt(PortalWecomSuiteApp app) throws AesException {
        return new WXBizMsgCrypt(app.getToken(), app.getEncodingAesKey(), app.getSuiteId());
    }

    private String readTag(String xml, String tag) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
        var nodes = document.getDocumentElement().getElementsByTagName(tag);
        return nodes.getLength() == 0 ? null : nodes.item(0).getTextContent();
    }
}
