package fi.vm.sade.valinta.kooste.kela.route.impl;

import static fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl.DokumenttiUtils.defaultExpirationDate;
import static fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl.DokumenttiUtils.generateId;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.MINUTES;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import fi.vm.sade.organisaatio.resource.api.KelaResource;
import fi.vm.sade.organisaatio.resource.api.TasoJaLaajuusDTO;
import fi.vm.sade.rajapinnat.kela.tkuva.data.TKUVAYHVA;
import fi.vm.sade.rajapinnat.kela.tkuva.util.KelaUtil;
import fi.vm.sade.valinta.kooste.external.resource.dokumentti.DokumenttiAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri.OppijanumerorekisteriAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.AbstractHakukohde;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.Haku;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.ValintaTulosServiceAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.Change;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.Muutoshistoria;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.ValintaTulosServiceDto;
import fi.vm.sade.valinta.kooste.kela.dto.*;
import fi.vm.sade.valinta.kooste.kela.komponentti.HakukohdeSource;
import fi.vm.sade.valinta.kooste.kela.komponentti.LinjakoodiSource;
import fi.vm.sade.valinta.kooste.kela.komponentti.OppilaitosSource;
import fi.vm.sade.valinta.kooste.kela.komponentti.TilaSource;
import fi.vm.sade.valinta.kooste.kela.komponentti.TutkinnontasoSource;
import fi.vm.sade.valinta.kooste.kela.komponentti.impl.HaunTyyppiKomponentti;
import fi.vm.sade.valinta.kooste.kela.komponentti.impl.KelaDokumentinLuontiKomponenttiImpl;
import fi.vm.sade.valinta.kooste.kela.komponentti.impl.KelaHakijaRiviKomponenttiImpl;
import fi.vm.sade.valinta.kooste.kela.komponentti.impl.LinjakoodiKomponentti;
import fi.vm.sade.valinta.kooste.kela.komponentti.impl.OppilaitosKomponentti;
import fi.vm.sade.valinta.kooste.kela.route.KelaRoute;
import fi.vm.sade.valinta.kooste.valvomo.dto.Poikkeus;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class KelaRouteImpl implements KelaRoute {
  private static final Logger LOG = LoggerFactory.getLogger(KelaRouteImpl.class);

  private final KelaHakijaRiviKomponenttiImpl kelaHakijaKomponentti;
  private final KelaDokumentinLuontiKomponenttiImpl kelaDokumentinLuontiKomponentti;
  private final ValintaTulosServiceAsyncResource valintaTulosServiceAsyncResource;
  private final DokumenttiAsyncResource dokumenttiAsyncResource;
  private final HaunTyyppiKomponentti haunTyyppiKomponentti;
  private final OppijanumerorekisteriAsyncResource oppijanumerorekisteriAsyncResource;
  private final OppilaitosKomponentti oppilaitosKomponentti;
  private final TarjontaAsyncResource tarjontaAsyncResource;
  private final LinjakoodiKomponentti linjakoodiKomponentti;
  private final KelaResource kelaResource;

  @Autowired
  public KelaRouteImpl(
      DokumenttiAsyncResource dokumenttiAsyncResource,
      KelaHakijaRiviKomponenttiImpl kelaHakijaKomponentti,
      KelaDokumentinLuontiKomponenttiImpl kelaDokumentinLuontiKomponentti,
      TarjontaAsyncResource tarjontaAsyncResource,
      HaunTyyppiKomponentti haunTyyppiKomponentti,
      OppijanumerorekisteriAsyncResource oppijanumerorekisteriAsyncResource,
      OppilaitosKomponentti oppilaitosKomponentti,
      LinjakoodiKomponentti linjakoodiKomponentti,
      ValintaTulosServiceAsyncResource valintaTulosServiceAsyncResource,
      KelaResource kelaResource) {
    this.valintaTulosServiceAsyncResource = valintaTulosServiceAsyncResource;
    this.oppilaitosKomponentti = oppilaitosKomponentti;
    this.linjakoodiKomponentti = linjakoodiKomponentti;
    this.haunTyyppiKomponentti = haunTyyppiKomponentti;
    this.tarjontaAsyncResource = tarjontaAsyncResource;
    this.dokumenttiAsyncResource = dokumenttiAsyncResource;
    this.kelaHakijaKomponentti = kelaHakijaKomponentti;
    this.kelaDokumentinLuontiKomponentti = kelaDokumentinLuontiKomponentti;
    this.oppijanumerorekisteriAsyncResource = oppijanumerorekisteriAsyncResource;
    this.kelaResource = kelaResource;
  }
  /*
  private void kelaFailed(Exception e) {
    String virhe = null;
    String stacktrace = null;
    try {
      virhe = simple("${exception.message}").evaluate(exchange, String.class);
      stacktrace = simple("${exception.stacktrace}").evaluate(exchange, String.class);
    } catch (Exception e) {
    }
    LOG.error("Keladokumentin luonti paattyi virheeseen! {}\r\n{}", virhe, stacktrace);
    dokumenttiprosessi(exchange)
      .getPoikkeukset()
      .add(new Poikkeus(Poikkeus.KOOSTEPALVELU, "Kela-dokumentin luonti", virhe));
    dokumenttiprosessi(exchange).addException(virhe);
    dokumenttiprosessi(exchange).luovutaUudelleenYritystenKanssa();
  }*/

  private KelaLuontiJaHaut kelaluontiHaeJaValmistaHaku(KelaLuonti luonti) {
    List<Haku> haut = Lists.newArrayList();
    for (String hakuOid : luonti.getHakuOids()) {
      try {
        haut.add(tarjontaAsyncResource.haeHaku(hakuOid).get(5, MINUTES));
      } catch (Exception e) {
        luonti
          .getProsessi()
          .getPoikkeuksetUudelleenYrityksessa()
          .add(new Poikkeus(Poikkeus.TARJONTA, "Haun haku oid:lla.", hakuOid));
        throw new RuntimeException(
          String.format("Haun %s haku epäonnistui", hakuOid), e);
      }
    }
    return new KelaLuontiJaHaut(luonti, haut);
  }
  private KelaLuontiJaHaut kelaluontiTarkistaHaunTyyppi(KelaLuontiJaHaut luontiJaHaut) {
    Collection<Poikkeus> poikkeuksetUudelleenYrityksessa =
      luontiJaHaut.getLuonti().getProsessi().getPoikkeuksetUudelleenYrityksessa();
    if (luontiJaHaut.getHaut().isEmpty()) {
      String virhe = "Ei hakuja luonnissa " + luontiJaHaut.getLuonti().getUuid();
      poikkeuksetUudelleenYrityksessa.add(
        new Poikkeus(
          Poikkeus.KOOSTEPALVELU, virhe, luontiJaHaut.getLuonti().getUuid()));
      throw new RuntimeException(virhe);
    }
    boolean kaikkiHautKk =
      luontiJaHaut.getHaut().stream().allMatch(Haku::isKorkeakouluhaku);
    if (!kaikkiHautKk
      && luontiJaHaut.getHaut().stream().anyMatch(Haku::isKorkeakouluhaku)) {
      String virhe =
        "Annettujen hakujen on kaikkien oltava kk-hakuja tai niistä mikään ei saa olla kk-haku!";
      poikkeuksetUudelleenYrityksessa.add(
        new Poikkeus(
          Poikkeus.KOOSTEPALVELU, virhe, luontiJaHaut.getLuonti().getUuid()));
      throw new RuntimeException(virhe);
    }
    luontiJaHaut.getLuonti().setKkHaku(kaikkiHautKk);
    return new KelaLuontiJaHaut(luontiJaHaut.getLuonti(), luontiJaHaut.getHaut());
  }

  private KelaLuontiJaAbstraktitHaut kelaluontiKeraaHakujenDatat(KelaLuontiJaHaut luontiJaHaut) {
    Collection<KelaAbstraktiHaku> haut = Lists.newArrayList();
    // Varmistetaan etta ainoastaan hyvaksyttyja ja vastaanottaneita
    LOG.info("Filtteroidaan haussa ylimaaraiset hakijat pois keladokumentista!");
    for (Haku haku : luontiJaHaut.getHaut()) {
      LOG.info("haetaan haku:" + haku.oid);
      try {
        Collection<ValintaTulosServiceDto> hakijat =
          valintaTulosServiceAsyncResource
            .getHaunValintatulokset(haku.oid)
            .timeout(30, MINUTES)
            .blockingFirst()
            .stream()
            .filter(
              vts ->
                vts.getHakutoiveet().stream()
                  .filter(
                    h ->
                      h != null
                        && h.getValintatila() != null
                        && h.getVastaanottotila() != null)
                  .anyMatch(
                    hakutoive ->
                      hakutoive.getVastaanottotila().isVastaanottanut()
                        && hakutoive.getValintatila().isHyvaksytty()))
            .collect(Collectors.toList());
        KelaHaku kelahaku =
          new KelaHaku(hakijat, haku, luontiJaHaut.getLuonti().getCache());
        LOG.info("hakijat:" + hakijat.size());
        haut.add(kelahaku);
      } catch (Exception e) {
        LOG.error("Virhe kelaluonnissa!", e);
        luontiJaHaut
          .getLuonti()
          .getProsessi()
          .getPoikkeuksetUudelleenYrityksessa()
          .add(
            new Poikkeus(
              Poikkeus.SIJOITTELU,
              "Vastaanottaneiden haku sijoittelusta epäonnistui haulle, koska: "
                + e.getMessage(),
              haku.oid));
        throw new RuntimeException(e);
      }
    }
    return new KelaLuontiJaAbstraktitHaut(luontiJaHaut.getLuonti(), haut);
  }

  private KelaLuontiJaRivit kelaLuontiJaRivit(KelaLuontiJaAbstraktitHaut luontiJaRivit) {
    List<String> henkiloOidit =
      luontiJaRivit.getHaut().stream()
        .flatMap(h -> h.getPersonOids().stream())
        .distinct()
        .collect(Collectors.toList());
    try {
      oppijanumerorekisteriAsyncResource
        .haeHenkilot(henkiloOidit)
        .get(1, TimeUnit.HOURS)
        .forEach((oid, h) -> luontiJaRivit.getLuonti().getCache().put(oid, h));
    } catch (Exception e) {
      String msg = "Henkilöiden haku oppijanumerorekisteristä epäonnistui";
      luontiJaRivit
        .getLuonti()
        .getProsessi()
        .getPoikkeukset()
        .add(Poikkeus.oppijanumerorekisteripoikkeus(msg));
      throw new RuntimeException(msg, e);
    }
    List<KelaHakijaRivi> rivit = Lists.newArrayList();
    HakukohdeSource hakukohdeSource =
      new HakukohdeSource() {
        Cache<String, AbstractHakukohde> hakukohdeCache =
          CacheBuilder.newBuilder().build();

        public AbstractHakukohde getHakukohdeByOid(String oid) {
          try {
            return hakukohdeCache.get(
              oid, () -> tarjontaAsyncResource.haeHakukohde(oid).get(5, MINUTES));
          } catch (Exception t) {
            LOG.error(
              "Ei saatu tarjonnalta hakukohdetta oidilla {} (/tarjonta-service/rest/hakukohde/...",
              oid,
              t);
            throw new RuntimeException(t);
          }
        }
      };
    LinjakoodiSource linjakoodiSource =
      new LinjakoodiSource() {
        Cache<String, String> linjaCache =
          CacheBuilder.<String, String>newBuilder().build();

        public String getLinjakoodi(String uri) {
          try {
            return linjaCache.get(
              uri, () -> linjakoodiKomponentti.haeLinjakoodi(uri));
          } catch (Throwable t) {
            throw new RuntimeException(t);
          }
        }
      };
    OppilaitosSource oppilaitosSource =
      new OppilaitosSource() {
        Cache<String, String> numeroCache =
          CacheBuilder.<String, String>newBuilder().build();

        public String getOppilaitosnumero(String tarjoajaOid) {
          try {
            return numeroCache.get(
              tarjoajaOid,
              () -> oppilaitosKomponentti.haeOppilaitosnumero(tarjoajaOid));
          } catch (Throwable t) {
            throw new RuntimeException(t);
          }
        }
      };

    TutkinnontasoSource tutkinnontasoSource =
      new TutkinnontasoSource() {
        Map<String, TasoJaLaajuusDTO> c = Maps.newHashMap();
        Map<String, String> d = Maps.newHashMap();

        @Override
        public TasoJaLaajuusDTO getTutkinnontaso(String hakukohdeOid) {
          if (!c.containsKey(hakukohdeOid)) {
            try {
              TasoJaLaajuusDTO tutkinnontaso =
                kelaResource.tutkinnontaso(hakukohdeOid);
              LOG.info(
                "Tutkinnon tasokoodi hakukohteelle {} oli {}",
                hakukohdeOid,
                tutkinnontaso.getTasoCode());
              c.put(hakukohdeOid, tutkinnontaso);
              return tutkinnontaso;
            } catch (Exception e) {
              LOG.error(
                "Ei saatu kela-rajapinnalta tutkinnon tasoa hakukohteelle"
                  + hakukohdeOid,
                e);
              throw e;
            }
          } else {
            return c.get(hakukohdeOid);
          }
        }

        @Override
        public String getKoulutusaste(String hakukohdeOid) {
          if (!d.containsKey(hakukohdeOid)) {
            try {
              String koulutusaste = kelaResource.koulutusaste(hakukohdeOid);
              if (!koulutusaste.startsWith("ERROR")) {
                d.put(hakukohdeOid, kelaResource.koulutusaste(hakukohdeOid));
              } else {
                throw new RuntimeException(
                  "rajapinta palautti koulutusasteen ERROR");
              }

            } catch (Exception e) {
              LOG.error(
                "Ei saatu kela-rajapinnalta koulutusastetta hakukohteelle"
                  + hakukohdeOid,
                e);
              throw e;
            }
          }
          return d.get(hakukohdeOid);
        }
      };
    TilaSource tilaSource =
      (hakemusOid, hakuOid, hakukohdeOid, valintatapajonoOid) -> {
        final int maxTries = 20;
        for (int tries = 0; tries < maxTries; ++tries) {
          try {
            List<Muutoshistoria> muutoshistoriat =
              valintaTulosServiceAsyncResource
                .getMuutoshistoria(hakemusOid, valintatapajonoOid)
                .timeout(30, MINUTES)
                .blockingFirst();

            final Predicate<Change> isVastaanottoChange =
              (change) -> "vastaanottotila".equals(change.getField());
            final Predicate<Map.Entry<String, Date>> isVastaanotto =
              entry ->
                asList(
                  "VASTAANOTTANUT_SITOVASTI",
                  "VASTAANOTTANUT",
                  "EHDOLLISESTI_VASTAANOTTANUT")
                  .contains(entry.getKey());

            Optional<Map.Entry<String, Date>> newestVastaanottoFieldStatus =
              muutoshistoriat.stream()
                .flatMap(
                  m ->
                    m.getChanges().stream()
                      .filter(isVastaanottoChange)
                      .map(
                        c ->
                          Maps.immutableEntry(
                            c.getTo(),
                            Date.from(
                              m.getTimestamp().toInstant()))))
                .sorted(
                  Comparator.<Map.Entry<String, Date>, Date>comparing(
                      Map.Entry::getValue)
                    .reversed())
                .findFirst();

            return newestVastaanottoFieldStatus
              .filter(isVastaanotto)
              .map(Map.Entry::getValue)
              .orElse(null);
          } catch (Exception e) {
            LOG.error(
              "tilaResource ei jaksa palvella {}. Yritetään vielä uudestaan. "
                + tries
                + "/20...",
              e);
            try {
              Thread.sleep(10000L);
            } catch (InterruptedException e1) {
            }
          }
        }
        throw new RuntimeException(
          "Tried "
            + maxTries
            + " times to get muutoshistoriat from VTS and gave up!");
      };
    for (KelaAbstraktiHaku kelahaku : luontiJaRivit.getHaut()) {
      rivit.addAll(
        kelahaku.createHakijaRivit(
          luontiJaRivit.getLuonti().getAlkuPvm(),
          luontiJaRivit.getLuonti().getLoppuPvm(),
          kelahaku.getHaku().oid,
          luontiJaRivit.getLuonti().getProsessi(),
          luontiJaRivit.getLuonti().getCache(),
          hakukohdeSource,
          linjakoodiSource,
          oppilaitosSource,
          tutkinnontasoSource,
          tilaSource));
    }

    if (rivit.isEmpty()) {
      String virhe =
        "Kela-dokumenttia ei voi luoda hauille joissa ei ole yhtään valittua hakijaa!";
      luontiJaRivit
        .getLuonti()
        .getProsessi()
        .getPoikkeuksetUudelleenYrityksessa()
        .add(new Poikkeus(Poikkeus.KOOSTEPALVELU, virhe));
      throw new RuntimeException(virhe);
    }
    luontiJaRivit.getLuonti().getProsessi().setKokonaistyo(rivit.size() + 1);
    return new KelaLuontiJaRivit(luontiJaRivit.getLuonti(), rivit);
  }

  private KelaLuontiJaDokumentti kelaLuontiJaDokumentti(KelaLuontiJaRivit luontiJaRivit) {
    Collection<TKUVAYHVA> rivit =
      luontiJaRivit.getRivit().stream()
        .map(
          rivi -> {
            try {
              return kelaHakijaKomponentti.luo(rivi);
            } catch (Throwable t) {
              LOG.error(
                "Rivin luonti haussa {} ja hakukohteessa {} olevalle hakemukselle {} epäonnistui.",
                rivi.getHakuOid(),
                rivi.getHakukohde(),
                rivi.getHakemusOid(),
                t);
              throw t;
            } finally {
              luontiJaRivit.getLuonti().getProsessi().inkrementoiTehtyjaToita();
            }
          })
        .collect(Collectors.toList());

    try {
      SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM");
      return new KelaLuontiJaDokumentti(
        luontiJaRivit.getLuonti(),
        kelaDokumentinLuontiKomponentti.luo(
          rivit,
          (luontiJaRivit.getLuonti().getAineistonNimi()
            + "                          ")
            .substring(0, 26)
            + "["
            + DATE_FORMAT.format(luontiJaRivit.getLuonti().getAlkuPvm())
            + "-"
            + DATE_FORMAT.format(luontiJaRivit.getLuonti().getLoppuPvm())
            + "]",
          luontiJaRivit.getLuonti().getOrganisaationNimi(),
          (luontiJaRivit.getLuonti().isKkHaku() ? "OUHARE" : "OUYHVA")));
    } catch (Exception e) {
      String virhe = "Kela-dokumenttia ei saatu luotua!";
      luontiJaRivit
        .getLuonti()
        .getProsessi()
        .getPoikkeuksetUudelleenYrityksessa()
        .add(new Poikkeus(Poikkeus.KOOSTEPALVELU, virhe));
      throw new RuntimeException(virhe);
    }
  }

  private String vientiDokumenttipalveluun(KelaLuontiJaDokumentti luontiJaDokumentti) {
    String id = generateId();
    LOG.info(
      "Aloitetaan keladokumentin(uuid {} ja dokumenttiId) siirtovaihe dokumenttipalveluun.",
      luontiJaDokumentti.getLuonti().getUuid(),
      id);
    try {
      InputStream filedata =
        new ByteArrayInputStream(luontiJaDokumentti.getDokumentti());
      Long expirationTime = defaultExpirationDate().getTime();
      List<String> tags = luontiJaDokumentti.getLuonti().getProsessi().getTags();

      dokumenttiAsyncResource
        .tallenna(
          id,
          luontiJaDokumentti.getLuonti().isKkHaku()
            ? KelaUtil.createTiedostoNimiOuhare(new Date())
            : KelaUtil.createTiedostoNimiYhva14(new Date()),
          expirationTime,
          tags,
          "application/octet-stream",
          filedata)
        .subscribe(
          ok -> {
            luontiJaDokumentti.getLuonti().getProsessi().setDokumenttiId(id);
            luontiJaDokumentti
              .getLuonti()
              .getProsessi()
              .inkrementoiTehtyjaToita();
            LOG.info("DONE");
          });
    } catch (Exception e) {
      luontiJaDokumentti
        .getLuonti()
        .getProsessi()
        .getPoikkeuksetUudelleenYrityksessa()
        .add(
          new Poikkeus(
            Poikkeus.DOKUMENTTIPALVELU,
            "Kela-dokumentin tallennus dokumenttipalveluun epäonnistui"));
      LOG.error("Virhetilanne", e);
    }
    return id;
  }

  public void aloitaKelaLuonti(KelaProsessi prosessi, KelaLuonti kelaLuonti) {
    prosessi.setKasittelyssa();
    prosessi.setKokonaistyo(1);
    try {
      String id = Optional.of(kelaLuonti)
        .map(this::kelaluontiHaeJaValmistaHaku)
        .map(this::kelaluontiTarkistaHaunTyyppi)
        .map(this::kelaluontiKeraaHakujenDatat)
        .map(this::kelaLuontiJaRivit)
        .map(this::kelaLuontiJaDokumentti)
        .map(this::vientiDokumenttipalveluun)
        .get();
      prosessi.setDokumenttiId(id);
      prosessi.inkrementoiTehtyjaToita();
    }catch (Exception e) {
      LOG.error("Virhe kela prosessissa!", e);
      prosessi.luovutaUudelleenYritystenKanssa();
    }
  }

}
