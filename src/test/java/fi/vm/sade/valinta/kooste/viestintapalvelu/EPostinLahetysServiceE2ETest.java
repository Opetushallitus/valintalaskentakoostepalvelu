package fi.vm.sade.valinta.kooste.viestintapalvelu;

import static fi.vm.sade.valinta.kooste.Integraatiopalvelimet.mockToInternalServerError;
import static fi.vm.sade.valinta.kooste.Integraatiopalvelimet.mockToNotFound;
import static fi.vm.sade.valinta.kooste.Integraatiopalvelimet.mockToReturnJson;
import static fi.vm.sade.valinta.kooste.Integraatiopalvelimet.mockToReturnJsonAndCheckBody;
import static fi.vm.sade.valinta.kooste.Integraatiopalvelimet.mockToReturnString;
import static fi.vm.sade.valinta.kooste.ValintalaskentakoostepalveluJetty.resourcesAddress;
import static fi.vm.sade.valinta.kooste.ValintalaskentakoostepalveluJetty.startShared;
import static fi.vm.sade.valinta.kooste.spec.ConstantsSpec.HAKU1;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;

import fi.vm.sade.valinta.kooste.MockOpintopolkuCasAuthenticationFilter;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.dto.ParametriDTO;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.dto.ParametritDTO;
import fi.vm.sade.valinta.kooste.external.resource.oppijantunnistus.dto.Recipient;
import fi.vm.sade.valinta.kooste.external.resource.oppijantunnistus.dto.TokensResponse;
import fi.vm.sade.valinta.kooste.util.SecurityUtil;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.EPostiRequest;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.EPostiResponse;
import fi.vm.sade.valinta.sharedutils.http.HttpResourceBuilder;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;

public class EPostinLahetysServiceE2ETest {

    private static final long EXPIRATION_TIME = System.currentTimeMillis() + 9999999;

    @Before
    public void init() {
        startShared();
        MockOpintopolkuCasAuthenticationFilter.setRolesToReturnInFakeAuthentication("ROLE_APP_HAKEMUS_READ_UPDATE_" + SecurityUtil.ROOTOID);
    }

    @Test
    public void testEPostinLahetysKaikkiOnnistuu() {
        mockGetBatchIdResponse();
        mockGetEPostiOsoitteetResponse();
        mockGetOhjausparametrit();
        mockPostSecurelinksResponse(".*https:\\/\\/fi\\.test\\.domain\\/token\\/"
                + ".*fi.*testi1@testi\\.fi.*testi2@testi\\.fi.*testi3@testi\\.fi.*");
        EPostiResponse status = sendEPostiExpectSuccess("hyvaksymiskirje", "fi");
        assertEquals(1234l, status.getBatchId().longValue());
        assertEquals(3, status.getNumberOfRecipients().intValue());
    }

    @Test
    public void testNoBatchIdFound1() throws Exception {
        mockToNotFound("GET", "/viestintapalvelu/api/v1/luotettu/letter/getBatchIdReadyForEPosti");
        String message = sendEPostiExpectFailure("hyvaksymiskirje", "fi");
        assertTrue(message.contains("getBatchIdReadyForEPosti HTTP 404"));
    }

    @Test
    public void testNoBatchIdFound2() throws Exception {
        mockToReturnString("GET", "/viestintapalvelu/api/v1/luotettu/letter/getBatchIdReadyForEPosti", null);
        String message = sendEPostiExpectFailure("hyvaksymiskirje", "fi");
        assertTrue(message.contains("Ei löydetty sopivaa kirjelähetyksen ID:tä"));
    }

    @Test
    public void testNoEmailsFound1() throws Exception {
        mockGetBatchIdResponse();
        mockToNotFound("GET", "/viestintapalvelu/api/v1/luotettu/letter/getEPostiAddressesForLetterBatch/1234");
        String message = sendEPostiExpectFailure("hyvaksymiskirje", "fi");
        assertTrue(message.contains("getEPostiAddressesForLetterBatch/1234 HTTP 404"));
    }

    @Test
    public void testNoEmailsFound2() throws Exception {
        mockGetBatchIdResponse();
        mockToReturnJson("GET", "/viestintapalvelu/api/v1/luotettu/letter/getEPostiAddressesForLetterBatch/1234", Arrays.asList());
        String message = sendEPostiExpectFailure("hyvaksymiskirje", "fi");
        assertTrue(message.contains("Ei löydetty sähköpostiosoitteita."));
    }

