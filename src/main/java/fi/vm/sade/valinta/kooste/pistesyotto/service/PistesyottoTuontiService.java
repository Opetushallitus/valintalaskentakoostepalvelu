package fi.vm.sade.valinta.kooste.pistesyotto.service;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import fi.vm.sade.auditlog.valintaperusteet.ValintaperusteetOperation;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteDTO;
import static fi.vm.sade.valinta.kooste.KoosteAudit.AUDIT;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.ApplicationAdditionalDataDTO;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
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

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import static fi.vm.sade.auditlog.valintaperusteet.LogMessage.builder;

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
public class PistesyottoTuontiService {
    private static final Logger LOG = LoggerFactory.getLogger(PistesyottoTuontiService.class);
    private final ValintalaskentaValintakoeAsyncResource valintakoeResource;
    private final ValintaperusteetAsyncResource valintaperusteetResource;
    private final ApplicationAsyncResource applicationAsyncResource;

    @Autowired
    public PistesyottoTuontiService(ValintalaskentaValintakoeAsyncResource valintakoeResource,
            ValintaperusteetAsyncResource valintaperusteetResource, ApplicationAsyncResource applicationAsyncResource) {
        this.valintakoeResource = valintakoeResource;
        this.valintaperusteetResource = valintaperusteetResource;
        this.applicationAsyncResource = applicationAsyncResource;
    }

