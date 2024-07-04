package fi.vm.sade.valinta.kooste.dokumentit.dao;

import fi.vm.sade.valinta.kooste.configuration.DatabaseConfiguration;
import fi.vm.sade.valinta.kooste.dokumentit.dto.DokumenttiDto;
import fi.vm.sade.valinta.kooste.testapp.RepositoryTestApp;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = {RepositoryTestApp.class})
@Import(DatabaseConfiguration.class)
@ComponentScan(basePackages = {"fi.vm.sade.valinta.kooste.dokumentit.dao"})
@ActiveProfiles("test")
public class DokumenttiRepositoryTest {

  static final Logger LOG = LoggerFactory.getLogger(DokumenttiRepositoryTest.class);

  @Autowired private DokumenttiRepository repo;

  @Test
  public void testLuodokumentti() {
    UUID uuid = repo.luoDokumentti("test");
    DokumenttiDto dto = repo.hae(uuid);
    Assertions.assertEquals("test", dto.getKuvaus());

    // valintalaskenta-ui haluaa että tämä on null jos tuonti ei valmis ja "valmis" jos tuonti
    // valmis
    Assertions.assertEquals(null, dto.getDokumenttiId());
    Assertions.assertEquals(uuid.toString(), dto.getUuid());
    Assertions.assertEquals(false, dto.isValmis());
    Assertions.assertEquals(false, dto.isVirheita());

    // valintalaskenta-ui haluaa että tämä on null jos ei virheitä
    Assertions.assertEquals(null, dto.getVirheilmoitukset());
  }

  @Test
  public void testMerkkaavalmiiksi() {
    UUID uuid = repo.luoDokumentti("test");
    repo.merkkaaValmiiksi(uuid);
    DokumenttiDto dto = repo.hae(uuid);
    Assertions.assertEquals(true, dto.isValmis());

    // valintalaskenta-ui haluaa että tämä on null jos tuonti ei valmis ja "valmis" jos tuonti
    // valmis
    Assertions.assertEquals("valmis", dto.getDokumenttiId());
  }

  @Test
  public void lisaaVirhe() {
    UUID uuid = repo.luoDokumentti("test");
    repo.lisaaVirheilmoitus(uuid, "virhe");
    DokumenttiDto dto = repo.hae(uuid);
    Assertions.assertEquals(
        ":virhe",
        dto.getVirheilmoitukset().stream()
            .map(v -> v.getTyyppi() + ":" + v.getIlmoitus())
            .collect(Collectors.joining(",")));
    Assertions.assertEquals(true, dto.isVirheita());
  }
}
