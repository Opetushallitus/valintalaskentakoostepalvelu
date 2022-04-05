package fi.vm.sade.valinta.kooste.external.resource.tarjonta;

import fi.vm.sade.tarjonta.service.resources.v1.dto.HakukohdeV1RDTO;
import java.util.*;
import java.util.stream.Collectors;

public class TarjontaHakukohde extends AbstractHakukohde {

  public TarjontaHakukohde(
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
        valintakokeet,
        ohjeetUudelleOpiskelijalle);
  }

  public TarjontaHakukohde(HakukohdeV1RDTO dto) {
    super(
        dto.getOid(),
        Tila.valueOf(dto.getTila().name()),
        dto.getHakukohteenNimet(),
        dto.getHakuOid(),
        dto.getTarjoajaOids(),
        new HashSet<>(dto.getHakukohdeKoulutusOids()),
        dto.getHakukohteenNimiUri(),
        dto.getPohjakoulutusvaatimus() != null
            ? Collections.singleton(dto.getPohjakoulutusvaatimus())
            : new HashSet<>(),
        dto.getValintojenAloituspaikatLkm(),
        dto.getValintakokeet().stream().map(Valintakoe::new).collect(Collectors.toSet()),
        new HashMap<>());

    if (dto.getOhjeetUudelleOpiskelijalle() != null) {
      this.ohjeetUudelleOpiskelijalle.put("kieli_fi", dto.getOhjeetUudelleOpiskelijalle());
      this.ohjeetUudelleOpiskelijalle.put("kieli_sv", dto.getOhjeetUudelleOpiskelijalle());
      this.ohjeetUudelleOpiskelijalle.put("kieli_en", dto.getOhjeetUudelleOpiskelijalle());
    }
  }
}
