package fi.vm.sade.valinta.kooste.dokumentit.dao;

import fi.vm.sade.valinta.kooste.dokumentit.dto.DokumenttiDto;
import java.util.UUID;

public interface DokumenttiRepository {
  DokumenttiDto hae(final UUID key);

  void paivitaKuvaus(final UUID uuid, String kuvaus);

  void merkkaaValmiiksi(final UUID uuid);

  UUID luoDokumentti(String kuvaus);

  void lisaaVirheilmoitus(UUID uuid, String ilmoitus);
}
