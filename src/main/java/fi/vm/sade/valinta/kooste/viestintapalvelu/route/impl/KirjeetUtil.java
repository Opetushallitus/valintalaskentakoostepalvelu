package fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl;

import com.google.common.collect.Maps;
import fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakutoiveenValintatapajonoDTO;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;

import static fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila.*;

public class KirjeetUtil {

    public static Map<HakemuksenTila, Integer> getHakemuksenTilaIntegerMap() {
        final Map<HakemuksenTila, Integer> tilaToPrioriteetti = Maps.newHashMap();
        tilaToPrioriteetti.put(HARKINNANVARAISESTI_HYVAKSYTTY, 1);
        tilaToPrioriteetti.put(HYVAKSYTTY, 2);
        tilaToPrioriteetti.put(VARASIJALTA_HYVAKSYTTY, 3);
        tilaToPrioriteetti.put(VARALLA, 4);
        tilaToPrioriteetti.put(PERUNUT, 5);
        tilaToPrioriteetti.put(PERUUTETTU, 6);
        tilaToPrioriteetti.put(PERUUNTUNUT, 7);
        tilaToPrioriteetti.put(HYLATTY, 8);
        return tilaToPrioriteetti;
    }

    public static Comparator<HakutoiveenValintatapajonoDTO> sortByTila() {
        return (o1, o2) -> {
            final Map<HakemuksenTila, Integer> tilaToPrioriteetti = getHakemuksenTilaIntegerMap();
            HakemuksenTila h1 = Optional.ofNullable(o1.getTila()).orElse(HYLATTY);
            HakemuksenTila h2 = Optional.ofNullable(o2.getTila()).orElse(HYLATTY);
            if (VARALLA.equals(h1) && VARALLA.equals(h2)) {
                Integer i1 = Optional.ofNullable(o1.getVarasijanNumero()).orElse(0);
                Integer i2 = Optional.ofNullable(o2.getVarasijanNumero()).orElse(0);
                return i1.compareTo(i2);
            }
            return tilaToPrioriteetti.get(h1).compareTo(tilaToPrioriteetti.get(h2));
        };
    }
}
