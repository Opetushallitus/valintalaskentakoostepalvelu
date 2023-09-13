package fi.vm.sade.valinta.kooste.proxy.resource.viestintapalvelu;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.dto.LetterBatchCountDto;
import org.junit.jupiter.api.Test;

public class ViestintapalveluDtoTest {

  @Test
  public void testLetterBatchDtoConversion() {
    String json =
        "{\"letterTotalCount\":4,\"letterReadyCount\":2,\"letterErrorCount\":1,\"letterPublishedCount\":1}";
    LetterBatchCountDto countDto = new Gson().fromJson(json, LetterBatchCountDto.class);
    assertTrue(countDto.letterTotalCount == 4);
    assertTrue(countDto.letterErrorCount == 1);
    assertTrue(countDto.letterErrorCount == 1);
    assertTrue(countDto.letterPublishedCount == 1);
  }
}
