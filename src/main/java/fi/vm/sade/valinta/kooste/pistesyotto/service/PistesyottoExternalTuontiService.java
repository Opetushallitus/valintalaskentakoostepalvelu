package fi.vm.sade.valinta.kooste.pistesyotto.service;


import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import fi.vm.sade.auditlog.valintaperusteet.ValintaperusteetOperation;
import fi.vm.sade.service.valintaperusteet.dto.HakukohdeJaValintaperusteDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteDTO;
import fi.vm.sade.service.valintaperusteet.dto.model.Funktiotyyppi;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.ApplicationAdditionalDataDTO;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaValintakoeAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.pistesyotto.dto.HakemusDTO;
import fi.vm.sade.valinta.kooste.pistesyotto.dto.VirheDTO;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.HakutoiveDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.OsallistuminenTulosDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeOsallistuminenDTO;
import fi.vm.sade.valintalaskenta.domain.valintakoe.Osallistuminen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import rx.Observable;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static fi.vm.sade.auditlog.valintaperusteet.LogMessage.builder;
import static fi.vm.sade.valinta.kooste.KoosteAudit.AUDIT;

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

    @Autowired
    public PistesyottoExternalTuontiService(
            ValintalaskentaValintakoeAsyncResource valintakoeResource,
            ValintaperusteetAsyncResource valintaperusteetResource, ApplicationAsyncResource applicationAsyncResource) {
        this.valintakoeResource = valintakoeResource;
        this.valintaperusteetResource = valintaperusteetResource;
        this.applicationAsyncResource = applicationAsyncResource;
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
                try {
                    pisteet = new BigDecimal(koe.getPisteet());
                } catch (NumberFormatException ne) {
                    return Optional.of("Arvon muuntaminen numeroksi " + koe.getPisteet() + " ei onnistunut");
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
                    StringBuilder errorMsg = new StringBuilder("Hakijaa ei ole kutsuttu hakutoiveen ").append(hakutoiveOid).append(" tunnisteella ").append(koe.getTunniste());
                    VirheDTO virheDTO = new VirheDTO();
                    virheDTO.setHakemusOid(hakemus.getHakemusOid());
                    virheDTO.setVirhe(errorMsg.toString());
                    return Stream.of(new OsallistuminenHakutoiveeseen(koe.getTunniste(), hakutoiveOid, virheDTO));
                }
            }).orElseGet(() -> {
                StringBuilder errorMsg = new StringBuilder("Puuttuva osallistumisen tulos hakutoiveelle ").append(hakutoiveOid).append(" tunnisteella ").append(koe.getTunniste());
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
            String hakutoiveOid, HakemusDTO hakemus, fi.vm.sade.valinta.kooste.pistesyotto.dto.ValintakoeDTO koe,
            ValintaperusteDTO valintaperusteDTO, Optional<ValintakoeOsallistuminenDTO> valintakoeOsallistuminenDTO) {
        return Stream.of(
                osallistumisenTunnistePuuttuuPalvelunKutsujanSyotteesta(hakutoiveOid, hakemus, koe),
                eiOsallistunutJaPisteetAnnettu(hakutoiveOid, hakemus, koe),
                valintaperusteetKaikkiKutsutaan(hakutoiveOid, hakemus,koe,valintaperusteDTO),
                valintalaskentaKutsutaan(hakutoiveOid, hakemus, koe, valintakoeOsallistuminenDTO, valintaperusteDTO)
        ).flatMap(s -> s.get()).findFirst().get();
    }

    private Stream<OsallistuminenHakutoiveeseen> validoi(final HakemusJaHakutoiveet hakemusJaHakutoiveet, List<Hakutoive> hakutoives) {
        final HakemusDTO hakemusDTO = hakemusJaHakutoiveet.hakemusDTO;
        List<OsallistuminenHakutoiveeseen> hakemuksenKokeetStream = hakemusDTO.getValintakokeet().stream().flatMap(koe ->
            hakutoives.stream().flatMap(h -> {
                // Valintakokeen tunnistetta ei löydy valintaperusteet
                if (! h.valintaperusteetDTO.stream().map(valintaperusteDTO -> valintaperusteDTO.getTunniste()).collect(Collectors.toSet()).contains(koe.getTunniste())) {
                    VirheDTO invalidIdentifier = new VirheDTO();
                    invalidIdentifier.setHakemusOid(hakemusDTO.getHakemusOid());
                    invalidIdentifier.setVirhe("Valintakoetta ei löydy annetulle tunnisteelle (" + koe.getTunniste() + ").");
                    return Arrays.asList(new OsallistuminenHakutoiveeseen(koe.getTunniste(), h.hakukohdeOid, invalidIdentifier)).stream();
                }
                Stream<ValintaperusteDTO> valintaperusteStream = h.valintaperusteetDTO.stream().filter(v -> v.getTunniste().equals(koe.getTunniste()));
                return valintaperusteStream.flatMap(v -> {
                    fi.vm.sade.valinta.kooste.pistesyotto.dto.ValintakoeDTO koe1 = koe;
                    Optional<ValintakoeOsallistuminenDTO> ot = h.osallistuminenDTO.stream().findAny();
                    return Stream.of(validoi(h.hakukohdeOid, hakemusDTO, koe1,v,ot));
                });
            })).collect(Collectors.toList());

        final Set<String> onnistuneetKokeet =
                hakemuksenKokeetStream.stream().filter(k -> !k.virhe.isPresent()).map(k -> k.tunniste).collect(Collectors.toSet());

        Stream<OsallistuminenHakutoiveeseen> osallistuminenHakutoiveeseenStream = hakemuksenKokeetStream.stream()
            // filter virheet where koe with same tunniste succeeded in another hakutoive
            .filter(k -> !(k.virhe.isPresent() && onnistuneetKokeet.contains(k.tunniste)));
        return osallistuminenHakutoiveeseenStream;
    }

    private List<HakemusJaHakutoiveet> collect(List<HakemusDTO> hakemukset, List<Hakemus> hakemuses) {
        Map<String, HakemusWrapper> hakemusOID2Hakemus = hakemuses.stream().collect(Collectors.toMap(h -> h.getOid(), h -> new HakemusWrapper(h)));
        Map<String, Collection<String>> hakemusOidJaHakutoiveet = hakemuses.stream().collect(Collectors.toMap(h -> h.getOid(), h -> new HakemusWrapper(h).getHakutoiveOids()));
        return hakemukset.stream().map(h -> new HakemusJaHakutoiveet(
                h,
                Optional.ofNullable(hakemusOidJaHakutoiveet.get(h.getHakemusOid())).orElse(Collections.emptyList()),
                hakemusOID2Hakemus.get(h.getHakemusOid())
        )).collect(Collectors.toList());
    }

    public void tuo(HakukohdeOIDAuthorityCheck authorityCheck, List<HakemusDTO> hakemukset, String username,
                    String hakuOid, String valinnanvaiheOid, BiConsumer<Integer, Collection<VirheDTO>> successHandler,
                    Consumer<Throwable> exceptionHandler) {
        Observable<List<Hakemus>> applicationsByHakemusOids = applicationAsyncResource.getApplicationsByHakemusOids(
                hakemukset.stream().map(HakemusDTO::getHakemusOid).collect(Collectors.toList()));

        applicationsByHakemusOids.subscribe(hakemuses -> {
            List<HakemusJaHakutoiveet> hakemusJaHakutoiveets = collect(hakemukset, hakemuses);
            Set<String> hakutoiveet = hakemusJaHakutoiveets.stream().flatMap(h -> h.hakutoiveet.stream()).collect(Collectors.toSet());
            Observable<List<HakukohdeJaValintaperusteDTO>> valintaperusteetHakutoiveille = valintaperusteetResource.findAvaimet(hakutoiveet);
            Observable<List<ValintakoeOsallistuminenDTO>> osallistumisetHakutoiveille = valintakoeResource.haeHakutoiveille(hakutoiveet);

            Observable.combineLatest(valintaperusteetHakutoiveille, osallistumisetHakutoiveille, (hakukohdeJaValintaperusteDTOs, osallistuminenDTOs) -> {
                Map<String, HakukohdeJaValintaperusteDTO> valintaperusteDTOMap = hakukohdeJaValintaperusteDTOs.stream().collect(
                        Collectors.toMap(h -> h.getHakukohdeOid(), hh -> hh));

                Map<String, List<ValintakoeOsallistuminenDTO>> osallistuminenDTOMap = osallistuminenDTOs.stream().collect(
                        Collectors.toMap(h -> h.getHakemusOid(), h -> Arrays.asList(h), (h0,h1) -> Lists.newArrayList(Iterables.concat(h0,h1))));

                return hakemusJaHakutoiveets.stream().flatMap(
                        h -> {
                            final HakemusDTO hakemusDTO = h.hakemusDTO;
                            final HakemusWrapper hakemus = h.hakemus;
                            if (hakemus == null) {
                                VirheDTO applicationNotFound = new VirheDTO();
                                applicationNotFound.setHakemusOid(hakemusDTO.getHakemusOid());
                                applicationNotFound.setVirhe("Hakemusta ei löydy");
                                return Stream.of(new OsallistuminenHakutoiveeseen(null, null, applicationNotFound));
                            }
                            if (! hakemusDTO.getHenkiloOid().equals(hakemus.getPersonOid())) {
                                VirheDTO conflictingPersonOid = new VirheDTO();
                                conflictingPersonOid.setHakemusOid(hakemusDTO.getHakemusOid());
                                conflictingPersonOid.setVirhe("Annettu henkilö OID ("+ hakemusDTO.getHenkiloOid() +") ei vastaa hakemukselta löytyvää henkilö OID:a ("+ hakemus.getPersonOid() +")");
                                return Stream.of(new OsallistuminenHakutoiveeseen(null, null, conflictingPersonOid));
                            }
                            List<Hakutoive> hakutoiveetList = h.hakutoiveet.stream().map(oid -> new Hakutoive(
                                    oid,
                                    valintaperusteDTOMap.get(oid).getValintaperusteDTO(),
                                    Optional.ofNullable(osallistuminenDTOMap.get(oid)).orElse(Collections.emptyList()))).collect(Collectors.toList());
                            return validoi(h, hakutoiveetList).map(o -> {
                                boolean isAuthorized = authorityCheck.test(o.hakukohdeOid);
                                if(isAuthorized) {
                                    return o;
                                } else {
                                    VirheDTO virheDTO = new VirheDTO();
                                    if(o.isVirhe()) {
                                        virheDTO.setHakemusOid(o.asVirheDTO().getHakemusOid());
                                    } else {
                                        virheDTO.setHakemusOid(o.asApplicationAdditionalDataDTO().getOid());
                                    }
                                    virheDTO.setVirhe("Tarvittavat muokkausoikeudet puuttuu hakutoiveelle " + o.hakukohdeOid);
                                    return new OsallistuminenHakutoiveeseen(o.tunniste, o.hakukohdeOid, virheDTO);
                                }
                            });
                        }
                ).collect(Collectors.toList());
            }).subscribe(osallistumiset -> {
                List<ApplicationAdditionalDataDTO> additionalData =
                        osallistumiset.stream().filter(o -> !o.isVirhe()).map(OsallistuminenHakutoiveeseen::asApplicationAdditionalDataDTO).collect(Collectors.toList());
                List<VirheDTO> virheet = osallistumiset.stream().filter(o -> o.isVirhe()).map(o -> o.asVirheDTO()).collect(Collectors.toList());
                if(!additionalData.isEmpty()) {
                    applicationAsyncResource.putApplicationAdditionalData(
                            hakuOid, additionalData).subscribe(response -> {
                        additionalData.forEach(p ->
                                AUDIT.log(builder()
                                        .id(username)
                                        .hakuOid(hakuOid)
                                        .hakijaOid(p.getPersonOid())
                                        .hakemusOid(p.getOid())
                                        .addAll(p.getAdditionalData())
                                        .setOperaatio(ValintaperusteetOperation.PISTETIEDOT_TUONTI_EXCEL)
                                        .build())
                        );
                        int onnistuneet = additionalData.stream().map(ad -> ad.getOid()).collect(Collectors.toSet()).size();
                        successHandler.accept(onnistuneet, virheet);
                    }, exception -> exceptionHandler.accept(exception));
                } else {
                    successHandler.accept(0, virheet);
                }
            },
            exception -> exceptionHandler.accept(exception));

        }, exception -> exceptionHandler.accept(exception));

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
