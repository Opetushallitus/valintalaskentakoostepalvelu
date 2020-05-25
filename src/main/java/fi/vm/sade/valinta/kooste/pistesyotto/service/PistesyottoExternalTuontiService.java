package fi.vm.sade.valinta.kooste.pistesyotto.service;


import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import fi.vm.sade.auditlog.Changes;
import fi.vm.sade.service.valintaperusteet.dto.HakukohdeJaValintaperusteDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteDTO;
import fi.vm.sade.service.valintaperusteet.dto.model.Funktiotyyppi;
import fi.vm.sade.valinta.kooste.KoosteAudit;
import fi.vm.sade.valinta.kooste.external.resource.ataru.AtaruAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.ApplicationAdditionalDataDTO;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaValintakoeAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintapiste.ValintapisteAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintapiste.dto.Valintapisteet;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.AuditSession;
import fi.vm.sade.valinta.kooste.pistesyotto.dto.HakemusDTO;
import fi.vm.sade.valinta.kooste.pistesyotto.dto.VirheDTO;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valinta.sharedutils.AuditLog;
import fi.vm.sade.valinta.sharedutils.ValintaResource;
import fi.vm.sade.valinta.sharedutils.ValintaperusteetOperation;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.HakutoiveDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.OsallistuminenTulosDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeOsallistuminenDTO;
import fi.vm.sade.valintalaskenta.domain.valintakoe.Osallistuminen;
import io.reactivex.Observable;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Used when importing points from outside, such as
 * - Soteli
 * - DIA-yhteisvalinta
 */
public class PistesyottoExternalTuontiService {
    private static final Logger LOG = LoggerFactory.getLogger(PistesyottoExternalTuontiService.class);
    private final ValintalaskentaValintakoeAsyncResource valintakoeResource;
    private final ValintaperusteetAsyncResource valintaperusteetResource;
    private final ApplicationAsyncResource applicationAsyncResource;
    private final AtaruAsyncResource ataruAsyncResource;
    private final ValintapisteAsyncResource valintapisteAsyncResource;
    @Autowired
    public PistesyottoExternalTuontiService(
            ValintalaskentaValintakoeAsyncResource valintakoeResource,
            ValintaperusteetAsyncResource valintaperusteetResource,
            ValintapisteAsyncResource valintapisteAsyncResource,
            ApplicationAsyncResource applicationAsyncResource,
            AtaruAsyncResource ataruAsyncResource) {
        this.valintakoeResource = valintakoeResource;
        this.valintaperusteetResource = valintaperusteetResource;
        this.valintapisteAsyncResource = valintapisteAsyncResource;
        this.applicationAsyncResource = applicationAsyncResource;
        this.ataruAsyncResource = ataruAsyncResource;
    }

