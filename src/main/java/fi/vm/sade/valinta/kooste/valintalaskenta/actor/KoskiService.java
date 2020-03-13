package fi.vm.sade.valinta.kooste.valintalaskenta.actor;

import static fi.vm.sade.service.valintaperusteet.dto.model.Funktionimi.ITEROIAMMATILLISETTUTKINNOT;
import static fi.vm.sade.service.valintaperusteet.dto.model.Funktionimi.ITEROIAMMATILLISETTUTKINNOT_LEIKKURIPVM_PARAMETRI;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import fi.vm.sade.service.valintaperusteet.dto.SyoteparametriDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteetDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteetFunktiokutsuDTO;
import fi.vm.sade.service.valintaperusteet.dto.model.Funktionimi;
import fi.vm.sade.valinta.kooste.external.resource.koski.KoskiAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.koski.KoskiOppija;
import fi.vm.sade.valinta.kooste.external.resource.koski.KoskiOppija.OpiskeluoikeusJsonUtil;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valintalaskenta.domain.dto.SuoritustiedotDTO;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class KoskiService {
    private static final Logger LOG = LoggerFactory.getLogger(KoskiService.class);

    private static final Predicate<String> INCLUDE_ALL = s -> true;
    private static final Predicate<String> EXCLUDE_ALL = s -> false;
    private static final Gson GSON = new Gson();
    private static final DateTimeFormatter FINNISH_DATE_FORMAT = DateTimeFormatter.ofPattern("d.M.yyyy");
    private final Predicate<String> koskiHakukohdeOidFilter;
    private final KoskiAsyncResource koskiAsyncResource;
    private final Set<Funktionimi> koskenFunktionimet;
    private final Set<String> koskenOpiskeluoikeusTyypit;

    @Autowired
    public KoskiService(@Value("${valintalaskentakoostepalvelu.laskenta.koskesta.haettavat.hakukohdeoidit:none}") String koskiHakukohdeOiditString,
                        @Value("${valintalaskentakoostepalvelu.laskenta.funktionimet.joille.haetaan.tiedot.koskesta}") String koskenFunktionimetString,
                        @Value("${valintalaskentakoostepalvelu.laskenta.opiskeluoikeustyypit.joille.haetaan.tiedot.koskesta}") String koskenOpiskeluoikeusTyypitString,
                        KoskiAsyncResource koskiAsyncResource) {
        this.koskiAsyncResource = koskiAsyncResource;
        this.koskiHakukohdeOidFilter = resolveKoskiHakukohdeOidFilter(koskiHakukohdeOiditString);
        this.koskenFunktionimet = resolveKoskenFunktionimet(koskenFunktionimetString);
        this.koskenOpiskeluoikeusTyypit = resolveKoskenOpiskeluoikeudet(koskenOpiskeluoikeusTyypitString);
    }

    /**
     * Populoi tiedot löytyneet tiedot <code>suoritustiedotDTO</code> :hon.
     */
    public CompletableFuture<Map<String, KoskiOppija>> haeKoskiOppijat(String hakukohdeOid,
                                                                       CompletableFuture<List<ValintaperusteetDTO>> valintaperusteet,
                                                                       CompletableFuture<List<HakemusWrapper>> hakemukset,
                                                                       SuoritustiedotDTO suoritustiedotDTO) {
        if (koskiHakukohdeOidFilter.test(hakukohdeOid)) {
            return CompletableFuture.allOf(valintaperusteet, hakemukset).thenComposeAsync(x -> {
                Collection<HakemusWrapper> hakemusWrappers = hakemukset.join();
                if (sisaltaaKoskiFunktioita(valintaperusteet.join())) {
                    LocalDate paivaJonkaMukaisiaTietojaKoskiDatastaKaytetaan = etsiKoskiDatanLeikkuriPvm(valintaperusteet.join(), hakukohdeOid);
                    return haeKoskiOppijat(hakukohdeOid, hakemusWrappers, suoritustiedotDTO, paivaJonkaMukaisiaTietojaKoskiDatastaKaytetaan);
                } else {
                    LOG.info("Ei haeta tietoja Koskesta hakukohteelle " + hakukohdeOid + " , koska siltä ei löydy Koski-tietoja käyttäviä valintaperusteita.");
                    return CompletableFuture.completedFuture(Collections.emptyMap());
                }
            });
        } else {
            LOG.info("Ei haeta tietoja Koskesta hakukohteelle " + hakukohdeOid + " , koska sitä ei ole listattu Koski-hakua varten.");
            return CompletableFuture.completedFuture(Collections.emptyMap());
        }
    }

    private CompletionStage<Map<String, KoskiOppija>> haeKoskiOppijat(String hakukohdeOid,
                                                                      Collection<HakemusWrapper> hakemusWrappers,
                                                                      SuoritustiedotDTO suoritustiedotDTO,
                                                                      LocalDate paivaJonkaMukaisiaTietojaKoskiDatastaKaytetaan) {
        LOG.info(String.format("Haetaan Koskesta tiedot %d oppijalle hakukohteen %s laskemista varten.", hakemusWrappers.size(), hakukohdeOid));
        List<String> oppijanumeroitJoiltaKoskiOpiskeluoikeudetPuuttuvat = hakemusWrappers.stream()
            .map(HakemusWrapper::getPersonOid)
            .filter(oppijanumero -> !suoritustiedotDTO.onKoskiopiskeluoikeudet(oppijanumero))
            .collect(Collectors.toList());
        int maaraJoilleTiedotJoLoytyvat = hakemusWrappers.size() - oppijanumeroitJoiltaKoskiOpiskeluoikeudetPuuttuvat.size();
        return koskiAsyncResource
            .findKoskiOppijat(oppijanumeroitJoiltaKoskiOpiskeluoikeudetPuuttuvat)
            .thenApplyAsync(koskioppijat -> {
                LOG.info(String.format("Saatiin Koskesta %s uuden oppijan tiedot, kun haettiin %d/%d oppijalle (%s:lle oli jo haettu tiedot) hakukohteen %s laskemista varten.",
                    koskioppijat.size(), oppijanumeroitJoiltaKoskiOpiskeluoikeudetPuuttuvat.size(), hakemusWrappers.size(), maaraJoilleTiedotJoLoytyvat, hakukohdeOid));
                haeUusimmatLeikkuripaivaaEdeltavatOpiskeluoikeudetTarvittaessa(koskioppijat, paivaJonkaMukaisiaTietojaKoskiDatastaKaytetaan);
                Map<String, KoskiOppija> koskiOppijatOppijanumeroittain = koskioppijat.stream().collect(Collectors.toMap(KoskiOppija::getOppijanumero, Function.identity()));
                oppijanumeroitJoiltaKoskiOpiskeluoikeudetPuuttuvat.forEach(oppijanumero -> {
                    KoskiOppija loytynytOppija = koskiOppijatOppijanumeroittain.get(oppijanumero);
                    if (loytynytOppija != null) {
                        suoritustiedotDTO.asetaKoskiopiskeluoikeudet(oppijanumero, GSON.toJson(loytynytOppija.haeOpiskeluoikeudet(koskenOpiskeluoikeusTyypit)));
                    } else {
                        suoritustiedotDTO.asetaKoskiopiskeluoikeudet(oppijanumero, "[]");
                    }
                });
                return koskiOppijatOppijanumeroittain;
            });
    }

    private boolean sisaltaaKoskiFunktioita(List<ValintaperusteetDTO> valintaperusteet) {
        if (koskenFunktionimet.isEmpty()) {
            return false;
        }
        return valintaperusteet.stream()
            .anyMatch(valintaperusteetDTO -> valintaperusteetDTO.getValinnanVaihe().getValintatapajono().stream()
                .anyMatch(jono -> jono.getJarjestyskriteerit().stream()
                    .anyMatch(kriteeri ->
                        sisaltaaKoskiFunktioita(kriteeri.getFunktiokutsu()))));
    }

    private boolean sisaltaaKoskiFunktioita(ValintaperusteetFunktiokutsuDTO funktiokutsu) {
        if (koskenFunktionimet.contains(funktiokutsu.getFunktionimi())) {
            return true;
        }
        return funktiokutsu.getFunktioargumentit().stream()
            .anyMatch(argumentti ->
                sisaltaaKoskiFunktioita(argumentti.getFunktiokutsu()));
    }

    private LocalDate etsiKoskiDatanLeikkuriPvm(List<ValintaperusteetDTO> valintaperusteetDTOS, String hakukohdeOid) {
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

    private void haeUusimmatLeikkuripaivaaEdeltavatOpiskeluoikeudetTarvittaessa(Set<KoskiOppija> koskioppijat, LocalDate leikkuriPvm) {
        koskioppijat.forEach(koskiOppija -> {
            if (koskiOppija.sisaltaaUudempiaOpiskeluoikeuksiaKuin(leikkuriPvm, koskenOpiskeluoikeusTyypit)) {
                LOG.info(String.format("Oppijalle %s löytyi Koskesta haluttujen tyyppien %s opiskeluoikeuksia, jotka on päivitetty leikkuripäivämäärän %s jälkeen. " +
                    "Haetaan tuoreimmat leikkuripäivämäärää edeltävät tiedot.", koskiOppija.getOppijanumero(), koskenOpiskeluoikeusTyypit,
                    FINNISH_DATE_FORMAT.format(leikkuriPvm)));
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
            LOG.info(String.format("Koskesta haetun oppijan %s opiskeluoikeuden %s aikaleima on %s eli ennen leikkuripäivämäärää %s ja versio %d, joten huomioidaan tämä opiskeluoikeus.",
                koskiOppija.getOppijanumero(),
                opiskeluoikeudenOid,
                aikaleima,
                FINNISH_DATE_FORMAT.format(leikkuriPvm),
                versionumero));
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

    private static Predicate<String> resolveKoskiHakukohdeOidFilter(String koskiHakukohdeOiditString) {
        if (StringUtils.isBlank(koskiHakukohdeOiditString)) {
            LOG.info("Saatiin '" + koskiHakukohdeOiditString + "' Koskesta haettaviksi hakukohdeoideiksi => ei haeta ollenkaan tietoja Koskesta.");
            return EXCLUDE_ALL;
        }
        if ("ALL".equals(koskiHakukohdeOiditString)) {
            LOG.info("Saatiin '" + koskiHakukohdeOiditString + "' Koskesta haettaviksi hakukohdeoideiksi => haetaan tiedot Koskesta kaikille hakukohteille, " +
                "joille löytyy Koski-dataa käyttäviä valintaperusteita.");
            return INCLUDE_ALL;
        }
        List<String> hakukohdeOids = Arrays.asList(koskiHakukohdeOiditString.split(","));
        LOG.info("Saatiin '" + koskiHakukohdeOiditString + "' Koskesta haettaviksi hakukohdeoideiksi => haetaan Koskesta tiedot seuraaville hakukohteille: " +
            hakukohdeOids + " , jos niistä löytyy Koski-dataa käyttäviä valintaperusteita.");
        return hakukohdeOids::contains;
    }

    private static Set<Funktionimi> resolveKoskenFunktionimet(String koskenFunktionimetString) {
        if (StringUtils.isBlank(koskenFunktionimetString)) {
            LOG.info("Saatiin '" + koskenFunktionimetString + "' funktionimiksi, joille haetaan dataa Koskesta => ei haeta ollenkaan tietoja Koskesta.");
            return Collections.emptySet();
        }
        Set<Funktionimi> funktionimet = Arrays.stream(koskenFunktionimetString.split(","))
            .map(Funktionimi::valueOf)
            .collect(Collectors.toSet());
        LOG.info("Saatiin '" + koskenFunktionimetString + "' funktionimiksi, joille haetaan dataa Koskesta => haetaan Koskesta tietoja vain hakukohteille, " +
            "joiden valintaperusteissa käytetään seuraavannimisiä funktioita: " + funktionimet);
        return funktionimet;
    }

    private static Set<String> resolveKoskenOpiskeluoikeudet(String koskenOpiskeluoikeusTyypitString) {
        if (StringUtils.isBlank(koskenOpiskeluoikeusTyypitString)) {
            LOG.info("Saatiin '" + koskenOpiskeluoikeusTyypitString + "' opiskeluoikeuksiksi, joille haetaan dataa Koskesta => ei haeta ollenkaan tietoja Koskesta.");
            return Collections.emptySet();
        }
        Set<String> opiskeluoikeusTyypit = Arrays.stream(koskenOpiskeluoikeusTyypitString.split(","))
            .collect(Collectors.toSet());
        LOG.info("Saatiin '" + koskenOpiskeluoikeusTyypitString + "' opiskeluoikeuksien tyypeiksi, joille haetaan dataa Koskesta => haetaan Koskesta tietoja vain opiskeluoikeuksista, " +
            "tyypin koodiarvo on jokin seuraavista: " + opiskeluoikeusTyypit);
        return opiskeluoikeusTyypit;
    }
}
