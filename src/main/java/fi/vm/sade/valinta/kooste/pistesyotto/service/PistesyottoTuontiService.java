package fi.vm.sade.valinta.kooste.pistesyotto.service;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import fi.vm.sade.auditlog.valintaperusteet.ValintaperusteetOperation;
import fi.vm.sade.service.valintaperusteet.dto.ValintakoeCreateDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteDTO;
import fi.vm.sade.valinta.http.HttpExceptionWithResponse;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.ApplicationAdditionalDataDTO;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.OrganisaatioAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.SuoritusrekisteriAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Oppija;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaValintakoeAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.pistesyotto.excel.PistesyottoArvo;
import fi.vm.sade.valinta.kooste.pistesyotto.excel.PistesyottoDataRiviListAdapter;
import fi.vm.sade.valinta.kooste.pistesyotto.excel.PistesyottoExcel;
import fi.vm.sade.valinta.kooste.pistesyotto.excel.PistesyottoRivi;
import fi.vm.sade.valinta.kooste.util.PoikkeusKasittelijaSovitin;
import fi.vm.sade.valinta.kooste.valvomo.dto.Poikkeus;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.DokumenttiProsessi;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeOsallistuminenDTO;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import rx.functions.Action1;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *         GET
 *         /valintalaskenta-laskenta-service/resources/valintakoe/hakutoive/{hakukohdeOid}
 *         GET
 *         /valintaperusteet-service/resources/valintalaskentakoostepalvelu/hakukohde/avaimet/{hakukohdeOid}
 *         GET
 *         /applications/additionalData/{hakuOid}/{hakukohdeOid}
 *         POST [hakemusOids]
 *         /applications/additionalData
 *         PUT [additionalDataDTO]
 *         /applications/additionalData/{hakuOid}/{hakukohdeOid}
 */
public class PistesyottoTuontiService extends AbstractPistesyottoKoosteService {
    private static final Logger LOG = LoggerFactory.getLogger(PistesyottoTuontiService.class);
    private final ValintalaskentaValintakoeAsyncResource valintakoeResource;
    private final ValintaperusteetAsyncResource valintaperusteetResource;

    @Autowired
    public PistesyottoTuontiService(
            ValintalaskentaValintakoeAsyncResource valintakoeResource,
            ValintaperusteetAsyncResource valintaperusteetResource,
            ApplicationAsyncResource applicationAsyncResource,
            SuoritusrekisteriAsyncResource suoritusrekisteriAsyncResource,
            TarjontaAsyncResource tarjontaAsyncResource,
            OrganisaatioAsyncResource organisaatioAsyncResource,
            ValintalaskentaValintakoeAsyncResource valintalaskentaValintakoeAsyncResource) {
        super(applicationAsyncResource, suoritusrekisteriAsyncResource, tarjontaAsyncResource, organisaatioAsyncResource, valintalaskentaValintakoeAsyncResource);
        this.valintakoeResource = valintakoeResource;
        this.valintaperusteetResource = valintaperusteetResource;
    }

