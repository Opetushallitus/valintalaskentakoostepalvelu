package fi.vm.sade.valinta.kooste.external.resource.tarjonta;

import fi.vm.sade.tarjonta.service.resources.v1.dto.HakukohdeV1RDTO;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.dto.KoutaHakukohde;
import java.util.*;
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
  public final boolean isKoutaHakukohde;

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
      String ohjeetUudelleOpiskelijalle,
      boolean isKoutaHakukohde) {
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
    this.isKoutaHakukohde = isKoutaHakukohde;
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
    this.isKoutaHakukohde = false;
  }

  public Hakukohde(KoutaHakukohde dto) {
    this.oid = dto.oid;
    switch (dto.tila) {
      case "julkaistu":
        this.tila = Tila.JULKAISTU;
        break;
      case "tallennettu":
        this.tila = Tila.LUONNOS;
        break;
      default:
        this.tila = Tila.POISTETTU;
    }
    this.nimi = new HashMap<>();
    dto.nimi.forEach((kieli, nimi) -> this.nimi.put("kieli_" + kieli, nimi));
    this.hakuOid = dto.hakuOid;
    this.tarjoajaOids = Set.of(dto.tarjoaja);
    this.toteutusOids = Collections.singleton(dto.toteutusOid);
    this.hakukohteetUri = null;
    this.pohjakoulutusvaatimusUrit = dto.pohjakoulutusvaatimusKoodiUrit;
    this.valintojenAloituspaikat = dto.aloituspaikat;
    this.valintakokeet =
        dto.valintakokeet.stream().map(Valintakoe::new).collect(Collectors.toSet());
    this.ohjeetUudelleOpiskelijalle = dto.uudenOpiskelijanUrl;
    this.isKoutaHakukohde = true;
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
