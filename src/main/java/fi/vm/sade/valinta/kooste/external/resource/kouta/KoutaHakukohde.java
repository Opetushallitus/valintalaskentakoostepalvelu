package fi.vm.sade.valinta.kooste.external.resource.kouta;

import fi.vm.sade.valinta.kooste.external.resource.kouta.dto.KoutaHakukohdeDTO;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.AbstractHakukohde;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class KoutaHakukohde extends AbstractHakukohde {
  public final Set<KoutaValintakoe> valintakokeet;

  protected KoutaHakukohde(
      String oid,
      Tila tila,
      Map<String, String> nimi,
      String hakuOid,
      Set<String> tarjoajaOids,
      Set<String> toteutusOids,
      String hakukohteetUri,
      Set<String> pohjakoulutusvaatimusUrit,
      Integer valintojenAloituspaikat,
      Set<KoutaValintakoe> valintakokeet,
      Map<String, String> ohjeetUudelleOpiskelijalle) {
    super(
        oid,
        tila,
        nimi,
        hakuOid,
        tarjoajaOids,
        toteutusOids,
        hakukohteetUri,
        pohjakoulutusvaatimusUrit,
        valintojenAloituspaikat,
        ohjeetUudelleOpiskelijalle);
    this.valintakokeet = valintakokeet;
  }

  public KoutaHakukohde(KoutaHakukohdeDTO dto) {
    super(
        dto.oid,
        parseTila(dto.tila),
        new HashMap<>(),
        dto.hakuOid,
        Set.of(dto.tarjoaja),
        Collections.singleton(dto.toteutusOid),
        null,
        dto.pohjakoulutusvaatimusKoodiUrit,
        dto.aloituspaikat,
        new HashMap<>());

    this.valintakokeet =
        dto.valintakokeet.stream().map(KoutaValintakoe::new).collect(Collectors.toSet());
    dto.nimi.forEach((kieli, nimi) -> this.nimi.put("kieli_" + kieli, nimi));
    if (dto.uudenOpiskelijanUrl != null) {
      dto.uudenOpiskelijanUrl.forEach(
          (kieli, uudenOpiskelijanUrl) ->
              this.ohjeetUudelleOpiskelijalle.put("kieli_" + kieli, uudenOpiskelijanUrl));
    }
  }

  private static Tila parseTila(String tila) {
    switch (tila) {
      case "julkaistu":
        return Tila.JULKAISTU;
      case "tallennettu":
        return Tila.LUONNOS;
      default:
        return Tila.POISTETTU;
    }
  }
}
