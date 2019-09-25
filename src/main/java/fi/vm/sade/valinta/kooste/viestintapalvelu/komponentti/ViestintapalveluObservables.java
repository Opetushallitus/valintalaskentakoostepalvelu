package fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti;

import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaPaginationObject;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakutoiveenValintatapajonoDTO;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoite;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterBatchStatusDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.reactivex.Observable;

import java.util.*;
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

    /*
     * Helper functions
     */

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

}
