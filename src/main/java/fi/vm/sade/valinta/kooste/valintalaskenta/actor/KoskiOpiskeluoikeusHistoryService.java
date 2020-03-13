package fi.vm.sade.valinta.kooste.valintalaskenta.actor;

import static fi.vm.sade.service.valintaperusteet.dto.model.Funktionimi.ITEROIAMMATILLISETTUTKINNOT;
import static fi.vm.sade.service.valintaperusteet.dto.model.Funktionimi.ITEROIAMMATILLISETTUTKINNOT_LEIKKURIPVM_PARAMETRI;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import fi.vm.sade.service.valintaperusteet.dto.SyoteparametriDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteetDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteetFunktiokutsuDTO;
import fi.vm.sade.valinta.kooste.external.resource.koski.KoskiAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.koski.KoskiOppija;
import fi.vm.sade.valinta.kooste.external.resource.koski.KoskiOppija.OpiskeluoikeusJsonUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class KoskiOpiskeluoikeusHistoryService {
    private static final Logger LOG = LoggerFactory.getLogger(KoskiOpiskeluoikeusHistoryService.class);

    private static final DateTimeFormatter FINNISH_DATE_FORMAT = DateTimeFormatter.ofPattern("d.M.yyyy");
    private final KoskiAsyncResource koskiAsyncResource;
    private final Set<String> koskenOpiskeluoikeusTyypit;

    @Autowired
    public KoskiOpiskeluoikeusHistoryService(Set<String> koskenOpiskeluoikeusTyypit, KoskiAsyncResource koskiAsyncResource) {
        this.koskiAsyncResource = koskiAsyncResource;
        this.koskenOpiskeluoikeusTyypit = koskenOpiskeluoikeusTyypit;
    }

    LocalDate etsiKoskiDatanLeikkuriPvm(List<ValintaperusteetDTO> valintaperusteetDTOS, String hakukohdeOid) {
        List<String> leikkuriPvmMerkkijonot = etsiTutkintojenIterointiFunktioKutsut(valintaperusteetDTOS)
            .flatMap(tutkintojenIterointiFunktio -> tutkintojenIterointiFunktio.getSyoteparametrit().stream()
                .filter(parametri -> ITEROIAMMATILLISETTUTKINNOT_LEIKKURIPVM_PARAMETRI.equals(parametri.getAvain()) && StringUtils.isNotBlank(parametri.getArvo()))
                .map(SyoteparametriDTO::getArvo))
            .collect(Collectors.toList());
        List<LocalDate> kaikkiLeikkuriPvmtValintaperusteista = leikkuriPvmMerkkijonot.stream()
            .map(pvm -> LocalDate.parse(pvm, FINNISH_DATE_FORMAT))
            .sorted()
            .collect(Collectors.toList());
        LocalDate kaytettavaLeikkuriPvm;
        if (!kaikkiLeikkuriPvmtValintaperusteista.isEmpty()) {
            kaytettavaLeikkuriPvm = kaikkiLeikkuriPvmtValintaperusteista.get(kaikkiLeikkuriPvmtValintaperusteista.size() - 1);
        } else {
            kaytettavaLeikkuriPvm = LocalDate.now();
        }
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

    void haeUusimmatLeikkuripaivaaEdeltavatOpiskeluoikeudetTarvittaessa(Set<KoskiOppija> koskioppijat, LocalDate leikkuriPvm) {
        koskioppijat.forEach(koskiOppija -> {
            final Set<JsonElement> liianUudetOpiskeluoikeudet = koskiOppija.opiskeluoikeudetJotkaOvatUudempiaKuin(leikkuriPvm, koskenOpiskeluoikeusTyypit);
            if (!liianUudetOpiskeluoikeudet.isEmpty()) {
                koskiOppija.setOpiskeluoikeudet(haeOpiskeluoikeuksistaPaivanMukainenVersio(
                    koskiOppija,
                    leikkuriPvm,
                    koskiOppija.haeOpiskeluoikeudet(koskenOpiskeluoikeusTyypit)));
            }
        });
    }

    private JsonArray haeOpiskeluoikeuksistaPaivanMukainenVersio(KoskiOppija koskiOppija, LocalDate leikkuriPvm, JsonArray opiskeluoikeudet) {
        JsonArray tulokset = new JsonArray();
        opiskeluoikeudet.forEach(opiskeluoikeus -> {
            if (OpiskeluoikeusJsonUtil.onUudempiKuin(leikkuriPvm, opiskeluoikeus)) {
                LocalDateTime aikaleima = OpiskeluoikeusJsonUtil.aikaleima(opiskeluoikeus);
                String opiskeluoikeudenOid = OpiskeluoikeusJsonUtil.oid(opiskeluoikeus);
                LOG.info(String.format("Koskesta haetun oppijan %s opiskeluoikeuden %s aikaleima on %s eli leikkuripäivämäärän %s jälkeen." +
                        " Etsitään opiskeluoikeudesta versioita, jotka olisi tallennettu ennen leikkuripäivämäärää.",
                    koskiOppija.getOppijanumero(),
                    opiskeluoikeudenOid,
                    aikaleima,
                    FINNISH_DATE_FORMAT.format(leikkuriPvm)));
                haePaivamaaranMukainenVersio(koskiOppija, leikkuriPvm, opiskeluoikeus).ifPresent(tulokset::add);
            } else {
                tulokset.add(opiskeluoikeus);
            }
        });
        return tulokset;
    }

    private Optional<JsonElement> haePaivamaaranMukainenVersio(KoskiOppija koskiOppija, LocalDate leikkuriPvm, JsonElement opiskeluoikeus) {
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
            return Optional.of(opiskeluoikeus);
        }
        if (versionumero <= 1) {
            LOG.info(String.format("Koskesta haetun oppijan %s opiskeluoikeuden %s aikaleima on %s eli leikkuripäivämäärän %s jälkeen," +
                    " ja versio %d, joten vanhempia versioita ei ole. Jätetään opiskeluoikeus huomioimatta.",
                koskiOppija.getOppijanumero(),
                opiskeluoikeudenOid,
                aikaleima,
                FINNISH_DATE_FORMAT.format(leikkuriPvm),
                versionumero));
            return Optional.empty();
        }
        JsonElement edellisenVersionOpiskeluoikeus = koskiAsyncResource.findVersionOfOpiskeluoikeus(opiskeluoikeudenOid, versionumero - 1).join();
        return haePaivamaaranMukainenVersio(koskiOppija, leikkuriPvm, edellisenVersionOpiskeluoikeus);
    }
}