    private void tuo(String username, String hakuOid, String hakukohdeOid, DokumenttiProsessi prosessi,
                     List<ValintakoeOsallistuminenDTO> osallistumistiedot,
                     List<ApplicationAdditionalDataDTO> pistetiedot,
                     List<ValintaperusteDTO> valintaperusteet,
                     Set<String> kaikkiKutsutaanTunnisteet,
                     InputStream stream) {
        String hakuNimi = StringUtils.EMPTY;
        String hakukohdeNimi = StringUtils.EMPTY;
        String tarjoajaNimi = StringUtils.EMPTY;
        PoikkeusKasittelijaSovitin poikkeusilmoitus = new PoikkeusKasittelijaSovitin(t -> {
            LOG.error("Pistesyötön tuonti epäonnistui", t);
            prosessi.getPoikkeukset().add(new Poikkeus(Poikkeus.KOOSTEPALVELU, "Pistesyötön tuonti:", t.getMessage()));
        });
        try {

            final List<String> valintakoeTunnisteet = valintaperusteet.stream().map(vp -> vp.getTunniste()).collect(Collectors.toList());

            List<Hakemus> hakemukset = Collections.emptyList();
            // LOG.error("Excelin luonti");
            PistesyottoDataRiviListAdapter pistesyottoTuontiAdapteri = new PistesyottoDataRiviListAdapter();
            final PistesyottoExcel pistesyottoExcel = new PistesyottoExcel(hakuOid, hakukohdeOid, null, hakuNimi, hakukohdeNimi, tarjoajaNimi, hakemukset,
                    kaikkiKutsutaanTunnisteet, valintakoeTunnisteet, osallistumistiedot, valintaperusteet, pistetiedot, pistesyottoTuontiAdapteri, null);
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
            List<ApplicationAdditionalDataDTO> uudetPistetiedot =
                    pistesyottoTuontiAdapteri
                            .getRivit().stream()
                            .filter(PistesyottoRivi::isValidi)
                            .flatMap(rivi -> {
                                final String hakemusOid = rivi.getOid();
                                ApplicationAdditionalDataDTO additionalData = pistetiedotMapping.get(hakemusOid);
                                Map<String, String> newPistetiedot = rivi.asAdditionalData(
                                        valintakoetunniste -> pistesyottoExcel.onkoHakijaOsallistujaValintakokeeseen(hakemusOid, valintakoetunniste));
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
                applicationAsyncResource.putApplicationAdditionalData(
                        hakuOid, hakukohdeOid, uudetPistetiedot).subscribe(response -> {
                    uudetPistetiedot.forEach(p ->
                                    AUDIT.log(builder()
                                            .id(username)
                                            .hakuOid(hakuOid)
                                            .hakukohdeOid(hakukohdeOid)
                                            .hakijaOid(p.getPersonOid())
                                            .hakemusOid(p.getOid())
                                            .addAll(p.getAdditionalData())
                                            .setOperaatio(ValintaperusteetOperation.PISTETIEDOT_TUONTI_EXCEL)
                                            .build())
                    );
                    prosessi.setDokumenttiId("valmis");
                    prosessi.inkrementoiTehtyjaToita();
                }, poikkeusilmoitus);
            }
        } catch (Throwable t) {
            poikkeusilmoitus.accept(t);
        }
    }

    private List<String> getPistesyottoExcelVirheet(PistesyottoDataRiviListAdapter pistesyottoTuontiAdapteri, List<ApplicationAdditionalDataDTO> hakemukset) {
        return getPistesyottoExcelVirheet(pistesyottoTuontiAdapteri, hakemukset.stream().collect(Collectors.toMap(h -> h.getOid(), h -> h, (h1, h2) -> {
            return h2;
        })));
    }

    private List<String> getPistesyottoExcelVirheet(PistesyottoDataRiviListAdapter pistesyottoTuontiAdapteri, Map<String, ApplicationAdditionalDataDTO> oidToAdditionalMapping) {
        return (List<String>) pistesyottoTuontiAdapteri
                .getRivit().stream()
                        //.filter(rivi -> !rivi.isValidi())
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


    public void tuo(String username, String hakuOid, String hakukohdeOid, DokumenttiProsessi prosessi, InputStream stream) {
        prosessi.setKokonaistyo(5
                        // luonti
                        + 1);
        PoikkeusKasittelijaSovitin poikkeusilmoitus = new PoikkeusKasittelijaSovitin(t -> {
            LOG.error("Pistesyötön tuonti epäonnistui", t);
            prosessi.getPoikkeukset().add(new Poikkeus(Poikkeus.KOOSTEPALVELU, "Pistesyötön tuonti:", t.getMessage()));
        });
        AtomicReference<List<ValintakoeOsallistuminenDTO>> osallistumistiedot = new AtomicReference<>();
        AtomicReference<List<ApplicationAdditionalDataDTO>> additionaldata = new AtomicReference<>();
        AtomicReference<List<ValintaperusteDTO>> valintaperusteet = new AtomicReference<>();
        AtomicReference<Set<String>> kaikkiKutsutaanTunnisteetRef = new AtomicReference<>();
        AtomicInteger laskuri = new AtomicInteger(5);
        AtomicInteger laskuriYlimaaraisilleOsallistujille = new AtomicInteger(2);
        Supplier<Void> viimeisteleTuonti = () -> {
            if (laskuri.decrementAndGet() <= 0) {
                tuo(username, hakuOid, hakukohdeOid, prosessi, osallistumistiedot.get(), additionaldata.get(), valintaperusteet.get(), kaikkiKutsutaanTunnisteetRef.get(), stream);
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
        valintakoeResource.haeHakutoiveelle(hakukohdeOid).subscribe(
                o -> {
                    prosessi.inkrementoiTehtyjaToita();
                    osallistumistiedot.set(o);
                    tarkistaYlimaaraisetOsallistujat.get();
                    viimeisteleTuonti.get();
                }, poikkeusilmoitus);

        valintaperusteetResource
                .findAvaimet(hakukohdeOid).subscribe(
                v -> {
                    valintaperusteet.set(v);
                    prosessi.inkrementoiTehtyjaToita();
                    viimeisteleTuonti.get();
                },
                poikkeusilmoitus);

        valintaperusteetResource.haeValintakokeetHakukohteille(Arrays.asList(hakukohdeOid), hakukohdeJaValintakoe -> {
            prosessi.inkrementoiTehtyjaToita();
            Set<String> kaikkiKutsutaanTunnisteet =
                    hakukohdeJaValintakoe.stream().flatMap(h -> {
                        Optional.ofNullable(h.getValintakoeDTO()).orElse(Collections.emptyList()).forEach(vk ->
                                LOG.error("Valintakoetunniste=={}, kutsutaankokaikki={}", vk.getTunniste(), vk.getKutsutaankoKaikki()));
                        return h.getValintakoeDTO().stream();
                    }).filter(v -> Boolean.TRUE.equals(v.getKutsutaankoKaikki())).map(v -> v.getTunniste()).collect(Collectors.toSet());
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
