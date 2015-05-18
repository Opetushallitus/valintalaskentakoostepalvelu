package fi.vm.sade.valinta.kooste.viestintapalvelu.service;

import com.google.common.collect.Sets;
import com.google.gson.GsonBuilder;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO;
import fi.vm.sade.valinta.kooste.external.resource.dokumentti.DokumenttiAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.KoodistoCachedAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.dto.Koodi;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.SijoitteleAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.SijoitteluAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaValintakoeAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.ViestintapalveluAsyncResource;
import fi.vm.sade.valinta.kooste.function.SynkronoituLaskuri;
import fi.vm.sade.valinta.kooste.valvomo.dto.Poikkeus;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.DokumenttiProsessi;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoitteet;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Teksti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.HaeOsoiteKomponentti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.predicate.SijoittelussaHyvaksyttyHakija;
import fi.vm.sade.valintalaskenta.domain.dto.OsallistuminenDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeOsallistuminenDTO;
import fi.vm.sade.valintalaskenta.domain.valintakoe.Osallistuminen;
import org.apache.poi.util.IOUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * @author Jussi Jartamo
 */
@Service
public class OsoitetarratService {

    private final static Logger LOG = LoggerFactory.getLogger(OsoitetarratService.class);
    private final ApplicationAsyncResource applicationAsyncResource;
    private final HaeOsoiteKomponentti osoiteKomponentti;
    private final KoodistoCachedAsyncResource koodistoCachedAsyncResource;
    private final DokumenttiAsyncResource dokumenttiAsyncResource;
    private final ViestintapalveluAsyncResource viestintapalveluAsyncResource;
    private final ValintalaskentaValintakoeAsyncResource valintalaskentaValintakoeAsyncResource;
    private final ValintaperusteetAsyncResource valintaperusteetValintakoeResource;
    private final SijoitteluAsyncResource sijoitteluAsyncResource;

    @Autowired
    public OsoitetarratService(ApplicationAsyncResource applicationAsyncResource,
                               KoodistoCachedAsyncResource koodistoCachedAsyncResource,
                               DokumenttiAsyncResource dokumenttiAsyncResource,
                               ViestintapalveluAsyncResource viestintapalveluAsyncResource,
                               ValintaperusteetAsyncResource valintaperusteetValintakoeResource,
                               SijoitteluAsyncResource sijoitteluAsyncResource,
                               ValintalaskentaValintakoeAsyncResource valintalaskentaValintakoeAsyncResource,
                               HaeOsoiteKomponentti osoiteKomponentti) {
        this.applicationAsyncResource = applicationAsyncResource;
        this.koodistoCachedAsyncResource = koodistoCachedAsyncResource;
        this.dokumenttiAsyncResource = dokumenttiAsyncResource;
        this.viestintapalveluAsyncResource = viestintapalveluAsyncResource;
        this.valintaperusteetValintakoeResource = valintaperusteetValintakoeResource;
        this.valintalaskentaValintakoeAsyncResource = valintalaskentaValintakoeAsyncResource;
        this.sijoitteluAsyncResource = sijoitteluAsyncResource;
        this.osoiteKomponentti = osoiteKomponentti;
    }