    private void tuo(String username, String hakuOid, String hakukohdeOid, DokumenttiProsessi prosessi,
                     List<ValintakoeOsallistuminenDTO> osallistumistiedot,
                     List<ApplicationAdditionalDataDTO> pistetiedot,
                     List<ValintaperusteDTO> valintaperusteet,
                     Set<String> kaikkiKutsutaanTunnisteet,
                     InputStream stream,
                     List<Oppija> oppijat) {
        String hakuNimi = StringUtils.EMPTY;
        String hakukohdeNimi = StringUtils.EMPTY;
        String tarjoajaNimi = StringUtils.EMPTY;
        PoikkeusKasittelijaSovitin poikkeusilmoitus = new PoikkeusKasittelijaSovitin(t -> {
            logPistesyotonTuontiEpaonnistui(t);
            prosessi.getPoikkeukset().add(new Poikkeus(Poikkeus.KOOSTEPALVELU, "Pistesyötön tuonti:", t.getMessage()));
        });
        final Date valmistuminen = new Date();
        try {

            final List<String> valintakoeTunnisteet = valintakoeTunnisteet(valintaperusteet);

            List<Hakemus> hakemukset = Collections.emptyList();
            // LOG.error("Excelin luonti");
            PistesyottoDataRiviListAdapter pistesyottoTuontiAdapteri = new PistesyottoDataRiviListAdapter();
            final PistesyottoExcel pistesyottoExcel = new PistesyottoExcel(hakuOid, hakukohdeOid, null, hakuNimi, hakukohdeNimi, tarjoajaNimi, hakemukset,
                    kaikkiKutsutaanTunnisteet, valintakoeTunnisteet, osallistumistiedot, valintaperusteet, pistetiedot, pistesyottoTuontiAdapteri, ammatillisenKielikoeArvosanat(oppijat));
            pistesyottoExcel.getExcel().tuoXlsx(stream);
            // TARKISTETAAN VIRHEET
            List<String> virheet = getPistesyottoExcelVirheet(pistesyottoTuontiAdapteri, pistetiedot);
            if (!virheet.isEmpty()) {
                String v = virheet.stream().collect(Collectors.joining(", "));
                LOG.error("Virheitä pistesyöttöriveissä {}", v);
                prosessi.getPoikkeukset().add(new Poikkeus("Pistesyötön tuonti", "", v));
                return;
            }
            Map<String, ApplicationAdditionalDataDTO> pistetiedotMapping = asMap(pistetiedot);
            Map<String, List<AbstractPistesyottoKoosteService.SingleKielikoeTulos>> uudetKielikoetulokset = new HashMap<>();
            List<ApplicationAdditionalDataDTO> uudetPistetiedot =
                    pistesyottoTuontiAdapteri
                            .getRivit().stream()
                            .filter(PistesyottoRivi::isValidi)
                            .flatMap(rivi -> {
                                final String hakemusOid = rivi.getOid();
                                ApplicationAdditionalDataDTO additionalData = pistetiedotMapping.get(hakemusOid);
                                Map<String, String> newPistetiedot = rivi.asAdditionalData(
                                        valintakoetunniste -> pistesyottoExcel.onkoHakijaOsallistujaValintakokeeseen(hakemusOid, valintakoetunniste));
                                List<String> kielikoeAvaimet = newPistetiedot.keySet().stream().filter(a -> a.matches(PistesyottoExcel.KIELIKOE_REGEX)).collect(Collectors.toList());
                                if(0 < kielikoeAvaimet.size()) {
                                    uudetKielikoetulokset.put(hakemusOid, kielikoeAvaimet.stream().map(avain ->
                                        new SingleKielikoeTulos(avain, newPistetiedot.get(avain), valmistuminen)).collect(Collectors.toList()));
                                }
                                kielikoeAvaimet.forEach(newPistetiedot::remove);
                                additionalData.setAdditionalData(newPistetiedot);
                                return Stream.of(additionalData);
                            }).filter(Objects::nonNull)
                            .filter(a -> !a.getAdditionalData().isEmpty())
                            .collect(Collectors.toList());

            if (uudetPistetiedot.isEmpty()) {
                LOG.info("Pistesyötössä hakukohteeseen {} ei yhtäkään muuttunutta tietoa tallennettavaksi", hakukohdeOid);
                prosessi.inkrementoiTehtyjaToita();
                prosessi.setDokumenttiId("valmis");
            } else {
                LOG.info("Pistesyötössä hakukohteeseen {} muuttunutta {} tietoa tallennettavaksi", hakukohdeOid, uudetPistetiedot.size());

                Action1<Void> onSuccess = (a) -> {
                    prosessi.setDokumenttiId("valmis");
                    prosessi.inkrementoiTehtyjaToita();
                };

                Action1<Throwable> onError = (error) -> {
                    LOG.error("Pistetietojen tallennus epäonnistui", error);
                    poikkeusilmoitus.accept(error);
                };

                tallennaKoostetutPistetiedot(hakuOid, hakukohdeOid, uudetPistetiedot, uudetKielikoetulokset, username, ValintaperusteetOperation.PISTETIEDOT_TUONTI_EXCEL)
                        .subscribe(onSuccess, onError);


            }
        } catch (Throwable t) {
            poikkeusilmoitus.accept(t);
        }
    }

    private void logPistesyotonTuontiEpaonnistui(Throwable t) {
        if (t instanceof HttpExceptionWithResponse) {
            LOG.error("Pistesyötön tuonti epäonnistui HTTP-virheilmoitukseen. Sisältö: " + ((HttpExceptionWithResponse) t).contentToString(), t);
        } else {
            LOG.error("Pistesyötön tuonti epäonnistui", t);
        }
    }

