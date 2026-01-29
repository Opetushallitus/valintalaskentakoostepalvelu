package fi.vm.sade.valinta.kooste.proxy.resource.valintatulosservice;

import static fi.vm.sade.valinta.kooste.mocks.MockData.hakemusOid;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ValintaTulosServiceProxyResourceTest {
  private final ObjectMapper objectMapper = new ObjectMapper();

  public ValintaTulosServiceProxyResourceTest() {
    objectMapper.registerModule(
        new ValintaTulosServiceProxyResource.ValintaTulosServiceSerializersModule());
  }

  @Test
  public void vastaanottoAikarajaMennytDtoSerializesDeadlineNicely()
      throws JsonProcessingException {
    VastaanottoAikarajaMennytDTO dto = new VastaanottoAikarajaMennytDTO();
    dto.setHakemusOid(hakemusOid);
    String vastaanottoDeadline = "2016-07-15T12:00:00Z";
    dto.setVastaanottoDeadline(ZonedDateTime.of(2016, 7, 15, 12, 0, 0, 0, ZoneOffset.UTC));
    dto.setMennyt(true);
    String jsonString = objectMapper.writeValueAsString(dto);
    Assertions.assertEquals(
        String.format(
            "{\"hakemusOid\":\"%s\",\"mennyt\":true,\"vastaanottoDeadline\":\"%s\"}",
            hakemusOid, vastaanottoDeadline),
        jsonString);
  }

  @Test
  public void nullDeadlineLeavesElementOut() throws JsonProcessingException {
    VastaanottoAikarajaMennytDTO dto = new VastaanottoAikarajaMennytDTO();
    dto.setHakemusOid(hakemusOid);
    dto.setVastaanottoDeadline(null);
    dto.setMennyt(true);
    String jsonString = objectMapper.writeValueAsString(dto);
    Assertions.assertEquals(
        String.format("{\"hakemusOid\":\"%s\",\"mennyt\":true}", hakemusOid), jsonString);
  }
}
