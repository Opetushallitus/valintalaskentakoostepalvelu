package fi.vm.sade.valinta.kooste.pistesyotto.service;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteDTO;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.ApplicationAdditionalDataDTO;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaValintakoeAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.pistesyotto.excel.PistesyottoArvo;
import fi.vm.sade.valinta.kooste.pistesyotto.excel.PistesyottoDataRiviListAdapter;
import fi.vm.sade.valinta.kooste.pistesyotto.excel.PistesyottoExcel;
import fi.vm.sade.valinta.kooste.pistesyotto.excel.PistesyottoRivi;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.Laskuri;
import fi.vm.sade.valinta.kooste.valvomo.dto.Poikkeus;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.DokumenttiProsessi;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeOsallistuminenDTO;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.InputStream;
import java.util.*;
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
public class PistesyottoTuontiService {

    private static final Logger LOG = LoggerFactory.getLogger(PistesyottoTuontiService.class);
    private final ValintalaskentaValintakoeAsyncResource valintakoeResource;
    private final ValintaperusteetAsyncResource valintaperusteetResource;
    private final ApplicationAsyncResource applicationAsyncResource;

    @Autowired
    public PistesyottoTuontiService(
            ValintalaskentaValintakoeAsyncResource valintakoeResource,
            ValintaperusteetAsyncResource valintaperusteetResource,
            ApplicationAsyncResource applicationAsyncResource
    ) {
        this.valintakoeResource = valintakoeResource;
        this.valintaperusteetResource = valintaperusteetResource;
        this.applicationAsyncResource = applicationAsyncResource;
    }

    private void tuo(
            String hakuOid, String hakukohdeOid,
            DokumenttiProsessi prosessi,
            List<ValintakoeOsallistuminenDTO> osallistumistiedot,
            List<ApplicationAdditionalDataDTO> pistetiedot,
            List<ValintaperusteDTO> valintaperusteet,
            InputStream stream) {

        String hakuNimi = StringUtils.EMPTY;
        String hakukohdeNimi = StringUtils.EMPTY;
        String tarjoajaNimi = StringUtils.EMPTY;
        Consumer<Throwable> poikkeusilmoitus = t -> {
            LOG.error("Pistesyötön tuonti epäonnistui", t);
            prosessi.getPoikkeukset().add(
                    new Poikkeus(Poikkeus.KOOSTEPALVELU,
                            "Pistesyötön tuonti:", t.getMessage()));
        };
        try {

            Collection<String> valintakoeTunnisteet = FluentIterable
                    .from(valintaperusteet)
                    .transform(
                            new Function<ValintaperusteDTO, String>() {
                                @Override
                                public String apply(
                                        ValintaperusteDTO input) {
                                    return input.getTunniste();
                                }
                            }).toList();

            List<Hakemus> hakemukset = Collections.emptyList();
            // LOG.error("Excelin luonti");
            PistesyottoDataRiviListAdapter pistesyottoTuontiAdapteri = new PistesyottoDataRiviListAdapter();
            PistesyottoExcel pistesyottoExcel = new PistesyottoExcel(
                    hakuOid, hakukohdeOid, null, hakuNimi,
                    hakukohdeNimi, tarjoajaNimi, hakemukset,
                    Collections.emptySet(),
                    valintakoeTunnisteet, osallistumistiedot,
                    valintaperusteet, pistetiedot,
                    pistesyottoTuontiAdapteri);
            pistesyottoExcel.getExcel().tuoXlsx(stream);
            Map<String, ApplicationAdditionalDataDTO> pistetiedotMapping = asMap(pistetiedot);


            // TARKISTETAAN VIRHEET
            List<String> virheet = pistesyottoTuontiAdapteri
                    .getRivit().stream()
                    .filter(rivi -> !rivi.isValidi()).flatMap(
                            rivi -> {
                                LOG.warn("Rivi on muuttunut mutta viallinen joten ilmoitetaan virheestä!");

                                for (PistesyottoArvo arvo : rivi.getArvot()) {
                                    if (!arvo.isValidi()) {
                                        String virheIlmoitus = new StringBuffer()
                                                .append("Henkilöllä ")
                                                .append(rivi.getNimi())
                                                        //
                                                .append(" (")
                                                .append(rivi.getOid())
                                                .append(")")
                                                        //
                                                .append(" oli virheellinen arvo '")
                                                .append(arvo.getArvo())
                                                .append("'")
                                                .append(" kohdassa ")
                                                .append(arvo.getTunniste())
                                                .toString();
                                        return Stream.of(virheIlmoitus);
													 /*

													 */
                                    }
                                }
                                return Stream.empty();
                            }
                    ).collect(Collectors.toList());
            if (!virheet.isEmpty()) {
                String v = virheet.stream().collect(Collectors.joining(", "));
                LOG.error("Virheitä pistesyöttöriveissä {}", v);
                prosessi
                        .getPoikkeukset()
                        .add(new Poikkeus(
                                "Pistesyötön tuonti",
                                "", v));
                return;
            }
            List<ApplicationAdditionalDataDTO> uudetPistetiedot =
                    pistesyottoTuontiAdapteri
                            .getRivit().stream()
                            //
                            .filter(PistesyottoRivi::isValidi)
                                    //
                            .flatMap(rivi -> {
                                ApplicationAdditionalDataDTO additionalData = pistetiedotMapping
                                        .get(rivi.getOid());
                                Map<String, String> newPistetiedot = rivi
                                        .asAdditionalData();
                                if (newPistetiedot == null || newPistetiedot.isEmpty()) {
                                    LOG.info("Rivi on muuttunut ja eheä mutta pistetiedot on tyhjäjoukko. Hakemuspalvelu tallentaa vain muutokset joten ohitetaan rivi.");
                                    return Stream.empty();
                                }
                                LOG.debug("Rivi on muuttunut ja eheä. Tehdään päivitys hakupalveluun");
                                additionalData
                                        .setAdditionalData(newPistetiedot);
                                return Stream.of(additionalData);
                            }).filter(Objects::nonNull).collect(Collectors.toList());

            if (uudetPistetiedot.isEmpty()) {
                LOG.info("Ei tallennettavaa");
            } else {
                //applicationAsyncResource.
                applicationAsyncResource.putApplicationAdditionalData(
                        hakuOid, hakukohdeOid, uudetPistetiedot, response -> {
                            prosessi.setDokumenttiId("valmis");
                            prosessi.inkrementoiTehtyjaToita();
                        }, poikkeusilmoitus);
            }
        } catch(Throwable t) {
            poikkeusilmoitus.accept(t);
        }
    }



