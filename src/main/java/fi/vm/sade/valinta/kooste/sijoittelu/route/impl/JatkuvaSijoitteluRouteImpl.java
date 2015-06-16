package fi.vm.sade.valinta.kooste.sijoittelu.route.impl;

import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultExchange;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

import fi.vm.sade.valinta.kooste.Reititys;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.SijoitteleAsyncResource;
import fi.vm.sade.valinta.kooste.sijoittelu.dto.DelayedSijoittelu;
import fi.vm.sade.valinta.kooste.sijoittelu.dto.DelayedSijoitteluExchange;
import fi.vm.sade.valinta.kooste.sijoittelu.komponentti.JatkuvaSijoittelu;
import fi.vm.sade.valinta.kooste.sijoittelu.komponentti.ModuloiPaivamaaraJaTunnit;
import fi.vm.sade.valinta.kooste.util.Formatter;
import fi.vm.sade.valinta.seuranta.resource.SijoittelunSeurantaResource;
import fi.vm.sade.valinta.seuranta.sijoittelu.dto.SijoitteluDto;

public class JatkuvaSijoitteluRouteImpl extends RouteBuilder implements JatkuvaSijoittelu {
    private static final Logger LOG = LoggerFactory.getLogger(JatkuvaSijoitteluRouteImpl.class);
    private final String DEADLETTERCHANNEL = "direct:jatkuvan_sijoittelun_deadletterchannel";
    private final SijoitteleAsyncResource sijoitteluAsyncResource;
    private final SijoittelunSeurantaResource sijoittelunSeurantaResource;
    private final String jatkuvaSijoitteluTimer;
    private final String jatkuvaSijoitteluQueue;
    private final DelayQueue<DelayedSijoitteluExchange> jatkuvaSijoitteluDelayedQueue;
    private final int DELAY_WHEN_FAILS = (int) TimeUnit.MINUTES.toMillis(45L);
    private final int VAKIO_AJOTIHEYS = 24;
    private final ConcurrentHashMap<String, Long> ajossaHakuOids;

    @Value("${jatkuvasijoittelu.autostart:true}")
    private boolean autoStartup = true;

    @Autowired
    public JatkuvaSijoitteluRouteImpl(
            @Value("timer://jatkuvaSijoitteluTimer?${valintalaskentakoostepalvelu.jatkuvasijoittelu.timer:fixedRate=true&period=5minutes}") String jatkuvaSijoitteluTimer,
            @Value("seda:jatkuvaSijoitteluAjo?purgeWhenStopping=true&waitForTaskToComplete=Never&concurrentConsumers=1&queue=#jatkuvaSijoitteluDelayedQueue") String jatkuvaSijoitteluQueue,
            SijoitteleAsyncResource sijoitteluAsyncResource,
            SijoittelunSeurantaResource sijoittelunSeurantaResource,
            @Qualifier("jatkuvaSijoitteluDelayedQueue") DelayQueue<DelayedSijoitteluExchange> jatkuvaSijoitteluDelayedQueue) {
        this.jatkuvaSijoitteluTimer = jatkuvaSijoitteluTimer;
        this.jatkuvaSijoitteluQueue = jatkuvaSijoitteluQueue;
        this.sijoitteluAsyncResource = sijoitteluAsyncResource;
        this.sijoittelunSeurantaResource = sijoittelunSeurantaResource;
        this.jatkuvaSijoitteluDelayedQueue = jatkuvaSijoitteluDelayedQueue;
        this.ajossaHakuOids = new ConcurrentHashMap<>();
    }

    public JatkuvaSijoitteluRouteImpl(
            String jatkuvaSijoitteluTimer,
            String jatkuvaSijoitteluQueue,
            SijoitteleAsyncResource sijoitteluAsyncResource,
            SijoittelunSeurantaResource sijoittelunSeurantaResource,
            DelayQueue<DelayedSijoitteluExchange> jatkuvaSijoitteluDelayedQueue,
            ConcurrentHashMap<String, Long> ajossaHakuOids) {
        this.jatkuvaSijoitteluTimer = jatkuvaSijoitteluTimer;
        this.jatkuvaSijoitteluQueue = jatkuvaSijoitteluQueue;
        this.sijoitteluAsyncResource = sijoitteluAsyncResource;
        this.sijoittelunSeurantaResource = sijoittelunSeurantaResource;
        this.jatkuvaSijoitteluDelayedQueue = jatkuvaSijoitteluDelayedQueue;
        this.ajossaHakuOids = ajossaHakuOids;
    }