    private Supplier<Stream<OsallistuminenHakutoiveeseen>> osallistumisenTunnistePuuttuuPalvelunKutsujanSyotteesta(
            String hakutoiveOid, HakemusDTO hakemus, fi.vm.sade.valinta.kooste.pistesyotto.dto.ValintakoeDTO koe) {
        return () -> {
            boolean osallistumisenTunnistePuuttuuPalvelunKutsujanSyotteesta = koe.getOsallistuminen() == null;
            if (osallistumisenTunnistePuuttuuPalvelunKutsujanSyotteesta) {
                VirheDTO virheDTO = new VirheDTO();
                virheDTO.setHakemusOid(hakemus.getHakemusOid());
                virheDTO.setVirhe("Osallistumistieto puuttui tunnisteelle " + koe.getTunniste());
                return Stream.of(new OsallistuminenHakutoiveeseen(koe.getTunniste(), hakutoiveOid, virheDTO));
            } else {
                return Stream.empty();
            }
        };
    }
    private Supplier<Stream<OsallistuminenHakutoiveeseen>> eiOsallistunutJaPisteetAnnettu(
            String hakutoiveOid, HakemusDTO hakemus, fi.vm.sade.valinta.kooste.pistesyotto.dto.ValintakoeDTO koe
    ) {
        return () -> {
            if (koe.getOsallistuminen() == fi.vm.sade.valinta.kooste.pistesyotto.dto.ValintakoeDTO.Osallistuminen.EI_OSALLISTUNUT &&
                    koe.getPisteet() != null) {
                VirheDTO virheDTO = new VirheDTO();
                virheDTO.setHakemusOid(hakemus.getHakemusOid());
                virheDTO.setVirhe("Ristiriita: kokeelle " + koe.getTunniste() + " annettu pisteet (" +
                        koe.getPisteet() + ")  ja osallistuminen (" + koe.getOsallistuminen().name() + ").");
                return Stream.of(new OsallistuminenHakutoiveeseen(koe.getTunniste(), hakutoiveOid, virheDTO));
            } else {
                return Stream.empty();
            }
        };
    }
    private Optional<BigDecimal> asLimit(String limit) {
        return Optional.ofNullable(limit).flatMap(l -> {
            try {
                return Optional.ofNullable(new BigDecimal(l));
            } catch (Throwable t) {
                return Optional.empty();
            }
        });
    }
    private Optional<String> validoiSyote(fi.vm.sade.valinta.kooste.pistesyotto.dto.ValintakoeDTO koe, ValintaperusteDTO peruste) {
        if(fi.vm.sade.valinta.kooste.pistesyotto.dto.ValintakoeDTO.Osallistuminen.OSALLISTUI.equals(koe.getOsallistuminen())) {
            if (Funktiotyyppi.LUKUARVOFUNKTIO.equals(peruste.getFunktiotyyppi())) {
                BigDecimal pisteet;
                Optional<String> badPointsMessage = Optional.of(
                    String.format("Pisteiden arvon '%s' muuntaminen numeroksi ei onnistunut", koe.getPisteet()));
                try {
                    if (koe.getPisteet() == null) {
                        return badPointsMessage;
                    }
                    pisteet = new BigDecimal(koe.getPisteet());
                } catch (NumberFormatException ne) {
                    return badPointsMessage;
                }
                if (peruste.getArvot() != null && !peruste.getArvot().isEmpty()) {
                    // Diskreettiarvo
                    boolean diskreettiArvoLoytyi = peruste.getArvot().stream().map(BigDecimal::new).anyMatch(pisteet::equals);
                    if (diskreettiArvoLoytyi) {
                        return Optional.empty();
                    } else {
                        return Optional.of("Arvo " + koe.getPisteet() + " ei ole joukossa " + Arrays.toString(peruste.getArvot().toArray()));
                    }
                } else {
                    // Mahdollisesti raja-arvot
                    Optional<BigDecimal> max = asLimit(peruste.getMax());
                    Optional<BigDecimal> min = asLimit(peruste.getMin());
                    boolean ylarajaKunnossa = max.map(m -> pisteet.compareTo(m) <= 0).orElse(true);
                    boolean alarajaKunnossa = min.map(m -> pisteet.compareTo(m) >= 0).orElse(true);
                    if (!ylarajaKunnossa) {
                        return Optional.of("Suurin sallittu arvo on " + peruste.getMax());
                    }
                    if (!alarajaKunnossa) {
                        return Optional.of("Pienin sallittu arvo on " + peruste.getMin());
                    }
                    return Optional.empty();
                }
            } else if (Funktiotyyppi.TOTUUSARVOFUNKTIO.equals(peruste.getFunktiotyyppi())) {
                boolean pisteetIsBooleanString = Arrays.asList(Boolean.TRUE.toString(), Boolean.FALSE.toString()).contains(koe.getPisteet());
                if (!pisteetIsBooleanString) {
                    return Optional.of("Totuusarvo on muotoa true tai false");
                }
                return Optional.empty();
            } else {
                return Optional.of("Tuntematon funktiotyyppi " + peruste.getFunktiotyyppi());
            }
        } else {
            return Optional.empty();
        }
    }
    private Supplier<Stream<OsallistuminenHakutoiveeseen>> valintaperusteetKaikkiKutsutaan(
            String hakutoiveOid, HakemusDTO hakemus, fi.vm.sade.valinta.kooste.pistesyotto.dto.ValintakoeDTO koe,
            ValintaperusteDTO peruste) {
        return () -> {
            final boolean valintaperusteetKaikkiKutsutaan = Boolean.TRUE.equals(peruste.getSyotettavissaKaikille());
            if(valintaperusteetKaikkiKutsutaan) {
                return getOsallistuminenHakutoiveeseenStream(hakutoiveOid, hakemus, koe, peruste);
            } else {
                return Stream.empty();
            }
        };
    }
    private Supplier<Stream<OsallistuminenHakutoiveeseen>> valintalaskentaKutsutaan(
            String hakutoiveOid, HakemusDTO hakemus, fi.vm.sade.valinta.kooste.pistesyotto.dto.ValintakoeDTO koe,
            Optional<ValintakoeOsallistuminenDTO> valintakoeOsallistuminenDTO, ValintaperusteDTO peruste) {
        return () -> {
            Optional<HakutoiveDTO> hakutoiveDTO = valintakoeOsallistuminenDTO.flatMap(vk -> vk.getHakutoiveet().stream().filter(hk -> hakutoiveOid.equals(hk.getHakukohdeOid())).findAny());
            Optional<fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeDTO> valintakoeDTO1 =
                    hakutoiveDTO.flatMap(hk -> hk.getValinnanVaiheet().stream().flatMap(vv -> vv.getValintakokeet().stream()).filter(vk -> peruste.getTunniste().equals(vk.getValintakoeTunniste())).findAny());
            return valintakoeDTO1.map(vk -> {
                final Optional<OsallistuminenTulosDTO> o = Optional.ofNullable(vk.getOsallistuminenTulos());
                final boolean osallistuu = o.map(o0 -> Osallistuminen.OSALLISTUU.equals(o0.getOsallistuminen())).orElse(false);
                final boolean laskentaKaikkiKutsutaan = Boolean.TRUE.equals(vk.getKutsutaankoKaikki());
                if(laskentaKaikkiKutsutaan || osallistuu) {
                    return getOsallistuminenHakutoiveeseenStream(hakutoiveOid, hakemus, koe, peruste);
                } else {
                    String errorMsg = "Hakemuksen " + hakemus.getHakemusOid() + " hakijaa ei ole kutsuttu hakutoiveen " + hakutoiveOid + " tunnisteella " + koe.getTunniste();
                    LOG.error(errorMsg);
                    VirheDTO virheDTO = new VirheDTO();
                    virheDTO.setHakemusOid(hakemus.getHakemusOid());
                    virheDTO.setVirhe(errorMsg);
                    return Stream.of(new OsallistuminenHakutoiveeseen(koe.getTunniste(), hakutoiveOid, virheDTO));
                }
            }).orElseGet(() -> {
                StringBuilder errorMsg = new StringBuilder("Puuttuva osallistumisen tulos hakutoiveelle ").append(hakutoiveOid).append(" tunnisteella ").append(koe.getTunniste());
                LOG.error(errorMsg.toString());
                VirheDTO virheDTO = new VirheDTO();
                virheDTO.setHakemusOid(hakemus.getHakemusOid());
                virheDTO.setVirhe(errorMsg.toString());
                return Stream.of(new OsallistuminenHakutoiveeseen(koe.getTunniste(), hakutoiveOid, virheDTO));
            });
        };
    }

