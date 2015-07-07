package fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import fi.vm.sade.organisaatio.resource.dto.HakutoimistoDTO;
import fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaPaginationObject;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakutoiveenValintatapajonoDTO;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.OrganisaatioAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.SijoitteluAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.ViestintapalveluAsyncResource;
import fi.vm.sade.valinta.kooste.viestintapalvelu.Hakijapalvelu;
import fi.vm.sade.valinta.kooste.viestintapalvelu.OsoiteHaku;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.HyvaksymiskirjeDTO;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.KirjeProsessi;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.MetaHakukohde;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoite;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterBatch;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterBatchStatusDto;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterResponse;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.HaeOsoiteKomponentti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.HyvaksymiskirjeetKomponentti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.predicate.SijoittelussaHyvaksyttyHakija;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.HyvaksymiskirjeetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rx.Observable;
import rx.functions.Action3;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

import javax.ws.rs.core.Response;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.google.common.collect.Lists.newArrayList;
import static rx.Observable.from;
import static rx.Observable.zip;

@Service
public class HyvaksymiskirjeetServiceImpl implements HyvaksymiskirjeetService {
    private static final Logger LOG = LoggerFactory.getLogger(HyvaksymiskirjeetServiceImpl.class);
    private final ViestintapalveluAsyncResource viestintapalveluAsyncResource;
    private final HyvaksymiskirjeetKomponentti hyvaksymiskirjeetKomponentti;
    private final SijoitteluAsyncResource sijoitteluAsyncResource;
    private final ApplicationAsyncResource applicationAsyncResource;
    private final OrganisaatioAsyncResource organisaatioAsyncResource;
    private final HaeOsoiteKomponentti haeOsoiteKomponentti;

    @Autowired
    public HyvaksymiskirjeetServiceImpl(
            ViestintapalveluAsyncResource viestintapalveluAsyncResource,
            HyvaksymiskirjeetKomponentti hyvaksymiskirjeetKomponentti,
            SijoitteluAsyncResource sijoitteluAsyncResource,
            ApplicationAsyncResource applicationAsyncResource,
            OrganisaatioAsyncResource organisaatioAsyncResource,
            HaeOsoiteKomponentti haeOsoiteKomponentti) {
        this.viestintapalveluAsyncResource = viestintapalveluAsyncResource;
        this.hyvaksymiskirjeetKomponentti = hyvaksymiskirjeetKomponentti;
        this.sijoitteluAsyncResource = sijoitteluAsyncResource;
        this.applicationAsyncResource = applicationAsyncResource;
        this.organisaatioAsyncResource = organisaatioAsyncResource;
        this.haeOsoiteKomponentti = haeOsoiteKomponentti;
    }

