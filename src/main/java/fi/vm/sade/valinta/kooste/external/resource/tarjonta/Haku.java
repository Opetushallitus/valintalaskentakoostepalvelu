package fi.vm.sade.valinta.kooste.external.resource.tarjonta;

import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.dto.KoutaHaku;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Haku {
  public final String oid;
  public final Map<String, String> nimi;
  public final Set<String> tarjoajaOids;
  public final String kohdejoukkoUri;
  public final String hakukausiUri;
  public final Integer hakukausiVuosi;
  public final String koulutuksenAlkamiskausiUri;
  public final Integer koulutuksenAlkamisvuosi;
  public final String ataruLomakeAvain;

  public Haku(
      String oid,
      Map<String, String> nimi,
      Set<String> tarjoajaOids,
      String ataruLomakeAvain,
      String kohdejoukkoUri,
      String hakukausiUri,
      Integer hakukausiVuosi,
      String koulutuksenAlkamiskausiUri,
      Integer koulutuksenAlkamisvuosi) {
    this.oid = oid;
    this.nimi = nimi;
    this.tarjoajaOids = tarjoajaOids;
    this.kohdejoukkoUri = kohdejoukkoUri;
    this.hakukausiUri = hakukausiUri;
    this.hakukausiVuosi = hakukausiVuosi;
    this.koulutuksenAlkamiskausiUri = koulutuksenAlkamiskausiUri;
    this.koulutuksenAlkamisvuosi = koulutuksenAlkamisvuosi;
    this.ataruLomakeAvain = ataruLomakeAvain;
  }

  public Haku(HakuV1RDTO dto) {
    this.oid = dto.getOid();
    this.nimi = dto.getNimi();
    this.tarjoajaOids = new HashSet<>(Arrays.asList(dto.getTarjoajaOids()));
    this.kohdejoukkoUri = dto.getKohdejoukkoUri();
    this.hakukausiUri = dto.getHakukausiUri();
    this.hakukausiVuosi = dto.getHakukausiVuosi();
    this.koulutuksenAlkamiskausiUri = dto.getKoulutuksenAlkamiskausiUri();
    this.koulutuksenAlkamisvuosi = dto.getKoulutuksenAlkamisVuosi();
    this.ataruLomakeAvain = dto.getAtaruLomakeAvain();
  }

  public Haku(KoutaHaku dto) {
    this.oid = dto.oid;
    this.nimi = new HashMap<>();
    dto.nimi.forEach((kieli, nimi) -> this.nimi.put("kieli_" + kieli, nimi));
    this.tarjoajaOids = new HashSet<>();
    this.tarjoajaOids.add(dto.organisaatioOid);
    this.kohdejoukkoUri = dto.kohdejoukkoKoodiUri;
    this.hakukausiUri = null; // TODO
    this.hakukausiVuosi = null;
    this.koulutuksenAlkamiskausiUri = null; // TODO
    this.koulutuksenAlkamisvuosi = null;
    this.ataruLomakeAvain = dto.hakulomakeAtaruId;
  }

  public boolean isKorkeakouluhaku() {
    if (this.kohdejoukkoUri == null) {
      return false;
    }
    return this.kohdejoukkoUri.startsWith("haunkohdejoukko_12#");
  }

  public boolean isAmmatillinenJaLukio() {
    if (this.kohdejoukkoUri == null) {
      return false;
    }
    return this.kohdejoukkoUri.startsWith("haunkohdejoukko_11#");
  }

  public boolean isHakemuspalvelu() {
    return this.ataruLomakeAvain != null;
  }
}
