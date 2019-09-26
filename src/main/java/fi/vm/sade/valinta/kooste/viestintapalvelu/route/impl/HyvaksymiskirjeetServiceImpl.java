package fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl;

import fi.vm.sade.organisaatio.resource.dto.HakutoimistoDTO;
import fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaPaginationObject;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakutoiveDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakutoiveenValintatapajonoDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakukohdeV1RDTO;
import fi.vm.sade.valinta.kooste.external.resource.ataru.AtaruAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.dokumentti.DokumenttiAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.KoodistoCachedAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.OrganisaatioAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.ValintaTulosServiceAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.ViestintapalveluAsyncResource;
import fi.vm.sade.valinta.kooste.parametrit.ParametritParser;
import fi.vm.sade.valinta.kooste.parametrit.service.HakuParametritService;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.Varoitus;
import fi.vm.sade.valinta.kooste.valvomo.dto.Poikkeus;
import fi.vm.sade.valinta.kooste.viestintapalvelu.Hakijapalvelu;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.DokumenttiProsessi;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.HyvaksymiskirjeDTO;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.MetaHakukohde;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoite;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.ProsessiId;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterBatch;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterResponse;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.TemplateDetail;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.DokumenttiProsessiKomponentti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.HyvaksymiskirjeetKomponentti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.HyvaksymiskirjeetService;
import io.reactivex.Observable;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.ViestintapalveluAsyncResource.VIESTINTAPALVELUN_MAKSIMI_POLLAUS_AIKA;
import static io.reactivex.Observable.zip;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

@Service
public class HyvaksymiskirjeetServiceImpl implements HyvaksymiskirjeetService {
    private static final Logger LOG = LoggerFactory.getLogger(HyvaksymiskirjeetServiceImpl.class);
    private static final String VAKIOTEMPLATE = "default";
    private static final String VAKIODETAIL = "sisalto";

    private final ViestintapalveluAsyncResource viestintapalveluAsyncResource;
    private final HyvaksymiskirjeetKomponentti hyvaksymiskirjeetKomponentti;
    private final ValintaTulosServiceAsyncResource valintaTulosServiceAsyncResource;
    private final ApplicationAsyncResource applicationAsyncResource;
    private final AtaruAsyncResource ataruAsyncResource;
    private final TarjontaAsyncResource tarjontaAsyncResource;
    private final OrganisaatioAsyncResource organisaatioAsyncResource;
    private final HakuParametritService hakuParametritService;
    private final KoodistoCachedAsyncResource koodistoCachedAsyncResource;
    private final DokumenttiAsyncResource dokumenttiAsyncResource;
    private final DokumenttiProsessiKomponentti dokumenttiProsessiKomponentti;
    private final int pollingIntervalMillis;

    private SimpleDateFormat pvmMuoto = new SimpleDateFormat("dd.MM.yyyy");
    private SimpleDateFormat kelloMuoto = new SimpleDateFormat("HH.mm");

    @Autowired
    public HyvaksymiskirjeetServiceImpl(
            ViestintapalveluAsyncResource viestintapalveluAsyncResource,
            HyvaksymiskirjeetKomponentti hyvaksymiskirjeetKomponentti,
            ValintaTulosServiceAsyncResource valintaTulosServiceAsyncResource,
            ApplicationAsyncResource applicationAsyncResource,
            AtaruAsyncResource ataruAsyncResource,
            TarjontaAsyncResource tarjontaAsyncResource,
            OrganisaatioAsyncResource organisaatioAsyncResource,
            HakuParametritService hakuParametritService,
            KoodistoCachedAsyncResource koodistoCachedAsyncResource,
            DokumenttiAsyncResource dokumenttiAsyncResource,
            DokumenttiProsessiKomponentti dokumenttiProsessiKomponentti,
            @Value("${valintalaskentakoostepalvelu.hyvaksymiskirjeet.polling.interval.millis:10000}") int pollingIntervalMillis) {
        this.viestintapalveluAsyncResource = viestintapalveluAsyncResource;
        this.hyvaksymiskirjeetKomponentti = hyvaksymiskirjeetKomponentti;
        this.valintaTulosServiceAsyncResource = valintaTulosServiceAsyncResource;
        this.applicationAsyncResource = applicationAsyncResource;
        this.ataruAsyncResource = ataruAsyncResource;
        this.tarjontaAsyncResource = tarjontaAsyncResource;
        this.organisaatioAsyncResource = organisaatioAsyncResource;
        this.hakuParametritService = hakuParametritService;
        this.koodistoCachedAsyncResource = koodistoCachedAsyncResource;
        this.dokumenttiAsyncResource = dokumenttiAsyncResource;
        this.dokumenttiProsessiKomponentti = dokumenttiProsessiKomponentti;
        this.pollingIntervalMillis = pollingIntervalMillis;
    }

