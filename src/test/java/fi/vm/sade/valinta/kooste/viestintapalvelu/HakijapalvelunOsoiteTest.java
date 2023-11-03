package fi.vm.sade.valinta.kooste.viestintapalvelu;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import fi.vm.sade.organisaatio.resource.dto.OrganisaatioRDTO;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.dto.Organisaatio;
import fi.vm.sade.valinta.kooste.util.KieliUtil;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoite;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Teksti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.HaeOsoiteKomponentti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.LueHakijapalvelunOsoite;
import java.io.IOException;
import java.io.InputStreamReader;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

public class HakijapalvelunOsoiteTest {

  @Disabled
  @Test
  public void testaaHakijapalvelunOsoitteenHaku3()
      throws JsonSyntaxException, JsonIOException, IOException {
    Organisaatio organisaatio =
        new Gson()
            .fromJson(
                new InputStreamReader(
                    new ClassPathResource("organisaatio/osoite.json").getInputStream()),
                Organisaatio.class);
    HaeOsoiteKomponentti h = new HaeOsoiteKomponentti(null);
    System.err.println(new GsonBuilder().setPrettyPrinting().create().toJson(organisaatio));
    Osoite osoite =
        LueHakijapalvelunOsoite.lueHakijapalvelunOsoite(
            h, KieliUtil.SUOMI, organisaatio, new Teksti());
  }

  @Disabled
  @Test
  public void testaaHakijapalvelunOsoitteenHaku()
      throws JsonSyntaxException, JsonIOException, IOException {
    OrganisaatioRDTO organisaatio =
        new Gson()
            .fromJson(
                new InputStreamReader(
                    new ClassPathResource("organisaatio/organisaatiodto.json").getInputStream()),
                OrganisaatioRDTO.class);
  }
}
