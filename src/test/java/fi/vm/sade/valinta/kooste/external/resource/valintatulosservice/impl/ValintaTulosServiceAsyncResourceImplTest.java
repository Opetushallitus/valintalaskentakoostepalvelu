package fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.impl;

import fi.vm.sade.valinta.http.HttpResource;
import fi.vm.sade.valinta.kooste.proxy.resource.valintatulosservice.VastaanottoAikarajaMennytDTO;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ValintaTulosServiceAsyncResourceImplTest {
    private final HttpResource vtsHttpResource = new ValintaTulosServiceAsyncResourceImpl("http://localhost/this/should/be/valinta-tulos-service/url");

    @Test
    public void vastaanottoAikarajaMennytDTOsCanBeParsed() {
        String hakemusOid = "1.2.246.562.11.00004697189";
        String vastaanottoDeadline = "2016-07-15T12:00:00Z";

        VastaanottoAikarajaMennytDTO parsedDto = vtsHttpResource.gson().fromJson(
            " {\n" +
            "        \"hakemusOid\": \"" + hakemusOid + "\",\n" +
            "        \"mennyt\": true,\n" +
            "        \"vastaanottoDeadline\": \"" + vastaanottoDeadline + "\"\n" +
            "    }", VastaanottoAikarajaMennytDTO.class);
        Assert.assertEquals(hakemusOid, parsedDto.getHakemusOid());
        Assert.assertEquals(new DateTime(2016, 7, 15, 12, 0, 0, DateTimeZone.UTC), parsedDto.getVastaanottoDeadline());
        Assert.assertEquals(true, parsedDto.isMennyt());
    }
}
