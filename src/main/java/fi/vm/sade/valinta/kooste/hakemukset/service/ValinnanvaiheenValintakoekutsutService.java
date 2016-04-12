package fi.vm.sade.valinta.kooste.hakemukset.service;


import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import fi.vm.sade.service.valintaperusteet.dto.HakukohdeJaValintakoeDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintakoeDTO;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.KoodistoCachedAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.dto.Koodi;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaValintakoeAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.hakemukset.dto.HakemusDTO;
import fi.vm.sade.valinta.kooste.hakemukset.dto.HakukohdeDTO;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valinta.kooste.util.KieliUtil;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeOsallistuminenDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import rx.Observable;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ValinnanvaiheenValintakoekutsutService {
    private static final Logger LOG = LoggerFactory.getLogger(ValinnanvaiheenValintakoekutsutService.class);

    private ApplicationAsyncResource applicationAsyncResource;
    private ValintaperusteetAsyncResource valintaperusteetAsyncResource;
    private ValintalaskentaValintakoeAsyncResource valintalaskentaValintakoeAsyncResource;
    private KoodistoCachedAsyncResource koodistoCachedAsyncResource;

    @Autowired
    public ValinnanvaiheenValintakoekutsutService(
            ApplicationAsyncResource applicationAsyncResource,
            ValintaperusteetAsyncResource valintaperusteetAsyncResource,
            ValintalaskentaValintakoeAsyncResource valintalaskentaValintakoeAsyncResource,
            KoodistoCachedAsyncResource koodistoCachedAsyncResource) {
        this.applicationAsyncResource = applicationAsyncResource;
        this.valintalaskentaValintakoeAsyncResource = valintalaskentaValintakoeAsyncResource;
        this.valintaperusteetAsyncResource = valintaperusteetAsyncResource;
        this.koodistoCachedAsyncResource = koodistoCachedAsyncResource;
    }

    public void hae(String valinnanvaiheOid, String hakuOid, Consumer<Collection<HakemusDTO>> successHandler, Consumer<Throwable> exceptionHandler) {
        valintaperusteetAsyncResource.haeHakukohteetValinnanvaiheelle(valinnanvaiheOid)
                .flatMap(hakukohdeOidit -> {
                    LOG.info("Löydettiin {} hakukohdetta", hakukohdeOidit.size());
                    return Observable.from(Iterables.partition(hakukohdeOidit, 10))
                            .flatMap(hakukohdeOiditOsajoukko ->
                            {
                                LOG.info("Haetaan hakemukset hakukohteille {}", hakukohdeOiditOsajoukko);
                                return applicationAsyncResource.getApplicationsByOidsWithPOST(hakuOid, hakukohdeOiditOsajoukko);
                            })
                            .flatMap(x -> Observable.from(x));
                })
                .toList()
                .flatMap(hakemukset -> {
                    LOG.info("Löydettiin {} hakemusta", hakemukset.size());
                    if (hakemukset.size() > 0) {
                        LOG.info("Ensimmäisen hakemukset OID: {}", hakemukset.get(0).getOid());
                    }
                    Set<String> hakutoiveet = collect(hakemukset);
                    LOG.info("Hakutoivejoukon koko: {} hakutoivetta", hakutoiveet.size());
                    Observable<List<HakukohdeJaValintakoeDTO>> valintakokeetHakutoiveille =
                            valintaperusteetAsyncResource.haeValintakokeetHakutoiveille(hakutoiveet);
                    Observable<List<ValintakoeOsallistuminenDTO>> valintakoeOsallistumisetHakutoiveille = valintalaskentaValintakoeAsyncResource.haeHakutoiveille(hakutoiveet);
                    return Observable.combineLatest(valintakokeetHakutoiveille, valintakoeOsallistumisetHakutoiveille, (hakutoiveidenValintakokeet, hakutoiveidenValintakoeOsallistumiset) -> {
                        Map<String, HakukohdeJaValintakoeDTO> valintakoeDTOMap = hakutoiveidenValintakokeet.stream().collect(Collectors.toMap(HakukohdeJaValintakoeDTO::getHakukohdeOid, hh -> hh));
                        Map<String, List<ValintakoeOsallistuminenDTO>> osallistuminenDTOMap = hakutoiveidenValintakoeOsallistumiset.stream().collect(Collectors.toMap(ValintakoeOsallistuminenDTO::getHakemusOid, Arrays::asList, (h0, h1) -> Lists.newArrayList(Iterables.concat(h0,h1))));
                        return hakemukset.stream().map(hakemus -> {
                            assert hakemus != null;
                            if (osallistuminenDTOMap.get(hakemus.getOid()) == null) {
                                LOG.error("Listing hakemus for valinnanvaihe {} and haku {} failed", valinnanvaiheOid, hakuOid);
                                exceptionHandler.accept(new RuntimeException("null response from valintalaskentaValintakoeAsyncResource"));
                            }
                            assert osallistuminenDTOMap != null;
                            assert hakemus.getOid() != null;
                            Set<String> kutsututValintakokeet = osallistuminenDTOMap.get(hakemus.getOid()).stream()
                                    .filter(x -> x != null && x.getHakutoiveet() != null)
                                    .flatMap(x -> x.getHakutoiveet().stream())
                                    .filter(x -> x != null && x.getValinnanVaiheet() != null)
                                    .flatMap(x -> x.getValinnanVaiheet().stream())
                                    .filter(x -> x != null & x.getValintakokeet() != null)
                                    .flatMap(x -> x.getValintakokeet().stream())
                                    .filter(x -> x != null)
                                    .map(x -> x.getValintakoeOid())
                                    .collect(Collectors.toSet());

                            final List<String> hakutoiveOids = new HakemusWrapper(hakemus).getHakutoiveOids();
                            final List<HakukohdeJaValintakoeDTO> hakukohteet = hakutoiveOids
                                    .stream()
                                    .map(x -> valintakoeDTOMap.get(x))
                                    .filter(x -> x != null)
                                    .map(hakukohdeJaValintakoe -> {
                                        final List<ValintakoeDTO> valintakokeet = hakukohdeJaValintakoe.getValintakoeDTO();
                                        final List<ValintakoeDTO> filteredValintakokeet = valintakokeet.stream().filter(valintakoe -> {
                                            if (valintakoe.getKutsutaankoKaikki()) {
                                                return true;
                                            } else if (kutsututValintakokeet.contains(valintakoe.getOid())) {
                                                return true;
                                            }
                                            return false;
                                        }).collect(Collectors.toList());
                                        return new HakukohdeJaValintakoeDTO(hakukohdeJaValintakoe.getHakukohdeOid(), filteredValintakokeet);
                                    })
                                    .filter(hakukohdeJaValintakoeDTO -> !hakukohdeJaValintakoeDTO.getValintakoeDTO().isEmpty())
                                    .collect(Collectors.toList());
                            return hakemusToHakemusDTO(hakemus, hakukohteet);
                        }).collect(Collectors.toList());
                    });
                })
                .subscribe(hakemusDTOs -> {
                    LOG.info("Palautetaan {} hakemusta", hakemusDTOs.size());
                    successHandler.accept(hakemusDTOs);
                });
    }

    private Set<String> collect(List<Hakemus> hakemukset) {
        return hakemukset.stream().flatMap(h -> new HakemusWrapper(h).getHakutoiveOids().stream()).collect(Collectors.toSet());
    }

    private HakemusDTO hakemusToHakemusDTO(Hakemus hakemus, List<HakukohdeJaValintakoeDTO> valintakoeDTOs) {
        Map<String, Koodi> postCodes = null;
        try {
            postCodes = koodistoCachedAsyncResource.haeKoodisto(KoodistoCachedAsyncResource.POSTI);
        } catch (Exception e) {
            LOG.warn("KoodistoCachedAsyncResource threw exception while loading");
            postCodes = new HashMap<>();
        }
        HakemusDTO hakemusDTO = new HakemusDTO();
        hakemusDTO.setHakemusOid(hakemus.getOid());
        hakemusDTO.setHenkiloOid(hakemus.getPersonOid());
        hakemusDTO.setEtunimet(hakemus.getAnswers().getHenkilotiedot().get("Etunimet"));
        hakemusDTO.setSukunimi(hakemus.getAnswers().getHenkilotiedot().get("Sukunimi"));
        hakemusDTO.setKutsumanimi(hakemus.getAnswers().getHenkilotiedot().get("Kutsumanimi"));
        hakemusDTO.setSahkoposti(hakemus.getAnswers().getHenkilotiedot().get("Sähköposti"));
        hakemusDTO.setKatuosoite(hakemus.getAnswers().getHenkilotiedot().get("lahiosoite"));
        String postinumero = hakemus.getAnswers().getHenkilotiedot().get("Postinumero");
        hakemusDTO.setPostinumero(postinumero);
        hakemusDTO.setPostitoimipaikka(KoodistoCachedAsyncResource.haeKoodistaArvo(
                postCodes.get(postinumero),
                KieliUtil.SUOMI,
                postinumero));
        hakemusDTO.setHakukohteet(valintakoeDTOs.stream().map(vk -> new HakukohdeDTO(vk.getHakukohdeOid(), vk.getValintakoeDTO().stream().map(vv -> new fi.vm.sade.valinta.kooste.hakemukset.dto.ValintakoeDTO(vv.getSelvitettyTunniste())).collect(Collectors.toList()))).collect(Collectors.toList()));
        return hakemusDTO;
    }

}
