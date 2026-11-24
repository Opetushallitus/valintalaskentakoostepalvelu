package fi.vm.sade.valinta.kooste.pistesyotto.dto;

import static fi.vm.sade.valinta.kooste.external.resource.valintapiste.dto.Valintapisteet.*;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteDTO;
import fi.vm.sade.service.valintaperusteet.dto.model.Osallistuminen;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.ApplicationAdditionalDataDTO;
import fi.vm.sade.valinta.kooste.external.resource.valintapiste.dto.Valintapisteet;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.HakutoiveDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeOsallistuminenDTO;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.tuple.Pair;

@JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
public class HakemuksenKoetulosYhteenveto {
  @JsonProperty public ApplicationAdditionalDataDTO applicationAdditionalDataDTO;

  @JsonProperty
  private Map<String, HakukohteenOsallistumistiedotDTO> hakukohteidenOsallistumistiedot;

  @JsonCreator
  public HakemuksenKoetulosYhteenveto(
      @JsonProperty("applicationAdditionalDataDTO")
          ApplicationAdditionalDataDTO applicationAdditionalDataDTO,
      @JsonProperty("hakukohteidenOsallistumistiedot")
          Map<String, HakukohteenOsallistumistiedotDTO> hakukohteidenOsallistumistiedot) {
    this.applicationAdditionalDataDTO = applicationAdditionalDataDTO;
    this.hakukohteidenOsallistumistiedot = hakukohteidenOsallistumistiedot;
  }

  public HakemuksenKoetulosYhteenveto(
      Valintapisteet valintapisteet,
      Pair<String, List<ValintaperusteDTO>> valintaperusteet,
      ValintakoeOsallistuminenDTO kokeet) {
    this.hakukohteidenOsallistumistiedot =
        (kokeet == null ? Stream.<HakutoiveDTO>empty() : kokeet.getHakutoiveet().stream())
            .collect(
                Collectors.toMap(
                    HakutoiveDTO::getHakukohdeOid,
                    h -> new HakukohteenOsallistumistiedotDTO(h, kokeet.getHakutoiveet())));
    this.applicationAdditionalDataDTO = toAdditionalData(valintapisteet);
    valintaperusteet
        .getRight()
        .forEach(
            v -> {
              if (v.getSyotettavissaKaikille() != null && v.getSyotettavissaKaikille()) {
                this.hakukohteidenOsallistumistiedot.compute(
                    valintaperusteet.getLeft(),
                    (oid, h) -> {
                      if (h == null) {
                        return new HakukohteenOsallistumistiedotDTO(v);
                      } else {
                        return h.paivitaValintaperusteidenTiedolla(v);
                      }
                    });
              }
              this.applicationAdditionalDataDTO
                  .getAdditionalData()
                  .putIfAbsent(v.getTunniste(), "");
              this.applicationAdditionalDataDTO
                  .getAdditionalData()
                  .putIfAbsent(
                      v.getOsallistuminenTunniste(),
                      v.getVaatiiOsallistumisen()
                          ? Osallistuminen.MERKITSEMATTA.toString()
                          : Osallistuminen.EI_VAADITA.toString());
            });
  }

  public HakukohteenOsallistumistiedotDTO.KokeenOsallistumistietoDTO osallistumistieto(
      String hakukohdeOid, String koetunniste) {
    if (!this.hakukohteidenOsallistumistiedot.containsKey(hakukohdeOid)) {
      return null;
    }
    return this.hakukohteidenOsallistumistiedot.get(hakukohdeOid).osallistumistieto(koetunniste);
  }
}
