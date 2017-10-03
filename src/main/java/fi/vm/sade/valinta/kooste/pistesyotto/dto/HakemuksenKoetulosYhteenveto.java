package fi.vm.sade.valinta.kooste.pistesyotto.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteDTO;
import fi.vm.sade.service.valintaperusteet.dto.model.Osallistuminen;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.ApplicationAdditionalDataDTO;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.dto.ParametritDTO;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Arvosana;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Oppija;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Suoritus;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.SuoritusJaArvosanat;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.SuoritusJaArvosanatWrapper;
import fi.vm.sade.valinta.kooste.external.resource.valintapiste.dto.Valintapisteet;
import fi.vm.sade.valinta.kooste.util.OppijaToAvainArvoDTOConverter;
import fi.vm.sade.valinta.kooste.util.sure.AmmatillisenKielikoetuloksetSurestaConverter.SureHyvaksyttyArvosana;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.HakutoiveDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeOsallistuminenDTO;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static fi.vm.sade.valinta.kooste.pistesyotto.service.AbstractPistesyottoKoosteService.KIELIKOE_KEY_PREFIX;
import static fi.vm.sade.valinta.kooste.util.sure.AmmatillisenKielikoetuloksetSurestaConverter.SureHyvaksyttyArvosana.hyvaksytty;

@JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
public class HakemuksenKoetulosYhteenveto {
    @JsonProperty
    public  ApplicationAdditionalDataDTO applicationAdditionalDataDTO;
    @JsonProperty
    private  Map<String, HakukohteenOsallistumistiedotDTO> hakukohteidenOsallistumistiedot;

    private  String hakemusOid;

    @JsonCreator
    public HakemuksenKoetulosYhteenveto(
            @JsonProperty("applicationAdditionalDataDTO")
                    ApplicationAdditionalDataDTO applicationAdditionalDataDTO,
            @JsonProperty("hakukohteidenOsallistumistiedot")
                    Map<String, HakukohteenOsallistumistiedotDTO> hakukohteidenOsallistumistiedot) {
        this.applicationAdditionalDataDTO = applicationAdditionalDataDTO;
        this.hakukohteidenOsallistumistiedot = hakukohteidenOsallistumistiedot;
        this.hakemusOid = applicationAdditionalDataDTO.getOid();
    }

    private class TaltaHakemukseltaHyvaksyttyTaiUusinHyvaksyttyTaiTaltaHakemukselta implements Comparator<SuoritusJaArvosanat> {

        private final Predicate<Arvosana> tamaKieli;

        private TaltaHakemukseltaHyvaksyttyTaiUusinHyvaksyttyTaiTaltaHakemukselta(String kieli) {
            this.tamaKieli = isKieli(kieli);
        }

        @Override
        public int compare(SuoritusJaArvosanat s1, SuoritusJaArvosanat s2) {
            if (s1.getArvosanat().stream().noneMatch(tamaKieli)) {
                return 1;
            }
            if (s2.getArvosanat().stream().noneMatch(tamaKieli)) {
                return -1;
            }
            Arvosana a1 = s1.getArvosanat().stream().filter(tamaKieli).findAny().get();
            Arvosana a2 = s2.getArvosanat().stream().filter(tamaKieli).findAny().get();
            if (taltaHakemukselta(s1) && hyvaksytty(s1)) {
                return -1;
            }
            if (taltaHakemukselta(s2) && hyvaksytty(s2)) {
                return 1;
            }
            if (hyvaksytty(s1) && hyvaksytty(s2)) {
                return Arvosana.NEWEST_FIRST.compare(a1, a2);
            }
            if (hyvaksytty(s1)) {
                return -1;
            }
            if (hyvaksytty(s2)) {
                return 1;
            }
            if (taltaHakemukselta(s1)) {
                return -1;
            }
            if (taltaHakemukselta(s2)) {
                return 1;
            }
            return 0;
        }

        private boolean hyvaksytty(SuoritusJaArvosanat s) {
            return s.getArvosanat().stream().anyMatch(a -> isHyvaksytty(a) && tamaKieli.test(a));
        }
    }

