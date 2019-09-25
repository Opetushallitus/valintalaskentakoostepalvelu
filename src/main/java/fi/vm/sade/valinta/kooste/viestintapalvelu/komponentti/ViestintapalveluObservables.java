package fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti;

import com.google.common.collect.ImmutableMap;
import fi.vm.sade.organisaatio.resource.dto.HakutoimistoDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaPaginationObject;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakutoiveenValintatapajonoDTO;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valinta.kooste.viestintapalvelu.Hakijapalvelu;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.MetaHakukohde;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoite;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterBatch;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterBatchStatusDto;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.reactivex.Observable;
import io.reactivex.functions.BiFunction;

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

    public static Observable<List<HakukohdeJaResurssit>> hakukohteetJaResurssit(Observable<HakijaPaginationObject> koulutuspaikalliset,
                                                                                Function<List<String>, Observable<List<HakemusWrapper>>> haeHakemukset) {
        return resurssit(koulutuspaikalliset, haeHakemukset, (hakemukset, hakijat) -> hakukohteetOpetuskielella(hakijat, hakemukset));
    }

    public static <T> Observable<T> resurssit(Observable<HakijaPaginationObject> koulutusPaikalliset,
                                              Function<List<String>, Observable<List<HakemusWrapper>>> haeHakemukset,
                                              BiFunction<List<HakemusWrapper>, HakijaPaginationObject, T> zipper) {
        return Observable.zip(
                koulutusPaikalliset.flatMap(hakijat -> haeHakemukset.apply(hakijat.getResults().stream().map(HakijaDTO::getHakemusOid).collect(Collectors.toList()))),
                koulutusPaikalliset,
                (hakemukset, hakijat) -> {
                    LOG.info("Saatiin haulle hyväksyttyjä {} kpl", hakijat.getResults().size());
                    LOG.info("Saatiin haulle hakemuksia {} kpl", hakemukset.size());
                    return zipper.apply(hakemukset, hakijat);
                }
        );
    }

    /*
     * Helper functions
     */

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
