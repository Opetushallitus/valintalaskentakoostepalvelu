package fi.vm.sade.valinta.kooste.viestintapalvelu.predicate;

import fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakutoiveDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakutoiveenValintatapajonoDTO;

import java.util.function.Predicate;

/**
 * @author Jussi Jartamo
 */
public class SijoittelussaHyvaksyttyHakija implements Predicate<HakijaDTO> {
    private final String hakukohdeOid;

    public SijoittelussaHyvaksyttyHakija(String hakukohdeOid) {
        this.hakukohdeOid = hakukohdeOid;
    }

    @Override
    public boolean test(HakijaDTO input) {
        if (input.getHakutoiveet() == null) {
        } else {
            for (HakutoiveDTO h : input.getHakutoiveet()) {

                if (hakukohdeOid.equals(h.getHakukohdeOid())) {
                    final boolean checkFirstValintatapajonoOnly = false;
                    // sort by
                    // priority
//					Collections.sort(h.getHakutoiveenValintatapajonot(),
//							HakutoiveenValintatapajonoComparator.DEFAULT);

                    for (HakutoiveenValintatapajonoDTO vjono : h
                            .getHakutoiveenValintatapajonot()) {
                        if (HakemuksenTila.HYVAKSYTTY.equals(vjono.getTila())
                                || HakemuksenTila.VARASIJALTA_HYVAKSYTTY
                                .equals(vjono.getTila())) {
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
