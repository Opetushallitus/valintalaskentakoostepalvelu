package fi.vm.sade.valinta.kooste.pistesyotto.service;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import fi.vm.sade.auditlog.valintaperusteet.ValintaperusteetOperation;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteDTO;
import static fi.vm.sade.valinta.kooste.KoosteAudit.AUDIT;

import fi.vm.sade.valinta.http.HttpExceptionWithResponse;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.ApplicationAdditionalDataDTO;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.SuoritusrekisteriAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.*;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaValintakoeAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.pistesyotto.excel.PistesyottoArvo;
import fi.vm.sade.valinta.kooste.pistesyotto.excel.PistesyottoDataRiviListAdapter;
import fi.vm.sade.valinta.kooste.pistesyotto.excel.PistesyottoExcel;
import fi.vm.sade.valinta.kooste.pistesyotto.excel.PistesyottoRivi;
import fi.vm.sade.valinta.kooste.util.PoikkeusKasittelijaSovitin;
import fi.vm.sade.valinta.kooste.util.sure.AmmatillisenKielikoetuloksetSurestaConverter;
import fi.vm.sade.valinta.kooste.valvomo.dto.Poikkeus;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.DokumenttiProsessi;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeOsallistuminenDTO;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import static fi.vm.sade.auditlog.valintaperusteet.LogMessage.builder;
import static org.jasig.cas.client.util.CommonUtils.isNotEmpty;

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
    private final SuoritusrekisteriAsyncResource suoritusrekisteriAsyncResource;
    private final TarjontaAsyncResource tarjontaAsyncResource;

    @Autowired
    public PistesyottoTuontiService(ValintalaskentaValintakoeAsyncResource valintakoeResource,
            ValintaperusteetAsyncResource valintaperusteetResource, ApplicationAsyncResource applicationAsyncResource,
            SuoritusrekisteriAsyncResource suoritusrekisteriAsyncResource, TarjontaAsyncResource tarjontaAsyncResource) {
        this.valintakoeResource = valintakoeResource;
        this.valintaperusteetResource = valintaperusteetResource;
        this.applicationAsyncResource = applicationAsyncResource;
        this.suoritusrekisteriAsyncResource = suoritusrekisteriAsyncResource;
        this.tarjontaAsyncResource = tarjontaAsyncResource;
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
            if (t instanceof HttpExceptionWithResponse) {
                LOG.error("Pistesyötön tuonti epäonnistui HTTP-virheilmoitukseen. Sisältö: " + ((HttpExceptionWithResponse) t).contentToString(), t);
            } else {
                LOG.error("Pistesyötön tuonti epäonnistui", t);
            }
            prosessi.getPoikkeukset().add(new Poikkeus(Poikkeus.KOOSTEPALVELU, "Pistesyötön tuonti:", t.getMessage()));
        });
        try {

            final List<String> valintakoeTunnisteet = valintaperusteet.stream().map(vp -> vp.getTunniste()).collect(Collectors.toList());

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
            Map<String, Map<String, String>> uudetKielikoetulokset = new HashMap<>();
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
                                    uudetKielikoetulokset.put(hakemusOid, kielikoeAvaimet.stream().filter(avain -> isNotEmpty(newPistetiedot.get(avain))).collect(Collectors.toMap(
                                            avain -> avain,
                                            avain -> newPistetiedot.get(avain)
                                    )));
                                }
                                kielikoeAvaimet.stream().forEach(a -> newPistetiedot.remove(a));
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
                tallennaUudetKielikoetulokset(username, hakuOid, hakukohdeOid, prosessi, poikkeusilmoitus, uudetKielikoetulokset, pistetiedot, uudetPistetiedot);
            }
        } catch (Throwable t) {
            poikkeusilmoitus.accept(t);
        }
    }

    private void tallennaUudetKielikoetulokset(String username,
                                               String hakuOid,
                                               String hakukohdeOid,
                                               DokumenttiProsessi prosessi,
                                               PoikkeusKasittelijaSovitin poikkeusilmoitus,
                                               Map<String, Map<String, String>> uudetKielikoetulokset,
                                               List<ApplicationAdditionalDataDTO> pistetiedot,
                                               List<ApplicationAdditionalDataDTO> uudetPistetiedot) {
        String valmistuminen = new SimpleDateFormat("dd.MM.yyyy").format(new Date());
        AtomicReference<String> myontajaRef = new AtomicReference<>();
        Supplier<Void> tallennaAdditionalInfo = () -> {
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
            return null;
        };

        Supplier<Void> tallennaKielikoetulokset = () -> {

            AtomicInteger laskuri = new AtomicInteger(uudetKielikoetulokset.values().stream().mapToInt(map -> map.size()).sum());
            if(0 == laskuri.get()) {
                tallennaAdditionalInfo.get();
                return null;
            }
            uudetKielikoetulokset.keySet().stream().forEach(hakemusOid ->
            {
                String personOid = pistetiedot.stream().filter(p -> p.getOid().equals(hakemusOid)).findFirst().get().getPersonOid();
                Map<String, String> kielikoetulokset = uudetKielikoetulokset.get(hakemusOid);
                kielikoetulokset.keySet().stream().filter(t -> isNotEmpty(kielikoetulokset.get(t))).forEach(tunnus -> {
                    String kieli = tunnus.substring(9);

                    Suoritus suoritus = new Suoritus();
                    suoritus.setTila("VALMIS");
                    suoritus.setYksilollistaminen("Ei");
                    suoritus.setHenkiloOid(personOid);
                    suoritus.setVahvistettu(true);
                    suoritus.setSuoritusKieli(kieli.toUpperCase());
                    suoritus.setMyontaja(myontajaRef.get());
                    suoritus.setKomo("ammatillisenKielikoe");
                    suoritus.setValmistuminen(valmistuminen);

                    suoritusrekisteriAsyncResource.postSuoritus(suoritus).subscribe( tallennettuSuoritus -> {
                        prosessi.inkrementoiTehtyjaToita();
                        String arvioArvosana = kielikoetulokset.get(tunnus).toLowerCase();

                        Arvosana arvosana = new Arvosana();
                        arvosana.setAine("kielikoe");
                        arvosana.setLisatieto(kieli.toUpperCase());
                        arvosana.setArvio(new Arvio(arvioArvosana, AmmatillisenKielikoetuloksetSurestaConverter.SURE_ASTEIKKO_HYVAKSYTTY, null));
                        arvosana.setSuoritus(tallennettuSuoritus.getId());

                        suoritusrekisteriAsyncResource.postArvosana(arvosana).subscribe(arvosanaResponse -> {
                            AUDIT.log(builder()
                                    .id(username)
                                    .hakuOid(hakuOid)
                                    .hakukohdeOid(hakukohdeOid)
                                    .hakijaOid(personOid)
                                    .hakemusOid(hakemusOid)
                                    .addAll(ImmutableMap.of("kielikoe_" + kieli.toLowerCase(), arvioArvosana))
                                    .setOperaatio(ValintaperusteetOperation.PISTETIEDOT_TUONTI_EXCEL)
                                    .build());
                            if(0 == laskuri.decrementAndGet()) {
                                tallennaAdditionalInfo.get();
                            }
                        }, poikkeusilmoitus);
                    }, poikkeusilmoitus);
                });
            });
            return null;
        };
        tarjontaAsyncResource.haeHakukohde(hakukohdeOid).subscribe(hakukohde -> {
            prosessi.inkrementoiTehtyjaToita();
            myontajaRef.set(hakukohde.getTarjoajaOids().stream().findFirst().orElse(""));
            tallennaKielikoetulokset.get();
        }, poikkeusilmoitus);
    }

    private Map<String, List<Arvosana>> ammatillisenKielikoeArvosanat(List<Oppija> oppijat) {
        return oppijat.stream().collect(
                Collectors.toMap(Oppija::getOppijanumero,
                        o -> o.getSuoritukset().stream()
                                .filter(sa -> "ammatillisenKielikoe".equalsIgnoreCase(sa.getSuoritus().getKomo())).map(SuoritusJaArvosanat::getArvosanat).flatMap(List::stream)
                                .filter(a -> "kielikoe".equalsIgnoreCase(a.getAine())).collect(Collectors.toList()))
        );
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

    private Collection<String> valintakoeTunnisteet(List<ValintaperusteDTO> valintaperusteet) {
        return valintaperusteet.stream().map(v -> v.getTunniste()).collect(Collectors.toList());
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
            if(haeSuoritusrekisterista) {
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
                    haeSuoritusrekisteristaRef.set(valintakoeTunnisteet(v).stream().anyMatch(t -> t.startsWith("kielikoe_")));
                    haeKielikokeetSuoritusrekisterista.get();
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
