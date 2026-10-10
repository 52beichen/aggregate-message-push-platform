package dev.qingzhou.pushserver.controller.wecom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import dev.qingzhou.pushserver.model.entity.portal.PortalWecomSuiteApp;
import dev.qingzhou.pushserver.service.PortalWecomSuiteAppService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class WecomSuiteCallbackControllerTest {
    @Mock
    private PortalWecomSuiteAppService service;

    private WecomSuiteCallbackController controller;
    private PortalWecomSuiteApp app;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new WecomSuiteCallbackController(service);
        app = new PortalWecomSuiteApp();
        app.setSuiteId("ww-suite");
    }

    @Test
    void processesCreateAuthorizationEvent() throws Exception {
        controller.processDecrypted(8L, app, xml("create_auth", "<AuthCode>auth-code</AuthCode>"));

        verify(service).completeAuthorization(8L, "auth-code");
    }

    @Test
    void processesChangeAuthorizationEvent() throws Exception {
        controller.processDecrypted(8L, app, xml("change_auth", "<AuthCorpId>ww-corp</AuthCorpId>"));

        verify(service).refreshAuthorization(8L, "ww-corp");
    }

    @Test
    void processesCancelAuthorizationEvent() throws Exception {
        controller.processDecrypted(8L, app, xml("cancel_auth", "<AuthCorpId>ww-corp</AuthCorpId>"));

        verify(service).cancelAuthorization(8L, "ww-corp");
    }

    @Test
    void reportsReadyWhenOpenedWithoutWecomVerificationParameters() {
        assertEquals("WeCom suite callback endpoint is ready",
                controller.verify(8L, null, null, null, null));
        verifyNoInteractions(service);
    }

    private String xml(String infoType, String content) {
        return "<xml><SuiteId>ww-suite</SuiteId><InfoType>" + infoType + "</InfoType>"
                + content + "</xml>";
    }
}