    public void osoitetarratSijoittelussaHyvaksytyille(
            DokumenttiProsessi prosessi,
            String hakuOid,
            String hakukohdeOid) {
        Consumer<Throwable> poikkeuskasittelija = poikkeuskasittelija(prosessi);
        try {
            LOG.error("Luodaan osoitetarrat sijoittelussa hyväksytyille (haku={}, hakukohde={})", hakuOid, hakukohdeOid);
            prosessi.setKokonaistyo(
                    3 // luonti
                    + 1
                    // dokumenttipalveluun vienti
                    + 1);
            final AtomicReference<List<Hakemus>> haetutHakemuksetRef = new AtomicReference<>();
            final AtomicReference<Map<String, Koodi>> maatJaValtiot1Ref = new AtomicReference<>();
            final AtomicReference<Map<String, Koodi>> postiRef = new AtomicReference<>();
            final SynkronoituLaskuri laskuri = SynkronoituLaskuri.builder()
                    .setLaskurinAlkuarvo(3)
                    .setSuoritaJokaKerta(() -> {
                        prosessi.inkrementoiTehtyjaToita();
                    })
                    .setSynkronoituToiminto(() -> {
                        osoitetarratHakemuksille(haetutHakemuksetRef.get(), maatJaValtiot1Ref.get(),postiRef.get(), prosessi);
                    }).build();
            maatJaValtiot1(laskuri, maatJaValtiot1Ref, poikkeuskasittelija);
            posti(laskuri, postiRef, poikkeuskasittelija);
            sijoitteluAsyncResource.getKoulutuspaikkallisetHakijat(hakuOid, hakukohdeOid, hakijat -> {
                List<String> hyvaksytytHakijat =
                    hakijat.getResults().stream().filter(new SijoittelussaHyvaksyttyHakija(hakukohdeOid))
                            .map(h -> h.getHakemusOid())
                        .collect(Collectors.toList());
                boolean onkoHyvaksyttyjaHakijoita = !hyvaksytytHakijat.isEmpty();
                if(onkoHyvaksyttyjaHakijoita) {
                    applicationAsyncResource.getApplicationsByOids(hyvaksytytHakijat, hakemukset -> {
                        haetutHakemuksetRef.set(hakemukset);
                        laskuri.vahennaLaskuriaJaJosValmisNiinSuoritaToiminto();
                    },poikkeuskasittelija);
                } else {
                    LOG.error("Sijoittelussa ei ole hyväksyttyjä hakijoita");
                    prosessi.getPoikkeukset().add(
                            new Poikkeus(Poikkeus.KOOSTEPALVELU,
                                    "Osoitetarrojen luonti epäonnistui:", "Sijoittelussa ei ole hyväksyttyjä hakijoita"));
                }
            }, poikkeuskasittelija);
        } catch (Throwable t) {
            poikkeuskasittelija.accept(t);
        }

    }
    public void osoitetarratValintakokeeseenOsallistujille(
            DokumenttiProsessi prosessi,
            String hakuOid,
            String hakukohdeOid,
            Set<String> selvitetytTunnisteet) {
        Consumer<Throwable> poikkeuskasittelija = poikkeuskasittelija(prosessi);
        try {
            LOG.error("Luodaan osoitetarrat valintakokeeseen osallistujille (haku={}, hakukohde={})", hakuOid, hakukohdeOid);
            prosessi.setKokonaistyo(
                5
                        // luonti
                        + 1
                        // dokumenttipalveluun vienti
                        + 1);
            final AtomicReference<List<ValintakoeOsallistuminenDTO>> osallistumistiedotRef = new AtomicReference<>();
            final AtomicReference<List<Hakemus>> haetutHakemuksetRef = new AtomicReference<>();
            final AtomicReference<Map<String, Koodi>> maatJaValtiot1Ref = new AtomicReference<>();
            final AtomicReference<Map<String, Koodi>> postiRef = new AtomicReference<>();
            final SynkronoituLaskuri laskuri = SynkronoituLaskuri.builder()
                    .setLaskurinAlkuarvo(
                            2 // maat ja valtiot + posti
                            +
                            1 // ulkopuoliset hakijat
                    )
                    .setSuoritaJokaKerta(() -> {
                        prosessi.inkrementoiTehtyjaToita();
                    })
                    .setSynkronoituToiminto(() -> {
                        osoitetarratHakemuksille(haetutHakemuksetRef.get(), maatJaValtiot1Ref.get(),postiRef.get(), prosessi);
                    }).build();
            final SynkronoituLaskuri laskuriHakukohteenUlkopuolisilleHakijoille = SynkronoituLaskuri.builder()
                    .setLaskurinAlkuarvo(
                            1 // Kaikki hakijat jos kaikki kutsutaan
                                    +
                                    1 // Osallistumistiedot mistä selviää hakukohteen ulkopuoliset hakijat
                    )
                    .setSuoritaJokaKerta(() -> {
                        prosessi.inkrementoiTehtyjaToita();
                    })
                    .setSynkronoituToiminto(() -> {
                        // Haetaan ulkopuoliset hakijat
                        List<ValintakoeOsallistuminenDTO> osallistumistiedot = osallistumistiedotRef.get();
                        Set<String> valintakokeisiinOsallistujienHakemusOidit =
                                osallistumistiedot.stream()
                                        .filter(o ->
                                                o.getHakutoiveet().stream().anyMatch(h ->
                                                        hakukohdeOid.equals(h.getHakukohdeOid()) && h.getValinnanVaiheet().stream()
                                                                .anyMatch(v ->
                                                                        v.getValintakokeet().stream()
                                                                                .anyMatch(vk ->
                                                                                        selvitetytTunnisteet.contains(vk.getValintakoeTunniste()) &&
                                                                                                Osallistuminen.OSALLISTUU.equals(vk.getOsallistuminenTulos().getOsallistuminen())))))
                                        .map(o -> o.getHakemusOid()).collect(Collectors.toSet());

                        Set<String> mahdollisestiHakukohteenHakemusOidit =
                                haetutHakemuksetRef.get().stream().map(h -> h.getOid()).collect(Collectors.toSet());
                        if (mahdollisestiHakukohteenHakemusOidit.containsAll(valintakokeisiinOsallistujienHakemusOidit)) {
                            laskuri.vahennaLaskuriaJaJosValmisNiinSuoritaToiminto();
                        } else {
                            Set<String> puuttuvatHakemusOidit = Sets.newHashSet(valintakokeisiinOsallistujienHakemusOidit);
                            puuttuvatHakemusOidit.removeAll(mahdollisestiHakukohteenHakemusOidit);
                            applicationAsyncResource.getApplicationsByOids(puuttuvatHakemusOidit, puuttuvatHakemukset -> {
                                haetutHakemuksetRef.set(Stream.concat(haetutHakemuksetRef.get().stream(), puuttuvatHakemukset.stream()).collect(Collectors.toList()));
                                laskuri.vahennaLaskuriaJaJosValmisNiinSuoritaToiminto();
                            }, poikkeuskasittelija);
                        }
                    }).build();
            maatJaValtiot1(laskuri, maatJaValtiot1Ref, poikkeuskasittelija);
            posti(laskuri, postiRef, poikkeuskasittelija);

            valintaperusteetValintakoeResource.haeValintakokeetHakukohteelle(hakukohdeOid, valintakokeet -> {
                boolean kutsutaankoJossainKokeessaKaikki = valintakokeet.stream().anyMatch(vk -> selvitetytTunnisteet.contains(vk.getSelvitettyTunniste()) && Boolean.TRUE.equals(vk.getKutsutaankoKaikki()));
                if (kutsutaankoJossainKokeessaKaikki) {
                    applicationAsyncResource.getApplicationsByOid(hakuOid, hakukohdeOid, hakemukset -> {
                        haetutHakemuksetRef.set(hakemukset);
                        laskuriHakukohteenUlkopuolisilleHakijoille.vahennaLaskuriaJaJosValmisNiinSuoritaToiminto();
                    }, poikkeuskasittelija);
                } else {
                    haetutHakemuksetRef.set(Collections.emptyList());
                    laskuriHakukohteenUlkopuolisilleHakijoille.vahennaLaskuriaJaJosValmisNiinSuoritaToiminto();
                }
            }, poikkeuskasittelija);
            valintalaskentaValintakoeAsyncResource.haeHakutoiveelle(hakukohdeOid, osallistumiset -> {
                osallistumistiedotRef.set(osallistumiset);
                laskuriHakukohteenUlkopuolisilleHakijoille.vahennaLaskuriaJaJosValmisNiinSuoritaToiminto();
            }, poikkeuskasittelija);
        } catch(Throwable t) {
            poikkeuskasittelija.accept(t);
        }
    }

