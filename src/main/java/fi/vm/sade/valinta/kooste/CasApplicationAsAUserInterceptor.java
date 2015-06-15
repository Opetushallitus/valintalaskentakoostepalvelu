package fi.vm.sade.valinta.kooste;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import fi.vm.sade.authentication.cas.CasClient;
import fi.vm.sade.generic.ui.app.UserLiferayImpl;

/**
 * @Deprecated Ainoastaan Cache-ongelmien poistotarkoituksiin.
 */
@Deprecated
public class CasApplicationAsAUserInterceptor extends AbstractPhaseInterceptor<Message> {
    private static final Logger logger = LoggerFactory.getLogger(CasApplicationAsAUserInterceptor.class);

    private String webCasUrl;
    private String targetService;
    private String appClientUsername;
    private String appClientPassword;

    @Value("${auth.mode:cas}")
    private String authMode;

    public CasApplicationAsAUserInterceptor() {
        super(Phase.PRE_PROTOCOL);
    }

    @Override
    public void handleMessage(Message message) throws Fault {
        String serviceTicket = CasClient.getTicket(webCasUrl,
                appClientUsername, appClientPassword, targetService);

        HttpURLConnection httpConnection = (HttpURLConnection) message
                .get("http.connection");
        if (serviceTicket == null && "dev".equals(authMode)) {
            Set<GrantedAuthority> authorities = UserLiferayImpl.buildMockAuthorities();

            String mockUser = "1.2.246.562.24.00000000001";
            logger.warn("building mock user: " + mockUser + ", authorities: " + authorities);
            Authentication authentication = new TestingAuthenticationToken(mockUser, mockUser, new ArrayList<GrantedAuthority>(authorities));

            httpConnection.setRequestProperty("CasSecurityTicket", "oldDeprecatedSecurity_REMOVE");
            String user = authentication.getName();
            httpConnection.setRequestProperty("oldDeprecatedSecurity_REMOVE_username", user);
            httpConnection.setRequestProperty("oldDeprecatedSecurity_REMOVE_authorities", toString(authorities));
            logger.info("DEV Proxy ticket! user: " + user + ", authorities: " + authorities);
            return;
        }

        // put service ticket to SOAP message as a http header
        // 'CasSecurityTicket'
        httpConnection.setRequestProperty("CasSecurityTicket", serviceTicket);

        logger.info(
                "CasApplicationAsAUserInterceptor, targetService: {}, endpoint: {}, serviceuser: {}, CasSecurityTicket: {}",
                new Object[]{targetService,
                        message.get(Message.ENDPOINT_ADDRESS),
                        appClientUsername, serviceTicket});
    }

    public void setWebCasUrl(String webCasUrl) {
        this.webCasUrl = webCasUrl;
    }

    public void setTargetService(String targetService) {
        this.targetService = targetService;
    }

    public void setAppClientUsername(String appClientUsername) {
        this.appClientUsername = appClientUsername;
    }

    public void setAppClientPassword(String appClientPassword) {
        this.appClientPassword = appClientPassword;
    }

    private String toString(Collection<? extends GrantedAuthority> authorities) {
        StringBuilder sb = new StringBuilder();
        for (GrantedAuthority authority : authorities) {
            sb.append(authority.getAuthority()).append(",");
        }
        return sb.toString();
    }

}
