package fi.vm.sade.valinta.kooste.proxy.resource.jonotsijoittelussa.util;

import fi.vm.sade.valinta.kooste.proxy.resource.jonotsijoittelussa.dto.HakukohdePair;
import fi.vm.sade.valinta.kooste.proxy.resource.jonotsijoittelussa.dto.Jono;
import fi.vm.sade.valinta.kooste.proxy.resource.jonotsijoittelussa.dto.JonoPair;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.*;

public class JonoUtil {
    public static List<HakukohdePair> pairHakukohteet(List<Jono> fromLaskenta, List<Jono> fromValintaperusteet) {

        Map<String, List<Jono>> laskenta = fromLaskenta.stream().collect(Collectors.groupingBy(j -> j.hakukohdeOid, Collectors.toList()));
        Map<String, List<Jono>> valintaperusteet = fromValintaperusteet.stream().collect(Collectors.groupingBy(j -> j.hakukohdeOid, Collectors.toList()));

        Set<String> ids = Stream.concat(laskenta.keySet().stream(), valintaperusteet.keySet().stream()).collect(Collectors.toSet());
        return ids.stream().map(hk -> new HakukohdePair(hk, laskenta.getOrDefault(hk, emptyList()), valintaperusteet.getOrDefault(hk, emptyList()))).collect(Collectors.toList());
    }
    public static Set<String> puutteellisetHakukohteet(List<HakukohdePair> hkPairs) {
        return hkPairs.stream().flatMap(hk -> {
            final String hakukohdeOid = hk.hakukohdeOid;
            if(hk.hakukohteenKaikkiJonotVainLaskennassa()) {
                final boolean laskennanJonoEiSiirrettySijoitteluun = hk.laskennanJonot.stream().anyMatch(j -> !j.isValmisSijoiteltavaksiAndSiirretaanSijoitteluun());
                return laskennanJonoEiSiirrettySijoitteluun ? Stream.of(hakukohdeOid) : Stream.empty();
            }
            final boolean kaikkiJonotEiSiirrettySijoitteluun = hk.molemmistaLoytyvatJonot().stream().anyMatch(j -> !j.isMolemmissaValmisSijoiteltavaksiJaSiirretaanSijoitteluun());
            if(kaikkiJonotEiSiirrettySijoitteluun) {
                return Stream.of(hakukohdeOid);
            }
            final boolean valintaperusteidenAktiivinenJonoEiSiirretaSijoitteluun = hk.jonotVainValintaperusteissa().stream().anyMatch(j -> !(j.siirretaanSijoitteluun || j.isPassiivinen()));
            if(valintaperusteidenAktiivinenJonoEiSiirretaSijoitteluun) {
                return Stream.of(hakukohdeOid);
            }
            return Stream.empty();
        }).collect(Collectors.toSet());
    }
}
