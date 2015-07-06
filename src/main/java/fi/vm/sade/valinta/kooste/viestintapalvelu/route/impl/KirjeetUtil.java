package fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl;

import com.google.common.collect.Maps;
import fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila;
import fi.vm.sade.sijoittelu.tulos.dto.PistetietoDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakutoiveDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakutoiveenValintatapajonoDTO;
import fi.vm.sade.valinta.kooste.util.HakemusUtil;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Teksti;
import org.apache.commons.lang.StringUtils;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila.*;
import static fi.vm.sade.valinta.kooste.util.Formatter.ARVO_VALI;
import static fi.vm.sade.valinta.kooste.util.Formatter.suomennaNumero;
import static org.apache.commons.lang.StringUtils.EMPTY;

public class KirjeetUtil {
    static final Map<HakemuksenTila, Integer> tilaToPrioriteetti = Maps.newHashMap();
    static {
        tilaToPrioriteetti.put(HARKINNANVARAISESTI_HYVAKSYTTY, 1);
        tilaToPrioriteetti.put(HYVAKSYTTY, 2);
        tilaToPrioriteetti.put(VARASIJALTA_HYVAKSYTTY, 3);
        tilaToPrioriteetti.put(VARALLA, 4);
        tilaToPrioriteetti.put(PERUNUT, 5);
        tilaToPrioriteetti.put(PERUUTETTU, 6);
        tilaToPrioriteetti.put(PERUUNTUNUT, 7);
        tilaToPrioriteetti.put(HYLATTY, 8);
    }

    public static Comparator<HakutoiveenValintatapajonoDTO> sortByTila() {
        return (o1, o2) -> {
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

    public static StringBuilder createPaasyJaSoveltuvuuskoePisteet(HakutoiveDTO hakutoive) {
        StringBuilder pisteet = new StringBuilder();
        for (PistetietoDTO pistetieto : hakutoive.getPistetiedot()) {
            if (pistetieto.getArvo() != null) {
                try {
                    String arvo = StringUtils.trimToEmpty(pistetieto.getArvo()).replace(",", ".");
                    BigDecimal ehkaNumeroEhkaTotuusarvo = new BigDecimal(arvo);
                    pisteet.append(suomennaNumero(ehkaNumeroEhkaTotuusarvo)).append(ARVO_VALI);
                } catch (NumberFormatException notNumber) {
                    // OVT-6340 filtteroidaan totuusarvot pois
                }
            }
        }
        return pisteet;
    }

    public static String hylkaysPerusteText(String preferoituKielikoodi, List<HakutoiveenValintatapajonoDTO> hakutoiveenValintatapajonot) {
        HakutoiveenValintatapajonoDTO lastJono = hakutoiveenValintatapajonot.get(hakutoiveenValintatapajonot.size() - 1);
        return HYLATTY.equals(hakutoiveenValintatapajonot.get(0).getTila()) ? new Teksti(lastJono.getTilanKuvaukset()).getTeksti(preferoituKielikoodi, EMPTY) : EMPTY;
    }

    public static void putValinnanTulosHylkausPerusteAndVarasijaData(String preferoituKielikoodi, Map<String, Object> tulokset, List<HakutoiveenValintatapajonoDTO> hakutoiveenValintatapajonot) {
        if (!hakutoiveenValintatapajonot.isEmpty()) {
            HakutoiveenValintatapajonoDTO firstValintatapajono = hakutoiveenValintatapajonot.get(0);
            if (VARALLA.equals(firstValintatapajono.getTila()) && firstValintatapajono.getVarasijanNumero() != null) {
                tulokset.put("varasija", HakemusUtil.varasijanNumeroConverter(firstValintatapajono.getVarasijanNumero(), preferoituKielikoodi));
            }
            tulokset.put("hylkaysperuste", hylkaysPerusteText(preferoituKielikoodi, hakutoiveenValintatapajonot));
            tulokset.put("valinnanTulos", HakemusUtil.tilaConverter(firstValintatapajono.getTila(), preferoituKielikoodi, firstValintatapajono.isHyvaksyttyHarkinnanvaraisesti()));
        }
    }
}