    private Stream<OsallistuminenHakutoiveeseen> getOsallistuminenHakutoiveeseenStream(
            String hakutoiveOid, HakemusDTO hakemus, fi.vm.sade.valinta.kooste.pistesyotto.dto.ValintakoeDTO koe,
            ValintaperusteDTO peruste) {
        Optional<String> syoteVirhe = validoiSyote(koe,peruste);
        OsallistuminenHakutoiveeseen osallistuminenHakutoiveeseen = syoteVirhe.map(virhe -> {
            VirheDTO virheDto = new VirheDTO();
            virheDto.setHakemusOid(hakemus.getHakemusOid());
            virheDto.setVirhe("Validointivirhe pisteiden (" +koe.getPisteet()+ ") tuonnissa tunnisteelle " + koe.getTunniste() + ". " + virhe);
            return new OsallistuminenHakutoiveeseen(koe.getTunniste(), hakutoiveOid, virheDto);
        }).orElseGet(() -> {
        ApplicationAdditionalDataDTO additionalDataDTO = new ApplicationAdditionalDataDTO();
        additionalDataDTO.setOid(hakemus.getHakemusOid());
        additionalDataDTO.setPersonOid(hakemus.getHenkiloOid());
        additionalDataDTO.setAdditionalData(
                ImmutableMap.of(
                        peruste.getOsallistuminenTunniste(), koe.getOsallistuminen().toString(),
                        peruste.getTunniste(),
                        // only set pisteet for osallistuja
                        Optional.of(koe.getOsallistuminen()).filter(o -> fi.vm.sade.valinta.kooste.pistesyotto.dto.ValintakoeDTO.Osallistuminen.OSALLISTUI.equals(o)).map(o -> koe.getPisteet()).orElse("")
                ));
        return new OsallistuminenHakutoiveeseen(koe.getTunniste(), hakutoiveOid, additionalDataDTO);
        });
        return Stream.of(osallistuminenHakutoiveeseen);
    }