    @Test
    public void testSecurelinkError() throws Exception {
        mockGetBatchIdResponse();
        mockGetEPostiOsoitteetResponse();
        mockGetOhjausparametrit();
        mockToInternalServerError("POST", "/oppijan-tunnistus/api/v1/tokens");
        String message = sendEPostiExpectFailure("hyvaksymiskirje", "fi");
        assertTrue(message.contains("tokens HTTP 500"));
    }

    @Test
    public void testInvalidParameters() throws Exception {
        String message = sendEPostiExpectFailure("hyvaksymiskirje", "foo");
        assertTrue(message.contains("ei ole validi asiointikieli"));
        message = sendEPostiExpectFailure("kirje", "sv");
        assertTrue(message.contains("ei ole validi kirjeen tyyppi"));
        message = sendEPostiExpectFailure("hyvaksymiskirje", null);
        assertTrue(message.contains("ovat pakollisia parametreja"));
    }

    private Response sendEPosti(String kirjeenTyyppi, String asiointikieli) {
        HttpResourceBuilder.WebClientExposingHttpResource http = new HttpResourceBuilder(getClass().getName())
                .address(resourcesAddress + "/viestintapalvelu/securelinkit/aktivoi")
                .buildExposingWebClientDangerously();
        EPostiRequest request = new EPostiRequest();
        request.setAsiointikieli(asiointikieli);
        request.setHakuOid(HAKU1);
        request.setKirjeenTyyppi(kirjeenTyyppi);
        WebClient client = http.getWebClient()
                .accept(MediaType.APPLICATION_JSON_TYPE);
        return client.post(Entity.json(request));
    }

    private String sendEPostiExpectFailure(String kirjeenTyyppi, String asiointikieli) throws IOException {
        Response response = sendEPosti(kirjeenTyyppi, asiointikieli);
        assertEquals(500, response.getStatus());
        return IOUtils.toString((InputStream)response.getEntity());
    }

    private EPostiResponse sendEPostiExpectSuccess(String kirjeenTyyppi, String asiointikieli) {
        Response response = sendEPosti(kirjeenTyyppi, asiointikieli);
        assertEquals(200, response.getStatus());
        return new Gson().fromJson(new InputStreamReader((InputStream)response.getEntity()), EPostiResponse.class);
    }

    private void mockGetBatchIdResponse() {
        mockToReturnString("GET", "/viestintapalvelu/api/v1/luotettu/letter/getBatchIdReadyForEPosti", "1234");
    }

    private void mockGetEPostiOsoitteetResponse() {
        mockToReturnJson("GET", "/viestintapalvelu/api/v1/luotettu/letter/getEPostiAddressesForLetterBatch/1234",
                ImmutableMap.of("oid1", "testi1@testi.fi", "oid2", "testi2@testi.fi", "oid3", "testi3@testi.fi"));
    }

    private void mockPostSecurelinksResponse(String bodyRegexp) {
        mockToReturnJsonAndCheckBody("POST", "/oppijan-tunnistus/api/v1/tokens", createTokensResponse(), bodyRegexp);
    }

    private void mockGetOhjausparametrit() {
        ParametritDTO parametrit = new ParametritDTO();
        ParametriDTO parametri = new ParametriDTO();
        parametri.setDate(new java.util.Date(EXPIRATION_TIME));
        parametrit.setPH_HKP(parametri);
        mockToReturnJson("GET", "/ohjausparametrit-service/api/v1/rest/parametri/HAKU1", parametrit);
    }

    private TokensResponse createTokensResponse() {
        TokensResponse response = new TokensResponse();
        Recipient recipient1 = new Recipient();
        recipient1.setEmail("testi1@testi.fi");
        recipient1.setSecurelink("http://www.opintopolku.fi/1111111");
        Recipient recipient2 = new Recipient();
        recipient2.setEmail("testi2@testi.fi");
        recipient2.setSecurelink("http://www.opintopolku.fi/2222222");
        Recipient recipient3 = new Recipient();
        recipient3.setEmail("testi3@testi.fi");
        recipient3.setSecurelink("http://www.opintopolku.fi/3333333");
        response.setRecipients(Arrays.asList(recipient1, recipient2, recipient3));
        return response;
    }
}
