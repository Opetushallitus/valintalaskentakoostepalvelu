package fi.vm.sade.valinta.kooste.pistesyotto.service;

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
import rx.Observable;
import rx.functions.Func1;
import rx.functions.Func2;

import java.io.InputStream;
import java.util.*;
import java.util.function.Consumer;
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
            Collection<String> valintakoeTunnisteet = valintaperusteet.stream()
                    .map(v -> v.getTunniste())
                    .collect(Collectors.toList());

            Map<String, List<Arvosana>> kielikoeArvosanat = AbstractPistesyottoKoosteService.ammatillisenKielikoeArvosanat(oppijat);

            Set<String> kaikkiKutsutaanTunnisteet = hakukohdeJaValintakoe.stream()
                    .flatMap(h -> h.getValintakoeDTO().stream())
                    .filter(v -> Boolean.TRUE.equals(v.getKutsutaankoKaikki()))
                    .map(v -> v.getTunniste())
                    .collect(Collectors.toSet());

            InputStream xlsx = new PistesyottoExcel(hakuOid, hakukohdeOid, tarjoajaOid, hakuNimi, hakukohdeNimi,
                    tarjoajaNimi, hakemukset, kaikkiKutsutaanTunnisteet, valintakoeTunnisteet, osallistumistiedot,
                    valintaperusteet, pistetiedot, kielikoeArvosanat).getExcel().vieXlsx();

            prosessi.inkrementoiKokonaistyota();
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

    public void vie(String hakuOid, String hakukohdeOid, DokumenttiProsessi prosessi) {
        PoikkeusKasittelijaSovitin poikkeuskasittelija = new PoikkeusKasittelijaSovitin(poikkeus -> {
            LOG.error("Pistesyötön viennissä tapahtui poikkeus:", poikkeus);
            prosessi.getPoikkeukset().add(new Poikkeus(Poikkeus.KOOSTEPALVELU, "Pistesyötön vienti", poikkeus.getMessage()));
        });
        try {
            Func2<List<ValintakoeOsallistuminenDTO>, List<ApplicationAdditionalDataDTO>, Observable<List<ApplicationAdditionalDataDTO>>> heaPuuttuvatLisatiedot = (osallistumiset, lisatiedot) -> {
                Set<String> puuttuvatLisatiedot = osallistumiset.stream().map(o -> o.getHakemusOid()).collect(Collectors.toSet());
                puuttuvatLisatiedot.removeAll(lisatiedot.stream().map(l -> l.getOid()).collect(Collectors.toSet()));
                if (puuttuvatLisatiedot.isEmpty()) {
                    return Observable.just(lisatiedot);
                }
                prosessi.inkrementoiKokonaistyota();
                return applicationAsyncResource.getApplicationAdditionalData(puuttuvatLisatiedot)
                        .map(ls -> Stream.concat(lisatiedot.stream(), ls.stream()).collect(Collectors.toList()))
                        .doOnCompleted(() -> {
                            prosessi.inkrementoiTehtyjaToita();
                        });
            };
            Func2<List<ValintakoeOsallistuminenDTO>, List<Hakemus>, Observable<List<Hakemus>>> haePuuttuvatHakemukset = (osallistumiset, hakemukset) -> {
                Set<String> puuttuvatHakemukset = osallistumiset.stream().map(o -> o.getHakemusOid()).collect(Collectors.toSet());
                puuttuvatHakemukset.removeAll(hakemukset.stream().map(h -> h.getOid()).collect(Collectors.toSet()));
                if (puuttuvatHakemukset.isEmpty()) {
                    return Observable.just(hakemukset);
                }
                prosessi.inkrementoiKokonaistyota();
                return applicationAsyncResource.getApplicationsByHakemusOids(puuttuvatHakemukset)
                        .map(hs -> Stream.concat(hakemukset.stream(), hs.stream()).collect(Collectors.toList()))
                        .doOnCompleted(() -> {
                            prosessi.inkrementoiTehtyjaToita();
                        });
            };
            Func1<List<ValintaperusteDTO>, Observable<List<Oppija>>> haeKielikoetulokset = kokeet -> {
                if (kokeet.stream().map(k -> k.getTunniste()).anyMatch(t -> t.matches(PistesyottoExcel.KIELIKOE_REGEX))) {
                    prosessi.inkrementoiKokonaistyota();
                    return suoritusrekisteriAsyncResource.getOppijatByHakukohde(hakukohdeOid, hakuOid)
                            .doOnCompleted(() -> {
                                prosessi.inkrementoiTehtyjaToita();
                            });
                } else {
                    return Observable.just(new ArrayList<>());
                }
            };
            Observable<List<ValintakoeOsallistuminenDTO>> osallistumistiedotO = valintakoeResource.haeHakutoiveelle(hakukohdeOid);
            Observable<List<ApplicationAdditionalDataDTO>> lisatiedotO = Observable.merge(Observable.zip(
                    osallistumistiedotO,
                    applicationAsyncResource.getApplicationAdditionalData(hakuOid, hakukohdeOid),
                    heaPuuttuvatLisatiedot
            ));
            Observable<List<Hakemus>> hakemuksetO = Observable.merge(Observable.zip(
                    osallistumistiedotO,
                    applicationAsyncResource.getApplicationsByOid(hakuOid, hakukohdeOid),
                    haePuuttuvatHakemukset
            ));
            Observable<List<ValintaperusteDTO>> kokeetO = valintaperusteetResource.findAvaimet(hakukohdeOid);

            prosessi.inkrementoiKokonaistyota();
            Observable.zip(
                    osallistumistiedotO,
                    lisatiedotO,
                    hakemuksetO,
                    kokeetO,
                    kokeetO.flatMap(haeKielikoetulokset),
                    valintaperusteetResource.haeValintakokeetHakutoiveille(Collections.singletonList(hakukohdeOid)),
                    tarjontaAsyncResource.haeHakukohde(hakukohdeOid),
                    tarjontaAsyncResource.haeHaku(hakuOid),
                    (osallistumistiedot, lisatiedot, hakemukset, kokeet, kielikoetulokset, valintakoeosallistumiset, hakukohde, haku) -> {
                        vie(hakuOid, hakukohdeOid, prosessi, osallistumistiedot, hakemukset, kokeet, lisatiedot,
                                valintakoeosallistumiset, haku, hakukohde, kielikoetulokset);
                        return null;
                    }
            ).subscribe(v -> {
                prosessi.inkrementoiTehtyjaToita();
            }, t -> {
                poikkeuskasittelija.accept(t);
            });
        } catch (Exception t) {
            poikkeuskasittelija.accept(t);
        }
    }

    protected Date defaultExpirationDate() {
        return DateTime.now().plusHours(168).toDate(); // almost a day
    }
}