    private List<String> getPistesyottoExcelVirheet(PistesyottoDataRiviListAdapter pistesyottoTuontiAdapteri, List<ApplicationAdditionalDataDTO> hakemukset) {
        return getPistesyottoExcelVirheet(pistesyottoTuontiAdapteri, hakemukset.stream().collect(Collectors.toMap(ApplicationAdditionalDataDTO::getOid, h -> h, (h1, h2) -> h2)));
    }

    private List<String> getPistesyottoExcelVirheet(PistesyottoDataRiviListAdapter pistesyottoTuontiAdapteri, Map<String, ApplicationAdditionalDataDTO> oidToAdditionalMapping) {
        return pistesyottoTuontiAdapteri
                .getRivit().stream()
                .flatMap(
                        rivi -> {
                            String nimi = PistesyottoExcel.additionalDataToNimi(oidToAdditionalMapping.get(rivi.getOid()));
                            if (!Optional.ofNullable(rivi.getNimi()).orElse("").equals(nimi)) {
                                String virheIlmoitus = new StringBuffer()
                                        .append("Hakemuksella (OID = ")
                                        .append(rivi.getOid())
                                        .append(") nimet ei täsmää: ")
                                        .append(rivi.getNimi())
                                        .append(" != ")
                                        .append(nimi)
                                        .toString();
                                return Stream.of(virheIlmoitus);
                            }
                            if (!rivi.isValidi()) {
                                LOG.warn("Rivi on muuttunut mutta viallinen joten ilmoitetaan virheestä!");

                                for (PistesyottoArvo arvo : rivi.getArvot()) {
                                    if (!arvo.isValidi()) {
                                        String virheIlmoitus = new StringBuffer()
                                                .append("Henkilöllä ")
                                                .append(rivi.getNimi())
                                                .append(" (")
                                                .append(rivi.getOid())
                                                .append(")")
                                                .append(" oli virheellinen arvo '")
                                                .append(arvo.getArvo())
                                                .append("'")
                                                .append(" kohdassa ")
                                                .append(arvo.getTunniste())
                                                .toString();
                                        return Stream.of(virheIlmoitus);
                                    }
                                }
                            }
                            return Stream.empty();
                        }
                ).collect(Collectors.toList());
    }

    private List<String> valintakoeTunnisteet(List<ValintaperusteDTO> valintaperusteet) {
        return valintaperusteet.stream().map(ValintaperusteDTO::getTunniste).collect(Collectors.toList());
    }