    @Override
    public ProsessiId hyvaksymiskirjeetHakemuksille(HyvaksymiskirjeDTO hyvaksymiskirjeDTO, List<String> hakemusOids) {
        LOG.info(String.format("Aloitetaan hakukohteen %s hyväksymiskirjeiden muodostaminen %d hakemukselle", hyvaksymiskirjeDTO.getHakukohdeOid(), hakemusOids.size()));
        DokumenttiProsessi prosessi = new DokumenttiProsessi("", "", "", Collections.emptyList());
        dokumenttiProsessiKomponentti.tuoUusiProsessi(prosessi);
        prosessi.setKokonaistyo(1);

        try {
            ParametritParser haunParametrit = hakuParametritService.getParametritForHaku(hyvaksymiskirjeDTO.getHakuOid());
            Observable<List<HakemusWrapper>> hakemuksetFuture = tarjontaAsyncResource.haeHaku(hyvaksymiskirjeDTO.getHakuOid())
                    .flatMap(haku -> StringUtils.isEmpty(haku.getAtaruLomakeAvain())
                            ? applicationAsyncResource.getApplicationsByhakemusOidsInParts(hyvaksymiskirjeDTO.getHakuOid(), hakemusOids, applicationAsyncResource.DEFAULT_KEYS)
                            : ataruAsyncResource.getApplicationsByOids(hakemusOids));
            Observable<List<HakijaDTO>> hakijatFuture = Observable
                .fromIterable(hakemusOids)
                .concatMap(hakemus -> valintaTulosServiceAsyncResource.getHakijaByHakemus(hyvaksymiskirjeDTO.getHakuOid(), hakemus))
                .toList()
                .toObservable();

            zip(hakemuksetFuture,
                    hakijatFuture,
                    (hakemukset, hakijat) -> muodostaHyvaksymiskirjeet(
                            prosessi,
                            hakijat,
                            hakemukset.stream().collect(Collectors.toMap(HakemusWrapper::getOid, h -> h)),
                            hyvaksymiskirjeDTO.getHakuOid(),
                            hyvaksymiskirjeDTO.getHakukohdeOid(),
                            null,
                            false,
                            hyvaksymiskirjeDTO.getSisalto(),
                            hyvaksymiskirjeDTO.getTag(),
                            hyvaksymiskirjeDTO.getTemplateName(),
                            parsePalautusPvm(hyvaksymiskirjeDTO.getPalautusPvm(), haunParametrit),
                            parsePalautusAika(hyvaksymiskirjeDTO.getPalautusAika(), haunParametrit),
                            false
                    ))
                    .flatMap(x -> x)
                    .subscribe(
                            result -> result.getLeft().ifPresentOrElse(
                                    batchId -> {
                                        LOG.info(String.format("Hakukohteen %s hyväksymiskirjeiden muodostaminen %d hakemukselle valmistui", hyvaksymiskirjeDTO.getHakukohdeOid(), hakemusOids.size()));
                                        prosessi.inkrementoiTehtyjaToita();
                                        prosessi.setDokumenttiId(batchId);
                                    },
                                    () -> {
                                        LOG.error(String.format("Hakukohteen %s hyväksymiskirjeiden muodostaminen %d hakemukselle epäonnistui", hyvaksymiskirjeDTO.getHakukohdeOid(), hakemusOids.size()));
                                        prosessi.inkrementoiOhitettujaToita();
                                        result.getRight().forEach(e -> {
                                            LOG.error(String.format("Hakukohteen %s hyväksymiskirjeiden muodostaminen %d hakemukselle epäonnistui", hyvaksymiskirjeDTO.getHakukohdeOid(), hakemusOids.size()), e);
                                            prosessi.getPoikkeukset().add(Poikkeus.koostepalvelupoikkeus(e.getMessage()));
                                        });
                                    }
                            ),
                            e -> {
                                LOG.error(String.format("Hakukohteen %s hyväksymiskirjeiden muodostaminen %d hakemukselle epäonnistui", hyvaksymiskirjeDTO.getHakukohdeOid(), hakemusOids.size()), e);
                                prosessi.inkrementoiOhitettujaToita();
                                prosessi.getPoikkeukset().add(Poikkeus.koostepalvelupoikkeus(e.getMessage()));
                            }
                    );
        } catch (Throwable t) {
            LOG.error("Hyväksymiskirjeiden luonti hakemuksille haussa {} keskeytyi poikkeukseen: ", hyvaksymiskirjeDTO.getHakuOid(), t);
            prosessi.getPoikkeukset().add(Poikkeus.koostepalvelupoikkeus(t.getMessage()));
        }
        return prosessi.toProsessiId();
    }

