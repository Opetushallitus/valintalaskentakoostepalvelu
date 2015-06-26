package fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti;

import com.google.common.collect.ImmutableList;
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
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterBatchStatusDto;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

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
        public final Optional<String> hakukohdeOid;
        public final List<HakijaDTO> hakijat;
        public final Collection<Hakemus> hakemukset;

        public HakukohdeJaResurssit(String hakukohdeOid, List<HakijaDTO> hakijat, Collection<Hakemus> hakemukset) {
            this.hakukohdeOid = Optional.of(hakukohdeOid);
            this.hakijat = hakijat;
            this.hakemukset = hakemukset;
        }

        public HakukohdeJaResurssit(List<HakijaDTO> hakijat, Collection<Hakemus> hakemukset) {
            this.hakukohdeOid = Optional.empty();
            this.hakijat = hakijat;
            this.hakemukset = hakemukset;
        }
    }

    public static Observable<List<HakukohdeJaResurssit>> hakukohteetJaResurssit(SijoittelunTulosProsessi prosessi, Observable<HakijaPaginationObject> koulutusPaikalliset, Function<List<String>, Observable<List<Hakemus>>> hakemusFn) {

        Observable<HakijaPaginationObject> koulutuspaikallisetObs = koulutusPaikalliset
                .doOnNext(hakijat -> LOG.info("Saatiin haulle hyväksyttyjä {} kpl", hakijat.getTotalCount()));

        Observable<List<Hakemus>> hakijatObs = koulutuspaikallisetObs
                .flatMap(hakijat -> hakemusFn.apply(hakijat.getResults().stream().map(HakijaDTO::getHakemusOid).collect(Collectors.toList())));

        return hakijatObs
                .zipWith(koulutuspaikallisetObs, (hakemukset, hakijat) -> {
                    if (!prosessi.getAsiointikieli().isPresent()) {
                        return hakukohteetOpetuskielella(hakijat, hakemukset);
                    } else {
                        return filtteroiAsiointikielella(prosessi.getAsiointikieli().get(), hakijat, hakemukset);
                    }
                })
                .take(1)
                .doOnNext(list -> prosessi.setKokonaistyo(list.size()));
    }

    public static Observable<Map<String, Optional<Osoite>>> addresses(Optional<String> hakukohdeOid, Optional<String> tarjoajaOid,
                                                                      Map<String, MetaHakukohde> hyvaksymiskirjeessaKaytetytHakukohteet,
                                                                      Observable<HakutoimistoDTO> hakutoimistoObs) {
        if (hakukohdeOid.isPresent()) {
            MetaHakukohde kohdeHakukohde = hyvaksymiskirjeessaKaytetytHakukohteet.get(hakukohdeOid.get());
            return hakutoimistoObs.map(hakutoimistoDTO -> ImmutableMap.of(
                    tarjoajaOid.get(), Hakijapalvelu.osoite(hakutoimistoDTO, kohdeHakukohde.getHakukohteenKieli())
            ));
        } else { // koko haun kiinnostaville hakukohteille kerralla
            return Observable.from(hyvaksymiskirjeessaKaytetytHakukohteet.values())
                    .flatMap(meta -> hakutoimistoObs
                            .map(hakutoimisto -> new TarjoajaWithOsoite(meta.getTarjoajaOid(), Hakijapalvelu.osoite(hakutoimisto, meta.getHakukohteenKieli()))))
                    .collect(HashMap::new, (map, pair) -> map.put(pair.tarjoajaOid, pair.hakutoimisto));
        }
    }

    public static Observable<ResponseWithBatchId> pollDocumentStatus(Optional<String> hakukohdeOid, Observable<LetterResponse> letterResponseObs, Function<String, Observable<LetterBatchStatusDto>> statusFn) {
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

    public static Observable<String> processInterruptedDocument(Optional<String> hakukohdeOid, Observable<ResponseWithBatchId> valmisOrKeskeytettyObs) {
        return valmisOrKeskeytettyObs
                .filter(status -> KESKEYTETTY_STATUS.equals(status.resp.getStatus()))
                .map(status -> status.batchId)
                .flatMap(s -> Observable.error(new RuntimeException("Viestintäpalvelu palautti virheen hakukohteelle " + hakukohdeOid.get())));
    }

    public static Observable<String> processReadyDocument(Optional<String> hakukohdeOid, SijoittelunTulosProsessi prosessi,
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

    private static List<HakukohdeJaResurssit> filtteroiAsiointikielella(String asiointkieli, HakijaPaginationObject hakijat, List<Hakemus> hakemukset) {
        final Map<String, Hakemus> hakemuksetAsiointikielellaFiltteroituna = hakemukset
                .stream()
                .filter(h -> asiointkieli.equals(new HakemusWrapper(h).getAsiointikieli()))
                .collect(Collectors.toMap(Hakemus::getOid, h0 -> h0));

        final Set<String> oidit = hakemuksetAsiointikielellaFiltteroituna.keySet();
        List<HakijaDTO> hakijatAsiointikielellaFiltteroituna = hakijat.getResults()
                .stream()
                .filter(h -> oidit.contains(h.getHakemusOid())).collect(Collectors.toList());
        LOG.info("Saatiin haun hakemukset {} kpl ja asiointkielellä filtteröinnin jälkeen {} kpl", hakemukset.size(), hakemuksetAsiointikielellaFiltteroituna.size());

        return ImmutableList.of(new HakukohdeJaResurssit(hakijatAsiointikielellaFiltteroituna, hakemuksetAsiointikielellaFiltteroituna.values()));
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