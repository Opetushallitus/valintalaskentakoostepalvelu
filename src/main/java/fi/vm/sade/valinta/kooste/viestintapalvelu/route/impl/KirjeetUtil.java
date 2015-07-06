package fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl;

import com.google.common.collect.Maps;
import fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila;

import java.util.Map;

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
}
