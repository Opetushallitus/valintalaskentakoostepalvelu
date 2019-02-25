package fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl;

import static com.google.common.collect.Lists.newArrayList;
import static fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.ViestintapalveluAsyncResource.VIESTINTAPALVELUN_MAKSIMI_POLLAUS_AIKA;
import static io.reactivex.Observable.zip;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;

import fi.vm.sade.organisaatio.resource.dto.HakutoimistoDTO;
import fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaPaginationObject;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakutoiveenValintatapajonoDTO;
import fi.vm.sade.valinta.kooste.external.resource.ataru.AtaruAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.OrganisaatioAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.ValintaTulosServiceAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.ViestintapalveluAsyncResource;
import fi.vm.sade.valinta.kooste.parametrit.ParametritParser;
import fi.vm.sade.valinta.kooste.parametrit.service.HakuParametritService;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valinta.kooste.valvomo.dto.Poikkeus;
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
import io.reactivex.Observable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.Response;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class HyvaksymiskirjeetServiceImpl implements HyvaksymiskirjeetService {
    private static final Logger LOG = LoggerFactory.getLogger(HyvaksymiskirjeetServiceImpl.class);
    private final ViestintapalveluAsyncResource viestintapalveluAsyncResource;
    private final HyvaksymiskirjeetKomponentti hyvaksymiskirjeetKomponentti;
    private final ValintaTulosServiceAsyncResource valintaTulosServiceAsyncResource;
    private final ApplicationAsyncResource applicationAsyncResource;
    private AtaruAsyncResource ataruAsyncResource;
    private TarjontaAsyncResource tarjontaAsyncResource;
    private final OrganisaatioAsyncResource organisaatioAsyncResource;
    private final HaeOsoiteKomponentti haeOsoiteKomponentti;
    private final HakuParametritService hakuParametritService;
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
            HaeOsoiteKomponentti haeOsoiteKomponentti,
            HakuParametritService hakuParametritService,
            @Value("${valintalaskentakoostepalvelu.hyvaksymiskirjeet.polling.interval.millis:10000}") int pollingIntervalMillis) {
        this.viestintapalveluAsyncResource = viestintapalveluAsyncResource;
        this.hyvaksymiskirjeetKomponentti = hyvaksymiskirjeetKomponentti;
        this.valintaTulosServiceAsyncResource = valintaTulosServiceAsyncResource;
        this.applicationAsyncResource = applicationAsyncResource;
        this.ataruAsyncResource = ataruAsyncResource;
        this.tarjontaAsyncResource = tarjontaAsyncResource;
        this.organisaatioAsyncResource = organisaatioAsyncResource;
        this.haeOsoiteKomponentti = haeOsoiteKomponentti;
        this.hakuParametritService = hakuParametritService;
        this.pollingIntervalMillis = pollingIntervalMillis;
    }

    @Override
    public void hyvaksymiskirjeetHakemuksille(final KirjeProsessi prosessi,
                                              final HyvaksymiskirjeDTO hyvaksymiskirjeDTO,
                                              final List<String> hakemusOids) {
        try {
            final String organisaatioOid = hyvaksymiskirjeDTO.getTarjoajaOid();
            final String hakuOid = hyvaksymiskirjeDTO.getHakuOid();
            final String hakukohdeOid = hyvaksymiskirjeDTO.getHakukohdeOid();

            ParametritParser haunParametrit = hakuParametritService.getParametritForHaku(hyvaksymiskirjeDTO.getHakuOid());
            Observable<List<HakemusWrapper>> hakemuksetFuture = tarjontaAsyncResource.haeHaku(hakuOid)
                    .flatMap(haku -> StringUtils.isEmpty(haku.getAtaruLomakeAvain())
                            ? applicationAsyncResource.getApplicationsByhakemusOidsInParts(hakuOid, hakemusOids, applicationAsyncResource.DEFAULT_KEYS)
                            : ataruAsyncResource.getApplicationsByOids(hakemusOids));
            Observable<List<HakijaDTO>> hakijatFuture = Observable
                .fromIterable(hakemusOids)
                .concatMap(hakemus -> valintaTulosServiceAsyncResource.getHakijaByHakemus(hakuOid, hakemus))
                .toList()
                .toObservable();
            Observable<Optional<HakutoimistoDTO>> hakutoimistoObservable = organisaatioAsyncResource.haeHakutoimisto(organisaatioOid);

            zip(hakemuksetFuture,
                    hakijatFuture,
                    hakutoimistoObservable,
                    (hakemukset, hakijat, hakutoimisto) -> {
                        LOG.info("Saatiin " + hakijat.size() + "kpl hakemuksia Valintarekisteristä.");
                        LOG.info("Tehdaan valituille hakijoille hyvaksytyt filtterointi.");
                        final Set<String> kohdeHakijat = Sets.newHashSet(hakemusOids);
                        Collection<HakijaDTO> kohdeHakukohteessaHyvaksytyt = hakijat.stream()
                                .filter(new SijoittelussaHyvaksyttyHakija(hakukohdeOid))
                                .filter(h -> kohdeHakijat.contains(h.getHakemusOid()))
                                .collect(Collectors.toList());

                        if (kohdeHakukohteessaHyvaksytyt.isEmpty()) {
                            throw new RuntimeException(String.format("Haun %s hakukohteen %s annetuissa hakemuksista yksikään ei ollut hyväksytty. Kirjeitä ei voitu muodostaa.", hakuOid, hakukohdeOid));
                        }

                        Map<String, MetaHakukohde> hyvaksymiskirjeessaKaytetytHakukohteet = hyvaksymiskirjeetKomponentti.haeKiinnostavatHakukohteet(kohdeHakukohteessaHyvaksytyt);
                        MetaHakukohde kohdeHakukohde = hyvaksymiskirjeessaKaytetytHakukohteet.get(hyvaksymiskirjeDTO.getHakukohdeOid());
                        final boolean iPosti = false;

                        return hyvaksymiskirjeetKomponentti.teeHyvaksymiskirjeet(
                                ImmutableMap.of(organisaatioOid, hakutoimisto.map(h -> Hakijapalvelu.osoite(h, kohdeHakukohde.getHakukohteenKieli())).orElse(Optional.empty())),
                                hyvaksymiskirjeessaKaytetytHakukohteet,
                                kohdeHakukohteessaHyvaksytyt, hakemukset,
                                hyvaksymiskirjeDTO.getHakuOid(),
                                Optional.empty(),
                                hyvaksymiskirjeDTO.getSisalto(),
                                hyvaksymiskirjeDTO.getTag(),
                                hyvaksymiskirjeDTO.getTemplateName(),
                                parsePalautusPvm(hyvaksymiskirjeDTO.getPalautusPvm(), haunParametrit),
                                parsePalautusAika(hyvaksymiskirjeDTO.getPalautusAika(), haunParametrit),
                                iPosti
                        );
                    })
                    .subscribeOn(Schedulers.newThread())
                    .subscribe(
                            letterBatch -> letterBatchToViestintapalvelu().accept(new Object[] { letterBatch, prosessi, hyvaksymiskirjeDTO }),
                            throwable -> logErrorAndKeskeyta(prosessi, throwable, hyvaksymiskirjeDTO.getHakuOid(), hyvaksymiskirjeDTO.getHakukohdeOid()));
        } catch (Throwable t) {
            LOG.error("Hyväksymiskirjeiden luonti hakemuksille haussa {} keskeytyi poikkeukseen: ", hyvaksymiskirjeDTO.getHakuOid(), t);
            prosessi.keskeyta(Poikkeus.koostepalvelupoikkeus(t.getMessage()));
        }
    }

    @Override
    public void jalkiohjauskirjeHakukohteelle(final KirjeProsessi prosessi, final HyvaksymiskirjeDTO hyvaksymiskirjeDTO) {
        Observable<List<HakemusWrapper>> hakemuksetObservable = tarjontaAsyncResource.haeHaku(hyvaksymiskirjeDTO.getHakuOid())
                .flatMap(haku -> StringUtils.isEmpty(haku.getAtaruLomakeAvain())
                        ? applicationAsyncResource.getApplicationsByOidsWithPOST(hyvaksymiskirjeDTO.getHakuOid(), Collections.singletonList(hyvaksymiskirjeDTO.getHakukohdeOid()))
                        : ataruAsyncResource.getApplicationsByHakukohde(hyvaksymiskirjeDTO.getHakukohdeOid()));
        Observable<HakijaPaginationObject> hakijatObservable = valintaTulosServiceAsyncResource.getKaikkiHakijat(hyvaksymiskirjeDTO.getHakuOid(), hyvaksymiskirjeDTO.getHakukohdeOid());
        Observable<Response> organisaatioObservable = organisaatioAsyncResource.haeOrganisaatio(hyvaksymiskirjeDTO.getTarjoajaOid());
        zip(
                hakemuksetObservable,
                hakijatObservable,
                organisaatioObservable,
                (hakemukset, hakijat, organisaatioResponse) -> {
                    LOG.error("Tehdaan hakukohteeseen valitsemattomille filtterointi. Saatiin hakijoita {}", hakijat.getResults().size());
                    Collection<HakijaDTO> hylatyt = hakijat.getResults().stream()
                            .filter(HyvaksymiskirjeetServiceImpl::haussaHylatty)
                            .collect(Collectors.toList());

                    if (hylatyt.isEmpty()) {
                        LOG.error("Hakukohteessa {} ei ole jälkiohjattavia hakijoita!", hyvaksymiskirjeDTO.getHakukohdeOid());
                        throw new RuntimeException("Hakukohteessa " + hyvaksymiskirjeDTO.getHakukohdeOid() + " ei ole jälkiohjattavia hakijoita!");
                    }

                    Map<String, MetaHakukohde> hyvaksymiskirjeessaKaytetytHakukohteet = hyvaksymiskirjeetKomponentti.haeKiinnostavatHakukohteet(hylatyt);
                    MetaHakukohde kohdeHakukohde = hyvaksymiskirjeessaKaytetytHakukohteet.get(hyvaksymiskirjeDTO.getHakukohdeOid());
                    List<String> tarjoajaOidList = newArrayList(Arrays.asList(hyvaksymiskirjeDTO.getTarjoajaOid()));
                    Osoite hakijapalveluidenOsoite = OsoiteHaku.organisaatioResponseToHakijapalveluidenOsoite(haeOsoiteKomponentti, organisaatioAsyncResource, tarjoajaOidList,
                            kohdeHakukohde.getHakukohteenKieli(), organisaatioResponse);
                    final boolean iPosti = false;
                    return hyvaksymiskirjeetKomponentti.teeJalkiohjauskirjeet(
                            ImmutableMap.of(hyvaksymiskirjeDTO.getTarjoajaOid(),Optional.ofNullable(hakijapalveluidenOsoite)),
                            hyvaksymiskirjeessaKaytetytHakukohteet,
                            hylatyt, hakemukset,
                            hyvaksymiskirjeDTO.getHakukohdeOid(),
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
                .subscribeOn(Schedulers.newThread())
                .subscribe(
                        letterBatch -> letterBatchToViestintapalvelu().accept(new Object[] { letterBatch, prosessi, hyvaksymiskirjeDTO }),
                        throwable -> logErrorAndKeskeyta(prosessi, throwable, hyvaksymiskirjeDTO.getHakuOid(), hyvaksymiskirjeDTO.getHakukohdeOid()));
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
        final String organisaatioOid = hyvaksymiskirjeDTO.getTarjoajaOid();
        String hakukohdeOid = hyvaksymiskirjeDTO.getHakukohdeOid();
        String hakuOid = hyvaksymiskirjeDTO.getHakuOid();

        Observable<List<HakemusWrapper>> hakemuksetObservable = tarjontaAsyncResource.haeHaku(hakuOid)
                .flatMap(haku -> StringUtils.isEmpty(haku.getAtaruLomakeAvain())
                        ? applicationAsyncResource.getApplicationsByOidsWithPOST(hakuOid, Collections.singletonList(hakukohdeOid))
                        : ataruAsyncResource.getApplicationsByHakukohde(hakukohdeOid));
        Observable<HakijaPaginationObject> hakijatObservable = valintaTulosServiceAsyncResource.getKoulutuspaikalliset(hakuOid, hakukohdeOid);
        Observable<Optional<HakutoimistoDTO>> hakutoimistoObservable = organisaatioAsyncResource.haeHakutoimisto(organisaatioOid);
        ParametritParser haunParametrit = hakuParametritService.getParametritForHaku(hakuOid);

        zip(hakemuksetObservable,
                hakijatObservable,
                hakutoimistoObservable,
                (hakemukset, hakijat, hakutoimisto) -> {
                    LOG.info("Tehdaan hakukohteeseen valituille hyvaksytyt filtterointi. {}", hakutoimisto);
                    Collection<HakijaDTO> kohdeHakukohteessaHyvaksytyt = hakijat.getResults().stream()
                            .filter(new SijoittelussaHyvaksyttyHakija(hakukohdeOid))
                            .collect(Collectors.toList());

                    if (kohdeHakukohteessaHyvaksytyt.isEmpty()) {
                        throw new RuntimeException(String.format("Yhtään hyväksyttyä hakemusta ei löytynyt haun %s hakukohteessa %s. Kirjeitä ei voitu muodostaa.", hakuOid, hakukohdeOid));
                    }

                    Map<String, MetaHakukohde> hyvaksymiskirjeessaKaytetytHakukohteet = hyvaksymiskirjeetKomponentti.haeKiinnostavatHakukohteet(kohdeHakukohteessaHyvaksytyt);
                    MetaHakukohde kohdeHakukohde = hyvaksymiskirjeessaKaytetytHakukohteet.get(hakukohdeOid);
                    final boolean iPosti = false;
                    return hyvaksymiskirjeetKomponentti.teeHyvaksymiskirjeet(
                            ImmutableMap.of(organisaatioOid, hakutoimisto.flatMap(h -> Hakijapalvelu.osoite(h, kohdeHakukohde.getHakukohteenKieli()))),
                            hyvaksymiskirjeessaKaytetytHakukohteet,
                            kohdeHakukohteessaHyvaksytyt, hakemukset,
                            hakuOid,
                            Optional.empty(),
                            hyvaksymiskirjeDTO.getSisalto(),
                            hyvaksymiskirjeDTO.getTag(),
                            hyvaksymiskirjeDTO.getTemplateName(),
                            parsePalautusPvm(hyvaksymiskirjeDTO.getPalautusPvm(), haunParametrit),
                            parsePalautusAika(hyvaksymiskirjeDTO.getPalautusAika(), haunParametrit),
                            iPosti);
                })
                .subscribeOn(Schedulers.newThread())
                .subscribe(
                        letterBatch -> letterBatchToViestintapalvelu().accept(new Object[] { letterBatch, prosessi, hyvaksymiskirjeDTO }),
                        throwable -> logErrorAndKeskeyta(prosessi, throwable, hakuOid, hakukohdeOid));
    }

    private void logErrorAndKeskeyta(KirjeProsessi prosessi, Throwable throwable, String hakuOid, String hakukohdeOid) {
        LOG.error(String.format("Sijoittelu tai hakemuspalvelukutsu epaonnistui (haku %s, hakukohde %s)", hakuOid, hakukohdeOid), throwable);
        keskeytaProsessi(prosessi, throwable);
    }

    // LetterBatch, KirjeProsessi, HyvaksymiskirjeDTO
    private Consumer<Object[]> letterBatchToViestintapalvelu() {
        return arr -> {
            LetterBatch letterBatch = (LetterBatch) arr[0];
            KirjeProsessi prosessi = (KirjeProsessi) arr[1];
            HyvaksymiskirjeDTO kirje = (HyvaksymiskirjeDTO) arr[2];
            try {
                if (prosessi.isKeskeytetty()) {
                    LOG.warn("Hyväksymiskirjeiden luonti on keskeytetty kayttajantoimesta ennen niiden siirtoa viestintäpalveluun!");
                    return;
                }
                LOG.info("Aloitetaan hyvaksymiskirjeiden vienti viestintäpalveluun! Kirjeita {} kpl", letterBatch.getLetters().size());
                final LetterResponse batchId;
                try {
                    batchId = viestintapalveluAsyncResource.viePdfJaOdotaReferenssiObservable(letterBatch)
                        .timeout(164, SECONDS)
                        .toFuture()
                        .get(165L, SECONDS);
                } catch (Exception e) {
                    LOG.error("Viestintapalvelukutsu epaonnistui virheeseen", e);
                    throw new RuntimeException(e);
                }

                int timesToPoll = (int) (VIESTINTAPALVELUN_MAKSIMI_POLLAUS_AIKA.toMillis() / pollingIntervalMillis);
                LOG.info(String.format("Saatiin hyvaksymiskirjeiden seurantaId %s ja aloitetaan valmistumisen pollaus! " +
                        "(Timeout %s, pollausväli %s ms, pollataan %s kertaa)"
                    , batchId.getBatchId(), DurationFormatUtils.formatDurationHMS(VIESTINTAPALVELUN_MAKSIMI_POLLAUS_AIKA.toMillis()), pollingIntervalMillis, timesToPoll));

                prosessi.vaiheValmistui();
                if (batchId.getStatus().equals(LetterResponse.STATUS_SUCCESS)) {
                    PublishSubject<String> stop = PublishSubject.create();
                    Observable
                            .interval(pollingIntervalMillis, TimeUnit.MILLISECONDS)
                            .take(timesToPoll)
                            .takeUntil(stop)
                            .subscribe(
                                    pulse -> {
                                        try {
                                            LOG.info("Tehdaan status kutsu seurantaId:lle {}", batchId);
                                            LetterBatchStatusDto status = viestintapalveluAsyncResource.haeStatusObservable(batchId.getBatchId())
                                                .timeout(899, MILLISECONDS)
                                                .toFuture()
                                                .get(900L, TimeUnit.MILLISECONDS);
                                            if (prosessi.isKeskeytetty()) {
                                                String msg = "Hyvaksymiskirjeiden muodostuksen seuranta on keskeytetty kayttajantoimesta!";
                                                LOG.warn(msg);
                                                stop.onNext(msg);
                                                return;
                                            }
                                            if ("error".equals(status.getStatus())) {
                                                String msg = "Hyvaksymiskirjeiden muodostuksen seuranta paattyi viestintapalvelun sisaiseen virheeseen!";
                                                LOG.error(msg);
                                                prosessi.keskeyta();
                                                stop.onNext(msg);
                                            }
                                            if ("ready".equals(status.getStatus())) {
                                                prosessi.vaiheValmistui();
                                                String msg = "Hyvaksymiskirjeet valmistui!";
                                                LOG.info(msg);
                                                prosessi.valmistui(batchId.getBatchId());
                                                stop.onNext(msg);
                                            }
                                        } catch (Exception e) {
                                            LOG.error("Statuksen haku epaonnistui", e);
                                        }

                                    }, throwable -> {
                                        keskeytaProsessi(prosessi, throwable);
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

    private void keskeytaProsessi(KirjeProsessi prosessi, Throwable t) {
        String syy = t.getMessage();
        if (StringUtils.isNotBlank(syy)) {
            prosessi.keskeyta(syy);
        } else {
            prosessi.keskeyta();
        }
    }
}
