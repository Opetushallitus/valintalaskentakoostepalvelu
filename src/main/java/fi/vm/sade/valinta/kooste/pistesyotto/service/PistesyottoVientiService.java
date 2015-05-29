package fi.vm.sade.valinta.kooste.pistesyotto.service;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Sets;
import fi.vm.sade.service.valintaperusteet.dto.HakukohdeJaValintakoeDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteDTO;
import fi.vm.sade.tarjonta.service.resources.dto.HakukohdeDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.valinta.kooste.external.resource.dokumentti.DokumenttiAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.ApplicationAdditionalDataDTO;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaValintakoeAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.pistesyotto.excel.PistesyottoExcel;
import fi.vm.sade.valinta.kooste.util.PoikkeusKasittelijaSovitin;
import fi.vm.sade.valinta.kooste.valvomo.dto.Poikkeus;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.DokumenttiProsessi;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Teksti;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeOsallistuminenDTO;
import org.jgroups.util.*;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.InputStream;
import java.util.*;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Jussi Jartamo
 */
public class PistesyottoVientiService {

    private static final Logger LOG = LoggerFactory.getLogger(PistesyottoVientiService.class);
    private final ValintalaskentaValintakoeAsyncResource valintakoeResource;
    private final ValintaperusteetAsyncResource valintaperusteetResource;
    private final ApplicationAsyncResource applicationAsyncResource;
    private final TarjontaAsyncResource tarjontaAsyncResource;
    private final DokumenttiAsyncResource dokumenttiAsyncResource;

    @Autowired
    public PistesyottoVientiService(
            ValintalaskentaValintakoeAsyncResource valintakoeResource,
            ValintaperusteetAsyncResource valintaperusteetResource,
            ApplicationAsyncResource applicationAsyncResource,
            TarjontaAsyncResource tarjontaAsyncResource,
            DokumenttiAsyncResource dokumenttiAsyncResource
    ) {
        this.dokumenttiAsyncResource = dokumenttiAsyncResource;
        this.tarjontaAsyncResource = tarjontaAsyncResource;
        this.valintakoeResource = valintakoeResource;
        this.valintaperusteetResource = valintaperusteetResource;
        this.applicationAsyncResource = applicationAsyncResource;
    }
    private void vie(String hakuOid, String hakukohdeOid, DokumenttiProsessi prosessi,
                     List<ValintakoeOsallistuminenDTO> osallistumistiedot,
                     List<Hakemus> hakemukset,
                     List<ValintaperusteDTO> valintaperusteet,
                     List<ApplicationAdditionalDataDTO> pistetiedot,
                     List<HakukohdeJaValintakoeDTO> hakukohdeJaValintakoe,
                     HakuV1RDTO hakuV1RDTO,
                     HakukohdeDTO hakukohdeDTO) {
        Consumer<Throwable> poikkeuskasittelija = poikkeus -> {
            LOG.error("Pistesyötön viennissä tapahtui poikkeus:", poikkeus);
            prosessi.getPoikkeukset().add(
                    new Poikkeus(Poikkeus.KOOSTEPALVELU,
                            "Pistesyötön vienti", poikkeus.getMessage()));
        };
        try {
            String hakuNimi = new Teksti(hakuV1RDTO.getNimi()).getTeksti();
            String tarjoajaOid = hakukohdeDTO.getTarjoajaOid();
            String hakukohdeNimi = new Teksti(hakukohdeDTO.getHakukohdeNimi()).getTeksti();
            String tarjoajaNimi = new Teksti(hakukohdeDTO.getTarjoajaNimi()).getTeksti();
            Collection<String> valintakoeTunnisteet =
                    valintaperusteet.stream().map(v -> v.getTunniste()).collect(Collectors.toList());

            Set<String> kaikkiKutsutaanTunnisteet = //Collections.emptySet();
                    hakukohdeJaValintakoe.stream().flatMap(h -> {
                        Optional.ofNullable(h.getValintakoeDTO()).orElse(Collections.emptyList()).forEach(vk ->
                                LOG.error("Valintakoetunniste=={}, kutsutaankokaikki={}", vk.getTunniste(), vk.getKutsutaankoKaikki()));
                        return h.getValintakoeDTO().stream();
                    }).filter(v -> Boolean.TRUE.equals(v.getKutsutaankoKaikki())).map(v -> v.getTunniste()).collect(Collectors.toSet());
            PistesyottoExcel pistesyottoExcel = new PistesyottoExcel(
                    hakuOid, hakukohdeOid, tarjoajaOid, hakuNimi,
                    hakukohdeNimi, tarjoajaNimi, hakemukset,
                    kaikkiKutsutaanTunnisteet,
                    valintakoeTunnisteet, osallistumistiedot,
                    valintaperusteet, pistetiedot);
            InputStream xlsx = pistesyottoExcel.getExcel()
                    .vieXlsx();
            prosessi.inkrementoiTehtyjaToita();
            String id = generateId();
            Long expirationTime = defaultExpirationDate().getTime();
            List<String> tags = prosessi
                    .getTags();
            // LOG.error("Excelin tallennus");
            dokumenttiAsyncResource.tallenna(id, "pistesyotto.xlsx",
                    expirationTime, tags,
                    "application/octet-stream", xlsx, response -> {
                        prosessi.setDokumenttiId(id);
                        prosessi.inkrementoiTehtyjaToita();
                    }, poikkeuskasittelija);
        } catch(Throwable t) {
            poikkeuskasittelija.accept(t);
        }
    }

