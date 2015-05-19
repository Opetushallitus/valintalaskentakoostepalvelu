package fi.vm.sade.valinta.kooste.valintalaskentatulos.service;

import com.google.common.collect.Sets;
import fi.vm.sade.service.valintaperusteet.dto.HakukohdeJaValintakoeDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintakoeDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteDTO;
import fi.vm.sade.tarjonta.service.resources.dto.HakukohdeDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.valinta.kooste.external.resource.dokumentti.DokumenttiAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.ApplicationAdditionalDataDTO;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.KoodistoCachedAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.dto.Koodi;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaValintakoeAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.function.SynkronoituLaskuri;
import fi.vm.sade.valinta.kooste.function.SynkronoituToiminto;
import fi.vm.sade.valinta.kooste.valintalaskentatulos.komponentti.ValintalaskentaTulosExcelKomponentti;
import fi.vm.sade.valinta.kooste.valvomo.dto.Poikkeus;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.DokumenttiProsessi;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Teksti;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeOsallistuminenDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintatieto.HakemusOsallistuminenDTO;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Jussi Jartamo
 */
@Service
public class ValintakoekutsutExcelService {
    private static final Logger LOG = LoggerFactory
            .getLogger(ValintakoekutsutExcelService.class);

    private final ValintalaskentaValintakoeAsyncResource valintalaskentaAsyncResource;
    //private final ValintatietoResource valintatietoService;
    private final ValintaperusteetAsyncResource valintaperusteetValintakoeResource;
    private final ApplicationAsyncResource applicationResource;
    private final KoodistoCachedAsyncResource koodistoCachedAsyncResource;
    private final TarjontaAsyncResource tarjontaAsyncResource;
    private final DokumenttiAsyncResource dokumenttiAsyncResource;
    private final ValintalaskentaTulosExcelKomponentti valintalaskentaTulosExcelKomponentti
            = new ValintalaskentaTulosExcelKomponentti();

    @Autowired
    public ValintakoekutsutExcelService(
            ValintalaskentaValintakoeAsyncResource valintalaskentaAsyncResource,
            ValintaperusteetAsyncResource valintaperusteetValintakoeResource,
            ApplicationAsyncResource applicationResource,
            KoodistoCachedAsyncResource koodistoCachedAsyncResource,
            TarjontaAsyncResource tarjontaAsyncResource,
            DokumenttiAsyncResource dokumenttiAsyncResource
    ) {
        this.valintalaskentaAsyncResource = valintalaskentaAsyncResource;
        this.valintaperusteetValintakoeResource = valintaperusteetValintakoeResource;
        this.applicationResource = applicationResource;
        this.koodistoCachedAsyncResource = koodistoCachedAsyncResource;
        this.tarjontaAsyncResource = tarjontaAsyncResource;
        this.dokumenttiAsyncResource = dokumenttiAsyncResource;
    }

