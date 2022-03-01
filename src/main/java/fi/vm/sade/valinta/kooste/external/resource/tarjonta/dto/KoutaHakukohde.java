package fi.vm.sade.valinta.kooste.external.resource.tarjonta.dto;

import java.util.Map;
import java.util.Set;

public class KoutaHakukohde {
  public final String oid;
  public final String tila;
  public final Map<String, String> nimi;
  public final String hakuOid;
  public final String tarjoaja;
  public final String toteutusOid;
  public final Integer aloituspaikat;
  public final Set<KoutaValintakoe> valintakokeet;
  public final Set<String> pohjakoulutusvaatimusKoodiUrit;
  public final Map<String, String> uudenOpiskelijanUrl;

  private KoutaHakukohde() {
    this.oid = null;
    this.tila = null;
    this.nimi = null;
    this.hakuOid = null;
    this.tarjoaja = null;
    this.toteutusOid = null;
    this.aloituspaikat = null;
    this.valintakokeet = null;
    this.pohjakoulutusvaatimusKoodiUrit = null;
    this.uudenOpiskelijanUrl = null;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    KoutaHakukohde that = (KoutaHakukohde) o;

    return oid.equals(that.oid);
  }

  @Override
  public int hashCode() {
    return oid.hashCode();
  }

  public static class KoutaValintakoe {
    public final String id;
    public final String tyyppi;

    private KoutaValintakoe() {
      this.id = null;
      this.tyyppi = null;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      KoutaValintakoe that = (KoutaValintakoe) o;

      return id.equals(that.id);
    }

    @Override
    public int hashCode() {
      return id.hashCode();
    }
  }
}