    @Override
    public ProsessiId jalkiohjauskirjeHakukohteelle(HyvaksymiskirjeDTO hyvaksymiskirjeDTO) {
        LOG.info(String.format("Aloitetaan hakukohteen %s jälkiohjauskirjeiden muodostaminen", hyvaksymiskirjeDTO.getHakukohdeOid()));
        DokumenttiProsessi prosessi = new DokumenttiProsessi("", "", "", Collections.emptyList());
        dokumenttiProsessiKomponentti.tuoUusiProsessi(prosessi);
        prosessi.setKokonaistyo(1);

        Observable<List<HakemusWrapper>> hakemuksetObservable = tarjontaAsyncResource.haeHaku(hyvaksymiskirjeDTO.getHakuOid())
                .flatMap(haku -> StringUtils.isEmpty(haku.getAtaruLomakeAvain())
                        ? applicationAsyncResource.getApplicationsByOidsWithPOST(hyvaksymiskirjeDTO.getHakuOid(), Collections.singletonList(hyvaksymiskirjeDTO.getHakukohdeOid()))
                        : ataruAsyncResource.getApplicationsByHakukohde(hyvaksymiskirjeDTO.getHakukohdeOid()));
        Observable<HakijaPaginationObject> hakijatObservable = valintaTulosServiceAsyncResource.getKaikkiHakijat(hyvaksymiskirjeDTO.getHakuOid(), hyvaksymiskirjeDTO.getHakukohdeOid());
        zip(
                hakemuksetObservable,
                hakijatObservable,
                (hakemukset, hakijat) -> {
                    LOG.error("Tehdaan hakukohteeseen valitsemattomille filtterointi. Saatiin hakijoita {}", hakijat.getResults().size());
                    Collection<HakijaDTO> hylatyt = hakijat.getResults().stream()
                            .filter(HyvaksymiskirjeetServiceImpl::haussaHylatty)
                            .collect(Collectors.toList());

                    if (hylatyt.isEmpty()) {
                        LOG.error("Hakukohteessa {} ei ole jälkiohjattavia hakijoita!", hyvaksymiskirjeDTO.getHakukohdeOid());
                        throw new RuntimeException("Hakukohteessa " + hyvaksymiskirjeDTO.getHakukohdeOid() + " ei ole jälkiohjattavia hakijoita!");
                    }

                    Map<String, MetaHakukohde> hyvaksymiskirjeessaKaytetytHakukohteet = hyvaksymiskirjeetKomponentti.haeKiinnostavatHakukohteet(hylatyt);

                    return hakukohteidenHakutoimistojenOsoitteet(prosessi, hyvaksymiskirjeessaKaytetytHakukohteet, null)
                            .map(hakutoimistojenOsoitteet -> HyvaksymiskirjeetKomponentti.teeHyvaksymiskirjeet(
                                    koodistoCachedAsyncResource::haeKoodisto,
                                    hakutoimistojenOsoitteet,
                                    hyvaksymiskirjeessaKaytetytHakukohteet,
                                    hylatyt,
                                    hakemukset.stream().collect(Collectors.toMap(HakemusWrapper::getOid, h -> h)),
                                    hyvaksymiskirjeDTO.getHakukohdeOid(),
                                    hyvaksymiskirjeDTO.getHakuOid(),
                                    Optional.empty(),
                                    hyvaksymiskirjeDTO.getSisalto(),
                                    hyvaksymiskirjeDTO.getTag(),
                                    hyvaksymiskirjeDTO.getTemplateName(),
                                    hyvaksymiskirjeDTO.getPalautusPvm(),
                                    hyvaksymiskirjeDTO.getPalautusAika(),
                                    false
                            ));
                })
                .flatMap(x -> x)
                .flatMap(letterBatch -> letterBatchToViestintapalvelu(prosessi, letterBatch))
                .subscribe(
                        result -> result.getLeft().ifPresentOrElse(
                                batchId -> {
                                    LOG.info(String.format("Hakukohteen %s jälkiohjauskirjeiden muodostaminen valmistui", hyvaksymiskirjeDTO.getHakukohdeOid()));
                                    prosessi.inkrementoiTehtyjaToita();
                                    prosessi.setDokumenttiId(batchId);
                                },
                                () -> {
                                    LOG.error(String.format("Hakukohteen %s jälkiohjauskirjeiden muodostaminen epäonnistui", hyvaksymiskirjeDTO.getHakukohdeOid()));
                                    prosessi.inkrementoiOhitettujaToita();
                                    result.getRight().forEach(e -> {
                                        LOG.error(String.format("Hakukohteen %s jälkiohjauskirjeiden muodostaminen epäonnistui", hyvaksymiskirjeDTO.getHakukohdeOid()), e);
                                        prosessi.getPoikkeukset().add(Poikkeus.koostepalvelupoikkeus(e.getMessage()));
                                    });
                                }
                        ),
                        e -> {
                            LOG.error(String.format("Hakukohteen %s jälkiohjauskirjeiden muodostaminen epäonnistui", hyvaksymiskirjeDTO.getHakukohdeOid()), e);
                            prosessi.inkrementoiOhitettujaToita();
                            prosessi.getPoikkeukset().add(Poikkeus.koostepalvelupoikkeus(e.getMessage()));
                        }
                );
        return prosessi.toProsessiId();
    }

