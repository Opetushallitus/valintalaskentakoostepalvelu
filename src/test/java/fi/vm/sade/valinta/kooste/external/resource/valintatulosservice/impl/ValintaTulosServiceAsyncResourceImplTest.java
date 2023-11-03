package fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import fi.vm.sade.valinta.kooste.proxy.resource.valintatulosservice.VastaanottoAikarajaMennytDTO;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;

public class ValintaTulosServiceAsyncResourceImplTest {

  @Test
  public void vastaanottoAikarajaMennytDTOsCanBeParsed() {
    String hakemusOid = "1.2.246.562.11.00004697189";
    String vastaanottoDeadline = "2016-07-15T12:00:00Z";
    VastaanottoAikarajaMennytDTO parsedDto =
        ValintaTulosServiceAsyncResourceImpl.getGson()
            .fromJson(
                " {\n"
                    + "        \"hakemusOid\": \""
                    + hakemusOid
                    + "\",\n"
                    + "        \"mennyt\": true,\n"
                    + "        \"vastaanottoDeadline\": \""
                    + vastaanottoDeadline
                    + "\"\n"
                    + "    }",
                VastaanottoAikarajaMennytDTO.class);
    assertEquals(hakemusOid, parsedDto.getHakemusOid());
    assertEquals(
        new DateTime(2016, 7, 15, 12, 0, 0, DateTimeZone.UTC), parsedDto.getVastaanottoDeadline());
    assertEquals(true, parsedDto.isMennyt());
  }
}