    public void tuo(String username, String hakuOid, String hakukohdeOid, DokumenttiProsessi prosessi, InputStream stream) {
        prosessi.setKokonaistyo(7); //Kuusi + valmistuminen
        PoikkeusKasittelijaSovitin poikkeusilmoitus = new PoikkeusKasittelijaSovitin(t -> {
            logPistesyotonTuontiEpaonnistui(t);
            prosessi.getPoikkeukset().add(new Poikkeus(Poikkeus.KOOSTEPALVELU, "Pistesyötön tuonti:", t.getMessage()));
        });
        AtomicReference<List<ValintakoeOsallistuminenDTO>> osallistumistiedot = new AtomicReference<>();
        AtomicReference<List<ApplicationAdditionalDataDTO>> additionaldata = new AtomicReference<>();
        AtomicReference<List<ValintaperusteDTO>> valintaperusteet = new AtomicReference<>();
        AtomicReference<Set<String>> kaikkiKutsutaanTunnisteetRef = new AtomicReference<>();
        AtomicInteger laskuri = new AtomicInteger(6);
        AtomicInteger laskuriYlimaaraisilleOsallistujille = new AtomicInteger(2);
        AtomicReference<List<Oppija>> oppijatRef = new AtomicReference<>(new ArrayList<Oppija>());
        AtomicReference<Boolean> haeSuoritusrekisteristaRef = new AtomicReference<>(null);

        Supplier<Void> viimeisteleTuonti = () -> {
            if (laskuri.decrementAndGet() <= 0) {
                tuo(username, hakuOid, hakukohdeOid, prosessi, osallistumistiedot.get(), additionaldata.get(), valintaperusteet.get(), kaikkiKutsutaanTunnisteetRef.get(), stream, oppijatRef.get());
            }
            return null;
        };
        Supplier<Void> tarkistaYlimaaraisetOsallistujat = () -> {
            if (laskuriYlimaaraisilleOsallistujille.decrementAndGet() <= 0) {
                //osallistumistiedot.get().stream().filter(o -> o.getHakutoiveet().stream().anyMatch(o2 -> hakukohdeOid.equals(o2.getHakukohdeOid())));
                Set<String> osallistujienHakemusOids = Sets.newHashSet(osallistumistiedot.get().stream().map(o -> o.getHakemusOid()).collect(Collectors.toSet()));
                osallistujienHakemusOids.removeAll(additionaldata.get().stream().map(a -> a.getOid()).collect(Collectors.toSet()));
                if (!osallistujienHakemusOids.isEmpty()) {
                    // haetaan puuttuvat
                    applicationAsyncResource.getApplicationAdditionalData(osallistujienHakemusOids, a -> {
                        additionaldata.set(Stream.concat(additionaldata.get().stream(), a.stream()).collect(Collectors.toList()));
                        prosessi.inkrementoiTehtyjaToita();
                        viimeisteleTuonti.get();
                    }, poikkeusilmoitus);
                } else {
                    prosessi.inkrementoiTehtyjaToita();
                    viimeisteleTuonti.get();
                }
            }
            return null;
        };
        Supplier<Void> haeKielikokeetSuoritusrekisterista = () -> {
            Boolean haeSuoritusrekisterista = haeSuoritusrekisteristaRef.get();
            if (haeSuoritusrekisterista) {
                suoritusrekisteriAsyncResource.getOppijatByHakukohde(hakukohdeOid, hakuOid).subscribe(oppijat -> {
                    oppijatRef.set(oppijat);
                    prosessi.inkrementoiTehtyjaToita();
                    viimeisteleTuonti.get();
                }, poikkeusilmoitus);
            } else {
                prosessi.inkrementoiTehtyjaToita();
                viimeisteleTuonti.get();
            }
            return null;
        };
        valintakoeResource.haeHakutoiveelle(hakukohdeOid).subscribe(o -> {
            prosessi.inkrementoiTehtyjaToita();
            osallistumistiedot.set(o);
            tarkistaYlimaaraisetOsallistujat.get();
            viimeisteleTuonti.get();
        }, poikkeusilmoitus);

        valintaperusteetResource.findAvaimet(hakukohdeOid).subscribe(v -> {
                valintaperusteet.set(v);
                prosessi.inkrementoiTehtyjaToita();
                haeSuoritusrekisteristaRef.set(valintakoeTunnisteet(v).stream().anyMatch(t -> t.startsWith("kielikoe_")));
                haeKielikokeetSuoritusrekisterista.get();
                viimeisteleTuonti.get();
            },
            poikkeusilmoitus);

        valintaperusteetResource.haeValintakokeetHakukohteille(Collections.singletonList(hakukohdeOid), hakukohdeJaValintakoe -> {
            prosessi.inkrementoiTehtyjaToita();
            Set<String> kaikkiKutsutaanTunnisteet =
                    hakukohdeJaValintakoe.stream().flatMap(h -> {
                        Optional.ofNullable(h.getValintakoeDTO()).orElse(Collections.emptyList()).forEach(vk ->
                                LOG.error("Valintakoetunniste=={}, kutsutaankokaikki={}", vk.getTunniste(), vk.getKutsutaankoKaikki()));
                        return h.getValintakoeDTO().stream();
                    }).filter(v -> Boolean.TRUE.equals(v.getKutsutaankoKaikki())).map(ValintakoeCreateDTO::getTunniste).collect(Collectors.toSet());
            kaikkiKutsutaanTunnisteetRef.set(kaikkiKutsutaanTunnisteet);
            viimeisteleTuonti.get();
        }, poikkeusilmoitus);

        applicationAsyncResource.getApplicationAdditionalData(hakuOid, hakukohdeOid,
                a -> {
                    additionaldata.set(a);
                    prosessi.inkrementoiTehtyjaToita();
                    tarkistaYlimaaraisetOsallistujat.get();
                    viimeisteleTuonti.get();
                },
                poikkeusilmoitus);

    }

    private Map<String, ApplicationAdditionalDataDTO> asMap(Collection<ApplicationAdditionalDataDTO> datas) {
        Map<String, ApplicationAdditionalDataDTO> mapping = Maps.newHashMap();
        for (ApplicationAdditionalDataDTO data : datas) {
            mapping.put(data.getOid(), data);
        }
        return mapping;
    }
}