    public static ApplicationAdditionalDataDTO toAdditionalData(Valintapisteet v) {
        Map<String, String> immutableAdditionalData = v.getPisteet().stream().flatMap(p ->
                Stream.of(
                        Pair.of(p.getTunniste(), p.getArvo()),
                        Pair.of(withOsallistuminenSuffix(p.getTunniste()), p.getOsallistuminen().toString())
                )
        ).collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));


        return new ApplicationAdditionalDataDTO(
                v.getHakemusOID(),
                v.getOppijaOID(),
                v.getEtunimet(),
                v.getSukunimi(),
                new HashMap<>(immutableAdditionalData)
        );
    };
    private static String withOsallistuminenSuffix(String tunniste) {
        return new StringBuilder(tunniste).append("-OSALLISTUMINEN").toString();
    }

    public HakemuksenKoetulosYhteenveto(Valintapisteet valintapisteet,
                                        Pair<String, List<ValintaperusteDTO>> valintaperusteet,
                                        ValintakoeOsallistuminenDTO kokeet,
                                        Oppija oppija,
                                        ParametritDTO ohjausparametrit) {
        this.hakemusOid = valintapisteet.getHakemusOID();
        Map<String, Pair<Suoritus, Arvosana>> naytettavatKielikoetulokset = createKielikoetulokset(oppija, ohjausparametrit, hakemusOid);
        this.hakukohteidenOsallistumistiedot = (kokeet == null ? Stream.<HakutoiveDTO>empty() : kokeet.getHakutoiveet().stream())
                .collect(Collectors.toMap(
                        HakutoiveDTO::getHakukohdeOid,
                        h -> new HakukohteenOsallistumistiedotDTO(h, naytettavatKielikoetulokset, hakemusOid, kokeet.getHakutoiveet())
                ));
        this.applicationAdditionalDataDTO = toAdditionalData(valintapisteet);
        naytettavatKielikoetulokset.forEach((koetunniste, tulos) -> {
            SureHyvaksyttyArvosana arvosana = SureHyvaksyttyArvosana.valueOf(tulos.getRight().getArvio().getArvosana());
            switch (arvosana) {
                case hyvaksytty:
                    this.applicationAdditionalDataDTO.getAdditionalData().put(koetunniste, "true");
                    this.applicationAdditionalDataDTO.getAdditionalData().put(
                            withOsallistuminenSuffix(koetunniste),
                            hakemusOid.equals(tulos.getLeft().getMyontaja()) ?
                                    Osallistuminen.OSALLISTUI.toString() :
                                    Osallistuminen.MERKITSEMATTA.toString()
                    );
                    break;
                case hylatty:
                    this.applicationAdditionalDataDTO.getAdditionalData().put(koetunniste, "false");
                    this.applicationAdditionalDataDTO.getAdditionalData().put(withOsallistuminenSuffix(koetunniste), Osallistuminen.OSALLISTUI.toString());
                    break;
                case ei_osallistunut:
                    this.applicationAdditionalDataDTO.getAdditionalData().put(koetunniste, "");
                    this.applicationAdditionalDataDTO.getAdditionalData().put(withOsallistuminenSuffix(koetunniste), Osallistuminen.EI_OSALLISTUNUT.toString());
                    break;
                default:
                    throw new RuntimeException(String.format(
                            "Odottamaton kielikokeen arvostelu %s arvosanassa %s", arvosana, tulos.getRight().getId()
                    ));
            }
        });
        valintaperusteet.getRight().forEach(v -> {
            if (v.getSyotettavissaKaikille() != null && v.getSyotettavissaKaikille()) {
                this.hakukohteidenOsallistumistiedot.compute(valintaperusteet.getLeft(), (oid, h) -> {
                    if (h == null) {
                        return new HakukohteenOsallistumistiedotDTO(v);
                    } else {
                        return h.paivitaValintaperusteidenTiedolla(v);
                    }
                });
            }
            this.applicationAdditionalDataDTO.getAdditionalData().putIfAbsent(v.getTunniste(), "");
            this.applicationAdditionalDataDTO.getAdditionalData().putIfAbsent(
                    v.getOsallistuminenTunniste(),
                    v.getVaatiiOsallistumisen() ?
                            Osallistuminen.MERKITSEMATTA.toString() :
                            Osallistuminen.EI_VAADITA.toString()
            );
        });
    }

    private Map<String, Pair<Suoritus, Arvosana>> createKielikoetulokset(Oppija oppija, ParametritDTO ohjausparametrit, String hakemusOid) {
        if (oppija != null) {
            List<SuoritusJaArvosanat> sal = OppijaToAvainArvoDTOConverter.removeLaskennanAlkamisenJalkeenMyonnetytArvosanat(
                    oppija.getSuoritukset().stream().filter(SuoritusJaArvosanatWrapper::isAmmatillisenKielikoe),
                    ohjausparametrit
            ).collect(Collectors.toList());
            Stream<String> kieletJoilleNaytettaviaTuloksia = sal.stream()
                    .flatMap(sa -> sa.getArvosanat().stream()
                            .filter(a -> isHyvaksytty(a) || taltaHakemukselta(sa))
                            .map(a -> a.getLisatieto().toLowerCase()))
                    .distinct();
            return kieletJoilleNaytettaviaTuloksia.collect(Collectors.toMap(
                    kieli -> KIELIKOE_KEY_PREFIX + kieli,
                    kieli -> {
                        SuoritusJaArvosanat sa = sal.stream()
                                .sorted(new TaltaHakemukseltaHyvaksyttyTaiUusinHyvaksyttyTaiTaltaHakemukselta(kieli))
                                .findFirst().get();
                        return Pair.of(sa.getSuoritus(), sa.getArvosanat().stream().filter(isKieli(kieli)).findAny().get());
                    }
            ));
        } else {
            return new HashMap<>();
        }
    }

    private boolean taltaHakemukselta(SuoritusJaArvosanat s) {
        return this.hakemusOid.equals(s.getSuoritus().getMyontaja());
    }

    private boolean isHyvaksytty(Arvosana arvosana) {
        return hyvaksytty.name().equals(arvosana.getArvio().getArvosana());
    }

    private Predicate<Arvosana> isKieli(String kieli) {
        return arvosana -> kieli.equals(arvosana.getLisatieto().toLowerCase());
    }

    public HakukohteenOsallistumistiedotDTO.KokeenOsallistumistietoDTO osallistumistieto(String hakukohdeOid,
                                                                                         String koetunniste) {
        if (!this.hakukohteidenOsallistumistiedot.containsKey(hakukohdeOid)) {
            return null;
        }
        return this.hakukohteidenOsallistumistiedot.get(hakukohdeOid).osallistumistieto(koetunniste);
    }
}