    private OsallistuminenHakutoiveeseen validoi(
            String hakutoiveOid, HakemusDTO pistetiedotHakemukselle, fi.vm.sade.valinta.kooste.pistesyotto.dto.ValintakoeDTO koe,
            ValintaperusteDTO valintaperusteDTO, Optional<ValintakoeOsallistuminenDTO> valintakoeOsallistuminenDTO) {
        return Stream.of(
                osallistumisenTunnistePuuttuuPalvelunKutsujanSyotteesta(hakutoiveOid, pistetiedotHakemukselle, koe),
                eiOsallistunutJaPisteetAnnettu(hakutoiveOid, pistetiedotHakemukselle, koe),
                valintaperusteetKaikkiKutsutaan(hakutoiveOid, pistetiedotHakemukselle, koe, valintaperusteDTO),
                valintalaskentaKutsutaan(hakutoiveOid, pistetiedotHakemukselle, koe, valintakoeOsallistuminenDTO, valintaperusteDTO)
        ).flatMap(s -> s.get()).findFirst().get();
    }

    private Stream<OsallistuminenHakutoiveeseen> luoOsallistumisetHakemukselle(final HakemusJaHakutoiveet hakemusJaHakutoiveet, List<Hakutoive> hakutoives) {
        final HakemusDTO pistetiedotHakemukselle = hakemusJaHakutoiveet.hakemusDTO;
        List<OsallistuminenHakutoiveeseen> hakemuksenKokeetStream = pistetiedotHakemukselle.getValintakokeet().stream().flatMap(koe ->
            hakutoives.stream().flatMap(hakutoive -> {
                // Valintakokeen tunnistetta ei löydy tämän hakutoiveen valintaperusteista
                if (!hakutoive.valintaperusteetDTO.stream().map(valintaperusteDTO -> valintaperusteDTO.getTunniste()).collect(Collectors.toSet()).contains(koe.getTunniste())) {
                    String errorMessage = "Valintakoetta ei löydy annetulle tunnisteelle (" + koe.getTunniste() + ") käsiteltäessä hakemusta " + pistetiedotHakemukselle.getHakemusOid() + " hakutoiveelle " + hakutoive.hakukohdeOid;
                    LOG.warn(errorMessage);
                    VirheDTO invalidIdentifier = new VirheDTO();
                    invalidIdentifier.setHakemusOid(pistetiedotHakemukselle.getHakemusOid());
                    invalidIdentifier.setVirhe(errorMessage);
                    return Arrays.asList(new OsallistuminenHakutoiveeseen(koe.getTunniste(), hakutoive.hakukohdeOid, invalidIdentifier)).stream();
                }
                Stream<ValintaperusteDTO> valintaperusteStream = hakutoive.valintaperusteetDTO.stream().filter(v -> v.getTunniste().equals(koe.getTunniste()));
                return valintaperusteStream.flatMap(valintaperuste -> {
                    Optional<ValintakoeOsallistuminenDTO> osallistuminen = hakutoive.osallistuminenDTO.stream().findAny();
                    return Stream.of(validoi(hakutoive.hakukohdeOid, pistetiedotHakemukselle, koe, valintaperuste, osallistuminen));
                });
            })).collect(Collectors.toList());

        final Set<String> onnistuneetKokeet =
                hakemuksenKokeetStream.stream().filter(k -> !k.virhe.isPresent()).map(k -> k.tunniste).collect(Collectors.toSet());

        Stream<OsallistuminenHakutoiveeseen> osallistuminenHakutoiveeseenStream = hakemuksenKokeetStream.stream()
            // filter virheet where koe with same tunniste succeeded in another hakutoive
            .filter(k -> !(k.virhe.isPresent() && onnistuneetKokeet.contains(k.tunniste)));
        return osallistuminenHakutoiveeseenStream;
    }

