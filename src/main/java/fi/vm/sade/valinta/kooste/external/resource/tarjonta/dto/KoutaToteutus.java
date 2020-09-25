package fi.vm.sade.valinta.kooste.external.resource.tarjonta.dto;

import java.util.List;
import java.util.Set;

public class KoutaToteutus {
  public final String oid;
  public final String koulutusOid;
  public final Set<String> tarjoajat;
  public final Metadata metadata;

  private KoutaToteutus() {
    this.oid = null;
    this.koulutusOid = null;
    this.tarjoajat = null;
    this.metadata = null;
  }

  public static class Metadata {
    public final Opetus opetus;
    public final List<Osaamisala> osaamisalat;

    private Metadata() {
      this.opetus = null;
      this.osaamisalat = null;
    }
  }

  public static class Opetus {
    public final Set<String> opetuskieliKoodiUrit;

    private Opetus() {
      this.opetuskieliKoodiUrit = null;
    }
  }

  public static class Osaamisala {
    public final String koodi;

    private Osaamisala() {
      this.koodi = null;
    }
  }
}