    @Override
    public void hyvaksymiskirjeetHakemuksille(final KirjeProsessi prosessi,
                                              final HyvaksymiskirjeDTO hyvaksymiskirjeDTO,
                                              final List<String> hakemusOids) {

        String organisaatioOid = hyvaksymiskirjeDTO.getTarjoajaOid();

        Future<List<Hakemus>> hakemuksetFuture = applicationAsyncResource.getApplicationsByOids(hakemusOids);
        Future<HakijaPaginationObject> hakijatFuture = sijoitteluAsyncResource.getKoulutuspaikkallisetHakijat(hyvaksymiskirjeDTO.getHakuOid(), hyvaksymiskirjeDTO.getHakukohdeOid());
        Observable<HakutoimistoDTO> hakutoimistoObservable = organisaatioAsyncResource.haeHakutoimisto(organisaatioOid);
        final String hakukohdeOid = hyvaksymiskirjeDTO.getHakukohdeOid();

        zip(
                from(hakemuksetFuture),
                from(hakijatFuture),
                hakutoimistoObservable,
                (hakemukset, hakijat, hakutoimisto) -> {
                    LOG.info("Tehdaan valituille hakijoille hyvaksytyt filtterointi.");
                    final Set<String> kohdeHakijat = Sets.newHashSet(hakemusOids);
                    Collection<HakijaDTO> kohdeHakukohteessaHyvaksytyt = hakijat.getResults().stream()
                            .filter(new SijoittelussaHyvaksyttyHakija(hakukohdeOid))
                            .filter(h -> kohdeHakijat.contains(h.getHakemusOid()))
                            .collect(Collectors.toList());
                    Map<String, MetaHakukohde> hyvaksymiskirjeessaKaytetytHakukohteet = hyvaksymiskirjeetKomponentti.haeKiinnostavatHakukohteet(kohdeHakukohteessaHyvaksytyt);
                    MetaHakukohde kohdeHakukohde = hyvaksymiskirjeessaKaytetytHakukohteet.get(hyvaksymiskirjeDTO.getHakukohdeOid());
                    final boolean iPosti = false;
                    return hyvaksymiskirjeetKomponentti.teeHyvaksymiskirjeet(
                            ImmutableMap.of(organisaatioOid, Hakijapalvelu.osoite(hakutoimisto, kohdeHakukohde.getHakukohteenKieli())),
                            hyvaksymiskirjeessaKaytetytHakukohteet,
                            kohdeHakukohteessaHyvaksytyt, hakemukset,
                            hyvaksymiskirjeDTO.getHakuOid(),
                            Optional.empty(),
                            hyvaksymiskirjeDTO.getSisalto(),
                            hyvaksymiskirjeDTO.getTag(),
                            hyvaksymiskirjeDTO.getTemplateName(),
                            hyvaksymiskirjeDTO.getPalautusPvm(),
                            hyvaksymiskirjeDTO.getPalautusAika(),
                            iPosti
                    );
                })
                //
                .subscribeOn(Schedulers.newThread())
                .subscribe(
                        letterBatch -> letterBatchToViestintapalvelu().call(letterBatch, prosessi, hyvaksymiskirjeDTO),
                        throwable -> logErrorAndKeskeyta(prosessi, throwable));
    }