    private static boolean haussaHylatty(HakijaDTO hakija) {
        return Optional.ofNullable(hakija.getHakutoiveet())
                .map(hakutoiveet -> hakutoiveet.stream()
                        .flatMap(hakutoive -> hakutoive.getHakutoiveenValintatapajonot().stream())
                        .map(HakutoiveenValintatapajonoDTO::getTila)
                        .noneMatch(HakemuksenTila::isHyvaksytty))
                .orElse(true);
    }

    @Override
    public ProsessiId hyvaksymiskirjeetHakukohteelle(final HyvaksymiskirjeDTO hyvaksymiskirjeDTO) {
        LOG.info(String.format("Aloitetaan hakukohteen %s hyväksymiskirjeiden muodostaminen", hyvaksymiskirjeDTO.getHakukohdeOid()));
        DokumenttiProsessi prosessi = new DokumenttiProsessi("", "", "", Collections.emptyList());
        dokumenttiProsessiKomponentti.tuoUusiProsessi(prosessi);
        prosessi.setKokonaistyo(1);
        String hakukohdeOid = hyvaksymiskirjeDTO.getHakukohdeOid();
        String hakuOid = hyvaksymiskirjeDTO.getHakuOid();

        Observable<List<HakemusWrapper>> hakemuksetObservable = tarjontaAsyncResource.haeHaku(hakuOid)
                .flatMap(haku -> StringUtils.isEmpty(haku.getAtaruLomakeAvain())
                        ? applicationAsyncResource.getApplicationsByOidsWithPOST(hakuOid, Collections.singletonList(hakukohdeOid))
                        : ataruAsyncResource.getApplicationsByHakukohde(hakukohdeOid));
        Observable<HakijaPaginationObject> hakijatObservable = valintaTulosServiceAsyncResource.getKoulutuspaikalliset(hakuOid, hakukohdeOid);
        ParametritParser haunParametrit = hakuParametritService.getParametritForHaku(hakuOid);

        zip(hakemuksetObservable,
                hakijatObservable,
                (hakemukset, hakijat) -> muodostaHyvaksymiskirjeet(
                        prosessi,
                        hakijat.getResults(),
                        hakemukset.stream().collect(Collectors.toMap(HakemusWrapper::getOid, h -> h)),
                        hakuOid,
                        hakukohdeOid,
                        null,
                        hyvaksymiskirjeDTO.getVainTulosEmailinKieltaneet(),
                        hyvaksymiskirjeDTO.getSisalto(),
                        hyvaksymiskirjeDTO.getTag(),
                        hyvaksymiskirjeDTO.getTemplateName(),
                        parsePalautusPvm(hyvaksymiskirjeDTO.getPalautusPvm(), haunParametrit),
                        parsePalautusAika(hyvaksymiskirjeDTO.getPalautusAika(), haunParametrit),
                        false
                ))
                .flatMap(x -> x)
                .subscribe(
                        result -> result.getLeft().ifPresentOrElse(
                                batchId -> {
                                    LOG.info(String.format("Hakukohteen %s hyväksymiskirjeiden muodostaminen valmistui", hyvaksymiskirjeDTO.getHakukohdeOid()));
                                    prosessi.inkrementoiTehtyjaToita();
                                    prosessi.setDokumenttiId(batchId);
                                },
                                () -> {
                                    LOG.error(String.format("Hakukohteen %s hyväksymiskirjeiden muodostaminen epäonnistui", hyvaksymiskirjeDTO.getHakukohdeOid()));
                                    prosessi.inkrementoiOhitettujaToita();
                                    result.getRight().forEach(e -> {
                                        LOG.error(String.format("Hakukohteen %s hyväksymiskirjeiden muodostaminen epäonnistui", hyvaksymiskirjeDTO.getHakukohdeOid()), e);
                                        prosessi.getPoikkeukset().add(Poikkeus.koostepalvelupoikkeus(e.getMessage()));
                                    });
                                }
                        ),
                        e -> {
                            LOG.error(String.format("Hakukohteen %s hyväksymiskirjeiden muodostaminen epäonnistui", hyvaksymiskirjeDTO.getHakukohdeOid()), e);
                            prosessi.inkrementoiOhitettujaToita();
                            prosessi.getPoikkeukset().add(Poikkeus.koostepalvelupoikkeus(e.getMessage()));
                        }
                );
        return prosessi.toProsessiId();
    }

