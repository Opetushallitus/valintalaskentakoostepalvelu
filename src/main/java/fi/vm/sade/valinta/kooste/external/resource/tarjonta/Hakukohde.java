package fi.vm.sade.valinta.kooste.external.resource.tarjonta;

import fi.vm.sade.tarjonta.service.resources.v1.dto.HakukohdeV1RDTO;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class Hakukohde {
  public final String oid;
  public final Tila tila;
  public final Map<String, String> nimi;
  public final String hakuOid;
  public final Set<String> tarjoajaOids;
  public final Set<String> toteutusOids;
  public final String hakukohteetUri;
  public final Set<String> pohjakoulutusvaatimusUrit;
  public final Integer valintojenAloituspaikat;
  public final Set<Valintakoe> valintakokeet;
  public final String ohjeetUudelleOpiskelijalle;

  public Hakukohde(
      String oid,
      Tila tila,
      Map<String, String> nimi,
      String hakuOid,
      Set<String> tarjoajaOids,
      Set<String> toteutusOids,
      String hakukohteetUri,
      Set<String> pohjakoulutusvaatimusUrit,
      Integer valintojenAloituspaikat,
      Set<Valintakoe> valintakokeet,
      String ohjeetUudelleOpiskelijalle) {
    this.oid = oid;
    this.tila = tila;
    this.nimi = nimi;
    this.hakuOid = hakuOid;
    this.tarjoajaOids = tarjoajaOids;
    this.toteutusOids = toteutusOids;
    this.hakukohteetUri = hakukohteetUri;
    this.pohjakoulutusvaatimusUrit = pohjakoulutusvaatimusUrit;
    this.valintojenAloituspaikat = valintojenAloituspaikat;
    this.valintakokeet = valintakokeet;
    this.ohjeetUudelleOpiskelijalle = ohjeetUudelleOpiskelijalle;
  }

  public Hakukohde(HakukohdeV1RDTO dto) {
    this.oid = dto.getOid();
    this.tila = Tila.valueOf(dto.getTila().name());
    this.nimi = dto.getHakukohteenNimet();
    this.hakuOid = dto.getHakuOid();
    this.tarjoajaOids = dto.getTarjoajaOids();
    this.toteutusOids = new HashSet<>(dto.getHakukohdeKoulutusOids());
    this.hakukohteetUri = dto.getHakukohteenNimiUri();
    this.pohjakoulutusvaatimusUrit =
        dto.getPohjakoulutusvaatimus() != null
            ? Collections.singleton(dto.getPohjakoulutusvaatimus())
            : new HashSet<>();
    this.valintojenAloituspaikat = dto.getValintojenAloituspaikatLkm();
    this.valintakokeet =
        dto.getValintakokeet().stream().map(Valintakoe::new).collect(Collectors.toSet());
    this.ohjeetUudelleOpiskelijalle = dto.getOhjeetUudelleOpiskelijalle();
  }

  public enum Tila {
    POISTETTU,
    LUONNOS,
    VALMIS,
    JULKAISTU,
    PERUTTU,
    KOPIOITU,
    PUUTTEELLINEN;
  }
}
