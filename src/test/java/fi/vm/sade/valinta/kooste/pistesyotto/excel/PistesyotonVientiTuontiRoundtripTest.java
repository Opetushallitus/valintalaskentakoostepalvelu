package fi.vm.sade.valinta.kooste.pistesyotto.excel;

import static fi.vm.sade.valinta.kooste.spec.hakemus.HakemusSpec.hakemus;

import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteDTO;
import fi.vm.sade.valinta.kooste.excel.Excel;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.ApplicationAdditionalDataDTO;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeOsallistuminenDTO;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PistesyotonVientiTuontiRoundtripTest extends PistesyotonTuontiTestBase {

  private static final Logger LOG = LoggerFactory.getLogger(PistesyotonVientiTuontiRoundtripTest.class);

  @Test
  public void testaaOutput()
      throws FileNotFoundException, IOException, JsonIOException, JsonSyntaxException, Exception {
    List<ValintakoeOsallistuminenDTO> osallistumistiedot = lueOsallistumisTiedot(
        "List_ValintakoeOsallistuminenDTO.json");
    List<ValintaperusteDTO> valintaperusteet = lueValintaperusteet("List_ValintaperusteDTO.json");
    List<ApplicationAdditionalDataDTO> pistetiedot = luePistetiedot("List_ApplicationAdditionalDataDTO.json");
    PistesyottoExcel pistesyottoExcel = new PistesyottoExcel("testioidi1", "1.2.246.562.5.85532589612", "jep", "",
        "", "", Optional.empty(), createHakemukset(pistetiedot), Collections.<String>emptySet(),
        Arrays.asList("1_2_246_562_5_85532589612_urheilija_lisapiste",
            "Eläintenhoidon koulutusohjelma, pk (Maatalousalan perustutkinto), pääsy- ja soveltuvuuskoe",
            "kielikoe_fi"),
        osallistumistiedot, valintaperusteet, pistetiedot,
        Collections.singletonList(new PistesyottoDataRiviListAdapter()));
    Excel excel = pistesyottoExcel.getExcel();

    excel.tuoXlsx(excel.vieXlsx());

    // tallenna(excel);
  }

  private Collection<HakemusWrapper> createHakemukset(List<ApplicationAdditionalDataDTO> pistetiedot) {
    List<HakemusWrapper> hakemukset = new ArrayList<>();
    for (int i = 0; i < pistetiedot.size(); i++) {
      hakemukset.add(createHakemus(pistetiedot.get(i), i));
    }
    return hakemukset;
  }

  private HakemusWrapper createHakemus(ApplicationAdditionalDataDTO pistetieto, int i) {
    if (i % 2 == 0) {
      return hakemus().setOid(pistetieto.getOid()).setSyntymaaika("1.1.1900").build();
    } else {
      return hakemus().setOid(pistetieto.getOid()).setHenkilotunnus("010101-100x").build();
    }
  }
}