    @Override
    public ProsessiId hyvaksymiskirjeetHaulle(String hakuOid, String asiointikieli, String defaultValue) {
        LOG.info(String.format("Aloitetaan haun %s hyväksymiskirjeiden muodostaminen asiointikielelle %s", hakuOid, asiointikieli));
        DokumenttiProsessi prosessi = new DokumenttiProsessi("hyvaksymiskirjeet", "Luo hyvaksymiskirjeet haulle", hakuOid, Arrays.asList("hyvaksymiskirjeet", "haulle"));
        dokumenttiProsessiKomponentti.tuoUusiProsessi(prosessi);
        prosessi.setKokonaistyo(1);
        ParametritParser haunParametrit = hakuParametritService.getParametritForHaku(hakuOid);
        valintaTulosServiceAsyncResource.getKoulutuspaikalliset(hakuOid)
                .flatMap(valintatulokset -> tarjontaAsyncResource.haeHaku(hakuOid)
                        .flatMap(haku -> {
                            List<String> hakemusOids = valintatulokset.getResults().stream().map(HakijaDTO::getHakemusOid).collect(Collectors.toList());
                            return StringUtils.isEmpty(haku.getAtaruLomakeAvain())
                                    ? applicationAsyncResource.getApplicationsByhakemusOidsInParts(hakuOid, hakemusOids, ApplicationAsyncResource.DEFAULT_KEYS)
                                    : ataruAsyncResource.getApplicationsByOids(hakemusOids);
                        })
                        .flatMap(hakemukset -> muodostaHyvaksymiskirjeet(
                                prosessi,
                                valintatulokset.getResults(),
                                hakemukset.stream().collect(Collectors.toMap(HakemusWrapper::getOid, h -> h)),
                                hakuOid,
                                null,
                                asiointikieli,
                                false,
                                defaultValue,
                                hakuOid,
                                "hyvaksymiskirje",
                                parsePalautusPvm(null, haunParametrit),
                                parsePalautusAika(null, haunParametrit),
                                true
                        )))
                .subscribe(
                        result -> result.getLeft().ifPresentOrElse(
                                batchId -> {
                                    LOG.info(String.format("Haun %s hyväksymiskirjeiden muodostaminen asiointikielelle %s valmistui", hakuOid, asiointikieli));
                                    prosessi.inkrementoiTehtyjaToita();
                                    prosessi.setDokumenttiId(batchId);
                                },
                                () -> {
                                    LOG.error(String.format("Haun %s hyväksymiskirjeiden muodostaminen asiointikielelle %s epäonnistui", hakuOid, asiointikieli));
                                    prosessi.inkrementoiOhitettujaToita();
                                    result.getRight().forEach(e -> {
                                        LOG.error(String.format("Haun %s hyväksymiskirjeiden muodostaminen asiointikielelle %s epäonnistui", hakuOid, asiointikieli), e);
                                        prosessi.getPoikkeukset().add(Poikkeus.koostepalvelupoikkeus(e.getMessage()));
                                    });
                                }
                        ),
                        e -> {
                            LOG.error(String.format("Haun %s hyväksymiskirjeiden muodostaminen asiointikielelle %s epäonnistui", hakuOid, asiointikieli), e);
                            prosessi.inkrementoiOhitettujaToita();
                            prosessi.getPoikkeukset().add(Poikkeus.koostepalvelupoikkeus(e.getMessage()));
                        }
                );
        return prosessi.toProsessiId();
    }

