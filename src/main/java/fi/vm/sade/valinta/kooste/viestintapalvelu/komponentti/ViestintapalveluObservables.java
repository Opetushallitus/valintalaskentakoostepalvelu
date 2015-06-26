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

    public static Observable<HaunResurssit> haunResurssit(String asiointikieli, Observable<HakijaPaginationObject> koulutuspaikalliset, Function<List<String>, Observable<List<Hakemus>>> haeHakemukset) {
        return resurssit(koulutuspaikalliset, haeHakemukset, (hakemukset, hakijat) -> filtteroiAsiointikielella(asiointikieli, hakijat, hakemukset));
    }

    public static Observable<List<HakukohdeJaResurssit>> hakukohteetJaResurssit(Observable<HakijaPaginationObject> koulutuspaikalliset, Function<List<String>, Observable<List<Hakemus>>> haeHakemukset) {
        return resurssit(koulutuspaikalliset, haeHakemukset, (hakemukset, hakijat) -> hakukohteetOpetuskielella(hakijat, hakemukset));
    }

    public static <T> Observable<T> resurssit(Observable<HakijaPaginationObject> koulutusPaikalliset, Function<List<String>,
            Observable<List<Hakemus>>> haeHakemukset, Func2<List<Hakemus>, HakijaPaginationObject, T> zipper) {

        Observable<HakijaPaginationObject> koulutuspaikallisetObs = koulutusPaikalliset
                .doOnNext(hakijat -> LOG.info("Saatiin haulle hyväksyttyjä {} kpl", hakijat.getTotalCount()));

        return koulutuspaikallisetObs
                .flatMap(hakijat -> haeHakemukset.apply(hakijat.getResults().stream().map(HakijaDTO::getHakemusOid).collect(Collectors.toList())))
                .zipWith(koulutuspaikallisetObs, zipper)
                .take(1);
    }

    public static Observable<Map<String, Optional<Osoite>>> addresses(Optional<String> hakukohdeOid, Optional<String> tarjoajaOid,
                                                                      Map<String, MetaHakukohde> hyvaksymiskirjeessaKaytetytHakukohteet,
                                                                      Function<String, Observable<HakutoimistoDTO>> hakutoimistoFn) {
        if (hakukohdeOid.isPresent()) {
            MetaHakukohde kohdeHakukohde = hyvaksymiskirjeessaKaytetytHakukohteet.get(hakukohdeOid.get());
            return hakutoimistoFn.apply(kohdeHakukohde.getTarjoajaOid()).map(hakutoimistoDTO -> ImmutableMap.of(
                    tarjoajaOid.get(), Hakijapalvelu.osoite(hakutoimistoDTO, kohdeHakukohde.getHakukohteenKieli())
            ));
        } else { // koko haun kiinnostaville hakukohteille kerralla
            return Observable.from(hyvaksymiskirjeessaKaytetytHakukohteet.values())
                    .flatMap(meta -> hakutoimistoFn.apply(meta.getTarjoajaOid())
                            .map(hakutoimisto -> new TarjoajaWithOsoite(meta.getTarjoajaOid(), Hakijapalvelu.osoite(hakutoimisto, meta.getHakukohteenKieli()))))
                    .collect(HashMap::new, (map, pair) -> map.put(pair.tarjoajaOid, pair.hakutoimisto));
        }
    }

    public static Observable<LetterBatch> kirje(String hakuOid, Optional<String> asiointikieli, List<HakijaDTO> hyvaksytytHakijat, Collection<Hakemus> hakemukset, String defaultValue, Map<String, MetaHakukohde> hyvaksymiskirjeessaKaytetytHakukohteet, Observable<Map<String, Optional<Osoite>>> addresses, HyvaksymiskirjeetKomponentti hyvaksymiskirjeetKomponentti) {
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

    public static Observable<String> batchId(Optional<String> hakukohdeOid, SijoittelunTulosProsessi prosessi, Observable<LetterBatch> hyvaksymiskirje,
                                             Function<LetterBatch, Observable<LetterResponse>> vieDokumentti, Function<String, Observable<LetterBatchStatusDto>> haeStatusFn,
                                             Function<String, Observable<String>> renameFn) {

        Observable<ViestintapalveluObservables.ResponseWithBatchId> valmisOrKeskeytettyObs =
                hyvaksymiskirje
                        .doOnNext(batch -> LOG.info("##### Tehdään viestintäpalvelukutsu {}", hakukohdeOid))
                        .flatMap(batch -> status(hakukohdeOid, vieDokumentti.apply(batch), haeStatusFn));

        return valmis(hakukohdeOid, prosessi, valmisOrKeskeytettyObs, renameFn).mergeWith(keskeytetty(hakukohdeOid, valmisOrKeskeytettyObs));
    }

    public static Observable<ResponseWithBatchId> status(Optional<String> hakukohdeOid, Observable<LetterResponse> letterResponseObs, Function<String, Observable<LetterBatchStatusDto>> statusFn) {
        return letterResponseObs
                .doOnNext(letterResponse -> {
                    LOG.info("##### Viestintäpalvelukutsu onnistui {}", hakukohdeOid);
                    LOG.info("##### Odotetaan statusta, batchid={}", letterResponse.getBatchId());
                })
                .flatMap(letterResponse -> Observable.interval(1, TimeUnit.SECONDS)
                        .take((int) TimeUnit.MINUTES.toSeconds(getDelay(hakukohdeOid)))
                        .flatMap(i -> statusFn.apply(letterResponse.getBatchId())
                                .zipWith(Observable.just(letterResponse.getBatchId()), ResponseWithBatchId::new))
                        .skipWhile(status -> !VALMIS_STATUS.equals(status.resp.getStatus()) && !KESKEYTETTY_STATUS.equals(status.resp.getStatus())))
                .take(1);
    }

    public static Observable<String> keskeytetty(Optional<String> hakukohdeOid, Observable<ResponseWithBatchId> valmisOrKeskeytettyObs) {
        return valmisOrKeskeytettyObs
                .filter(status -> KESKEYTETTY_STATUS.equals(status.resp.getStatus()))
                .map(status -> status.batchId)
                .flatMap(s -> Observable.error(new RuntimeException("Viestintäpalvelu palautti virheen hakukohteelle " + hakukohdeOid.get())));
    }

    public static Observable<String> valmis(Optional<String> hakukohdeOid, SijoittelunTulosProsessi prosessi,
                                            Observable<ResponseWithBatchId> valmisOrKeskeytettyObs, Function<String, Observable<String>> renameFn) {
        Observable<ResponseWithBatchId> valmisObs = valmisOrKeskeytettyObs.filter(status -> VALMIS_STATUS.equals(status.resp.getStatus()));
        if (hakukohdeOid.isPresent()) {
            return valmisObs
                    .doOnNext(s -> LOG.info("##### Dokumentti {} valmistui hakukohteelle {} joten uudelleen nimetään se", s.batchId, hakukohdeOid.get()))
                    .flatMap(s -> renameFn.apply(s.batchId)
                            .doOnNext(str -> LOG.info("Uudelleen nimeäminen onnistui hakukohteelle {}", hakukohdeOid.get()))
                            .doOnError(error -> LOG.error("Uudelleen nimeäminen epäonnistui hakukohteelle {}", hakukohdeOid.get(), error))
                            .onErrorReturn(error -> s.batchId)
                            .map(name -> s.batchId));
        } else {
            return valmisObs.doOnNext(s -> prosessi.setDokumenttiId(s.batchId)).map(s -> s.batchId);
        }
    }

    public static Long getDelay(Optional<String> hakukohdeOid) {
        return hakukohdeOid.map(h -> 3L).orElse(780L);
    }

    /*
     * Helper functions
     */

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