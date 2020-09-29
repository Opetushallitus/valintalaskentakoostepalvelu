package fi.vm.sade.valinta.kooste.external.resource.tarjonta;

import fi.vm.sade.tarjonta.service.resources.v1.dto.koulutus.KoulutusV1RDTO;
import java.util.Set;

public class Toteutus {
  public final String oid;
  public final String koulutusOid;
  public final String alkamiskausiUri;
  public final Integer alkamisvuosi;
  public final Set<String> opetuskielet;

  public Toteutus(
      String oid,
      String koulutusOid,
      String alkamiskausiUri,
      Integer alkamisvuosi,
      Set<String> opetuskielet) {
    this.oid = oid;
    this.koulutusOid = koulutusOid;
    this.alkamiskausiUri = alkamiskausiUri;
    this.alkamisvuosi = alkamisvuosi;
    this.opetuskielet = opetuskielet;
  }

  public Toteutus(KoulutusV1RDTO dto) {
    this.oid = dto.getOid();
    this.koulutusOid = dto.getKomoOid();
    this.alkamiskausiUri =
        dto.getKoulutuksenAlkamiskausi() == null ? null : dto.getKoulutuksenAlkamiskausi().getUri();
    this.alkamisvuosi = dto.getKoulutuksenAlkamisvuosi();
    this.opetuskielet = dto.getOpetuskielis().getUris().keySet();
  }
}