    public ProsessiId hyvaksymiskirjeetHaulleHakukohteittain(String hakuOid, Optional<String> defaultValue) {
        LOG.info(String.format("Aloitetaan haun %s hyväksymiskirjeiden muodostaminen hakukohteittain", hakuOid));
        DokumenttiProsessi prosessi = new DokumenttiProsessi("hyvaksymiskirjeet", "Luo hyvaksymiskirjeet haulle", hakuOid, Arrays.asList("hyvaksymiskirjeet", "haulle"));
        dokumenttiProsessiKomponentti.tuoUusiProsessi(prosessi);
        ParametritParser haunParametrit = hakuParametritService.getParametritForHaku(hakuOid);
        valintaTulosServiceAsyncResource.getKoulutuspaikalliset(hakuOid)
                .flatMap(valintatulokset -> tarjontaAsyncResource.haeHaku(hakuOid)
                        .flatMap(haku -> {
                            List<String> hakemusOids = valintatulokset.getResults().stream().map(HakijaDTO::getHakemusOid).collect(Collectors.toList());
                            return StringUtils.isEmpty(haku.getAtaruLomakeAvain())
                                    ? applicationAsyncResource.getApplicationsByhakemusOidsInParts(hakuOid, hakemusOids, ApplicationAsyncResource.DEFAULT_KEYS)
                                    : ataruAsyncResource.getApplicationsByOids(hakemusOids);
                        })
                        .flatMap(hakemukset -> {
                            Map<String, HakemusWrapper> hakemuksetByOid = hakemukset.stream().collect(Collectors.toMap(HakemusWrapper::getOid, h -> h));
                            List<String> hakukohteetJoissaHyvaksyttyja = valintatulokset.getResults().stream()
                                    .flatMap(hakija -> hakija.getHakutoiveet().stream())
                                    .filter(hakutoive -> hakutoive.getHakutoiveenValintatapajonot().stream().anyMatch(valintatapajono -> valintatapajono.getTila().isHyvaksytty()))
                                    .map(HakutoiveDTO::getHakukohdeOid)
                                    .distinct()
                                    .collect(Collectors.toList());
                            prosessi.setKokonaistyo(hakukohteetJoissaHyvaksyttyja.size());
                            return Observable.fromIterable(hakukohteetJoissaHyvaksyttyja)
                                    .flatMap(hakukohdeOid -> muodostaHyvaksymiskirjeet(
                                            prosessi,
                                            valintatulokset.getResults(),
                                            hakemuksetByOid,
                                            hakuOid,
                                            hakukohdeOid,
                                            null,
                                            false,
                                            defaultValue.orElse(null),
                                            hakuOid,
                                            "hyvaksymiskirje",
                                            parsePalautusPvm(null, haunParametrit),
                                            parsePalautusAika(null, haunParametrit),
                                            false)
                                            .flatMap(result -> {
                                                if (result.getLeft().isPresent()) {
                                                    return renameHakukohteenHyvaksymiskirjeet(prosessi, hakukohdeOid, result.getLeft().get());
                                                }
                                                return Observable.just(Pair.of(hakukohdeOid, result.getRight()));
                                            })
                                            .onErrorReturn(e -> Pair.of(hakukohdeOid, Collections.singletonList(e))),
                                    6);
                        }))
                .subscribe(
                        result -> {
                            if (result.getRight().isEmpty()) {
                                LOG.info(String.format("Haun %s hakukohteen %s hyväksymiskirjeiden muodostaminen valmistui", hakuOid, result.getLeft()));
                                prosessi.inkrementoiTehtyjaToita();
                            } else {
                                LOG.error(String.format("Haun %s hakukohteen %s hyväksymiskirjeiden muodostaminen epäonnistui", hakuOid, result.getLeft()));
                                prosessi.inkrementoiOhitettujaToita();
                                result.getRight().forEach(e -> {
                                    LOG.error(String.format("Haun %s hakukohteen %s hyväksymiskirjeiden muodostaminen epäonnistui", hakuOid, result.getLeft()), e);
                                    prosessi.getVaroitukset().add(new Varoitus(result.getLeft(), e.getMessage()));
                                });
                            }
                        },
                        e -> {
                            LOG.error(String.format("Haun %s hyväksymiskirjeiden muodostaminen hakukohteittain epäonnistui", hakuOid), e);
                            prosessi.getPoikkeukset().add(Poikkeus.koostepalvelupoikkeus(e.getMessage()));
                        },
                        () -> LOG.info(String.format("Haun %s hyväksymiskirjeiden muodostaminen hakukohteittain onnistui", hakuOid)));
        return prosessi.toProsessiId();
    }

