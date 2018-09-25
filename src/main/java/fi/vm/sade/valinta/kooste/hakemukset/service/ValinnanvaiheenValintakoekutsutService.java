package fi.vm.sade.valinta.kooste.hakemukset.service;


import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import fi.vm.sade.service.valintaperusteet.dto.HakukohdeJaValintaperusteDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteDTO;
import fi.vm.sade.valinta.kooste.external.resource.ataru.AtaruAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.KoodistoCachedAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.dto.Koodi;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaValintakoeAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.hakemukset.dto.HakemusDTO;
import fi.vm.sade.valinta.kooste.hakemukset.dto.HakukohdeDTO;
import fi.vm.sade.valinta.kooste.pistesyotto.service.HakukohdeOIDAuthorityCheck;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valinta.kooste.util.KieliUtil;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeOsallistuminenDTO;
import fi.vm.sade.valintalaskenta.domain.valintakoe.Osallistuminen;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import rx.Observable;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ValinnanvaiheenValintakoekutsutService {
    private static final Logger LOG = LoggerFactory.getLogger(ValinnanvaiheenValintakoekutsutService.class);

    private TarjontaAsyncResource tarjontaAsyncResource;
    private ApplicationAsyncResource applicationAsyncResource;
    private AtaruAsyncResource ataruAsyncResource;
    private ValintaperusteetAsyncResource valintaperusteetAsyncResource;
    private ValintalaskentaValintakoeAsyncResource valintalaskentaValintakoeAsyncResource;
    private KoodistoCachedAsyncResource koodistoCachedAsyncResource;

    @Autowired
    public ValinnanvaiheenValintakoekutsutService(
            TarjontaAsyncResource tarjontaAsyncResource,
            ApplicationAsyncResource applicationAsyncResource,
            AtaruAsyncResource ataruAsyncResource,
            ValintaperusteetAsyncResource valintaperusteetAsyncResource,
            ValintalaskentaValintakoeAsyncResource valintalaskentaValintakoeAsyncResource,
            KoodistoCachedAsyncResource koodistoCachedAsyncResource) {
        this.tarjontaAsyncResource = tarjontaAsyncResource;
        this.applicationAsyncResource = applicationAsyncResource;
        this.ataruAsyncResource = ataruAsyncResource;
        this.valintalaskentaValintakoeAsyncResource = valintalaskentaValintakoeAsyncResource;
        this.valintaperusteetAsyncResource = valintaperusteetAsyncResource;
        this.koodistoCachedAsyncResource = koodistoCachedAsyncResource;
    }

    public void hae(String valinnanvaiheOid, String hakuOid, final HakukohdeOIDAuthorityCheck authorityCheck, Consumer<Collection<HakemusDTO>> successHandler, Consumer<Throwable> exceptionHandler) {
        valintaperusteetAsyncResource.haeHakukohteetValinnanvaiheelle(valinnanvaiheOid)
            .subscribe(
                hakukohdeOidit -> {
                    LOG.info("Löydettiin {} hakukohdetta", hakukohdeOidit.size());
                    if (hakukohdeOidit.isEmpty()) {
                        exceptionHandler.accept(new ValinnanvaiheelleEiLoydyValintaryhmiaException(
                            String.format("Ei löytynyt yhtään hakukohdeoidia valintaryhmien perusteella haun %s valinnanvaiheelle %s", hakuOid, valinnanvaiheOid)));
                    } else {
                        tarjontaAsyncResource.haeHaku(hakuOid).flatMap(haku -> {
                            if (haku.getAtaruLomakeAvain() == null) {
                                return applicationAsyncResource.getApplicationsByOidsWithPOST(hakuOid, hakukohdeOidit);
                            } else {
                                return Observable.from(hakukohdeOidit)
                                        .flatMap(hakukohdeOid -> ataruAsyncResource.getApplicationsByHakukohde(hakukohdeOid)
                                                .flatMap(Observable::from))
                                        .distinct(HakemusWrapper::getOid)
                                        .toList();
                            }
                        }).subscribe(
                                hakemukset -> handleApplicationsResponse(hakemukset, authorityCheck, successHandler, exceptionHandler, hakukohdeOidit),
                                exceptionHandler::accept
                        );
                    }
                },
                e -> LOG.error("Ongelma haettaessa valintaryhmien perusteella hakukohteita valinnanvaiheelle " + valinnanvaiheOid, e));
    }

    private void handleApplicationsResponse(List<HakemusWrapper> hakemukset, HakukohdeOIDAuthorityCheck authorityCheck, Consumer<Collection<HakemusDTO>> successHandler, Consumer<Throwable> exceptionHandler, Set<String> hakukohdeOidit) {
        if (hakemukset == null) {
            exceptionHandler.accept(new RuntimeException("null response from applicationAsyncResource"));
            return;
        }
        LOG.info("Löydettiin {} hakemusta", hakemukset.size());
        Set<String> hakutoiveet = Sets.intersection(collectHakutoiveOidsOf(hakemukset), hakukohdeOidit).stream().filter(authorityCheck).collect(Collectors.toSet());
        LOG.info("Hakutoivejoukon koko: {} hakutoivetta", hakutoiveet.size());
        if (hakutoiveet.isEmpty()) {
            successHandler.accept(new ArrayList<>());
            return;
        }

        Observable<List<HakukohdeJaValintaperusteDTO>> avaimetHakutoiveille = valintaperusteetAsyncResource.findAvaimet(hakutoiveet);
        Observable<List<ValintakoeOsallistuminenDTO>> valintakoeOsallistumisetHakutoiveille = valintalaskentaValintakoeAsyncResource.haeHakutoiveille(hakutoiveet);
        Observable.combineLatest(avaimetHakutoiveille, valintakoeOsallistumisetHakutoiveille, (hakutoiveidenAvaimet, hakutoiveidenValintakoeOsallistumiset) -> {
            LOG.info("hakutoiveidenAvaimet {}", hakutoiveidenAvaimet);
            Map<String, HakukohdeJaValintaperusteDTO> valintakoeDTOMap = hakutoiveidenAvaimet.stream().collect(Collectors.toMap(HakukohdeJaValintaperusteDTO::getHakukohdeOid, hh -> hh));
            Map<String, List<ValintakoeOsallistuminenDTO>> osallistuminenDTOMap = hakutoiveidenValintakoeOsallistumiset.stream().collect(Collectors.toMap(ValintakoeOsallistuminenDTO::getHakemusOid, Arrays::asList, (h0, h1) -> Lists.newArrayList(Iterables.concat(h0, h1))));

            return hakemukset
                    .stream()
                    .map(hakemus -> {
                        assert hakemus != null;
                        assert hakemus.getOid() != null;

                        Set<Pair<String, String>> kutsututValintakokeet = new HashSet<>();
                        List<ValintakoeOsallistuminenDTO> osallistumisetHakemukselle = osallistuminenDTOMap.get(hakemus.getOid());
                        if (osallistumisetHakemukselle != null) {
                            kutsututValintakokeet.addAll(osallistumisetHakemukselle
                                    .stream()
                                    .filter(x -> x != null && x.getHakutoiveet() != null)                   // ValintakoeosallistuminenDTO
                                    .flatMap(x -> x.getHakutoiveet().stream().filter(h -> authorityCheck.test(h.getHakukohdeOid())))
                                    .filter(x -> x != null && x.getValinnanVaiheet() != null)
                                    .map(x -> Pair.of(x.getHakukohdeOid(), x.getValinnanVaiheet()))         // HakutoiveDTO
                                    .flatMap(x -> x.getRight().stream()                                     // <Hakutoive, ValintakoeValinnanVaihe>
                                            .flatMap(y -> y.getValintakokeet().stream())
                                            .filter(z -> z != null)
                                            .map(z -> Pair.of(x.getLeft(), z)))
                                    .filter(x -> x != null && x.getRight() != null)
                                    .filter(x -> x.getRight().getOsallistuminenTulos() != null)
                                    .filter(x -> x.getRight().getOsallistuminenTulos().getOsallistuminen() == Osallistuminen.OSALLISTUU)
                                    .map(x -> Pair.of(x.getLeft(), x.getRight().getValintakoeTunniste()))   // <Hakutoive, ValintakoeDTO>
                                    .collect(Collectors.toSet()));                                          // <Hakutoive, ValintakoeTunniste>
                        }

                        final List<String> hakutoiveOids = hakemus.getHakutoiveOids().stream().filter(authorityCheck).collect(Collectors.toList());
                        final List<HakukohdeJaValintaperusteDTO> hakukohteet = hakutoiveOids
                                .stream()
                                .map(x -> valintakoeDTOMap.get(x))
                                .filter(x -> x != null)
                                .map(hakukohdeJaValintaperuste -> {
                                    final List<ValintaperusteDTO> valintaperusteet = hakukohdeJaValintaperuste.getValintaperusteDTO();
                                    final List<ValintaperusteDTO> filteredValintaperusteet = valintaperusteet.stream().filter(valintaperuste -> {
                                        if (valintaperuste.getSyotettavissaKaikille()) {
                                            return true;
                                        } else
                                            return kutsututValintakokeet.contains(Pair.of(hakukohdeJaValintaperuste.getHakukohdeOid(), valintaperuste.getTunniste()));
                                    }).collect(Collectors.toList());
                                    return new HakukohdeJaValintaperusteDTO(hakukohdeJaValintaperuste.getHakukohdeOid(), filteredValintaperusteet);
                                })
                                .filter(hakukohdeJaValintaperusteDTO -> hakukohdeJaValintaperusteDTO.getValintaperusteDTO() != null && !hakukohdeJaValintaperusteDTO.getValintaperusteDTO().isEmpty())
                                .collect(Collectors.toList());
                        return hakemusToHakemusDTO(hakemus, hakukohteet);
                    })
                    .filter(x -> !x.getHakukohteet().isEmpty())
                    .collect(Collectors.toList());
        }).subscribe(
            hakemusDTOs -> {
                LOG.info("Palautetaan {} hakemusta", hakemusDTOs.size());
                successHandler.accept(hakemusDTOs);
            },
            exceptionHandler::accept);
    }

    private Set<String> collectHakutoiveOidsOf(List<HakemusWrapper> hakemukset) {
        return hakemukset.stream().flatMap(h -> h.getHakutoiveOids().stream()).collect(Collectors.toSet());
    }

    private HakemusDTO hakemusToHakemusDTO(HakemusWrapper hakemus, List<HakukohdeJaValintaperusteDTO> valintaperusteDTOs) {
        Map<String, Koodi> postCodes;
        try {
            postCodes = koodistoCachedAsyncResource.haeKoodisto(KoodistoCachedAsyncResource.POSTI);
        } catch (Exception e) {
            LOG.warn("KoodistoCachedAsyncResource threw exception while loading");
            postCodes = new HashMap<>();
        }
        HakemusDTO hakemusDTO = new HakemusDTO();
        hakemusDTO.setHakemusOid(hakemus.getOid());
        hakemusDTO.setHenkiloOid(hakemus.getPersonOid());
        hakemusDTO.setEtunimet(hakemus.getEtunimet());
        hakemusDTO.setSukunimi(hakemus.getSukunimi());
        hakemusDTO.setKutsumanimi(hakemus.getKutsumanimi());
        hakemusDTO.setSahkoposti(hakemus.getSahkopostiOsoite());
        hakemusDTO.setKatuosoite(hakemus.getSuomalainenLahiosoite());
        String postinumero = hakemus.getSuomalainenPostinumero();
        hakemusDTO.setPostinumero(postinumero);
        hakemusDTO.setPostitoimipaikka(KoodistoCachedAsyncResource.haeKoodistaArvo(
                postCodes.get(postinumero),
                KieliUtil.SUOMI,
                postinumero));
        hakemusDTO.setHakukohteet(valintaperusteDTOs.stream().map(vp -> new HakukohdeDTO(vp.getHakukohdeOid(), vp.getValintaperusteDTO().stream().map(vv -> new fi.vm.sade.valinta.kooste.hakemukset.dto.ValintakoeDTO(vv.getTunniste())).collect(Collectors.toList()))).collect(Collectors.toList()));
        return hakemusDTO;
    }

    public class ValinnanvaiheelleEiLoydyValintaryhmiaException extends RuntimeException {
        public ValinnanvaiheelleEiLoydyValintaryhmiaException(String message) {
            super(message);
        }
    }
}
