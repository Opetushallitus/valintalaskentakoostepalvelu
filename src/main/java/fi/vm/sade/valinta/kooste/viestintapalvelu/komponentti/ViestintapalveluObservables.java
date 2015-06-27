package fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti;

import com.google.common.collect.ImmutableMap;
import fi.vm.sade.organisaatio.resource.dto.HakutoimistoDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaPaginationObject;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakutoiveenValintatapajonoDTO;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.sijoitteluntulos.dto.SijoittelunTulosProsessi;
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
        public final Collection<Hakemus> hakemukset;

        public HakukohdeJaResurssit(String hakukohdeOid, List<HakijaDTO> hakijat, Collection<Hakemus> hakemukset) {
            this.hakukohdeOid = hakukohdeOid;
            this.hakijat = hakijat;
            this.hakemukset = hakemukset;
        }
    }

    public static class HaunResurssit {

        public final List<HakijaDTO> hakijat;
        public final Collection<Hakemus> hakemukset;

        public HaunResurssit(List<HakijaDTO> hakijat, Collection<Hakemus> hakemukset) {
            this.hakijat = hakijat;
            this.hakemukset = hakemukset;
        }
    }

    public static Observable<HaunResurssit> haunResurssit(String asiointikieli, Observable<HakijaPaginationObject> koulutuspaikalliset,
                                                          Function<List<String>, Observable<List<Hakemus>>> haeHakemukset) {
        return resurssit(koulutuspaikalliset, haeHakemukset, (hakemukset, hakijat) -> filtteroiAsiointikielella(asiointikieli, hakijat, hakemukset));
    }

    public static Observable<List<HakukohdeJaResurssit>> hakukohteetJaResurssit(Observable<HakijaPaginationObject> koulutuspaikalliset,
                                                                                Function<List<String>, Observable<List<Hakemus>>> haeHakemukset) {
        return resurssit(koulutuspaikalliset, haeHakemukset, (hakemukset, hakijat) -> hakukohteetOpetuskielella(hakijat, hakemukset));
    }

    public static <T> Observable<T> resurssit(Observable<HakijaPaginationObject> koulutusPaikalliset,
                                              Function<List<String>, Observable<List<Hakemus>>> haeHakemukset,
                                              Func2<List<Hakemus>, HakijaPaginationObject, T> zipper) {

        Observable<HakijaPaginationObject> koulutuspaikallisetObs = koulutusPaikalliset
                .doOnNext(hakijat -> LOG.info("Saatiin haulle hyväksyttyjä {} kpl", hakijat.getTotalCount()));

        return koulutuspaikallisetObs
                .flatMap(hakijat -> haeHakemukset.apply(hakijat.getResults().stream().map(HakijaDTO::getHakemusOid).collect(Collectors.toList())))
                .zipWith(koulutuspaikallisetObs, zipper)
                .take(1);
    }

    public static Observable<Map<String, Optional<Osoite>>> haunOsoitteet(Map<String, MetaHakukohde> hakukohteet, Function<String, Observable<HakutoimistoDTO>> hakutoimisto) {
        return Observable.from(hakukohteet.values())
                .flatMap(hakukohde -> hakutoimisto.apply(hakukohde.getTarjoajaOid())
                        .map(toimistonOsoite -> new TarjoajaWithOsoite(hakukohde.getTarjoajaOid(), Hakijapalvelu.osoite(toimistonOsoite, hakukohde.getHakukohteenKieli()))))
                .collect(HashMap::new, (map, pair) -> map.put(pair.tarjoajaOid, pair.hakutoimisto));
    }

    public static Observable<Map<String, Optional<Osoite>>> hakukohteenOsoite(String hakukohdeOid, String tarjoajaOid,
                                                                              Map<String, MetaHakukohde> hyvaksymiskirjeessaKaytetytHakukohteet,
                                                                              Function<String, Observable<HakutoimistoDTO>> hakutoimistoFn) {
        MetaHakukohde kohdeHakukohde = hyvaksymiskirjeessaKaytetytHakukohteet.get(hakukohdeOid);
        return hakutoimistoFn.apply(kohdeHakukohde.getTarjoajaOid())
                .map(hakutoimistoDTO -> ImmutableMap.of(tarjoajaOid, Hakijapalvelu.osoite(hakutoimistoDTO, kohdeHakukohde.getHakukohteenKieli())));
    }

    public static Observable<LetterBatch> kirjeet(String hakuOid, Optional<String> asiointikieli, List<HakijaDTO> hyvaksytytHakijat,
                                                  Collection<Hakemus> hakemukset, String defaultValue, Map<String, MetaHakukohde> hyvaksymiskirjeessaKaytetytHakukohteet,
                                                  Observable<Map<String, Optional<Osoite>>> addresses, HyvaksymiskirjeetKomponentti hyvaksymiskirjeetKomponentti) {
        return addresses.map(hakijapalveluidenOsoite -> hyvaksymiskirjeetKomponentti
                .teeHyvaksymiskirjeet(
                        HyvaksymiskirjeetServiceImpl.todellisenJonosijanRatkaisin(hyvaksytytHakijat),
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
                        null,
                        null,
                        asiointikieli.isPresent()));
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

    private static List<HakukohdeJaResurssit> hakukohteetOpetuskielella(HakijaPaginationObject hakijat, List<Hakemus> hakemukset) {
        return getHakukohteenResurssitHakemuksistaJaHakijoista(
                hakemukset.stream().collect(Collectors.toMap(Hakemus::getOid, h0 -> h0)),
                hakijat.getResults().stream().collect(Collectors.groupingBy(ViestintapalveluObservables::hakutoiveMissaHakijaOnHyvaksyttyna)));
    }

    private static HaunResurssit filtteroiAsiointikielella(String asiointkieli, HakijaPaginationObject hakijat, List<Hakemus> hakemukset) {
        final Map<String, Hakemus> hakemuksetAsiointikielellaFiltteroituna = hakemukset
                .stream()
                .filter(h -> asiointkieli.equals(new HakemusWrapper(h).getAsiointikieli()))
                .collect(Collectors.toMap(Hakemus::getOid, h0 -> h0));

        final Set<String> oidit = hakemuksetAsiointikielellaFiltteroituna.keySet();
        List<HakijaDTO> hakijatAsiointikielellaFiltteroituna = hakijat.getResults()
                .stream()
                .filter(h -> oidit.contains(h.getHakemusOid())).collect(Collectors.toList());
        LOG.info("Saatiin haun hakemukset {} kpl ja asiointkielellä filtteröinnin jälkeen {} kpl", hakemukset.size(), hakemuksetAsiointikielellaFiltteroituna.size());

        return new HaunResurssit(hakijatAsiointikielellaFiltteroituna, hakemuksetAsiointikielellaFiltteroituna.values());
    }

    private static List<HakukohdeJaResurssit> getHakukohteenResurssitHakemuksistaJaHakijoista(Map<String, Hakemus> hakemuksetAsiointikielellaFiltteroituna, Map<String, List<HakijaDTO>> hyvaksytytHakutoiveittain) {
        return hyvaksytytHakutoiveittain.entrySet()
                .stream()
                .map(e -> new HakukohdeJaResurssit(
                                e.getKey(),
                                e.getValue(),
                                e.getValue().stream().map(v -> hakemuksetAsiointikielellaFiltteroituna.get(v.getHakemusOid())).collect(Collectors.toList()))
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