    private Observable<Pair<Optional<String>, List<Throwable>>> muodostaHyvaksymiskirjeet(DokumenttiProsessi prosessi,
                                                                                          List<HakijaDTO> hakijat,
                                                                                          Map<String, HakemusWrapper> hakemukset,
                                                                                          String hakuOid,
                                                                                          String hakukohdeJossaHyvaksytty,
                                                                                          String asiointikieli,
                                                                                          boolean vainTulosEmailinKieltaneet,
                                                                                          String vakiosisalto,
                                                                                          String tag,
                                                                                          String templateName,
                                                                                          String palautusPvm,
                                                                                          String palautusAika,
                                                                                          boolean sahkoinenKorkeakoulunMassaposti) {
        List<HakijaDTO> kasiteltavatHakijat = hakijat.stream()
                .filter(hakija -> !vainTulosEmailinKieltaneet || !hakemukset.get(hakija.getHakemusOid()).getLupaTulosEmail())
                .filter(hakija -> asiointikieli == null || asiointikieli.equalsIgnoreCase(hakemukset.get(hakija.getHakemusOid()).getAsiointikieli()))
                .filter(hakija -> hakija.getHakutoiveet().stream()
                        .filter(hakutoive -> hakukohdeJossaHyvaksytty == null || hakukohdeJossaHyvaksytty.equals(hakutoive.getHakukohdeOid()))
                        .flatMap(hakutoive -> hakutoive.getHakutoiveenValintatapajonot().stream())
                        .anyMatch(valintatapajono -> valintatapajono.getTila().isHyvaksytty()))
                .collect(Collectors.toList());
        Map<String, MetaHakukohde> hakukohteet = hyvaksymiskirjeetKomponentti.haeKiinnostavatHakukohteet(kasiteltavatHakijat);
        return Observable.zip(
                vakiosisalto == null ? tarjontaAsyncResource.haeHakukohde(hakukohdeJossaHyvaksytty).flatMap(this::haeHakukohteenVakiosisalto) : Observable.just(vakiosisalto),
                hakukohteidenHakutoimistojenOsoitteet(prosessi, hakukohteet, asiointikieli),
                (sisalto, osoitteet) -> HyvaksymiskirjeetKomponentti.teeHyvaksymiskirjeet(
                        koodistoCachedAsyncResource::haeKoodisto,
                        osoitteet,
                        hakukohteet,
                        kasiteltavatHakijat,
                        hakemukset,
                        null,
                        hakuOid,
                        Optional.ofNullable(asiointikieli),
                        sisalto,
                        tag,
                        templateName,
                        palautusPvm,
                        palautusAika,
                        sahkoinenKorkeakoulunMassaposti)
        ).flatMap(letterBatch -> letterBatchToViestintapalvelu(prosessi, letterBatch));
    }

    private Observable<String> haeHakukohteenVakiosisalto(HakukohdeV1RDTO hakukohde) {
        return viestintapalveluAsyncResource.haeKirjepohja(
                hakukohde.getHakuOid(),
                hakukohde.getTarjoajaOids().iterator().next(),
                "hyvaksymiskirje",
                KirjeetHakukohdeCache.getOpetuskieli(hakukohde.getOpetusKielet()),
                hakukohde.getOid()
        ).flatMap(kirjepohjat -> kirjepohjat.stream()
                .filter(kirjepohja -> VAKIOTEMPLATE.equals(kirjepohja.getName()))
                .flatMap(kirjepohja -> kirjepohja.getTemplateReplacements().stream().filter(tdd -> VAKIODETAIL.equals(tdd.getName())))
                .map(TemplateDetail::getDefaultValue)
                .map(Observable::just)
                .findAny()
                .orElse(Observable.error(new RuntimeException(String.format("Ei %s tai %s templateDetailia hakukohteelle %s", VAKIOTEMPLATE, VAKIODETAIL, hakukohde.getOid())))));
    }

