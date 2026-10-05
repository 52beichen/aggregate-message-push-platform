package dev.qingzhou.pushserver.controller.wecom;

import dev.qingzhou.pushserver.manager.wecom.AesException;
import dev.qingzhou.pushserver.manager.wecom.WXBizMsgCrypt;
import dev.qingzhou.pushserver.model.entity.portal.PortalWecomSuiteApp;
import dev.qingzhou.pushserver.service.PortalWecomSuiteAppService;
import java.io.StringReader;
import javax.xml.parsers.DocumentBuilderFactory;
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
    private final PortalWecomSuiteAppService service;

    public WecomSuiteCallbackController(PortalWecomSuiteAppService service) {
        this.service = service;
    }

    @GetMapping
    public String verify(@PathVariable Long suiteAppId,
                         @RequestParam("msg_signature") String signature,
                         @RequestParam("timestamp") String timestamp,
                         @RequestParam("nonce") String nonce,
                         @RequestParam("echostr") String echostr) {
        try {
            PortalWecomSuiteApp app = service.getById(suiteAppId);
            if (app == null) return "FAILED";
            return crypt(app).VerifyURL(signature, timestamp, nonce, echostr);
        } catch (Exception ex) {
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
            if ("suite_ticket".equalsIgnoreCase(readTag(xml, "InfoType"))) {
                service.saveSuiteTicket(suiteAppId, readTag(xml, "SuiteId"), readTag(xml, "SuiteTicket"));
                try {
                    service.refreshSuiteAccessToken(suiteAppId);
                } catch (RuntimeException ignored) {
                    // Keep the ticket; the next callback or an explicit refresh can retry token exchange.
                }
            }
            return "success";
        } catch (Exception ex) {
            return "FAILED";
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