    public void tuo(String hakuOid, String hakukohdeOid, DokumenttiProsessi prosessi,InputStream stream){
        Consumer<Throwable> poikkeusilmoitus = t -> {
            LOG.error("Pistesyötön tuonti epäonnistui", t);
            prosessi.getPoikkeukset().add(
                    new Poikkeus(Poikkeus.KOOSTEPALVELU,
                            "Pistesyötön tuonti:", t.getMessage()));
        };
        AtomicReference<List<ValintakoeOsallistuminenDTO>> osallistumistiedot = new AtomicReference<>();
        AtomicReference<List<ApplicationAdditionalDataDTO>> additionaldata = new AtomicReference<>();
        AtomicReference<List<ValintaperusteDTO>> valintaperusteet = new AtomicReference<>();
        AtomicInteger laskuri = new AtomicInteger(4);
        AtomicInteger laskuriYlimaaraisilleOsallistujille = new AtomicInteger(2);
        Supplier<Void> viimeisteleTuonti = () -> {
            if(laskuri.decrementAndGet() <= 0) {
                tuo(hakuOid,hakukohdeOid,prosessi,osallistumistiedot.get(),additionaldata.get(),valintaperusteet.get(),stream);
            }
            return null;
        };
        Supplier<Void> tarkistaYlimaaraisetOsallistujat = () -> {
            if(laskuriYlimaaraisilleOsallistujille.decrementAndGet() <= 0) {
                //osallistumistiedot.get().stream().filter(o -> o.getHakutoiveet().stream().anyMatch(o2 -> hakukohdeOid.equals(o2.getHakukohdeOid())));
                Set<String> osallistujienHakemusOids = Sets.newHashSet(osallistumistiedot.get().stream().map(o -> o.getHakemusOid()).collect(Collectors.toSet()));
                osallistujienHakemusOids.removeAll(additionaldata.get().stream().map(a -> a.getOid()).collect(Collectors.toSet()));
                if(!osallistujienHakemusOids.isEmpty()) {
                    // haetaan puuttuvat
                    applicationAsyncResource.getApplicationAdditionalData(osallistujienHakemusOids, a -> {
                        additionaldata.set(Stream.concat(additionaldata.get().stream(), a.stream()).collect(Collectors.toList()));
                        viimeisteleTuonti.get();
                    },poikkeusilmoitus);
                } else {
                    viimeisteleTuonti.get();
                }
            }
            return null;
        };
        valintakoeResource.haeHakutoiveelle(hakukohdeOid,
                o -> {
                    prosessi
                    .inkrementoiTehtyjaToita();
                    osallistumistiedot.set(o);
                    tarkistaYlimaaraisetOsallistujat.get();
                    viimeisteleTuonti.get();
        }, poikkeusilmoitus);

        valintaperusteetResource
                .findAvaimet(hakukohdeOid,
                        v -> {
                            valintaperusteet.set(v);
                            prosessi
                                    .inkrementoiTehtyjaToita();
                            viimeisteleTuonti.get();
                        },
                        poikkeusilmoitus);

        applicationAsyncResource.getApplicationAdditionalData(hakuOid, hakukohdeOid,
                a -> {
                    additionaldata.set(a);
                    prosessi
                            .inkrementoiTehtyjaToita();
                    tarkistaYlimaaraisetOsallistujat.get();
                    viimeisteleTuonti.get();
                },
                poikkeusilmoitus);

    }
    private Map<String, ApplicationAdditionalDataDTO> asMap(
            Collection<ApplicationAdditionalDataDTO> datas) {
        Map<String, ApplicationAdditionalDataDTO> mapping = Maps.newHashMap();
        for (ApplicationAdditionalDataDTO data : datas) {
            mapping.put(data.getOid(), data);
        }
        return mapping;
    }
}
