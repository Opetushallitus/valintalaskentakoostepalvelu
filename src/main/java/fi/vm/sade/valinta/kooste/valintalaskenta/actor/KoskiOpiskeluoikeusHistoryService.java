package fi.vm.sade.valinta.kooste.valintalaskenta.actor;

import static fi.vm.sade.service.valintaperusteet.dto.model.Funktionimi.ITEROIAMMATILLISETTUTKINNOT;
import static fi.vm.sade.service.valintaperusteet.dto.model.Funktionimi.ITEROIAMMATILLISETTUTKINNOT_LEIKKURIPVM_PARAMETRI;
import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import fi.vm.sade.service.valintaperusteet.dto.SyoteparametriDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteetDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteetFunktiokutsuDTO;
import fi.vm.sade.valinta.kooste.external.resource.koski.KoskiAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.koski.KoskiOppija;
import fi.vm.sade.valinta.kooste.external.resource.koski.KoskiOppija.OpiskeluoikeusJsonUtil;
import fi.vm.sade.valinta.kooste.util.CompletableFutureUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class KoskiOpiskeluoikeusHistoryService {
    private static final Logger LOG = LoggerFactory.getLogger(KoskiOpiskeluoikeusHistoryService.class);

    private static final DateTimeFormatter FINNISH_DATE_FORMAT = DateTimeFormatter.ofPattern("d.M.yyyy");
    private final KoskiAsyncResource koskiAsyncResource;

    @Autowired
    public KoskiOpiskeluoikeusHistoryService(KoskiAsyncResource koskiAsyncResource) {
        this.koskiAsyncResource = koskiAsyncResource;
    }

    LocalDate etsiKoskiDatanLeikkuriPvm(List<ValintaperusteetDTO> valintaperusteetDTOS, String hakukohdeOid) {
        List<String> leikkuriPvmMerkkijonot = etsiTutkintojenIterointiFunktioKutsut(valintaperusteetDTOS)
            .flatMap(tutkintojenIterointiFunktio -> tutkintojenIterointiFunktio.getSyoteparametrit().stream()
                .filter(parametri -> ITEROIAMMATILLISETTUTKINNOT_LEIKKURIPVM_PARAMETRI.equals(parametri.getAvain()) && StringUtils.isNotBlank(parametri.getArvo()))
                .map(SyoteparametriDTO::getArvo))
            .collect(Collectors.toList());
        LocalDate kaytettavaLeikkuriPvm = leikkuriPvmMerkkijonot.stream()
            .map(pvm -> LocalDate.parse(pvm, FINNISH_DATE_FORMAT))
            .max(Comparator.naturalOrder()).orElse(LocalDate.now());
        LOG.info(String.format("Saatiin hakukohteen %s valintaperusteista Koski-datan leikkuripäivämäärät %s. Käytetään leikkuripäivämääränä arvoa %s.",
            hakukohdeOid, leikkuriPvmMerkkijonot, FINNISH_DATE_FORMAT.format(kaytettavaLeikkuriPvm)));
        return kaytettavaLeikkuriPvm;
    }

    static Stream<ValintaperusteetFunktiokutsuDTO> etsiTutkintojenIterointiFunktioKutsut(List<ValintaperusteetDTO> valintaperusteetDTOS) {
        return valintaperusteetDTOS.stream()
            .flatMap(valintaperusteetDTO -> valintaperusteetDTO.getValinnanVaihe().getValintatapajono().stream())
            .flatMap(jono -> jono.getJarjestyskriteerit().stream())
            .flatMap(kriteeri ->
                etsiFunktiokutsutRekursiivisesti(
                    kriteeri.getFunktiokutsu(),
                    fk -> ITEROIAMMATILLISETTUTKINNOT.equals(fk.getFunktionimi())).stream());
    }

    static private List<ValintaperusteetFunktiokutsuDTO> etsiFunktiokutsutRekursiivisesti(ValintaperusteetFunktiokutsuDTO juuriFunktioKutsu,
                                                                                   Predicate<ValintaperusteetFunktiokutsuDTO> predikaatti) {
        List<ValintaperusteetFunktiokutsuDTO> tulokset = new LinkedList<>();
        if (predikaatti.test(juuriFunktioKutsu)) {
            tulokset.add(juuriFunktioKutsu);
        }
        tulokset.addAll(juuriFunktioKutsu.getFunktioargumentit().stream()
            .flatMap(argumentti -> etsiFunktiokutsutRekursiivisesti(argumentti.getFunktiokutsu(), predikaatti).stream())
            .collect(Collectors.toSet()));
        return tulokset;
    }

    void haeVanhemmatOpiskeluoikeudetTarvittaessa(Set<KoskiOppija> koskioppijat, LocalDate leikkuriPvm) {
        koskioppijat.forEach(koskiOppija ->
            koskiOppija.setOpiskeluoikeudet(haeOpiskeluoikeuksistaPaivanMukainenVersio(
                koskiOppija,
                leikkuriPvm).join()));
    }

    private CompletableFuture<JsonArray> haeOpiskeluoikeuksistaPaivanMukainenVersio(KoskiOppija koskiOppija, LocalDate leikkuriPvm) {
        return CompletableFutureUtil.sequence(Lists.newArrayList(koskiOppija.getOpiskeluoikeudet()).stream().map(opiskeluoikeus -> {
            if (OpiskeluoikeusJsonUtil.onUudempiKuin(leikkuriPvm, opiskeluoikeus)) {
                LocalDateTime aikaleima = OpiskeluoikeusJsonUtil.aikaleima(opiskeluoikeus);
                String opiskeluoikeudenOid = OpiskeluoikeusJsonUtil.oid(opiskeluoikeus);
                LOG.info(String.format("Koskesta haetun oppijan %s opiskeluoikeuden %s aikaleima on %s eli leikkuripäivämäärän %s jälkeen." +
                        " Etsitään opiskeluoikeudesta versioita, jotka olisi tallennettu ennen leikkuripäivämäärää.",
                    koskiOppija.getOppijanumero(),
                    opiskeluoikeudenOid,
                    aikaleima,
                    FINNISH_DATE_FORMAT.format(leikkuriPvm)));
                return haePaivamaaranMukainenVersio(koskiOppija, leikkuriPvm, CompletableFuture.completedFuture(opiskeluoikeus));
            } else {
                return CompletableFuture.completedFuture(Optional.of(opiskeluoikeus));
            }
        }).collect(Collectors.toList())).thenApplyAsync(opiskeluoikeusOptiot -> {
            JsonArray result = new JsonArray();
            opiskeluoikeusOptiot.forEach(o -> o.ifPresent(result::add));
            return result;
        });
    }

    private CompletableFuture<Optional<JsonElement>> haePaivamaaranMukainenVersio(KoskiOppija koskiOppija, LocalDate leikkuriPvm, CompletableFuture<JsonElement> opiskeluoikeusF) {
        return opiskeluoikeusF.thenComposeAsync(opiskeluoikeus -> {
            int versionumero = OpiskeluoikeusJsonUtil.versionumero(opiskeluoikeus);
            LocalDateTime aikaleima = OpiskeluoikeusJsonUtil.aikaleima(opiskeluoikeus);
            String opiskeluoikeudenOid = OpiskeluoikeusJsonUtil.oid(opiskeluoikeus);
            if (!OpiskeluoikeusJsonUtil.onUudempiKuin(leikkuriPvm, aikaleima)) {
                LOG.info(String.format("Koskesta haetun oppijan %s opiskeluoikeuden %s version %d aikaleima on %s eli ennen leikkuripäivämäärää %s, joten huomioidaan tämä opiskeluoikeus.",
                    koskiOppija.getOppijanumero(),
                    opiskeluoikeudenOid,
                    versionumero,
                    aikaleima,
                    FINNISH_DATE_FORMAT.format(leikkuriPvm)
                ));
                return CompletableFuture.completedFuture(Optional.of(opiskeluoikeus));
            }
            if (versionumero <= 1) {
                LOG.info(String.format("Koskesta haetun oppijan %s opiskeluoikeuden %s aikaleima on %s eli leikkuripäivämäärän %s jälkeen," +
                        " ja versio %d, joten vanhempia versioita ei ole. Jätetään opiskeluoikeus huomioimatta.",
                    koskiOppija.getOppijanumero(),
                    opiskeluoikeudenOid,
                    aikaleima,
                    FINNISH_DATE_FORMAT.format(leikkuriPvm),
                    versionumero));
                return CompletableFuture.completedFuture(Optional.empty());
            }
            CompletableFuture<JsonElement> edellisenVersionOpiskeluoikeus = koskiAsyncResource.findVersionOfOpiskeluoikeus(opiskeluoikeudenOid, versionumero - 1);
            return haePaivamaaranMukainenVersio(koskiOppija, leikkuriPvm, edellisenVersionOpiskeluoikeus);
        });
    }
}