    public void vie(String hakuOid, String hakukohdeOid, DokumenttiProsessi prosessi) {
        PoikkeusKasittelijaSovitin poikkeuskasittelija = new PoikkeusKasittelijaSovitin(poikkeus -> {
            LOG.error("Pistesyötön viennissä tapahtui poikkeus:", poikkeus);
            prosessi.getPoikkeukset().add(
                    new Poikkeus(Poikkeus.KOOSTEPALVELU,
                            "Pistesyötön vienti", poikkeus.getMessage()));
        });
        try {
            prosessi.setKokonaistyo(
                    7
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
            AtomicReference<HakukohdeDTO> hakukohdeRef = new AtomicReference<>();
            Supplier<Void> viimeisteleTuonti;
            {
                AtomicInteger laskuri = new AtomicInteger(7 + 2 // <- ylimaaraisten osallistujen hakemukset ja lisatiedot
                );
                viimeisteleTuonti = () -> {
                    if (laskuri.decrementAndGet() <= 0) {
                        vie(hakuOid, hakukohdeOid, prosessi,
                                osallistumistiedotRef.get(),
                                hakemusRef.get(),
                                valintaperusteRef.get(),
                                lisatiedotRef.get(),
                                hakukohdeJaValintakoeRef.get(),
                                hakuRef.get(),
                                hakukohdeRef.get());
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
            valintakoeResource.haeHakutoiveelle(hakukohdeOid, osallistumistiedot -> {
                osallistumistiedotRef.set(osallistumistiedot);
                prosessi.inkrementoiTehtyjaToita();
                viimeisteleTuonti.get();
                tarkistaYlimaaraisetOsallistujat.get();
            }, poikkeuskasittelija);
            applicationAsyncResource
                    .getApplicationsByOid(hakuOid, hakukohdeOid, hakemukset -> {
                        hakemusRef.set(hakemukset);
                        prosessi.inkrementoiTehtyjaToita();
                        viimeisteleTuonti.get();
                        tarkistaYlimaaraisetOsallistujat.get();
                    }, poikkeuskasittelija);
            valintaperusteetResource
                    .findAvaimet(hakukohdeOid, avaimet -> {
                        prosessi.inkrementoiTehtyjaToita();
                        valintaperusteRef.set(avaimet);
                        viimeisteleTuonti.get();
                    }, poikkeuskasittelija);
            applicationAsyncResource
                    .getApplicationAdditionalData(hakuOid,
                            hakukohdeOid, lisatiedot -> {
                                prosessi.inkrementoiTehtyjaToita();
                                lisatiedotRef.set(lisatiedot);
                                viimeisteleTuonti.get();
                                tarkistaYlimaaraisetOsallistujat.get();
                            }, poikkeuskasittelija);
            valintaperusteetResource.haeValintakokeetHakukohteille(Arrays.asList(hakukohdeOid), hakukohdeJaValintakoe -> {
                prosessi.inkrementoiTehtyjaToita();
                hakukohdeJaValintakoeRef.set(hakukohdeJaValintakoe);
                viimeisteleTuonti.get();
            }, poikkeuskasittelija);
            tarjontaAsyncResource.haeHakukohde(hakukohdeOid).subscribe(hakukohde -> {
                prosessi.inkrementoiTehtyjaToita();
                hakukohdeRef.set(hakukohde);
                viimeisteleTuonti.get();
            }, poikkeuskasittelija);
            tarjontaAsyncResource.haeHaku(hakuOid, haku -> {
                prosessi.inkrementoiTehtyjaToita();
                hakuRef.set(haku);
                viimeisteleTuonti.get();
            }, poikkeuskasittelija);
        } catch(Throwable t) {
            poikkeuskasittelija.accept(t);
        }
    }

    protected Date defaultExpirationDate() {
        return DateTime.now().plusHours(168).toDate(); // almost a day
    }

    protected String generateId() {
        return UUID.randomUUID().toString();
    }
}
