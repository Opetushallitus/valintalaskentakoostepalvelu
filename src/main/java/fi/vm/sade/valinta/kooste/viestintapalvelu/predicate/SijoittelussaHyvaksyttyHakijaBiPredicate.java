package fi.vm.sade.valinta.kooste.viestintapalvelu.predicate;

import java.util.Collections;
import java.util.Optional;
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
        return Optional.ofNullable(hakija.getHakutoiveet()).orElse(Collections.emptySet()).stream()
                .filter(h -> hakukohdeOid.equals(h.getHakukohdeOid()))
                .findAny()
                .map(
                        h -> h.getHakutoiveenValintatapajonot()
                                .stream()
                                .filter(vjono -> vjono.getTila() != null && vjono.getTila().isHyvaksytty())
                                .findAny()
                                .map(vjono -> true)
                                .orElse(false)
                )
                .orElse(false);
    }
}