    private List<HakemusJaHakutoiveet> collect(List<HakemusDTO> hakemukset, List<HakemusWrapper> hakemuses) {
        Map<String, HakemusWrapper> hakemusOID2Hakemus = hakemuses.stream().collect(Collectors.toMap(HakemusWrapper::getOid, h -> h));
        Map<String, Collection<String>> hakemusOidJaHakutoiveet = hakemuses.stream().collect(Collectors.toMap(HakemusWrapper::getOid, HakemusWrapper::getHakutoiveOids));
        return hakemukset.stream().map(h -> new HakemusJaHakutoiveet(
                h,
                Optional.ofNullable(hakemusOidJaHakutoiveet.get(h.getHakemusOid())).orElse(Collections.emptyList()),
                hakemusOID2Hakemus.get(h.getHakemusOid())
        )).collect(Collectors.toList());
    }

    private Observable<List<HakemusWrapper>> getHakemuksetByHakemusOids(List<String> hakemusOids) {
        return Observable.fromFuture(ataruAsyncResource.getApplicationsByOids(hakemusOids))
                .flatMap(hakemukset -> {
                    if (hakemukset.isEmpty()) {
                        return applicationAsyncResource.getApplicationsByHakemusOids(hakemusOids);
                    } else {
                        return Observable.just(hakemukset);
                    }
                });
    }