    public void osoitetarratHakemuksille(
            DokumenttiProsessi prosessi,
            List<String> hakemusOids) {
        Consumer<Throwable> poikkeuskasittelija = poikkeuskasittelija(prosessi);
        try {
            LOG.error("Luodaan osoitetarrat hakemuksille (size={})", hakemusOids.size());
            prosessi.setKokonaistyo(
                    3
                            // luonti
                            + 1
                            // dokumenttipalveluun vienti
                            + 1);
            final AtomicReference<List<Hakemus>> haetutHakemuksetRef = new AtomicReference<>();
            final AtomicReference<Map<String,Koodi>> maatJaValtiot1Ref = new AtomicReference<>();
            final AtomicReference<Map<String,Koodi>> postiRef = new AtomicReference<>();
            final SynkronoituLaskuri laskuri = SynkronoituLaskuri.builder()
                    .setLaskurinAlkuarvo(3)
                    .setSuoritaJokaKerta(() -> {
                        prosessi.inkrementoiTehtyjaToita();
                    })
                    .setSynkronoituToiminto(() -> {
                        osoitetarratHakemuksille(haetutHakemuksetRef.get(), maatJaValtiot1Ref.get(),postiRef.get(), prosessi);
                    }).build();
            maatJaValtiot1(laskuri, maatJaValtiot1Ref, poikkeuskasittelija);
            posti(laskuri, postiRef, poikkeuskasittelija);
            applicationAsyncResource.getApplicationsByOids(hakemusOids, hakemukset -> {
                haetutHakemuksetRef.set(hakemukset);
                laskuri.vahennaLaskuriaJaJosValmisNiinSuoritaToiminto();
            }, poikkeuskasittelija);
        } catch (Throwable t) {
            poikkeuskasittelija.accept(t);
        }
    }