    @Override
    public Collection<DelayedSijoittelu> haeJonossaOlevatSijoittelut() {
        return jatkuvaSijoitteluDelayedQueue.stream()
                .map(d -> d.getDelayedSijoittelu())
                .collect(Collectors.toList());
    }

    public void teeJatkuvaSijoittelu() {
        LOG.info("Jatkuvansijoittelun ajastin kaynnistyi");
        Map<String, SijoitteluDto> aktiivisetSijoittelut = getAktiivisetSijoittelut();
        LOG.info("Jatkuvansijoittelun ajastin sai seurannalta {} aktiivista sijoittelua.", aktiivisetSijoittelut.size());
        poistaYritetytJaahylta();
        poistaSammutetutTaiJoidenAjankohtaEiOleViela(aktiivisetSijoittelut);
        laitaAjoon(aktiivisetSijoittelut);
    }

    private void laitaAjoon(Map<String, SijoitteluDto> aktiivisetSijoittelut) {
        aktiivisetSijoittelut.forEach((hakuOid, sijoitteluDto) -> {
            boolean hakuEiJonossa = jatkuvaSijoitteluDelayedQueue.stream().filter(j -> hakuOid.equals(j.getHakuOid())).distinct().count() == 0L;
            if (!ajossaHakuOids.containsKey(hakuOid) && hakuEiJonossa) {
                DateTime asetusAjankohta = aloitusajankohtaTaiNyt(sijoitteluDto);
                Integer intervalli = ajotiheysTaiVakio(sijoitteluDto.getAjotiheys());
                DateTime suoritusAjankohta = ModuloiPaivamaaraJaTunnit.moduloiSeuraava(asetusAjankohta, DateTime.now(), intervalli);
                LOG.info("Jatkuva sijoittelu haulle {} joka on asetettu {} intervallilla {} laitetaan suoritettavaksi seuraavan kerran {}",
                        hakuOid, Formatter.paivamaara(asetusAjankohta.toDate()), intervalli, Formatter.paivamaara(suoritusAjankohta.toDate()));
                jatkuvaSijoitteluDelayedQueue.add(new DelayedSijoitteluExchange(new DelayedSijoittelu(hakuOid, suoritusAjankohta), new DefaultExchange(getContext())));
            }
        });
    }

    private void poistaSammutetutTaiJoidenAjankohtaEiOleViela(Map<String, SijoitteluDto> aktiivisetSijoittelut) {
        jatkuvaSijoitteluDelayedQueue
                .forEach(d -> {
                    // Poistetaan tyojonosta passiiviset sijoittelut
                    if (!aktiivisetSijoittelut.containsKey(d.getDelayedSijoittelu().getHakuOid())) {
                        jatkuvaSijoitteluDelayedQueue.remove(d);
                        LOG.warn("Sijoittelu haulle {} poistettu ajastuksesta {}. Joko aloitusajankohtaa siirrettiin tulevaisuuteen tai jatkuvasijoittelu ei ole enaa aktiivinen haulle.",
                                d.getDelayedSijoittelu().getHakuOid(), Formatter.paivamaara(new Date(d.getDelayedSijoittelu().getWhen())));
                    }
                    if (ajossaHakuOids.containsKey(d.getDelayedSijoittelu().getHakuOid())) {
                        LOG.info("Sijoittelu haulle {} poistettu ajastuksesta {}. Ylimaarainen sijoitteluajo tai joko parhaillaan ajossa tai epaonnistunut.",
                                d.getDelayedSijoittelu().getHakuOid(), Formatter.paivamaara(new Date(d.getDelayedSijoittelu().getWhen())));
                        jatkuvaSijoitteluDelayedQueue.remove(d);
                    }
                });
    }

    private void poistaYritetytJaahylta() {
        ajossaHakuOids.forEach((hakuOid, activationTime) -> {
            DateTime activated = new DateTime(activationTime);
            DateTime expires = new DateTime(activationTime).plusMillis(DELAY_WHEN_FAILS);
            boolean vanheneekoNyt = expires.isBeforeNow() || expires.isEqualNow();
            LOG.debug("Aktivoitu {} ja vanhenee {} vanheneeko nyt {}", Formatter.paivamaara(activated.toDate()), Formatter.paivamaara(expires.toDate()), vanheneekoNyt);
            if (vanheneekoNyt) {
                LOG.debug("Jaahy haulle {} vanhentui", hakuOid);
                ajossaHakuOids.remove(hakuOid);
            }
        });
    }