    public void luoExcel(DokumenttiProsessi prosessi, String hakuOid, String hakukohdeOid, List<String> valintakoeTunnisteet,
                         Set<String> hakemusOids) {
        Consumer<Throwable> poikkeuskasittelija = poikkeus -> {
            LOG.error("Valintakoekutsut excelin luonnissa tapahtui poikkeus:", poikkeus);
            prosessi.getPoikkeukset().add(
                    new Poikkeus(Poikkeus.KOOSTEPALVELU,
                            "Valintakoekutsut excelin luonnissa tapahtui poikkeus:", poikkeus.getMessage()));
        };
        try {
            prosessi.setKokonaistyo(
                    8
                            // luonti
                            + 1
                            // dokumenttipalveluun vienti
                            + 1);
            final boolean useWhitelist = !Optional.ofNullable(hakemusOids).orElse(Collections.emptySet()).isEmpty();
            final AtomicReference<HakuV1RDTO> hakuRef = new AtomicReference<>();
            final AtomicReference<HakukohdeDTO> hakukohdeRef = new AtomicReference<>();
            final AtomicReference<List<HakemusOsallistuminenDTO>> tiedotHakukohteelleRef = new AtomicReference<>();
            final AtomicReference<Map<String, ValintakoeDTO>> valintakokeetRef = new AtomicReference<>();
            final AtomicReference<List<Hakemus>> haetutHakemuksetRef = new AtomicReference<>(Collections.emptyList());
            final Consumer<List<Hakemus>> lisaaHakemuksiaAtomisestiHakemuksetReferenssiin = hakemuksia -> {
                haetutHakemuksetRef.getAndUpdate(vanhatHakemukset -> {
                    return Stream.concat(vanhatHakemukset.stream(), hakemuksia.stream()).collect(Collectors.toList());
                });
            };
            final AtomicReference<Map<String,Koodi>> maatJaValtiot1Ref = new AtomicReference<>();
            final AtomicReference<Map<String,Koodi>> postiRef = new AtomicReference<>();
            final SynkronoituLaskuri laskuri = SynkronoituLaskuri.builder()
                    .setLaskurinAlkuarvo(8)
                    .setSuoritaJokaKerta(() -> {
                        prosessi.inkrementoiTehtyjaToita();
                    })
                    .setSynkronoituToiminto(() -> {
                        String hakuNimi = new Teksti(hakuRef.get().getNimi()).getTeksti();
                        String hakukohdeNimi = new Teksti(hakukohdeRef.get().getHakukohdeNimi()).getTeksti();
                        try {
                            InputStream filedata = valintalaskentaTulosExcelKomponentti.luoTuloksetXlsMuodossa(
                                    hakuNimi,
                                    hakukohdeNimi,
                                    hakukohdeOid,
                                    maatJaValtiot1Ref.get(),
                                    postiRef.get(),
                                    tiedotHakukohteelleRef.get(),
                                    valintakokeetRef.get(),
                                    haetutHakemuksetRef.get().stream().distinct().collect(Collectors.toList()),
                                    Optional.ofNullable(hakemusOids).orElse(Collections.emptySet())
                            );
                            prosessi.inkrementoiTehtyjaToita();
                            String id = UUID.randomUUID().toString();
                            long expirationDate = DateTime.now().plusHours(168).toDate().getTime();

                            dokumenttiAsyncResource.tallenna(
                                    id, "valintakoekutsut.xls", expirationDate, prosessi.getTags(), "application/vnd.ms-excel", filedata,
                                    ok -> {
                                        prosessi.inkrementoiTehtyjaToita();
                                        prosessi.setDokumenttiId(id);
                                    }, poikkeuskasittelija);
                        } catch (Throwable t) {
                            poikkeuskasittelija.accept(t);
                        }
                    }).build();
            koodistoCachedAsyncResource.haeKoodisto(KoodistoCachedAsyncResource.MAAT_JA_VALTIOT_1, maatJaValtiot1 -> {
                maatJaValtiot1Ref.set(maatJaValtiot1);
                laskuri.vahennaLaskuriaJaJosValmisNiinSuoritaToiminto();
            }, poikkeuskasittelija);
            koodistoCachedAsyncResource.haeKoodisto(KoodistoCachedAsyncResource.POSTI, posti -> {
                postiRef.set(posti);
                laskuri.vahennaLaskuriaJaJosValmisNiinSuoritaToiminto();
            }, poikkeuskasittelija);
            if (useWhitelist) {
                // haetaan whitelistin hakemukset
                applicationResource.getApplicationsByOids(hakemusOids, hakemukset -> {
                    lisaaHakemuksiaAtomisestiHakemuksetReferenssiin.accept(hakemukset);
                    laskuri.vahennaLaskuriaJaJosValmisNiinSuoritaToiminto();
                }, poikkeuskasittelija);
            } else {
                // vaihtoehtoisesti haetaan hakemukset kun tiedetaan osallistujat laskennan tuloksista
            }
            valintaperusteetValintakoeResource.haeValintakokeetHakukohteelle(
                    hakukohdeOid,
                    valintakokeet -> {
                        List<ValintakoeDTO> kiinnostavatValintakokeet = valintakokeet.stream().filter(v -> valintakoeTunnisteet.contains(v.getSelvitettyTunniste()))
                                .collect(Collectors.toList());
                        valintakokeetRef.set(kiinnostavatValintakokeet.stream().collect(Collectors.toMap(v -> v.getSelvitettyTunniste(), v -> v)));
                        boolean onkoJossainValintakokeessaKaikkiHaetaan =
                                kiinnostavatValintakokeet.stream().anyMatch(vk -> Boolean.TRUE.equals(vk.getKutsutaankoKaikki()));
                        // estetään ettei haeta kahteen kertaan kaikkia hakemuksia ja siten tuplata muistin käyttöä. joissain hakukohteissa on tuhansia hakemuksia ja hakemusten koko voi olla megatavuja.
                        final SynkronoituLaskuri voikoHakeaJoOsallistujienHakemuksetVaiOnkoKaikkienHakemustenHakuKesken = SynkronoituLaskuri.builder()
                                .setLaskurinAlkuarvo(2)
                                .setSynkronoituToiminto(() -> {
                                    if (!useWhitelist) {
                                        Set<String> osallistujienHakemusOids = Sets.newHashSet(tiedotHakukohteelleRef.get().stream()
                                                .filter(o -> hakukohdeOid.equals(o.getHakukohdeOid()))
                                                .map(o -> o.getHakemusOid()).collect(Collectors.toSet()));
                                        Set<String> joHaetutHakemukset = haetutHakemuksetRef.get().stream().map(h -> h.getOid()).collect(
                                                Collectors.toSet()
                                        );
                                        osallistujienHakemusOids.removeAll(joHaetutHakemukset); // ei haeta jo haettuja hakemuksia
                                        // haetaan osallistujille hakemukset
                                        applicationResource.getApplicationsByOids(osallistujienHakemusOids, hakemukset -> {
                                            lisaaHakemuksiaAtomisestiHakemuksetReferenssiin.accept(hakemukset);
                                            laskuri.vahennaLaskuriaJaJosValmisNiinSuoritaToiminto();
                                        }, poikkeuskasittelija);
                                    }
                                    laskuri.vahennaLaskuriaJaJosValmisNiinSuoritaToiminto();
                                }).build();

                        if(onkoJossainValintakokeessaKaikkiHaetaan && !useWhitelist) {
                            applicationResource.getApplicationsByOid(hakuOid,hakukohdeOid,hakemukset -> {
                                lisaaHakemuksiaAtomisestiHakemuksetReferenssiin.accept(hakemukset);
                                laskuri.vahennaLaskuriaJaJosValmisNiinSuoritaToiminto();
                                voikoHakeaJoOsallistujienHakemuksetVaiOnkoKaikkienHakemustenHakuKesken.vahennaLaskuriaJaJosValmisNiinSuoritaToiminto();
                            },poikkeuskasittelija);
                        } else {
                            laskuri.vahennaLaskuriaJaJosValmisNiinSuoritaToiminto();
                            voikoHakeaJoOsallistujienHakemuksetVaiOnkoKaikkienHakemustenHakuKesken.vahennaLaskuriaJaJosValmisNiinSuoritaToiminto();
                        }
                        valintalaskentaAsyncResource.haeValintatiedotHakukohteelle(hakukohdeOid,
                                kiinnostavatValintakokeet.stream().map(v -> v.getSelvitettyTunniste()).collect(Collectors.toList()), osallistuminen -> {
                                    List<HakemusOsallistuminenDTO> hakukohteeseenOsallistujat = osallistuminen.stream()
                                            .filter(o -> hakukohdeOid.equals(o.getHakukohdeOid()))
                                            .collect(Collectors.toList());
                                    tiedotHakukohteelleRef.set(hakukohteeseenOsallistujat);
                                    voikoHakeaJoOsallistujienHakemuksetVaiOnkoKaikkienHakemustenHakuKesken.vahennaLaskuriaJaJosValmisNiinSuoritaToiminto();
                                }, poikkeuskasittelija);
                        laskuri.vahennaLaskuriaJaJosValmisNiinSuoritaToiminto();
                    },
                    poikkeuskasittelija
            );

            tarjontaAsyncResource.haeHakukohde(hakuOid, hakukohdeOid, hakukohde -> {
                hakukohdeRef.set(hakukohde);
                laskuri.vahennaLaskuriaJaJosValmisNiinSuoritaToiminto();
            }, poikkeuskasittelija);
            tarjontaAsyncResource.haeHaku(hakuOid, haku -> {
                hakuRef.set(haku);
                laskuri.vahennaLaskuriaJaJosValmisNiinSuoritaToiminto();
            }, poikkeuskasittelija);

        } catch (Throwable t) {
            poikkeuskasittelija.accept(t);
        }
    }
}