    private void osoitetarratHakemuksille(
            List<Hakemus> haetutHakemukset,
            Map<String,Koodi> maatJaValtiot1,
            Map<String,Koodi> posti,
            DokumenttiProsessi prosessi) {
        Consumer<Throwable> poikkeuskasittelija = poikkeuskasittelija(prosessi);
        try {
            Osoitteet osoitteet =
                    new Osoitteet(
                            haetutHakemukset.stream().map(h ->
                                    osoiteKomponentti.haeOsoite(maatJaValtiot1, posti, h))
                                    .collect(Collectors.toList()));
            // Aakkosjarjestykseen
            osoitteet.getAddressLabels().sort(
                    (o1, o2) -> {
                        int i = Optional.ofNullable(o1.getFirstName()).orElse("").compareTo(Optional.ofNullable(o2.getFirstName()).orElse(""));
                        if(i == 0) {
                            return Optional.ofNullable(o1.getLastName()).orElse("").compareTo(Optional.ofNullable(o2.getLastName()).orElse(""));
                        } else {
                            return i;
                        }
                    }
            );
            //LOG.error("{}", new GsonBuilder().setPrettyPrinting().create().toJson(osoitteet));
            viestintapalveluAsyncResource.haeOsoitetarrat(osoitteet, response -> {
                prosessi.inkrementoiTehtyjaToita();
                String id = UUID.randomUUID().toString();
                try {
                    InputStream inputStream = pipeInputStreams((InputStream)response.getEntity());
                    dokumenttiAsyncResource.tallenna(id, "osoitetarrat.pdf",
                            defaultExpirationDate().getTime(), prosessi.getTags(), "application/pdf",
                            inputStream
                            , ok -> {
                                prosessi.inkrementoiTehtyjaToita();
                                prosessi.setDokumenttiId(id);
                            }, poikkeus -> {
                                LOG.error("Osoitetarrojen luonti epäonnistui dokumentin tallennukseen:", poikkeus);
                                prosessi.getPoikkeukset().add(
                                        new Poikkeus(Poikkeus.KOOSTEPALVELU,
                                                "Osoitetarrojen tallennus epäonnistui:", poikkeus.getMessage()));
                            });
                } catch(Throwable t) {
                    poikkeuskasittelija.accept(t);
                }
            }, poikkeus -> {
                LOG.error("Osoitetarrojen luonti epäonnistui viestintäpalvelukutsuun:", poikkeus);
                prosessi.getPoikkeukset().add(
                        new Poikkeus(Poikkeus.KOOSTEPALVELU,
                                "Osoitetarrojen luonti viestintäpalvelussa epäonnistui:", poikkeus.getMessage()));
            });
        } catch (Throwable t) {
            poikkeuskasittelija.accept(t);
        }
    }
    private Date defaultExpirationDate() {
        return DateTime.now().plusHours(168).toDate(); // almost a day
    }
    private Consumer<Throwable> poikkeuskasittelija(DokumenttiProsessi prosessi) {
        return poikkeus -> {
            LOG.error("Osoitetarrojen luonti epäonnistui:", poikkeus);
            prosessi.getPoikkeukset().add(
                    new Poikkeus(Poikkeus.KOOSTEPALVELU,
                            "Osoitetarrojen luonti epäonnistui:", poikkeus.getMessage()));
        };
    }
    private void maatJaValtiot1(final SynkronoituLaskuri laskuri, AtomicReference<Map<String,Koodi>> maatJaValtiot1Ref, Consumer<Throwable> poikkeuskasittelija) {
        koodistoCachedAsyncResource.haeKoodisto(KoodistoCachedAsyncResource.MAAT_JA_VALTIOT_1, maatJaValtiot1 -> {
            maatJaValtiot1Ref.set(maatJaValtiot1);
            laskuri.vahennaLaskuriaJaJosValmisNiinSuoritaToiminto();
        }, poikkeuskasittelija);
    }
    private void posti(final SynkronoituLaskuri laskuri, AtomicReference<Map<String,Koodi>> postiRef, Consumer<Throwable> poikkeuskasittelija) {
        koodistoCachedAsyncResource.haeKoodisto(KoodistoCachedAsyncResource.POSTI, posti -> {
            postiRef.set(posti);
            laskuri.vahennaLaskuriaJaJosValmisNiinSuoritaToiminto();
        }, poikkeuskasittelija);
    }
    private InputStream pipeInputStreams(InputStream incoming)
            throws IOException {
        byte[] dokumentti = IOUtils.toByteArray(incoming);
        if (dokumentti == null || dokumentti.length == 0) {
            throw new RuntimeException(
                    "Viestintäpalvelu palautti tyhjän dokumentin!");
        }
        InputStream p = new ByteArrayInputStream(dokumentti);
        incoming.close();
        return p;
    }
}