    public void tuo(HakukohdeOIDAuthorityCheck authorityCheck, List<HakemusDTO> pistetiedotHakemuksille, AuditSession auditSession,
                    String hakuOid, BiConsumer<Integer, Collection<VirheDTO>> successHandler,
                    Consumer<Throwable> exceptionHandler) {
        getHakemuksetByHakemusOids(pistetiedotHakemuksille.stream().map(HakemusDTO::getHakemusOid).collect(Collectors.toList()))
                .subscribe(hakemusWrappers -> {
                    List<HakemusJaHakutoiveet> pistetiedotJaHakemukset = collect(pistetiedotHakemuksille, hakemusWrappers);
                    Set<String> hakutoiveet = pistetiedotJaHakemukset.stream().flatMap(h -> h.hakutoiveet.stream()).collect(Collectors.toSet());
                    Observable<List<HakukohdeJaValintaperusteDTO>> valintaperusteetHakutoiveille = valintaperusteetResource.findAvaimet(hakutoiveet);
                    Observable<List<ValintakoeOsallistuminenDTO>> osallistumisetHakutoiveille = valintakoeResource.haeHakutoiveille(hakutoiveet);

                    Observable.combineLatest(valintaperusteetHakutoiveille, osallistumisetHakutoiveille, (hakukohdeJaValintaperusteDTOs, osallistuminenDTOs) -> {
                        Map<String, HakukohdeJaValintaperusteDTO> hakukohdeOidToValintaperusteDTOMap = hakukohdeJaValintaperusteDTOs.stream().collect(
                                Collectors.toMap(HakukohdeJaValintaperusteDTO::getHakukohdeOid, hh -> hh));

                        osallistuminenDTOs.stream().forEach(o -> o.getHakutoiveet().forEach(ht -> ht.getValinnanVaiheet().forEach(vv -> vv.getValintakokeet())));
                        Map<String, List<ValintakoeOsallistuminenDTO>> hakemusOidToValintakoeOsallistuminenDTOMap = osallistuminenDTOs.stream().collect(
                                Collectors.toMap(ValintakoeOsallistuminenDTO::getHakemusOid, Arrays::asList, (h0, h1) -> Lists.newArrayList(Iterables.concat(h0, h1))));

                        return pistetiedotJaHakemukset.stream().flatMap(
                                pistetiedotJaHakemus -> {
                                    final HakemusDTO pistetiedot = pistetiedotJaHakemus.hakemusDTO;
                                    final HakemusWrapper hakemus = pistetiedotJaHakemus.hakemus;
                                    if (hakemus == null) {
                                        String errorMessage = "Hakemusta ei löydy tuotaessa pistetietoja hakemukselle " + pistetiedot.getHakemusOid();
                                        LOG.error(errorMessage);
                                        VirheDTO applicationNotFound = new VirheDTO();
                                        applicationNotFound.setHakemusOid(pistetiedot.getHakemusOid());
                                        applicationNotFound.setVirhe(errorMessage);
                                        return Stream.of(new OsallistuminenHakutoiveeseen(null, null, applicationNotFound));
                                    }
                                    if (!pistetiedot.getHenkiloOid().equals(hakemus.getPersonOid())) {
                                        String errorMessage = "Annettu henkilö OID (" + pistetiedot.getHenkiloOid() + ") ei vastaa hakemukselta löytyvää henkilö OID:a (" + hakemus.getPersonOid() + ")";
                                        LOG.error(errorMessage);
                                        VirheDTO conflictingPersonOid = new VirheDTO();
                                        conflictingPersonOid.setHakemusOid(pistetiedot.getHakemusOid());
                                        conflictingPersonOid.setVirhe(errorMessage);
                                        return Stream.of(new OsallistuminenHakutoiveeseen(null, null, conflictingPersonOid));
                                    }
                                    List<Hakutoive> hakutoiveetList = pistetiedotJaHakemus.hakutoiveet.stream().map(hakukohdeOid -> new Hakutoive(
                                            hakukohdeOid,
                                            hakukohdeOidToValintaperusteDTOMap.get(hakukohdeOid).getValintaperusteDTO(),
                                            Optional.ofNullable(hakemusOidToValintakoeOsallistuminenDTOMap.get(pistetiedot.getHakemusOid())).orElse(Collections.emptyList()))).collect(Collectors.toList());
                                    return luoOsallistumisetHakemukselle(pistetiedotJaHakemus, hakutoiveetList).map(osallistuminen -> {
                                        boolean isAuthorized = authorityCheck.test(osallistuminen.hakukohdeOid);
                                        if (isAuthorized) {
                                            return osallistuminen;
                                        } else {
                                            String errorMessage = "Tarvittavat muokkausoikeudet puuttuvat hakutoiveelle " + osallistuminen.hakukohdeOid;
                                            VirheDTO virheDTO = new VirheDTO();
                                            if (osallistuminen.isVirhe()) {
                                                virheDTO.setHakemusOid(osallistuminen.asVirheDTO().getHakemusOid());
                                            } else {
                                                virheDTO.setHakemusOid(osallistuminen.asApplicationAdditionalDataDTO().getOid());
                                            }
                                            virheDTO.setVirhe(errorMessage);
                                            return new OsallistuminenHakutoiveeseen(osallistuminen.tunniste, osallistuminen.hakukohdeOid, virheDTO);
                                        }
                                    });
                                }
                        ).collect(Collectors.toList());
                    }).subscribe(osallistumiset -> {
                                List<ApplicationAdditionalDataDTO> additionalData =
                                        osallistumiset.stream().filter(o -> !o.isVirhe()).map(OsallistuminenHakutoiveeseen::asApplicationAdditionalDataDTO).collect(Collectors.toList());
                                List<VirheDTO> virheet = osallistumiset.stream().filter(OsallistuminenHakutoiveeseen::isVirhe).map(OsallistuminenHakutoiveeseen::asVirheDTO).collect(Collectors.toList());
                                if (!additionalData.isEmpty()) {
                                    List<Valintapisteet> vp = additionalData.stream().map(a -> Pair.of(auditSession.getPersonOid(), a)).map(Valintapisteet::new).collect(Collectors.toList());
                                    Observable.fromFuture(valintapisteAsyncResource.putValintapisteet(Optional.empty(), vp, auditSession)).subscribe(conflictingHakemusOids -> {
                                        additionalData.forEach(pistetieto -> {
                                            Map<String, String> additionalAuditInfo = new HashMap<>();
                                            additionalAuditInfo.put("Username from params", auditSession.getPersonOid());
                                            additionalAuditInfo.put("hakuOid", hakuOid);
                                            additionalAuditInfo.put("hakijaOid", pistetieto.getPersonOid());
                                            AuditLog.log(KoosteAudit.AUDIT, auditSession.asAuditUser(), ValintaperusteetOperation.PISTETIEDOT_TUONTI_EXCEL, ValintaResource.PISTESYOTTOEXTERNALSERVICE, pistetieto.getOid(), Changes.addedDto(pistetieto), additionalAuditInfo);
                                        });
                                        List<VirheDTO> valintapisteVirheet = conflictingHakemusOids.stream()
                                                .map(hakemusOid -> {
                                                    String errorMessage = "Yritettiin kirjoittaa yli uudempia pistetietoja hakemukselle " + hakemusOid;
                                                    LOG.error(errorMessage);
                                                    VirheDTO virhe = new VirheDTO();
                                                    virhe.setHakemusOid(hakemusOid);
                                                    virhe.setVirhe(errorMessage);
                                                    return virhe;
                                                })
                                                .collect(Collectors.toList());
                                        virheet.addAll(valintapisteVirheet);
                                        int onnistuneet = additionalData.stream().map(ApplicationAdditionalDataDTO::getOid).collect(Collectors.toSet()).size() - valintapisteVirheet.size();
                                        successHandler.accept(onnistuneet, virheet);
                                    }, exceptionHandler::accept);
                                } else {
                                    successHandler.accept(0, virheet);
                                }
                            },
                            exceptionHandler::accept);

                }, exceptionHandler::accept);

    }
    private static class OsallistuminenHakutoiveeseen {
        public final String tunniste;
        public final String hakukohdeOid;
        private final Optional<VirheDTO> virhe;
        private final Optional<ApplicationAdditionalDataDTO> addData;