    private Map<String, SijoitteluDto> getAktiivisetSijoittelut() {
        return sijoittelunSeurantaResource
                    .hae().stream().filter(Objects::nonNull)
                    .filter(sijoitteluDto -> {
                        return sijoitteluDto.isAjossa();
                    })
                    .filter(sijoitteluDto -> {
                        DateTime aloitusajankohtaTaiNyt = aloitusajankohtaTaiNyt(sijoitteluDto);
                        // jos aloitusajankohta on jo mennyt tai
                        // se on nyt niin sijoittelu on
                        // aktiivinen sen osalta
                        return laitetaankoJoTyoJonoonEliEnaaTuntiJaljellaAktivointiin(aloitusajankohtaTaiNyt);
                    })
                    .collect(Collectors.toMap(s -> s.getHakuOid(), s -> s));
    }

    @Override
    public void configure() throws Exception {
        from(DEADLETTERCHANNEL)
                .routeId("Jatkuvan sijoittelun deadletterchannel")
                .process(exchange -> LOG.error("Jatkuvasijoittelu paattyi virheeseen {}\r\n{}",
                        simple("${exception.message}").evaluate(exchange, String.class),
                        simple("${exception.stacktrace}").evaluate(exchange, String.class)))
                .stop();

        //
        // Tarkistaa onko tilanne muuttunut ja laittaa delay jonoon puuttuvat
        // sijoittelut
        //
        from(jatkuvaSijoitteluTimer)
                .errorHandler(deadLetterChannel(DEADLETTERCHANNEL))
                .routeId("Jatkuvan sijoittelun ajastin")
                .autoStartup(autoStartup)
                .process(exchange -> teeJatkuvaSijoittelu());

        //
        // Vie sijoittelu queuesta toita sijoitteluun sita mukaa kuin vanhenee
        //
        from(jatkuvaSijoitteluQueue)
                .errorHandler(deadLetterChannel(DEADLETTERCHANNEL))
                .routeId("Jatkuvan sijoittelun ajuri")
                .process(
                        Reititys.<DelayedSijoittelu>kuluttaja(sijoitteluHakuOid -> {
                            //
                            // Aloitetaan sijoittelu ainoastaan jos se
                            // ei ole jo ajossa
                            //
                            if (ajossaHakuOids.putIfAbsent(sijoitteluHakuOid.getHakuOid(), System.currentTimeMillis()) == null) {
                                LOG.error("Jatkuvasijoittelu kaynnistyy nyt haulle {}", sijoitteluHakuOid.getHakuOid());
                                sijoitteluAsyncResource.sijoittele(
                                        sijoitteluHakuOid.getHakuOid(),
                                        done -> {
                                            LOG.warn("Jatkuva sijoittelu saatiin tehtya haulle {}", sijoitteluHakuOid.getHakuOid());
                                            sijoittelunSeurantaResource.merkkaaSijoittelunAjetuksi(sijoitteluHakuOid.getHakuOid());
                                            LOG.warn("Jatkuva sijoittelu merkattiin ajetuksi haulle {}", sijoitteluHakuOid.getHakuOid());
                                        },
                                        poikkeus -> {
                                            LOG.error("Jatkuvan sijoittelun suorittaminen ei onnistunut haulle {}. {}", sijoitteluHakuOid.getHakuOid(), poikkeus.getMessage());
                                        });

                            } else {
                                LOG.error("Jatkuvasijoittelu ei kaynnisty haulle {} koska uudelleen kaynnistysviivetta on viela jaljella", sijoitteluHakuOid.getHakuOid());
                            }
                        }));
    }

    public int ajotiheysTaiVakio(Integer ajotiheys) {
        return Optional.ofNullable(ajotiheys).orElse(VAKIO_AJOTIHEYS);
    }

    public boolean laitetaankoJoTyoJonoonEliEnaaTuntiJaljellaAktivointiin(DateTime aloitusAika) {
        return aloitusAika.isBefore(DateTime.now().plusHours(1));
    }

    private DateTime aloitusajankohtaTaiNyt(SijoitteluDto sijoitteluDto) {
        return new DateTime(Optional.ofNullable(sijoitteluDto.getAloitusajankohta()).orElse(new Date()));
    }
}
