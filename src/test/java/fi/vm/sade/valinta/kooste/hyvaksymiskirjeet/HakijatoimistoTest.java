package fi.vm.sade.valinta.kooste.hyvaksymiskirjeet;

import com.google.common.collect.ImmutableMap;
import fi.vm.sade.organisaatio.resource.dto.HakutoimistoDTO;
import fi.vm.sade.valinta.kooste.Integraatiopalvelimet;
import fi.vm.sade.valinta.kooste.external.resource.HttpClient;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.impl.OrganisaatioAsyncResourceImpl;
import fi.vm.sade.valinta.sharedutils.http.DateDeserializer;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static fi.vm.sade.valinta.kooste.ValintalaskentakoostepalveluJetty.startShared;
import static javax.ws.rs.HttpMethod.GET;
import static org.junit.Assert.assertEquals;

public class HakijatoimistoTest {
    @Before
    public void init() {
        startShared();
    }

    @Test
    public void testaaHyvaksymiskirjeetServicenLapi() throws InterruptedException, ExecutionException, TimeoutException {
        String tarjoajaOid = "tarjoajaOid";
        HakutoimistoDTO hakutoimisto = new HakutoimistoDTO(ImmutableMap.of("jee", "jee"), Collections.emptyMap());
        Integraatiopalvelimet.mockToReturnJson(GET, "/organisaatio-service/rest/organisaatio/v2/" + tarjoajaOid + "/hakutoimisto", hakutoimisto);

        OrganisaatioAsyncResourceImpl o = new OrganisaatioAsyncResourceImpl(
                new HttpClient(
                        java.net.http.HttpClient.newBuilder().build(),
                        null,
                        DateDeserializer.gsonBuilder().create()
                )
        );
        assertEquals(Optional.of(hakutoimisto), o.haeHakutoimisto(tarjoajaOid).get(10, TimeUnit.SECONDS));
    }

    @Test
    public void testaaHakijatoimistonValinnaisuus() throws InterruptedException, ExecutionException, TimeoutException {
        String tarjoajaOid = "tarjoajaOid";
        Integraatiopalvelimet.mockToNotFound(GET, "/organisaatio-service/rest/organisaatio/v2/" + tarjoajaOid + "/hakutoimisto");

        OrganisaatioAsyncResourceImpl o = new OrganisaatioAsyncResourceImpl(
                new HttpClient(
                        java.net.http.HttpClient.newBuilder().build(),
                        null,
                        DateDeserializer.gsonBuilder().create()
                )
        );
        assertEquals(Optional.empty(), o.haeHakutoimisto(tarjoajaOid).get(10, TimeUnit.SECONDS));
    }
}
