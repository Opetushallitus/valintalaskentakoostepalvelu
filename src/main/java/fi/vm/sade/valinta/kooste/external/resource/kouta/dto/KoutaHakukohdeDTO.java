package fi.vm.sade.valinta.kooste.external.resource.kouta.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class KoutaHakukohdeDTO {
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
  public final BigDecimal alinHyvaksyttyKeskiarvo;
  public final List<PainotettuArvosana> painotetutArvosanat;
  public final Set<KoutaValintakoe> valintaperusteValintakokeet;

  private KoutaHakukohdeDTO() {
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
    this.alinHyvaksyttyKeskiarvo = null;
    this.painotetutArvosanat = null;
    this.valintaperusteValintakokeet = null;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    KoutaHakukohdeDTO that = (KoutaHakukohdeDTO) o;

    return oid.equals(that.oid);
  }

  @Override
  public int hashCode() {
    return oid.hashCode();
  }

  public static class KoutaValintakoe {
    public final String id;
    public final String tyyppi;
    public final BigDecimal vahimmaispisteet;

    private KoutaValintakoe() {
      this.id = null;
      this.tyyppi = null;
      this.vahimmaispisteet = null;
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

  public static class PainotettuArvosana {
    public final String koodiUri;
    public final BigDecimal painokerroin;

    private PainotettuArvosana() {
      koodiUri = null;
      painokerroin = null;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      PainotettuArvosana that = (PainotettuArvosana) o;

      return koodiUri.equals(that.koodiUri);
    }

    @Override
    public int hashCode() {
      return koodiUri.hashCode();
    }
  }
}
