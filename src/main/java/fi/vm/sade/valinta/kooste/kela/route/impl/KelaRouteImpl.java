package fi.vm.sade.valinta.kooste.kela.route.impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import fi.vm.sade.organisaatio.resource.api.KelaResource;
import fi.vm.sade.rajapinnat.kela.tkuva.data.TKUVAYHVA;
import fi.vm.sade.rajapinnat.kela.tkuva.util.KelaUtil;
import fi.vm.sade.tarjonta.service.resources.HakukohdeResource;
import fi.vm.sade.tarjonta.service.resources.dto.HakukohdeDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.valinta.dokumenttipalvelu.resource.DokumenttiResource;
import fi.vm.sade.valinta.kooste.Reititys;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationResource;
import fi.vm.sade.valinta.kooste.external.resource.haku.HakuV1Resource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.ValintaTulosServiceAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.ValintaTulosServiceDto;
import fi.vm.sade.valinta.kooste.kela.dto.*;
import fi.vm.sade.valinta.kooste.kela.komponentti.*;
import fi.vm.sade.valinta.kooste.kela.komponentti.impl.*;
import fi.vm.sade.valinta.kooste.kela.route.KelaRoute;
import fi.vm.sade.valinta.kooste.sijoittelu.dto.LogEntry;
import fi.vm.sade.valinta.kooste.sijoittelu.dto.Valintatulos;
import fi.vm.sade.valinta.kooste.sijoittelu.resource.TilaResource;
import fi.vm.sade.valinta.kooste.valvomo.dto.Poikkeus;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl.AbstractDokumenttiRouteBuilder;
import org.apache.camel.Endpoint;
import org.apache.camel.builder.DefaultErrorHandlerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import rx.observables.BlockingObservable;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class KelaRouteImpl extends AbstractDokumenttiRouteBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(KelaRouteImpl.class);

    private final int MAKSIMI_MAARA_HAKEMUKSIA_KERRALLA_HAKEMUSPALVELULTA = 10000;

    private final KelaHakijaRiviKomponenttiImpl kelaHakijaKomponentti;
    private final KelaDokumentinLuontiKomponenttiImpl kelaDokumentinLuontiKomponentti;
    private final ValintaTulosServiceAsyncResource valintaTulosServiceAsyncResource;
    private final DokumenttiResource dokumenttiResource;
    private final HaunTyyppiKomponentti haunTyyppiKomponentti;
    private final ApplicationResource applicationResource;
    private final OppilaitosKomponentti oppilaitosKomponentti;
    private final HakuV1Resource hakuResource;
    private final LinjakoodiKomponentti linjakoodiKomponentti;
    private final HakukohdeResource hakukohdeResource;
    private final String kelaLuonti;
    private final KelaResource kelaResource;
    private final TilaResource tilaResource;

    @Autowired
    public KelaRouteImpl(
            @Value(KelaRoute.SEDA_KELA_LUONTI) String kelaLuonti,
            DokumenttiResource dokumenttiResource,
            KelaHakijaRiviKomponenttiImpl kelaHakijaKomponentti,
            KelaDokumentinLuontiKomponenttiImpl kelaDokumentinLuontiKomponentti,
            HakuV1Resource hakuResource,
            HaunTyyppiKomponentti haunTyyppiKomponentti,
            ApplicationResource applicationResource,
            OppilaitosKomponentti oppilaitosKomponentti,
            LinjakoodiKomponentti linjakoodiKomponentti,
            HakukohdeResource hakukohdeResource,
            ValintaTulosServiceAsyncResource valintaTulosServiceAsyncResource,
            KelaResource kelaResource,
            TilaResource tilaResource) {
        this.valintaTulosServiceAsyncResource = valintaTulosServiceAsyncResource;
        this.hakukohdeResource = hakukohdeResource;
        this.oppilaitosKomponentti = oppilaitosKomponentti;
        this.linjakoodiKomponentti = linjakoodiKomponentti;
        this.haunTyyppiKomponentti = haunTyyppiKomponentti;
        this.hakuResource = hakuResource;
        this.kelaLuonti = kelaLuonti;
        this.dokumenttiResource = dokumenttiResource;
        this.kelaHakijaKomponentti = kelaHakijaKomponentti;
        this.kelaDokumentinLuontiKomponentti = kelaDokumentinLuontiKomponentti;
        this.applicationResource = applicationResource;
        this.kelaResource = kelaResource;
        this.tilaResource = tilaResource;
    }

    private DefaultErrorHandlerBuilder deadLetterChannel() {
        return deadLetterChannel(KelaRoute.DIRECT_KELA_FAILED)
                .logExhaustedMessageHistory(true).logExhausted(true)
                .logStackTrace(true).logRetryStackTrace(true).logHandled(true);
    }

    public final void configure() {
        Endpoint haeHakuJaValmistaHaku = endpoint("direct:kelaluonti_hae_ja_valmista_haku");
        Endpoint tarkistaHaunTyyppi = endpoint("direct:kelaluonti_tarkista_haun_tyyppi");
        Endpoint keraaHakujenDatat = endpoint("direct:kelaluonti_keraa_hakujen_datat");
        Endpoint vientiDokumenttipalveluun = endpoint("direct:kelaluonti_vienti_dokumenttipalveluun");

        from(KelaRoute.DIRECT_KELA_FAILED)
                .routeId("KELALUONTI_DEADLETTERCHANNEL")
                .process(exchange -> {
                    String virhe = null;
                    String stacktrace = null;
                    try {
                        virhe = simple("${exception.message}").evaluate(exchange, String.class);
                        stacktrace = simple("${exception.stacktrace}").evaluate(exchange, String.class);
                    } catch (Exception e) {
                    }
                    LOG.error("Keladokumentin luonti paattyi virheeseen! {}\r\n{}", virhe, stacktrace);
                    dokumenttiprosessi(exchange).getPoikkeukset().add(new Poikkeus(Poikkeus.KOOSTEPALVELU, "Kela-dokumentin luonti", virhe));
                    dokumenttiprosessi(exchange).addException(virhe);
                    dokumenttiprosessi(exchange).luovutaUudelleenYritystenKanssa();
                })
                .stop();

        from(haeHakuJaValmistaHaku)
                .routeId("KELALUONTI_HAKU")
                .errorHandler(deadLetterChannel())
                .process(Reititys.<KelaLuonti, KelaLuontiJaHaut>funktio(luonti -> {
                    List<Haku> haut = Lists.newArrayList();
                    for (String hakuOid : luonti.getHakuOids()) {
                        try {
                            haut.add(new TunnistamatonHaku(hakuResource.findByOid(hakuOid).getResult()));
                        } catch (Exception e) {
                            luonti.getProsessi()
                                    .getPoikkeuksetUudelleenYrityksessa()
                                    .add(new Poikkeus(Poikkeus.TARJONTA, "Haun haku oid:lla.", hakuOid));
                            throw e;
                        }
                    }
                    return new KelaLuontiJaHaut(luonti, haut);
                }))
                .to(tarkistaHaunTyyppi);

        from(tarkistaHaunTyyppi)
                .routeId("KELALUONTI_TARKISTA_TYYPPI")
                .errorHandler(deadLetterChannel())
                .process(Reititys.<KelaLuontiJaHaut, KelaLuontiJaHaut>funktio(luontiJaHaut -> {
                    Collection<Poikkeus> poikkeuksetUudelleenYrityksessa =
                            luontiJaHaut.getLuonti().getProsessi().getPoikkeuksetUudelleenYrityksessa();
                    if (luontiJaHaut.getHaut().isEmpty()) {
                        String virhe = "Ei hakuja luonnissa " + luontiJaHaut.getLuonti().getUuid();
                        poikkeuksetUudelleenYrityksessa.add(new Poikkeus(Poikkeus.KOOSTEPALVELU,
                                virhe, luontiJaHaut.getLuonti().getUuid()));
                        throw new RuntimeException(virhe);
                    }
                    KelaCache cache = luontiJaHaut.getLuonti().getCache();
                    Collection<Haku> haut = luontiJaHaut.getHaut().stream()
                            .map(haku -> {
                                updateHauntyyppiCache(haku, cache, poikkeuksetUudelleenYrityksessa);
                                updateHaunKohdejoukkoCache(haku, cache, poikkeuksetUudelleenYrityksessa);
                                HakuV1RDTO asTarjontaHakuDTO = haku.getAsTarjontaHakuDTO();
                                String hakutyypinArvo = cache.getHakutyyppi(asTarjontaHakuDTO.getHakutyyppiUri());
                                String haunKohdejoukonArvo = cache.getHaunKohdejoukko(asTarjontaHakuDTO.getKohdejoukkoUri());

                                // Koodistosta saa hakutyypille arvon ja nimen.
                                // Oletetaan etta nimi voi vaihtua mutta koodi pysyy vakiona.

                                boolean lisahaku = "03".equals(hakutyypinArvo);
                                boolean kkhaku = "12".equals(haunKohdejoukonArvo);
                                return new TunnistettuHaku(asTarjontaHakuDTO, kkhaku, lisahaku);
                            }).collect(Collectors.toList());
                    boolean kaikkiHautKk = haut.stream().allMatch(Haku::isKorkeakouluhaku);
                    if (!kaikkiHautKk && haut.stream().anyMatch(Haku::isKorkeakouluhaku)) {
                        String virhe = "Annettujen hakujen on kaikkien oltava kk-hakuja tai niistä mikään ei saa olla kk-haku!";
                        poikkeuksetUudelleenYrityksessa.add(new Poikkeus(Poikkeus.KOOSTEPALVELU,
                                virhe, luontiJaHaut.getLuonti().getUuid()));
                        throw new RuntimeException(virhe);
                    }
                    luontiJaHaut.getLuonti().setKkHaku(kaikkiHautKk);
                    return new KelaLuontiJaHaut(luontiJaHaut.getLuonti(), haut);
                }))
                .to(keraaHakujenDatat);

        from(keraaHakujenDatat)
                .routeId("KELALUONTI_KERAA_HAKUJEN_DATAT")
                .errorHandler(deadLetterChannel())
                .process(Reititys.<KelaLuontiJaHaut, KelaLuontiJaAbstraktitHaut>funktio(luontiJaHaut -> {
                    Collection<KelaAbstraktiHaku> haut = Lists.newArrayList();
                    // Varmistetaan etta ainoastaan hyvaksyttyja ja vastaanottaneita
                    LOG.info("Filtteroidaan haussa ylimaaraiset hakijat pois keladokumentista!");
                    for (Haku tunnistettuHaku : luontiJaHaut.getHaut()) {
                        HakuV1RDTO haku = tunnistettuHaku.getAsTarjontaHakuDTO();
                        if (haku == null) {
                            throw new RuntimeException("Reitillä oli null hakuDTO!");
                        }
                        log.info("haetaan haku:" + haku.getOid());
                        try {
                            Collection<ValintaTulosServiceDto> hakijat =
                                    BlockingObservable.from(valintaTulosServiceAsyncResource.getHaunValintatulokset(haku.getOid()))
                                            .first()
                                            .stream()
                                            .filter(vts -> vts.getHakutoiveet().stream()
                                                    .filter(h -> h != null && h.getValintatila() != null && h.getVastaanottotila() != null)
                                                    .anyMatch(hakutoive -> hakutoive.getVastaanottotila().isVastaanottanut()
                                                            && hakutoive.getValintatila().isHyvaksytty())
                                            ).collect(Collectors.toList());
                            KelaHaku kelahaku = new KelaHaku(hakijat, haku, luontiJaHaut.getLuonti().getCache());
                            log.info("hakijat:" + hakijat.size());
                            haut.add(kelahaku);
                        } catch (Exception e) {
                            LOG.error("Virhe kelaluonnissa!", e);
                            luontiJaHaut
                                    .getLuonti()
                                    .getProsessi()
                                    .getPoikkeuksetUudelleenYrityksessa()
                                    .add(new Poikkeus(Poikkeus.SIJOITTELU, "Vastaanottaneiden haku sijoittelusta epäonnistui haulle, koska: " + e.getMessage(), haku.getOid()));
                            throw new RuntimeException(e);
                        }
                    }
                    return new KelaLuontiJaAbstraktitHaut(luontiJaHaut.getLuonti(), haut);
                }));

        from(kelaLuonti)
                .errorHandler(deadLetterChannel())
                .routeId("KELALUONTI")
                .to(haeHakuJaValmistaHaku)
                .process(Reititys.<KelaLuontiJaAbstraktitHaut>kuluttaja(luonti -> {
                    Collection<String> hakemusOidit = Sets.newHashSet();
                    for (KelaAbstraktiHaku kelahaku : luonti.getHaut()) {
                        hakemusOidit.addAll(kelahaku.getHakemusOids());
                    }
                    hakemusOidit = Lists.newArrayList(hakemusOidit);
                    try {
                        int n = 0;
                        Collection<List<String>> oiditSivutettuna = Lists
                                .newArrayList();
                        do {
                            List<String> osajoukkoOideista = FluentIterable
                                    .from(hakemusOidit)
                                    .skip(n)
                                    .limit(MAKSIMI_MAARA_HAKEMUKSIA_KERRALLA_HAKEMUSPALVELULTA)
                                    .toList();
                            oiditSivutettuna.add(osajoukkoOideista);
                            n += MAKSIMI_MAARA_HAKEMUKSIA_KERRALLA_HAKEMUSPALVELULTA;
                        } while (n < hakemusOidit.size());
                        List<Hakemus> hakemukset = Lists.newArrayList();
                        LOG.warn("Haetaan {} hakemusta, {} erässä", hakemusOidit.size(), oiditSivutettuna.size());
                        for (List<String> oidit : oiditSivutettuna) {
                            try {
                                List<Hakemus> h = applicationResource.getApplicationsByOids(oidit);
                                hakemukset.addAll(h);
                                LOG.warn("Saatiin erä hakemuksia {}. {}/{}", h.size(), hakemukset.size(), hakemusOidit.size());
                            } catch (Exception e) {
                                LOG.error("Hakemuspalvelu ei jaksa tarjoilla hakemuksia. Yritetään vielä uudestaan.", e);
                                Thread.sleep(50L);
                                hakemukset.addAll(applicationResource.getApplicationsByOids(oidit));
                            }
                        }
                        hakemukset.forEach(luonti.getLuonti().getCache()::put);
                    } catch (Exception e) {
                        String virhe = "Ei saatu hakemuksia hakupalvelulta!";
                        luonti.getLuonti()
                                .getProsessi()
                                .getPoikkeuksetUudelleenYrityksessa()
                                .add(new Poikkeus(Poikkeus.HAKU, virhe));
                        throw new RuntimeException(virhe);
                    }
                }))
                .process(Reititys.<KelaLuontiJaAbstraktitHaut, KelaLuontiJaRivit>funktio(luontiJaRivit -> {
                    List<KelaHakijaRivi> rivit = Lists.newArrayList();
                    HakukohdeSource hakukohdeSource = new HakukohdeSource() {
                        Cache<String, HakukohdeDTO> hakukohdeCache = CacheBuilder.<String, String>newBuilder().build();

                        public HakukohdeDTO getHakukohdeByOid(String oid) {
                            try {
                                return hakukohdeCache.get(oid, () -> hakukohdeResource.getByOID(oid));
                            } catch (Throwable t) {
                                LOG.error("Ei saatu tarjonnalta hakukohdetta oidilla {} (/tarjonta-service/rest/hakukohde/...", oid, t);
                                throw new RuntimeException(t);
                            }
                        }
                    };
                    LinjakoodiSource linjakoodiSource = new LinjakoodiSource() {
                        Cache<String, String> linjaCache = CacheBuilder.<String, String>newBuilder().build();

                        public String getLinjakoodi(String uri) {
                            try {
                                return linjaCache.get(uri, () -> linjakoodiKomponentti.haeLinjakoodi(uri));
                            } catch (Throwable t) {
                                throw new RuntimeException(t);
                            }
                        }
                    };
                    OppilaitosSource oppilaitosSource = new OppilaitosSource() {
                        Cache<String, String> koodiCache = CacheBuilder.<String, String>newBuilder().build();
                        Cache<String, String> numeroCache = CacheBuilder.<String, String>newBuilder().build();

                        public String getOppilaitosKoodi(String tarjoajaOid) {
                            try {
                                return koodiCache.get(tarjoajaOid, () -> oppilaitosKomponentti.haeOppilaitosKoodi(tarjoajaOid));
                            } catch (Throwable t) {
                                throw new RuntimeException(t);
                            }
                        }

                        public String getOppilaitosnumero(String tarjoajaOid) {
                            try {
                                return numeroCache.get(tarjoajaOid, () -> oppilaitosKomponentti.haeOppilaitosnumero(tarjoajaOid));
                            } catch (Throwable t) {
                                throw new RuntimeException(t);
                            }
                        }
                    };

                    TutkinnontasoSource tutkinnontasoSource = new TutkinnontasoSource() {
                        Map<String, String> c = Maps.newHashMap();
                        Map<String, String> d = Maps.newHashMap();

                        @Override
                        public String getTutkinnontaso(String hakukohdeOid) {
                            if (!c.containsKey(hakukohdeOid)) {
                                try {
                                    c.put(hakukohdeOid, kelaResource.tutkinnontaso(hakukohdeOid));
                                } catch (Exception e) {
                                    LOG.error("Ei saatu kela-rajapinnalta tutkinnon tasoa hakukohteelle" + hakukohdeOid, e);
                                    throw e;
                                }
                            }
                            return c.get(hakukohdeOid);
                        }

                        @Override
                        public String getKoulutusaste(String hakukohdeOid) {
                            if (!d.containsKey(hakukohdeOid)) {
                                try {
                                    String koulutusaste = kelaResource.koulutusaste(hakukohdeOid);
                                    if (!koulutusaste.startsWith("ERROR")) {
                                        d.put(hakukohdeOid, kelaResource.koulutusaste(hakukohdeOid));
                                    } else {
                                        throw new RuntimeException("rajapinta palautti koulutusasteen ERROR");
                                    }

                                } catch (Exception e) {
                                    LOG.error("Ei saatu kela-rajapinnalta koulutusastetta hakukohteelle" + hakukohdeOid, e);
                                    throw e;
                                }
                            }
                            return d.get(hakukohdeOid);
                        }
                    };
                    TilaSource tilaSource = (hakemusOid, hakuOid, hakukohdeOid, valintatapajonoOid) -> {
                        Valintatulos valintatulos = null;
                        int tries = 0;

                        while (true) {
                            try {
                                valintatulos = tilaResource.hakemus(hakuOid, hakukohdeOid, valintatapajonoOid, hakemusOid);
                                if (tries > 0) {
                                    LOG.error("retry ok");
                                }
                                break;
                            } catch (Exception e) {
                                if (tries == 20) {
                                    LOG.error("give up");
                                    throw e;
                                }
                                tries++;
                                LOG.error("tilaResource ei jaksa palvella {}. Yritetään vielä uudestaan. " + tries + "/20...", e);
                                try {
                                    Thread.sleep(10000L);
                                } catch (InterruptedException e1) {
                                }
                            }
                        }

                        for (LogEntry logEntry : valintatulos.getLogEntries()) {
                            if (logEntry.getMuutos().trim().toUpperCase().endsWith("VASTAANOTTANUT_SITOVASTI")) {
                                return logEntry;
                            }
                        }
                        for (LogEntry logEntry : valintatulos.getLogEntries()) {
                            if (logEntry.getMuutos().trim().toUpperCase().endsWith("VASTAANOTTANUT")) {
                                return logEntry;
                            }
                        }
                        for (LogEntry logEntry : valintatulos.getLogEntries()) {
                            if (logEntry.getMuutos().trim().toUpperCase().endsWith("EHDOLLISESTI_VASTAANOTTANUT")) {
                                return logEntry;
                            }
                        }
                        LOG.error("No logentries for event VASTAANOTTANUT_SITOVASTI, VASTAANOTTANUT or EHDOLLISESTI_VASTAANOTTANUT for hakuOid:" + hakuOid + ",hakukohdeOid:" + hakukohdeOid + ", valintatapajonoOid:" + valintatapajonoOid + ", hakemusOid:" + hakemusOid + " valintatulos tila:" + valintatulos.getTila());
                        return (LogEntry) null;
                    };
                    for (KelaAbstraktiHaku kelahaku : luontiJaRivit
                            .getHaut()) {
                        rivit.addAll(kelahaku.createHakijaRivit(
                                luontiJaRivit.getLuonti().getAlkuPvm(),
                                luontiJaRivit.getLuonti().getLoppuPvm(),
                                kelahaku.getHaku().getOid(), //TODO_-
                                luontiJaRivit.getLuonti().getProsessi(),
                                luontiJaRivit.getLuonti().getCache(),
                                hakukohdeSource, linjakoodiSource,
                                oppilaitosSource, tutkinnontasoSource, tilaSource));
                    }

                    if (rivit.isEmpty()) {
                        String virhe = "Kela-dokumenttia ei voi luoda hauille joissa ei ole yhtään valittua hakijaa!";
                        luontiJaRivit
                                .getLuonti()
                                .getProsessi()
                                .getPoikkeuksetUudelleenYrityksessa()
                                .add(new Poikkeus(
                                        Poikkeus.KOOSTEPALVELU, virhe));
                        throw new RuntimeException(virhe);
                    }
                    luontiJaRivit.getLuonti().getProsessi().setKokonaistyo(rivit.size() + 1);
                    return new KelaLuontiJaRivit(luontiJaRivit.getLuonti(), rivit);
                }))
                .process(Reititys.<KelaLuontiJaRivit, KelaLuontiJaDokumentti>funktio(luontiJaRivit -> {
                    Collection<TKUVAYHVA> rivit = luontiJaRivit
                            .getRivit()
                            .stream()
                            .map(rivi -> {
                                try {
                                    return kelaHakijaKomponentti.luo(rivi);
                                } catch (Throwable t) {
                                    LOG.error("Rivin luonti haussa {} ja hakukohteessa {} olevalle hakemukselle {} epäonnistui.", rivi.getHakuOid(), rivi.getHakukohde(), rivi.getHakemusOid(), t);
                                    throw t;
                                } finally {
                                    luontiJaRivit.getLuonti()
                                            .getProsessi()
                                            .inkrementoiTehtyjaToita();
                                }
                            }).collect(Collectors.toList());

                    try {
                        SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM");
                        return new KelaLuontiJaDokumentti(
                                luontiJaRivit.getLuonti(),
                                kelaDokumentinLuontiKomponentti
                                        .luo(rivit,
                                                (luontiJaRivit.getLuonti().getAineistonNimi() + "                          ").substring(0, 26)
                                                        + "[" + DATE_FORMAT.format(luontiJaRivit.getLuonti().getAlkuPvm())
                                                        + "-"
                                                        + DATE_FORMAT.format(luontiJaRivit.getLuonti().getLoppuPvm()) + "]",
                                                luontiJaRivit
                                                        .getLuonti()
                                                        .getOrganisaationNimi(),
                                                (luontiJaRivit.getLuonti().isKkHaku() ? "OUHARE" : "OUYHVA")
                                        ));
                    } catch (Exception e) {
                        String virhe = "Kela-dokumenttia ei saatu luotua!";
                        luontiJaRivit
                                .getLuonti()
                                .getProsessi()
                                .getPoikkeuksetUudelleenYrityksessa()
                                .add(new Poikkeus(
                                        Poikkeus.KOOSTEPALVELU, virhe));
                        throw new RuntimeException(virhe);
                    }
                }))
                .to(vientiDokumenttipalveluun);

        from(vientiDokumenttipalveluun)
                .routeId("KELALUONTI_DOKUMENTTIPALVELUUN")
                .errorHandler(deadLetterChannel())
                .process(Reititys.<KelaLuontiJaDokumentti>kuluttaja(luontiJaDokumentti -> {
                    String id = generateId();
                    LOG.info(
                            "Aloitetaan keladokumentin(uuid {} ja dokumenttiId) siirtovaihe dokumenttipalveluun.",
                            luontiJaDokumentti.getLuonti().getUuid(), id);
                    try {
                        InputStream filedata = new ByteArrayInputStream(luontiJaDokumentti.getDokumentti());
                        Long expirationTime = defaultExpirationDate().getTime();
                        List<String> tags = luontiJaDokumentti.getLuonti().getProsessi().getTags();

                        dokumenttiResource.tallenna(id,
                                luontiJaDokumentti.getLuonti().isKkHaku() ? KelaUtil.createTiedostoNimiOuhare(new Date()) : KelaUtil.createTiedostoNimiYhva14(new Date()),
                                expirationTime, tags,
                                "application/octet-stream", filedata);

                        luontiJaDokumentti.getLuonti().getProsessi().setDokumenttiId(id);
                        luontiJaDokumentti.getLuonti().getProsessi().inkrementoiTehtyjaToita();
                        LOG.info("DONE");
                    } catch (Exception e) {
                        luontiJaDokumentti
                                .getLuonti()
                                .getProsessi()
                                .getPoikkeuksetUudelleenYrityksessa()
                                .add(new Poikkeus(
                                        Poikkeus.DOKUMENTTIPALVELU,
                                        "Kela-dokumentin tallennus dokumenttipalveluun epäonnistui"));
                        log.error("Virhetilanne", e);
                    }
                }));
    }

    private void updateHaunKohdejoukkoCache(Haku haku, KelaCache cache, Collection<Poikkeus> poikkeuksetUudelleenYrityksessa) {
        String haunKohdejoukkoUri = haku.getAsTarjontaHakuDTO().getKohdejoukkoUri();
        try {
            if (!cache.containsHaunKohdejoukko(haunKohdejoukkoUri)) {
                cache.putHaunKohdejoukko(haunKohdejoukkoUri, haunTyyppiKomponentti.haunKohdejoukko(haunKohdejoukkoUri));
            }
        } catch (Exception e) {
            poikkeuksetUudelleenYrityksessa.add(new Poikkeus(Poikkeus.KOODISTO,
                    "Haun kohdejoukolle " + haunKohdejoukkoUri + " ei saatu arvoa koodistosta",
                    haunKohdejoukkoUri));
            throw e;
        }
    }

    private void updateHauntyyppiCache(Haku haku, KelaCache cache, Collection<Poikkeus> poikkeuksetUudelleenYrityksessa) {
        String hakutyyppiUri = haku.getAsTarjontaHakuDTO().getHakutyyppiUri();
        try {
            if (!cache.containsHakutyyppi(hakutyyppiUri)) {
                cache.putHakutyyppi(hakutyyppiUri, haunTyyppiKomponentti.haunTyyppi(hakutyyppiUri));
            }
        } catch (Exception e) {
            poikkeuksetUudelleenYrityksessa.add(new Poikkeus(Poikkeus.KOODISTO,
                    "Haun tyypille " + hakutyyppiUri + " ei saatu arvoa koodistosta",
                    hakutyyppiUri));
            throw e;
        }
    }
}
