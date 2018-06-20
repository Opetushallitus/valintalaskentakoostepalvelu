package fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti;

import com.google.common.collect.ImmutableMap;
import fi.vm.sade.organisaatio.resource.dto.HakutoimistoDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaPaginationObject;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakutoiveenValintatapajonoDTO;
import fi.vm.sade.valinta.kooste.parametrit.ParametritParser;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valinta.kooste.viestintapalvelu.Hakijapalvelu;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.MetaHakukohde;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoite;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterBatch;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterBatchStatusDto;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterResponse;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl.HyvaksymiskirjeetServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.functions.Func2;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ViestintapalveluObservables {

    private static final Logger LOG = LoggerFactory.getLogger(ViestintapalveluObservables.class);
    private static final String VALMIS_STATUS = "ready";
    private static final String KESKEYTETTY_STATUS = "error";

    public static class ResponseWithBatchId {
        public final LetterBatchStatusDto resp;
        public final String batchId;

        public ResponseWithBatchId(LetterBatchStatusDto resp, String batchId) {
            this.resp = resp;
            this.batchId = batchId;
        }
    }

    public static class TarjoajaWithOsoite {

        public final String tarjoajaOid;
        public final Optional<Osoite> hakutoimisto;

        public TarjoajaWithOsoite(String tarjoajaOid, Optional<Osoite> hakutoimisto) {
            this.tarjoajaOid = tarjoajaOid;
            this.hakutoimisto = hakutoimisto;
        }
    }

    public static class HakutoiveJaJono {
        public final String hakukohdeOid;
        public final HakutoiveenValintatapajonoDTO jono;

        public HakutoiveJaJono(String hakukohdeOid, HakutoiveenValintatapajonoDTO jono) {
            this.hakukohdeOid = hakukohdeOid;
            this.jono = jono;
        }
    }

    public static class HakukohdeJaResurssit {
        public final String hakukohdeOid;
        public final List<HakijaDTO> hakijat;
        public final Collection<HakemusWrapper> hakemukset;

        public HakukohdeJaResurssit(String hakukohdeOid, List<HakijaDTO> hakijat, Collection<HakemusWrapper> hakemukset) {
            this.hakukohdeOid = hakukohdeOid;
            this.hakijat = hakijat;
            this.hakemukset = hakemukset;
        }
    }

    public static class HaunResurssit {

        public final List<HakijaDTO> hakijat;
        public final Collection<HakemusWrapper> hakemukset;

        public HaunResurssit(List<HakijaDTO> hakijat, Collection<HakemusWrapper> hakemukset) {
            this.hakijat = hakijat;
            this.hakemukset = hakemukset;
        }
    }

    public static Observable<HaunResurssit> haunResurssit(String asiointikieli,
                                                          Observable<HakijaPaginationObject> koulutuspaikalliset,
                                                          Function<List<String>, Observable<List<HakemusWrapper>>> haeHakemukset) {
        return resurssit(koulutuspaikalliset, haeHakemukset, (hakemukset, hakijat) ->
                filtteroiAsiointikielella(asiointikieli, new HaunResurssit(hakijat.getResults(), hakemukset)));
    }

    public static Observable<List<HakukohdeJaResurssit>> hakukohteetJaResurssit(Observable<HakijaPaginationObject> koulutuspaikalliset,
                                                                                Function<List<String>, Observable<List<HakemusWrapper>>> haeHakemukset) {
        return resurssit(koulutuspaikalliset, haeHakemukset, (hakemukset, hakijat) -> hakukohteetOpetuskielella(hakijat, hakemukset));
    }

    public static <T> Observable<T> resurssit(Observable<HakijaPaginationObject> koulutusPaikalliset,
                                              Function<List<String>, Observable<List<HakemusWrapper>>> haeHakemukset,
                                              Func2<List<HakemusWrapper>, HakijaPaginationObject, T> zipper) {
        return Observable.zip(
                koulutusPaikalliset.flatMap(hakijat -> haeHakemukset.apply(hakijat.getResults().stream().map(HakijaDTO::getHakemusOid).collect(Collectors.toList()))),
                koulutusPaikalliset,
                (hakemukset, hakijat) -> {
                    LOG.info("Saatiin haulle hyväksyttyjä {} kpl", hakijat.getResults().size());
                    LOG.info("Saatiin haulle hakemuksia {} kpl", hakemukset.size());
                    return zipper.call(hakemukset, hakijat);
                }
        );
    }

    public static Observable<Map<String, Optional<Osoite>>> haunOsoitteet(String asiointikieli, Map<String, MetaHakukohde> hakukohteet, Function<String, Observable<Optional<HakutoimistoDTO>>> hakutoimisto) {
        return Observable.from(hakukohteet.values())
                .flatMap(hakukohde -> {
                    String tarjoajaOid = hakukohde.getTarjoajaOid();
                    return hakutoimisto.apply(tarjoajaOid)
                            .map(toimisto -> {
                                Optional<Osoite> o = toimisto.map(t ->Hakijapalvelu.osoite(t, asiointikieli)).orElse(Optional.empty());
                                if (!o.isPresent()) {
                                    LOG.error("Tarjoajalla {} ei osoitetta", tarjoajaOid);
                                }
                                return o;
                            })
                            .map(osoite -> new TarjoajaWithOsoite(tarjoajaOid, osoite));
                })
                .collect(HashMap::new, (map, pair) -> map.put(pair.tarjoajaOid, pair.hakutoimisto));
    }

    public static Observable<Map<String, Optional<Osoite>>> hakukohteenOsoite(String hakukohdeOid, String tarjoajaOid,
                                                                              Map<String, MetaHakukohde> hyvaksymiskirjeessaKaytetytHakukohteet,
                                                                              Function<String, Observable<Optional<HakutoimistoDTO>>> hakutoimistoFn) {
        MetaHakukohde kohdeHakukohde = hyvaksymiskirjeessaKaytetytHakukohteet.get(hakukohdeOid);
        return hakutoimistoFn.apply(kohdeHakukohde.getTarjoajaOid())
                .map(hakutoimistoDTO -> ImmutableMap.of(tarjoajaOid,
                            hakutoimistoDTO.map(h -> Hakijapalvelu.osoite(h, kohdeHakukohde.getHakukohteenKieli())).orElse(Optional.<Osoite>empty())));
    }

    public static Observable<LetterBatch> kirjeet(String hakuOid, Optional<String> asiointikieli, List<HakijaDTO> hyvaksytytHakijat,
                                                  Collection<HakemusWrapper> hakemukset, String defaultValue, Map<String, MetaHakukohde> hyvaksymiskirjeessaKaytetytHakukohteet,
                                                  Observable<Map<String, Optional<Osoite>>> addresses, HyvaksymiskirjeetKomponentti hyvaksymiskirjeetKomponentti, HyvaksymiskirjeetServiceImpl hyvaksymiskirjeetServiceImpl,
                                                  ParametritParser haunParametrit, boolean sahkoinenKorkeakoulunMassaposti) {
        return addresses.map(hakijapalveluidenOsoite -> hyvaksymiskirjeetKomponentti
                .teeHyvaksymiskirjeet(
                        hakijapalveluidenOsoite,
                        hyvaksymiskirjeessaKaytetytHakukohteet,
                        hyvaksytytHakijat,
                        hakemukset,
                        hakuOid,
                        asiointikieli,
                        //
                        defaultValue,
                        hakuOid, // nimiUriToTag(h.getHakukohteenNimiUri(), hakukohdeOid.get());
                        "hyvaksymiskirje",
                        hyvaksymiskirjeetServiceImpl.parsePalautusPvm(null, haunParametrit),
                        hyvaksymiskirjeetServiceImpl.parsePalautusAika(null, haunParametrit),
                        asiointikieli.isPresent(),
                        sahkoinenKorkeakoulunMassaposti));
    }

    public static Observable<String> batchId(Observable<LetterBatch> hyvaksymiskirje,
                                             Function<LetterBatch, Observable<LetterResponse>> vieDokumentti,
                                             Function<String, Observable<LetterBatchStatusDto>> haeStatusFn, Long delay,
                                             Function<ResponseWithBatchId, Observable<String>> statusHandler) {

        return hyvaksymiskirje
                .doOnNext(batch -> LOG.info("##### Viedään dokumentti viestintäpalveluun"))
                .flatMap(vieDokumentti::apply)
                .doOnNext(letterResponse -> {
                    LOG.info("##### Dokumentin vietin onnistui");
                    LOG.info("##### Odotetaan statusta, batchid={}", letterResponse.getBatchId());
                })
                .flatMap(letterResponse -> Observable.interval(1, TimeUnit.SECONDS)
                        .take((int) TimeUnit.MINUTES.toSeconds(delay))
                        .flatMap(i -> haeStatusFn.apply(letterResponse.getBatchId())
                                .zipWith(Observable.just(letterResponse.getBatchId()), ResponseWithBatchId::new))
                        .skipWhile(status -> !VALMIS_STATUS.equals(status.resp.getStatus()) && !KESKEYTETTY_STATUS.equals(status.resp.getStatus())))
                .take(1)
                .flatMap(status -> {
                    if (KESKEYTETTY_STATUS.equals(status.resp.getStatus())) {
                        return Observable.error(new RuntimeException("Viestintäpalvelun statuspyyntö palautti virheen"));
                    } else {
                        LOG.info("Viestintäpalvelun status on valmis");
                        return statusHandler.apply(status);
                    }
                });
    }

    /*
     * Helper functions
     */

    public static Long getDelay(Optional<String> hakukohdeOid) {
        return hakukohdeOid.map(h -> 3L).orElse(780L);
    }

    private static List<HakukohdeJaResurssit> hakukohteetOpetuskielella(HakijaPaginationObject hakijat, List<HakemusWrapper> hakemukset) {
        return getHakukohteenResurssitHakemuksistaJaHakijoista(
                hakemukset.stream().collect(Collectors.toMap(HakemusWrapper::getOid, h0 -> h0)),
                hakijat.getResults().stream().collect(Collectors.groupingBy(ViestintapalveluObservables::hakutoiveMissaHakijaOnHyvaksyttyna)));
    }

    public static HaunResurssit filtteroiAsiointikielella(String asiointkieli, HaunResurssit haunResurssit) {
        HaunResurssit filteroidytResurssit = filtteroiHakemuksetJaHakijat(haunResurssit, hakemus -> asiointkieli.equalsIgnoreCase(hakemus.getAsiointikieli()));
        LOG.info("Asiointikielellä filtteröinnin jälkeen {} kpl", filteroidytResurssit.hakemukset.size());
        return filteroidytResurssit;
    }

    private static HaunResurssit filtteroiHakemuksetJaHakijat(HaunResurssit haunResurssit, Function<HakemusWrapper, Boolean> filter) {
        final Map<String, HakemusWrapper> hakemuksetFiltteroituna = haunResurssit.hakemukset
                .stream()
                .filter(h -> filter.apply(h))
                .collect(Collectors.toMap(HakemusWrapper::getOid, h0 -> h0));

        final Set<String> oidit = hakemuksetFiltteroituna.keySet();
        List<HakijaDTO> hakijatFiltteroituna = haunResurssit.hakijat
                .stream()
                .filter(h -> oidit.contains(h.getHakemusOid())).collect(Collectors.toList());

        return new HaunResurssit(hakijatFiltteroituna, hakemuksetFiltteroituna.values());
    }

    private static List<HakukohdeJaResurssit> getHakukohteenResurssitHakemuksistaJaHakijoista(Map<String, HakemusWrapper> hakemuksetAsiointikielellaFiltteroituna, Map<String, List<HakijaDTO>> hyvaksytytHakutoiveittain) {
        return hyvaksytytHakutoiveittain.entrySet()
                .stream()
                .map(e -> new HakukohdeJaResurssit(
                                e.getKey(),
                                e.getValue(),
                                e.getValue()
                                        .stream()
                                        .map(v -> Objects.requireNonNull(
                                                hakemuksetAsiointikielellaFiltteroituna.get(v.getHakemusOid()),
                                                "Hakemusta " + v.getHakemusOid() + " ei löydy"
                                        ))
                                        .collect(Collectors.toList()))
                ).collect(Collectors.toList());
    }

    private static String hakutoiveMissaHakijaOnHyvaksyttyna(HakijaDTO hakija) {
        return hakija.getHakutoiveet().stream()
                .flatMap(h -> h.getHakutoiveenValintatapajonot().stream().map(j -> new HakutoiveJaJono(h.getHakukohdeOid(), j)))
                .filter(hjj -> hjj.jono.getTila() != null && hjj.jono.getTila().isHyvaksytty())
                .findAny()
                .map(hjj -> hjj.hakukohdeOid)
                .get(); // jos heittää npe:n niin sijoittelu palauttaa hyväksymättömiä rajapinnan läpi
    }
}
