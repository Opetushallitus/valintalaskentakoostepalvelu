package fi.vm.sade.valinta.kooste.viestintapalvelu.predicate;

import java.util.Collections;
import java.util.function.BiPredicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakutoiveDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakutoiveenValintatapajonoDTO;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.HakutoiveenValintatapajonoComparator;

public class SijoittelussaHyvaksyttyHakijaBiPredicate implements BiPredicate<HakijaDTO, String> {
    private static final Logger LOG = LoggerFactory.getLogger(SijoittelussaHyvaksyttyHakijaBiPredicate.class);

    @Override
    public boolean test(HakijaDTO hakija, String hakukohdeOid) {
        if (hakija.getHakutoiveet() == null) {
        } else {
            for (HakutoiveDTO h : hakija.getHakutoiveet()) {
                if (hakukohdeOid.equals(h.getHakukohdeOid())) {
                    final boolean checkFirstValintatapajonoOnly = false;
                    Collections.sort(h.getHakutoiveenValintatapajonot(), HakutoiveenValintatapajonoComparator.DEFAULT);
                    for (HakutoiveenValintatapajonoDTO vjono : h.getHakutoiveenValintatapajonot()) {
                        if (vjono.getTila() == null) {
                            LOG.warn("Hakijalla (hakijaOid={},hakemusOid={}) ei ole hakutoiveen valintatapajonossa tilaa joten merkitaan automaattisesti ei hyvaksytyksi!",
                                    hakija.getHakijaOid(), hakija.getHakemusOid());
                        }
                        if (vjono.getTila() != null && vjono.getTila().isHyvaksytty()) {
                            return true;
                        }
                        if (checkFirstValintatapajonoOnly) {
                            return false;
                        }
                    }
                }
            }
        }
        return false;
    }
}
