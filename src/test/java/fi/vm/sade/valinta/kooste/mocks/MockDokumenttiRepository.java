package fi.vm.sade.valinta.kooste.mocks;

import fi.vm.sade.valinta.kooste.dokumentit.dao.DokumenttiRepository;
import fi.vm.sade.valinta.kooste.dokumentit.dto.DokumenttiDto;
import java.util.Collections;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Profile("mockresources")
@Service
public class MockDokumenttiRepository implements DokumenttiRepository {

  private static final UUID uuid = UUID.randomUUID();

  public DokumenttiDto hae(final UUID key) {
    return new DokumenttiDto(uuid.toString(), "kuvaus", false, Collections.emptyList());
  }

  public void paivitaKuvaus(final UUID uuid, String kuvaus) {}

  @Override
  public void merkkaaValmiiksi(UUID uuid) {}

  public UUID luoDokumentti(String kuvaus) {
    return uuid;
  }

  public void lisaaVirheilmoitus(UUID uuid, String ilmoitus) {}
}