        public OsallistuminenHakutoiveeseen(String tunniste, String hakukohdeOid, ApplicationAdditionalDataDTO applicationAdditionalDataDTO) {
            this.hakukohdeOid = hakukohdeOid;
            this.tunniste = tunniste;
            this.virhe = Optional.empty();
            this.addData = Optional.of(applicationAdditionalDataDTO);
        }
        public OsallistuminenHakutoiveeseen(String tunniste, String hakukohdeOid, VirheDTO virheDTO) {
            this.hakukohdeOid = hakukohdeOid;
            this.tunniste = tunniste;
            this.virhe = Optional.of(virheDTO);
            this.addData = Optional.empty();
        }

        public boolean isVirhe() {
            return virhe.isPresent();
        }
        public VirheDTO asVirheDTO() {
            return virhe.get();
        }

        public ApplicationAdditionalDataDTO asApplicationAdditionalDataDTO() {
            return addData.get();
        }
    }

    private static class Hakutoive {
        public final String hakukohdeOid;
        public final List<ValintaperusteDTO> valintaperusteetDTO;
        public final List<ValintakoeOsallistuminenDTO> osallistuminenDTO;
        public Hakutoive(String hakukohdeOid, List<ValintaperusteDTO> valintaperusteetDTO, List<ValintakoeOsallistuminenDTO> osallistuminenDTO) {
            this.hakukohdeOid = hakukohdeOid;
            this.valintaperusteetDTO = valintaperusteetDTO;
            this.osallistuminenDTO = osallistuminenDTO;
        }
    }

    private static class HakemusJaHakutoiveet {
        public final HakemusDTO hakemusDTO;
        public final Collection<String> hakutoiveet;
        public final HakemusWrapper hakemus;
        public HakemusJaHakutoiveet(HakemusDTO hakemusDTO, Collection<String> hakutoiveet, HakemusWrapper hakemus) {
            this.hakemusDTO = hakemusDTO;
            this.hakutoiveet = hakutoiveet;
            this.hakemus = hakemus;
        }

    }
}
