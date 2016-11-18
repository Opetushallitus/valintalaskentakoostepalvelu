package fi.vm.sade.valinta.kooste.pistesyotto.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteDTO;
import fi.vm.sade.service.valintaperusteet.dto.model.Osallistuminen;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.ApplicationAdditionalDataDTO;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.dto.ParametritDTO;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.*;
import fi.vm.sade.valinta.kooste.pistesyotto.excel.PistesyottoExcel;
import fi.vm.sade.valinta.kooste.pistesyotto.service.AbstractPistesyottoKoosteService;
import fi.vm.sade.valinta.kooste.util.OppijaToAvainArvoDTOConverter;
import fi.vm.sade.valinta.kooste.util.sure.AmmatillisenKielikoetuloksetSurestaConverter;
import fi.vm.sade.valinta.kooste.util.sure.AmmatillisenKielikoetuloksetSurestaConverter.SureHyvaksyttyArvosana;
import fi.vm.sade.valintalaskenta.domain.dto.OsallistuminenDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.HakutoiveDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeOsallistuminenDTO;
import org.apache.commons.lang3.tuple.Pair;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.SuoritusJaArvosanatWrapper.ARVOSANA_PVM_FORMATTER;
import static fi.vm.sade.valinta.kooste.pistesyotto.service.AbstractPistesyottoKoosteService.KIELIKOE_KEY_PREFIX;
import static fi.vm.sade.valinta.kooste.util.sure.AmmatillisenKielikoetuloksetSurestaConverter.SureHyvaksyttyArvosana.hyvaksytty;

@JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
public class PistetietoDTO {
    @JsonProperty
    public final ApplicationAdditionalDataDTO applicationAdditionalDataDTO;
    @JsonProperty
    private final Map<String, HakukohteenOsallistumistiedotDTO> hakukohteidenOsallistumistiedot;

    @JsonCreator
    public PistetietoDTO(
            @JsonProperty("applicationAdditionalDataDTO")
                    ApplicationAdditionalDataDTO applicationAdditionalDataDTO,
            @JsonProperty("hakukohteidenOsallistumistiedot")
                    Map<String, HakukohteenOsallistumistiedotDTO> hakukohteidenOsallistumistiedot) {
        this.applicationAdditionalDataDTO = applicationAdditionalDataDTO;
        this.hakukohteidenOsallistumistiedot = hakukohteidenOsallistumistiedot;
    }

    public PistetietoDTO(ApplicationAdditionalDataDTO additionalData,
                         Pair<String, List<ValintaperusteDTO>> valintaperusteet,
                         ValintakoeOsallistuminenDTO kokeet,
                         Oppija oppija,
                         ParametritDTO ohjausparametrit) {
        String hakemusOid = additionalData.getOid();
        Map<String, Pair<Suoritus, Arvosana>> kielikoetulokset = new HashMap<>();
        if (oppija != null) {
            OppijaToAvainArvoDTOConverter.removeLaskennanAlkamisenJalkeenMyonnetytArvosanat(
                    oppija.getSuoritukset().stream().filter(SuoritusJaArvosanatWrapper::isAmmatillisenKielikoe),
                    ohjausparametrit
            ).forEach(s -> {
                s.getArvosanat().forEach(a -> {
                    kielikoetulokset.compute(KIELIKOE_KEY_PREFIX + a.getLisatieto().toLowerCase(), (k, p) -> {
                        if (hakemusOid.equals(s.getSuoritus().getMyontaja())) {
                            if (p == null || !hakemusOid.equals(p.getLeft().getMyontaja()) || a.isMyonnettyAfter(p.getRight())) {
                                return Pair.of(s.getSuoritus(), a);
                            }
                        } else if (hyvaksytty.name().equals(a.getArvio().getArvosana())) {
                            if (p == null || (!hakemusOid.equals(p.getLeft().getMyontaja()) && a.isMyonnettyAfter(p.getRight()))) {
                                return Pair.of(s.getSuoritus(), a);
                            }
                        }
                        return p;
                    });
                });
            });
        }
        this.hakukohteidenOsallistumistiedot = (kokeet == null ? Stream.<HakutoiveDTO>empty() : kokeet.getHakutoiveet().stream())
                .collect(Collectors.toMap(
                        h -> h.getHakukohdeOid(),
                        h -> new HakukohteenOsallistumistiedotDTO(h, kielikoetulokset, hakemusOid)
                ));
        this.applicationAdditionalDataDTO = additionalData;
        kielikoetulokset.forEach((koetunniste, tulos) -> {
            SureHyvaksyttyArvosana arvosana = SureHyvaksyttyArvosana.valueOf(tulos.getRight().getArvio().getArvosana());
            switch (arvosana) {
                case hyvaksytty:
                    additionalData.getAdditionalData().put(koetunniste, "true");
                    additionalData.getAdditionalData().put(koetunniste + "-OSALLISTUMINEN", Osallistuminen.OSALLISTUI.toString());
                    break;
                case hylatty:
                    additionalData.getAdditionalData().put(koetunniste, "false");
                    additionalData.getAdditionalData().put(koetunniste + "-OSALLISTUMINEN", Osallistuminen.OSALLISTUI.toString());
                    break;
                case ei_osallistunut:
                    additionalData.getAdditionalData().put(koetunniste, "");
                    additionalData.getAdditionalData().put(koetunniste + "-OSALLISTUMINEN", Osallistuminen.EI_OSALLISTUNUT.toString());
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
            additionalData.getAdditionalData().putIfAbsent(v.getTunniste(), "");
            additionalData.getAdditionalData().putIfAbsent(
                    v.getOsallistuminenTunniste(),
                    v.getVaatiiOsallistumisen() ?
                            Osallistuminen.MERKITSEMATTA.toString() :
                            Osallistuminen.EI_VAADITA.toString()
            );
        });
    }

    public HakukohteenOsallistumistiedotDTO.KokeenOsallistumistietoDTO osallistumistieto(String hakukohdeOid,
                                                                                         String koetunniste) {
        if (!this.hakukohteidenOsallistumistiedot.containsKey(hakukohdeOid)) {
            return null;
        }
        return this.hakukohteidenOsallistumistiedot.get(hakukohdeOid).osallistumistieto(koetunniste);
    }
}
