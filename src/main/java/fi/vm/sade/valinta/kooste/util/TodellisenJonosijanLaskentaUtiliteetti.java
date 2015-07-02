package fi.vm.sade.valinta.kooste.util;

import com.google.common.collect.Maps;
import com.google.common.collect.TreeMultiset;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakutoiveDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakutoiveenValintatapajonoDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Multiset;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import static java.util.Optional.ofNullable;

public class TodellisenJonosijanLaskentaUtiliteetti {
    private static final Logger LOG = LoggerFactory.getLogger(TodellisenJonosijanLaskentaUtiliteetti.class);

    public static int laskeTodellinenJonosija(HakutoiveenValintatapajonoDTO valintatapajono, Multiset<Integer> jonosijat) {
        int todellinenKkJonosija = 1;
        final int todellinenJonoSija = getKkJonosija(valintatapajono);
        for (com.google.common.collect.Multiset.Entry<Integer> entry : jonosijat.entrySet()) {
            if (entry.getElement().equals(todellinenJonoSija)) {
                return todellinenKkJonosija;
            }
            todellinenKkJonosija += entry.getCount();
        }
        LOG.error("Jonosijaa {} ei ollut hakutoiveen jonosijoissa!", todellinenJonoSija);
        throw new RuntimeException("Jonosijaa " + todellinenJonoSija + " ei ollut hakutoiveen jonosijoissa!");
    }

    public static Map<String, TreeMultiset<Integer>> todellisenJonosijanRatkaisin(Collection<HakijaDTO> hakukohteenHakijat) {
        Map<String, TreeMultiset<Integer>> valintatapajonoToJonosijaToHakija = Maps.newHashMap();
        for (HakijaDTO hakija : hakukohteenHakijat) {
            for (HakutoiveDTO hakutoive : hakija.getHakutoiveet()) {
                for (HakutoiveenValintatapajonoDTO valintatapajono : hakutoive.getHakutoiveenValintatapajonot()) {
                    if (!valintatapajono.getTila().isHyvaksyttyOrVaralla()) {
                        continue;
                    }
                    if (!valintatapajonoToJonosijaToHakija.containsKey(valintatapajono.getValintatapajonoOid())) {
                        valintatapajonoToJonosijaToHakija.put(valintatapajono.getValintatapajonoOid(), TreeMultiset.<Integer>create());
                    }
                    int kkJonosija = getKkJonosija(valintatapajono);
                    valintatapajonoToJonosijaToHakija.get(valintatapajono.getValintatapajonoOid()).add(kkJonosija);
                }
            }
        }
        return valintatapajonoToJonosijaToHakija;
    }

    public static int getKkJonosija(HakutoiveenValintatapajonoDTO valintatapajono) {
        return Optional.ofNullable(
                valintatapajono.getJonosija()).orElse(0)
                                + Optional.ofNullable(
                                valintatapajono.getTasasijaJonosija())
                                .orElse(0) - 1;
    }
}
