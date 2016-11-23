package fi.vm.sade.valinta.kooste.pistesyotto.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteDTO;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Arvosana;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Suoritus;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.HakutoiveDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeDTO;
import fi.vm.sade.valintalaskenta.domain.valintakoe.Osallistuminen;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.tuple.Pair;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
public class HakukohteenOsallistumistiedotDTO {
    @JsonProperty
    private final Map<String, KokeenOsallistumistietoDTO> valintakokeidenOsallistumistiedot;

    @JsonCreator
    public HakukohteenOsallistumistiedotDTO(
            @JsonProperty("valintakokeidenOsallistumistiedot")
                    Map<String, KokeenOsallistumistietoDTO> valintakokeidenOsallistumistiedot) {
        this.valintakokeidenOsallistumistiedot = valintakokeidenOsallistumistiedot;
    }

    public HakukohteenOsallistumistiedotDTO(HakutoiveDTO hakutoiveDTO,
                                            Map<String, Pair<Suoritus, Arvosana>> kielikoetulokset,
                                            String hakemusOid, List<HakutoiveDTO> kaikkiOsallistumisenHakutoiveet) {
        this.valintakokeidenOsallistumistiedot = hakutoiveDTO.getValinnanVaiheet().stream()
                .flatMap(v -> v.getValintakokeet().stream())
                .collect(Collectors.toMap(
                        k -> k.getValintakoeTunniste(),
                        k -> new KokeenOsallistumistietoDTO(k, kielikoetulokset.get(k.getValintakoeTunniste()), hakemusOid, hakutoiveDTO.getHakukohdeOid(), kaikkiOsallistumisenHakutoiveet)
                ));
    }

    public HakukohteenOsallistumistiedotDTO(ValintaperusteDTO v) {
        this.valintakokeidenOsallistumistiedot = new HashMap<>();
        this.valintakokeidenOsallistumistiedot.put(v.getTunniste(), new KokeenOsallistumistietoDTO(v));
    }

    public HakukohteenOsallistumistiedotDTO paivitaValintaperusteidenTiedolla(ValintaperusteDTO v) {
        Map<String, KokeenOsallistumistietoDTO> m = new HashMap<>(this.valintakokeidenOsallistumistiedot);
        m.compute(v.getTunniste(), (tunniste, koe) -> {
            if (koe == null) {
                return new KokeenOsallistumistietoDTO(v);
            } else {
                return koe.paivitaValintaperusteidenTiedolla(v);
            }
        });
        return new HakukohteenOsallistumistiedotDTO(m);
    }

    public KokeenOsallistumistietoDTO osallistumistieto(String koetunniste) {
        return this.valintakokeidenOsallistumistiedot.get(koetunniste);
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

        public KokeenOsallistumistietoDTO(ValintakoeDTO koe, Pair<Suoritus, Arvosana> kielikoetulos, String hakemusOid, String tamanHakutoiveenOid, List<HakutoiveDTO> kaikkiOsallistumisenHakutoiveet) {
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
                    } else if (sisaltaaOsallistumisenToisessaHakutoiveessa(kaikkiOsallistumisenHakutoiveet, tamanHakutoiveenOid, koe)) {
                        this.osallistumistieto = Osallistumistieto.TOISESSA_HAKUTOIVEESSA;
                        this.lahdeHakemusOid = Optional.empty();
                        this.lahdeMyontajaOid = Optional.empty();
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

        private boolean sisaltaaOsallistumisenToisessaHakutoiveessa(List<HakutoiveDTO> kaikkiOsallistumisenHakutoiveet, String tamanHakutoiveenOid, ValintakoeDTO tamaKoe) {
            return kaikkiOsallistumisenHakutoiveet.stream()
                .filter(toive -> !tamanHakutoiveenOid.equals(toive.getHakukohdeOid()))
                .flatMap(toive -> toive.getValinnanVaiheet().stream())
                .flatMap(vaihe -> vaihe.getValintakokeet().stream())
                .filter(koeEriToiveelta -> koeEriToiveelta.getValintakoeTunniste().equals(tamaKoe.getValintakoeTunniste()))
                .anyMatch(samaKoeEriToiveelta -> Osallistuminen.OSALLISTUU.equals(samaKoeEriToiveelta.getOsallistuminenTulos().getOsallistuminen()));
        }

        public KokeenOsallistumistietoDTO(ValintaperusteDTO v) {
            this.osallistumistieto = Boolean.TRUE.equals(v.getSyotettavissaKaikille()) ?
                    Osallistumistieto.OSALLISTUI :
                    Osallistumistieto.EI_KUTSUTTU;
            this.lahdeHakemusOid = Optional.empty();
            this.lahdeMyontajaOid = Optional.empty();
        }

        public KokeenOsallistumistietoDTO paivitaValintaperusteidenTiedolla(ValintaperusteDTO v) {
            if (Boolean.TRUE.equals(v.getSyotettavissaKaikille()) &&
                    this.osallistumistieto == Osallistumistieto.EI_KUTSUTTU) {
                return new KokeenOsallistumistietoDTO(v);
            } else {
                return this;
            }
        }

        @Override
        public String toString() {
            return ToStringBuilder.reflectionToString(this);
        }
    }
}
