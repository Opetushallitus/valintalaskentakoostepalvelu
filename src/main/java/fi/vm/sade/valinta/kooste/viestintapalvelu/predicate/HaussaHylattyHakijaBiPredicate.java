package fi.vm.sade.valinta.kooste.viestintapalvelu.predicate;

import java.util.function.BiPredicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakutoiveDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakutoiveenValintatapajonoDTO;

public class HaussaHylattyHakijaBiPredicate implements BiPredicate<HakijaDTO, String> {
    private static final Logger LOG = LoggerFactory.getLogger(HaussaHylattyHakijaBiPredicate.class);

    @Override
    public boolean test(HakijaDTO hakija, String hakukohdeOid) {
        if (hakija.getHakutoiveet() == null) {
        } else {
            LOG.error("Tutkitaan onko hakija (hakemusOid=={}) hyväksytty edes yhteen hakukohteeseen.", hakija.getHakemusOid());
            for (HakutoiveDTO h : hakija.getHakutoiveet()) {
                for (HakutoiveenValintatapajonoDTO vjono : h.getHakutoiveenValintatapajonot()) {
                    if (vjono.getTila() != null && vjono.getTila().isHyvaksytty()) {
                        LOG.error("Hakija (hakemusOid=={}) oli hyväksytty hakukohteeseen {}, {}", hakija.getHakemusOid(), h.getHakukohdeOid(), vjono.getValintatapajonoOid());
                        return false;
                    }
                }
            }
        }
        return true;
    }
}