    @Override
    public void jalkiohjauskirjeHakukohteelle(final KirjeProsessi prosessi, final HyvaksymiskirjeDTO hyvaksymiskirjeDTO) {
        Observable<List<Hakemus>> hakemuksetObservable = applicationAsyncResource.getApplicationsByOid(hyvaksymiskirjeDTO.getHakuOid(), hyvaksymiskirjeDTO.getHakukohdeOid());
        Future<HakijaPaginationObject> hakijatFuture = sijoitteluAsyncResource.getKaikkiHakijat(hyvaksymiskirjeDTO.getHakuOid(), hyvaksymiskirjeDTO.getHakukohdeOid());
        Future<Response> organisaatioFuture = organisaatioAsyncResource.haeOrganisaatio(hyvaksymiskirjeDTO.getTarjoajaOid());
        zip(
                hakemuksetObservable,
                from(hakijatFuture),
                from(organisaatioFuture),
                (hakemukset, hakijat, organisaatioResponse) -> {
                    LOG.error("Tehdaan hakukohteeseen valitsemattomille filtterointi. Saatiin hakijoita {}", hakijat.getResults().size());
                    Collection<HakijaDTO> hylatyt = hakijat.getResults().stream()
                            .filter(HyvaksymiskirjeetServiceImpl::haussaHylatty)
                            .collect(Collectors.toList());
                    if (hylatyt.isEmpty()) {
                        LOG.error("Hakukohteessa {} ei ole jälkiohjattavia hakijoita!", hyvaksymiskirjeDTO.getHakukohdeOid());
                        prosessi.keskeyta("Hakukohteessa ei ole jälkiohjattavia hakijoita!");
                        throw new RuntimeException("Hakukohteessa " + hyvaksymiskirjeDTO.getHakukohdeOid() + " ei ole jälkiohjattavia hakijoita!");
                    }
                    Map<String, MetaHakukohde> hyvaksymiskirjeessaKaytetytHakukohteet = hyvaksymiskirjeetKomponentti.haeKiinnostavatHakukohteet(hylatyt);
                    MetaHakukohde kohdeHakukohde = hyvaksymiskirjeessaKaytetytHakukohteet.get(hyvaksymiskirjeDTO.getHakukohdeOid());
                    List<String> tarjoajaOidList = newArrayList(Arrays.asList(hyvaksymiskirjeDTO.getTarjoajaOid()));
                    Osoite hakijapalveluidenOsoite = OsoiteHaku.organisaatioResponseToHakijapalveluidenOsoite(haeOsoiteKomponentti, organisaatioAsyncResource, tarjoajaOidList,
                            kohdeHakukohde.getHakukohteenKieli(), organisaatioResponse);
                    final boolean iPosti = false;
                    return hyvaksymiskirjeetKomponentti.teeHyvaksymiskirjeet(
                            ImmutableMap.of(hyvaksymiskirjeDTO.getTarjoajaOid(),Optional.ofNullable(hakijapalveluidenOsoite)),
                            hyvaksymiskirjeessaKaytetytHakukohteet,
                            hylatyt, hakemukset,
                            hyvaksymiskirjeDTO.getHakuOid(),
                            Optional.empty(),
                            hyvaksymiskirjeDTO.getSisalto(),
                            hyvaksymiskirjeDTO.getTag(),
                            hyvaksymiskirjeDTO.getTemplateName(),
                            hyvaksymiskirjeDTO.getPalautusPvm(),
                            hyvaksymiskirjeDTO.getPalautusAika(),
                            iPosti
                    );
                })
                //
                .subscribeOn(Schedulers.newThread())
                .subscribe(
                        letterBatch -> letterBatchToViestintapalvelu().call(letterBatch, prosessi, hyvaksymiskirjeDTO),
                        throwable -> logErrorAndKeskeyta(prosessi, throwable));
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
    public void hyvaksymiskirjeetHakukohteelle(KirjeProsessi prosessi, final HyvaksymiskirjeDTO hyvaksymiskirjeDTO) {
        String organisaatioOid = hyvaksymiskirjeDTO.getTarjoajaOid();
        Observable<List<Hakemus>> hakemuksetObservable = applicationAsyncResource.getApplicationsByOid(hyvaksymiskirjeDTO.getHakuOid(), hyvaksymiskirjeDTO.getHakukohdeOid());
        Future<HakijaPaginationObject> hakijatFuture = sijoitteluAsyncResource.getKoulutuspaikkallisetHakijat(hyvaksymiskirjeDTO.getHakuOid(), hyvaksymiskirjeDTO.getHakukohdeOid());
        Observable<HakutoimistoDTO> hakutoimistoObservable = organisaatioAsyncResource.haeHakutoimisto(organisaatioOid);
        final String hakukohdeOid = hyvaksymiskirjeDTO.getHakukohdeOid();
        zip(
                hakemuksetObservable,
                from(hakijatFuture),
                hakutoimistoObservable,
                (hakemukset, hakijat, hakutoimisto) -> {
                    LOG.info("Tehdaan hakukohteeseen valituille hyvaksytyt filtterointi.");
                    Collection<HakijaDTO> kohdeHakukohteessaHyvaksytyt = hakijat.getResults().stream()
                            .filter(new SijoittelussaHyvaksyttyHakija(hakukohdeOid))
                            .collect(Collectors.toList());
                    Map<String, MetaHakukohde> hyvaksymiskirjeessaKaytetytHakukohteet = hyvaksymiskirjeetKomponentti.haeKiinnostavatHakukohteet(kohdeHakukohteessaHyvaksytyt);
                    MetaHakukohde kohdeHakukohde = hyvaksymiskirjeessaKaytetytHakukohteet.get(hyvaksymiskirjeDTO.getHakukohdeOid());
                    final boolean iPosti = false;
                    return hyvaksymiskirjeetKomponentti.teeHyvaksymiskirjeet(
                            ImmutableMap.of(organisaatioOid,Hakijapalvelu.osoite(hakutoimisto, kohdeHakukohde.getHakukohteenKieli())),
                            hyvaksymiskirjeessaKaytetytHakukohteet,
                            kohdeHakukohteessaHyvaksytyt, hakemukset,
                            hyvaksymiskirjeDTO.getHakuOid(),
                            Optional.empty(),
                            hyvaksymiskirjeDTO.getSisalto(),
                            hyvaksymiskirjeDTO.getTag(),
                            hyvaksymiskirjeDTO.getTemplateName(),
                            hyvaksymiskirjeDTO.getPalautusPvm(),
                            hyvaksymiskirjeDTO.getPalautusAika(),
                            iPosti);
                })
                //
                .subscribeOn(Schedulers.newThread())
                .subscribe(
                        letterBatch -> letterBatchToViestintapalvelu().call(letterBatch, prosessi, hyvaksymiskirjeDTO),
                        throwable -> logErrorAndKeskeyta(prosessi, throwable));
    }

    void logErrorAndKeskeyta(KirjeProsessi prosessi, Throwable throwable) {
        LOG.error("Sijoittelu tai hakemuspalvelukutsu epaonnistui", throwable);
        prosessi.keskeyta();
    }

    public Action3<LetterBatch, KirjeProsessi, HyvaksymiskirjeDTO> letterBatchToViestintapalvelu() {
        return (letterBatch, prosessi, kirje) -> {
            try {
                if (prosessi.isKeskeytetty()) {
                    LOG.error("Hyvaksymiskirjeiden luonti on keskeytetty kayttajantoimesta!");
                    return;
                }
                LOG.info("Tehdaan viestintapalvelukutsu kirjeille.");
                final LetterResponse batchId;
                try {
                    batchId = viestintapalveluAsyncResource.viePdfJaOdotaReferenssi(letterBatch).get(165L, TimeUnit.SECONDS);
                } catch (Exception e) {
                    LOG.error("Viestintapalvelukutsu epaonnistui virheeseen {}", e.getMessage());
                    throw new RuntimeException(e);
                }
                LOG.info("Saatiin kirjeen seurantaId {}", batchId.getBatchId());
                prosessi.vaiheValmistui();
                if (batchId.getStatus().equals(LetterResponse.STATUS_SUCCESS)) {
                    PublishSubject<String> stop = PublishSubject.create();
                    Observable
                            .interval(1, TimeUnit.SECONDS)
                            .take(ViestintapalveluAsyncResource.VIESTINTAPALVELUN_MAKSIMI_POLLAUS_SEKUNTIA)
                            .takeUntil(stop)
                            .subscribe(
                                    pulse -> {
                                        try {
                                            LOG.warn("Tehdaan status kutsu seurantaId:lle {}", batchId);
                                            LetterBatchStatusDto status = viestintapalveluAsyncResource.haeStatus(batchId.getBatchId()).get(900L, TimeUnit.MILLISECONDS);
                                            if (prosessi.isKeskeytetty()) {
                                                LOG.error("Hyvaksymiskirjeiden luonti on keskeytetty kayttajantoimesta!");
                                                stop.onNext(null);
                                                return;
                                            }
                                            if ("error".equals(status.getStatus())) {
                                                LOG.error("Hyvaksymiskirjeiden muodostus paattyi viestintapalvelun sisaiseen virheeseen!");
                                                prosessi.keskeyta();
                                                stop.onNext(null);
                                            }
                                            if ("ready".equals(status.getStatus())) {
                                                prosessi.vaiheValmistui();
                                                LOG.error("Hyvaksymiskirjeet valmistui!");
                                                prosessi.valmistui(batchId.getBatchId());
                                                stop.onNext(null);
                                            }
                                        } catch (Exception e) {
                                            LOG.error(
                                                    "Statuksen haku epaonnistui {}",
                                                    e.getMessage());
                                        }

                                    }, throwable -> {
                                        prosessi.keskeyta();
                                    }, () -> {
                                        prosessi.keskeyta();
                                    });
                } else {
                    prosessi.keskeyta("Hakemuksissa oli virheitä", batchId.getErrors());
                }
            } catch (Exception e) {
                LOG.error("Virhe hakukohteelle " + kirje.getHakukohdeOid(), e);
                prosessi.keskeyta();
            }
        };
    }
}
