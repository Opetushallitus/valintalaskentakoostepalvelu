package fi.vm.sade.valinta.kooste.pistesyotto.service;

import com.google.common.collect.Sets;
import fi.vm.sade.service.valintaperusteet.dto.HakukohdeJaValintakoeDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakukohdeV1RDTO;
import fi.vm.sade.valinta.kooste.external.resource.dokumentti.DokumenttiAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.ApplicationAdditionalDataDTO;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.SuoritusrekisteriAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Arvosana;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Oppija;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.HakukohdeHelper;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaValintakoeAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.pistesyotto.excel.PistesyottoExcel;
import fi.vm.sade.valinta.kooste.util.PoikkeusKasittelijaSovitin;
import fi.vm.sade.valinta.kooste.valvomo.dto.Poikkeus;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.DokumenttiProsessi;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Teksti;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeOsallistuminenDTO;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PistesyottoVientiService {
    private static final Logger LOG = LoggerFactory.getLogger(PistesyottoVientiService.class);
    private final ValintalaskentaValintakoeAsyncResource valintakoeResource;
    private final ValintaperusteetAsyncResource valintaperusteetResource;
    private final ApplicationAsyncResource applicationAsyncResource;
    private final TarjontaAsyncResource tarjontaAsyncResource;
    private final DokumenttiAsyncResource dokumenttiAsyncResource;
    private final SuoritusrekisteriAsyncResource suoritusrekisteriAsyncResource;

    @Autowired
    public PistesyottoVientiService(
            ValintalaskentaValintakoeAsyncResource valintakoeResource,
            ValintaperusteetAsyncResource valintaperusteetResource,
            ApplicationAsyncResource applicationAsyncResource,
            TarjontaAsyncResource tarjontaAsyncResource,
            DokumenttiAsyncResource dokumenttiAsyncResource,
            SuoritusrekisteriAsyncResource suoritusrekisteriAsyncResource) {
        this.dokumenttiAsyncResource = dokumenttiAsyncResource;
        this.tarjontaAsyncResource = tarjontaAsyncResource;
        this.valintakoeResource = valintakoeResource;
        this.valintaperusteetResource = valintaperusteetResource;
        this.applicationAsyncResource = applicationAsyncResource;
        this.suoritusrekisteriAsyncResource = suoritusrekisteriAsyncResource;
    }

    private void vie(String hakuOid, String hakukohdeOid, DokumenttiProsessi prosessi, List<ValintakoeOsallistuminenDTO> osallistumistiedot,
                     List<Hakemus> hakemukset, List<ValintaperusteDTO> valintaperusteet, List<ApplicationAdditionalDataDTO> pistetiedot,
                     List<HakukohdeJaValintakoeDTO> hakukohdeJaValintakoe, HakuV1RDTO hakuV1RDTO, HakukohdeV1RDTO hakukohdeDTO, List<Oppija> oppijat) {
        Consumer<Throwable> poikkeuskasittelija = poikkeus -> {
            LOG.error("Pistesyötön viennissä tapahtui poikkeus:", poikkeus);
            prosessi.getPoikkeukset().add(new Poikkeus(Poikkeus.KOOSTEPALVELU, "Pistesyötön vienti", poikkeus.getMessage()));
        };
        try {
            String hakuNimi = new Teksti(hakuV1RDTO.getNimi()).getTeksti();
            String tarjoajaOid = HakukohdeHelper.tarjoajaOid(hakukohdeDTO);
            String hakukohdeNimi = new Teksti(hakukohdeDTO.getHakukohteenNimet()).getTeksti();
            String tarjoajaNimi = new Teksti(hakukohdeDTO.getTarjoajaNimet()).getTeksti();
            Collection<String> valintakoeTunnisteet = valintakoeTunnisteet(valintaperusteet);

            Map<String, List<Arvosana>> kielikoeArvosanat = AbstractPistesyottoKoosteService.ammatillisenKielikoeArvosanat(oppijat);

            Set<String> kaikkiKutsutaanTunnisteet = hakukohdeJaValintakoe.stream()
                    .flatMap(h -> h.getValintakoeDTO().stream())
                    .filter(v -> Boolean.TRUE.equals(v.getKutsutaankoKaikki()))
                    .map(v -> v.getTunniste())
                    .collect(Collectors.toSet());

            InputStream xlsx = new PistesyottoExcel(hakuOid, hakukohdeOid, tarjoajaOid, hakuNimi, hakukohdeNimi,
                    tarjoajaNimi, hakemukset, kaikkiKutsutaanTunnisteet, valintakoeTunnisteet, osallistumistiedot,
                    valintaperusteet, pistetiedot, kielikoeArvosanat).getExcel().vieXlsx();

            prosessi.inkrementoiTehtyjaToita();
            String id = UUID.randomUUID().toString();
            dokumenttiAsyncResource.tallenna(id, "pistesyotto.xlsx", defaultExpirationDate().getTime(), prosessi.getTags(),
                    "application/octet-stream", xlsx, response -> {
                        prosessi.setDokumenttiId(id);
                        prosessi.inkrementoiTehtyjaToita();
                    }, poikkeuskasittelija);
        } catch (Throwable t) {
            poikkeuskasittelija.accept(t);
        }
    }

    private Collection<String> valintakoeTunnisteet(List<ValintaperusteDTO> valintaperusteet) {
        return valintaperusteet.stream().map(v -> v.getTunniste()).collect(Collectors.toList());
    }

    public void vie(String hakuOid, String hakukohdeOid, DokumenttiProsessi prosessi) {
        PoikkeusKasittelijaSovitin poikkeuskasittelija = new PoikkeusKasittelijaSovitin(poikkeus -> {
            LOG.error("Pistesyötön viennissä tapahtui poikkeus:", poikkeus);
            prosessi.getPoikkeukset().add(new Poikkeus(Poikkeus.KOOSTEPALVELU, "Pistesyötön vienti", poikkeus.getMessage()));
        });
        try {
            prosessi.setKokonaistyo(8
                            // luonti
                            + 1
                            // dokumenttipalveluun vienti
                            + 1);
            AtomicReference<List<ValintakoeOsallistuminenDTO>> osallistumistiedotRef = new AtomicReference<>();
            AtomicReference<List<Hakemus>> hakemusRef = new AtomicReference<>();
            AtomicReference<List<ValintaperusteDTO>> valintaperusteRef = new AtomicReference<>();
            AtomicReference<List<ApplicationAdditionalDataDTO>> lisatiedotRef = new AtomicReference<>();
            AtomicReference<List<HakukohdeJaValintakoeDTO>> hakukohdeJaValintakoeRef = new AtomicReference<>();
            AtomicReference<HakuV1RDTO> hakuRef = new AtomicReference<>();
            AtomicReference<HakukohdeV1RDTO> hakukohdeRef = new AtomicReference<>();
            AtomicReference<List<Oppija>> oppijatRef = new AtomicReference<>(new ArrayList<Oppija>());
            AtomicReference<Boolean> haeSuoritusrekisteristaRef = new AtomicReference<>(null);

            Supplier<Void> viimeisteleTuonti;
            {
                AtomicInteger laskuri = new AtomicInteger(8 + 2 // <- ylimaaraisten osallistujen hakemukset ja lisatiedot
                );
                viimeisteleTuonti = () -> {
                    if (laskuri.decrementAndGet() <= 0) {
                        vie(hakuOid, hakukohdeOid, prosessi, osallistumistiedotRef.get(), hakemusRef.get(), valintaperusteRef.get(),
                                lisatiedotRef.get(), hakukohdeJaValintakoeRef.get(), hakuRef.get(), hakukohdeRef.get(), oppijatRef.get());
                    }
                    return null;
                };
            }
            Supplier<Void> tarkistaYlimaaraisetOsallistujat;
            {
                AtomicInteger laskuriYlimaaraisilleOsallistujille = new AtomicInteger(3 // osallistujat + hakemukset + lisatiedot
                );
                tarkistaYlimaaraisetOsallistujat = () -> {
                    if (laskuriYlimaaraisilleOsallistujille.decrementAndGet() <= 0) {
                        //osallistumistiedot.get().stream().filter(o -> o.getHakutoiveet().stream().anyMatch(o2 -> hakukohdeOid.equals(o2.getHakukohdeOid())));
                        Set<String> osallistujienHakemusOids = Sets.newHashSet(osallistumistiedotRef.get().stream().map(o -> o.getHakemusOid()).collect(Collectors.toSet()));
                        osallistujienHakemusOids.removeAll(lisatiedotRef.get().stream().map(a -> a.getOid()).collect(Collectors.toSet()));
                        if (!osallistujienHakemusOids.isEmpty()) {
                            // haetaan puuttuvat lisätiedot ja hakemukset
                            applicationAsyncResource.getApplicationAdditionalData(osallistujienHakemusOids, a -> {
                                lisatiedotRef.set(Stream.concat(lisatiedotRef.get().stream(), a.stream()).collect(Collectors.toList()));
                                viimeisteleTuonti.get();
                            }, poikkeuskasittelija);
                            applicationAsyncResource.getApplicationsByOids(osallistujienHakemusOids, a -> {
                                hakemusRef.set(Stream.concat(hakemusRef.get().stream(), a.stream()).collect(Collectors.toList()));
                                viimeisteleTuonti.get();
                            }, poikkeuskasittelija);
                        } else {
                            viimeisteleTuonti.get();
                            viimeisteleTuonti.get();
                        }
                    }
                    return null;
                };
            }
            Supplier<Void> haeKielikokeetSuoritusrekisterista = () -> {
                Boolean haeSuoritusrekisterista = haeSuoritusrekisteristaRef.get();
                if(haeSuoritusrekisterista) {
                    suoritusrekisteriAsyncResource.getOppijatByHakukohde(hakukohdeOid, hakuOid).subscribe(oppijat -> {
                       oppijatRef.set(oppijat);
                       prosessi.inkrementoiTehtyjaToita();
                       viimeisteleTuonti.get();
                    }, poikkeuskasittelija);
                } else {
                    prosessi.inkrementoiTehtyjaToita();
                    viimeisteleTuonti.get();
                }
                return null;
            };
            valintakoeResource.haeHakutoiveelle(hakukohdeOid).subscribe(osallistumistiedot -> {
                osallistumistiedotRef.set(osallistumistiedot);
                prosessi.inkrementoiTehtyjaToita();
                viimeisteleTuonti.get();
                tarkistaYlimaaraisetOsallistujat.get();
            }, poikkeuskasittelija);
            applicationAsyncResource.getApplicationsByOid(hakuOid, hakukohdeOid).subscribe(hakemukset -> {
                        hakemusRef.set(hakemukset);
                        prosessi.inkrementoiTehtyjaToita();
                        viimeisteleTuonti.get();
                        tarkistaYlimaaraisetOsallistujat.get();
                    }, poikkeuskasittelija);
            valintaperusteetResource.findAvaimet(hakukohdeOid).subscribe(avaimet -> {
                prosessi.inkrementoiTehtyjaToita();
                valintaperusteRef.set(avaimet);
                haeSuoritusrekisteristaRef.set(valintakoeTunnisteet(avaimet).stream().anyMatch(t -> t.startsWith("kielikoe_")));
                haeKielikokeetSuoritusrekisterista.get();
                viimeisteleTuonti.get();
            }, poikkeuskasittelija);
            applicationAsyncResource.getApplicationAdditionalData(hakuOid, hakukohdeOid).subscribe(lisatiedot -> {
                prosessi.inkrementoiTehtyjaToita();
                lisatiedotRef.set(lisatiedot);
                viimeisteleTuonti.get();
                tarkistaYlimaaraisetOsallistujat.get();
            }, poikkeuskasittelija);
            valintaperusteetResource.haeValintakokeetHakutoiveille(Collections.singletonList(hakukohdeOid)).subscribe(hakukohdeJaValintakoe -> {
                prosessi.inkrementoiTehtyjaToita();
                hakukohdeJaValintakoeRef.set(hakukohdeJaValintakoe);
                viimeisteleTuonti.get();
            }, poikkeuskasittelija);
            tarjontaAsyncResource.haeHakukohde(hakukohdeOid).subscribe(hakukohde -> {
                prosessi.inkrementoiTehtyjaToita();
                hakukohdeRef.set(hakukohde);
                viimeisteleTuonti.get();
            }, poikkeuskasittelija);
            tarjontaAsyncResource.haeHaku(hakuOid).subscribe(haku -> {
                prosessi.inkrementoiTehtyjaToita();
                hakuRef.set(haku);
                viimeisteleTuonti.get();
            }, poikkeuskasittelija);
        } catch (Throwable t) {
            poikkeuskasittelija.accept(t);
        }
    }

    protected Date defaultExpirationDate() {
        return DateTime.now().plusHours(168).toDate(); // almost a day
    }
}
