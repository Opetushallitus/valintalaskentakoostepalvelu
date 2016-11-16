package fi.vm.sade.valinta.kooste.pistesyotto.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteDTO;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.*;
import fi.vm.sade.valinta.kooste.pistesyotto.excel.PistesyottoExcel;
import fi.vm.sade.valinta.kooste.pistesyotto.service.AbstractPistesyottoKoosteService;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.*;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeDTO;
import fi.vm.sade.valintalaskenta.domain.valintakoe.Osallistuminen;
import org.apache.commons.lang3.tuple.Pair;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
public class HakukohteenOsallistumistiedotDTO {
    public final Map<String, KokeenOsallistumistietoDTO> valintakokeidenOsallistumistiedot;

    public HakukohteenOsallistumistiedotDTO(HakukohteenOsallistumistiedotDTO old, ValintaperusteDTO v) {
        this.valintakokeidenOsallistumistiedot = old == null ?
                new HashMap<>() :
                new HashMap<>(old.valintakokeidenOsallistumistiedot);
        this.valintakokeidenOsallistumistiedot.compute(v.getTunniste(), (tunniste, koe) -> {
            if (koe == null || koe.osallistumistieto == Osallistumistieto.EI_KUTSUTTU) {
                return new KokeenOsallistumistietoDTO(v);
            }
            return koe;
        });
    }

    @JsonCreator
    public HakukohteenOsallistumistiedotDTO(
            @JsonProperty("valintakokeidenOsallistumistiedot")
                    Map<String, KokeenOsallistumistietoDTO> valintakokeidenOsallistumistiedot) {
        this.valintakokeidenOsallistumistiedot = valintakokeidenOsallistumistiedot;
    }

    public HakukohteenOsallistumistiedotDTO(HakutoiveDTO hakutoiveDTO,
                                            Map<String, Pair<Suoritus, Arvosana>> kielikoetulokset,
                                            String hakemusOid) {
        this.valintakokeidenOsallistumistiedot = hakutoiveDTO.getValinnanVaiheet().stream()
                .flatMap(v -> v.getValintakokeet().stream())
                .collect(Collectors.toMap(
                        k -> k.getValintakoeTunniste(),
                        k -> new KokeenOsallistumistietoDTO(k, kielikoetulokset.get(k.getValintakoeTunniste()), hakemusOid)
                ));
    }

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
    public static class KokeenOsallistumistietoDTO implements Serializable {
        public final Osallistumistieto osallistumistieto;
        public final Optional<String> lahdeHakemusOid;
        public final Optional<String> lahdeMyontajaOid;

        @JsonCreator
        public KokeenOsallistumistietoDTO(
                @JsonProperty("osallistumistieto") Osallistumistieto osallistumistieto,
                @JsonProperty("lahdeHakemusOid") Optional<String> lahdeHakemusOid,
                @JsonProperty("lahdeMyontajaOid") Optional<String> lahdeMyontajaOid) {
            if (osallistumistieto == Osallistumistieto.EI_KUTSUTTU && (lahdeHakemusOid.isPresent() || lahdeMyontajaOid.isPresent())) {
                throw new IllegalArgumentException(String.format(
                        "Jos ei kutsuttu, lähdehakemusta ja -myöntäjää ei tule asettaa: %s, %s, %s",
                        osallistumistieto, lahdeHakemusOid, lahdeMyontajaOid
                ));
            }
            this.osallistumistieto = osallistumistieto;
            this.lahdeHakemusOid = lahdeHakemusOid;
            this.lahdeMyontajaOid = lahdeMyontajaOid;
        }

        public KokeenOsallistumistietoDTO(ValintakoeDTO koe, Pair<Suoritus, Arvosana> kielikoetulos, String hakemusOid) {
            switch (koe.getOsallistuminenTulos().getOsallistuminen()) {
                case OSALLISTUU:
                case EI_VAADITA:
                    this.osallistumistieto = Osallistumistieto.OSALLISTUI;
                    this.lahdeHakemusOid = Optional.empty();
                    this.lahdeMyontajaOid = Optional.empty();
                    break;
                case EI_OSALLISTU:
                    if (kielikoetulos != null) {
                        String lahdeHakemusOid = kielikoetulos.getLeft().getMyontaja();
                        if (hakemusOid.equals(lahdeHakemusOid)) {
                            this.osallistumistieto = Osallistumistieto.TOISESSA_HAKUTOIVEESSA;
                            this.lahdeHakemusOid = Optional.empty();
                        } else {
                            this.osallistumistieto = Osallistumistieto.TOISELLA_HAKEMUKSELLA;
                            this.lahdeHakemusOid = Optional.of(lahdeHakemusOid);
                        }
                        this.lahdeMyontajaOid = Optional.of(kielikoetulos.getRight().getSource());
                    } else {
                        this.osallistumistieto = Osallistumistieto.EI_KUTSUTTU;
                        this.lahdeHakemusOid = Optional.empty();
                        this.lahdeMyontajaOid = Optional.empty();
                    }
                    break;
                default:
                    throw new RuntimeException(String.format(
                            "Odottamaton koeosallistumisen tila %s", koe.getOsallistuminenTulos().getOsallistuminen()
                    ));
            }
        }

        public KokeenOsallistumistietoDTO(ValintaperusteDTO v) {
            this.osallistumistieto = Osallistumistieto.OSALLISTUI;
            this.lahdeHakemusOid = Optional.empty();
            this.lahdeMyontajaOid = Optional.empty();
        }
    }
}