    private Observable<Map<String, Optional<Osoite>>> hakukohteidenHakutoimistojenOsoitteet(DokumenttiProsessi prosessi, Map<String, MetaHakukohde> hakukohteet, String asiointikieli) {
        if (prosessi.isKeskeytetty()) {
            return Observable.error(new RuntimeException("Kirjeiden muodostus keskeytetty"));
        }
        return Observable.fromIterable(hakukohteet.values().stream().map(MetaHakukohde::getTarjoajaOid).distinct()::iterator)
                .flatMap(tarjoajaOid -> organisaatioAsyncResource.haeHakutoimisto(tarjoajaOid).map(t -> Pair.of(tarjoajaOid, t)))
                .collectInto(new HashMap<String, Optional<HakutoimistoDTO>>(), (m, p) -> m.put(p.getLeft(), p.getRight()))
                .map(hakutoimistot -> hakukohteet.entrySet().stream()
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                e -> {
                                    MetaHakukohde hakukohde = e.getValue();
                                    return hakutoimistot.getOrDefault(hakukohde.getTarjoajaOid(), Optional.empty())
                                            .flatMap(t -> Hakijapalvelu.osoite(t, asiointikieli == null ? hakukohde.getHakukohteenKieli() : asiointikieli));
                                }
                        )))
                .toObservable();
    }

    private Observable<Pair<Optional<String>, List<Throwable>>> letterBatchToViestintapalvelu(DokumenttiProsessi prosessi, LetterBatch letterBatch) {
        return vieLetterBatch(prosessi, letterBatch)
                .flatMap(result -> result.getLeft()
                        .map(batchId -> Observable.interval(pollingIntervalMillis, MILLISECONDS)
                                .flatMap(i -> letterBatchProcessingStatus(prosessi, batchId))
                                .take(1)
                                .timeout(VIESTINTAPALVELUN_MAKSIMI_POLLAUS_AIKA.toMillis(),
                                        MILLISECONDS,
                                        Observable.just(Pair.of(Optional.empty(), Collections.singletonList(new RuntimeException("Kirjeiden vienti viestintäpalveluun aikakatkaistu"))))))
                        .orElse(Observable.just(result)));
    }

    private Observable<Pair<Optional<String>, List<Throwable>>> vieLetterBatch(DokumenttiProsessi prosessi, LetterBatch letterBatch) {
        if (prosessi.isKeskeytetty()) {
            return Observable.error(new RuntimeException("Kirjeiden muodostus keskeytetty"));
        }
        return viestintapalveluAsyncResource.viePdfJaOdotaReferenssiObservable(letterBatch)
                .<Pair<Optional<String>, List<Throwable>>>map(response -> {
                    if (response.getStatus().equals(LetterResponse.STATUS_SUCCESS)) {
                        return Pair.of(Optional.of(response.getBatchId()), Collections.emptyList());
                    }
                    return Pair.of(
                            Optional.empty(),
                            response.getErrors().entrySet().stream()
                                    .map(e1 -> new RuntimeException(e1.getKey() + ": " + e1.getValue()))
                                    .collect(Collectors.toList())
                    );
                })
                .onErrorReturn(e -> Pair.of(Optional.empty(), Collections.singletonList(e)));
    }

    private Observable<Pair<Optional<String>, List<Throwable>>> letterBatchProcessingStatus(DokumenttiProsessi prosessi, String batchId) {
        if (prosessi.isKeskeytetty()) {
            return Observable.error(new RuntimeException("Kirjeiden muodostus keskeytetty"));
        }
        return viestintapalveluAsyncResource.haeStatusObservable(batchId)
                .<Pair<Optional<String>, List<Throwable>>>flatMap(response -> {
                    if ("ready".equals(response.getStatus())) {
                        return Observable.just(Pair.of(Optional.of(batchId), Collections.emptyList()));
                    }
                    if ("error".equals(response.getStatus())) {
                        return Observable.error(new RuntimeException("Kirjeiden vienti viestintäpalveluun epäonnistui"));
                    }
                    return Observable.empty();
                })
                .onErrorReturn(e -> Pair.of(Optional.empty(), Collections.singletonList(e)));
    }

    private Observable<Pair<String, List<Throwable>>> renameHakukohteenHyvaksymiskirjeet(DokumenttiProsessi prosessi, String hakukohdeOid, String batchId) {
        if (prosessi.isKeskeytetty()) {
            return Observable.error(new RuntimeException("Kirjeiden muodostus keskeytetty"));
        }
        return dokumenttiAsyncResource.uudelleenNimea(batchId, "hyvaksymiskirje_" + hakukohdeOid + ".pdf")
                .<Pair<String, List<Throwable>>>map(response -> Pair.of(hakukohdeOid, Collections.emptyList()))
                .onErrorReturn(e -> Pair.of(hakukohdeOid, Collections.singletonList(e)));
    }

    public String parsePalautusPvm(String specifiedPvm, ParametritParser haunParametrit) {
        if(StringUtils.trimToNull(specifiedPvm) == null && haunParametrit.opiskelijanPaikanVastaanottoPaattyy() != null) {
            return pvmMuoto.format(haunParametrit.opiskelijanPaikanVastaanottoPaattyy());
        }
        return specifiedPvm;
    }

    public String parsePalautusAika(String specifiedAika, ParametritParser haunParametrit) {
        if(StringUtils.trimToNull(specifiedAika) == null && haunParametrit.opiskelijanPaikanVastaanottoPaattyy() != null) {
            return kelloMuoto.format(haunParametrit.opiskelijanPaikanVastaanottoPaattyy());
        }
        return specifiedAika;
    }
}
