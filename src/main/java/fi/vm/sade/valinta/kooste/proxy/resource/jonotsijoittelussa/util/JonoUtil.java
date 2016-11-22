package fi.vm.sade.valinta.kooste.proxy.resource.jonotsijoittelussa.util;

import fi.vm.sade.valinta.kooste.proxy.resource.jonotsijoittelussa.dto.Jono;
import fi.vm.sade.valinta.kooste.proxy.resource.jonotsijoittelussa.dto.JonoPair;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JonoUtil {
    private static final Function<Jono, String> toId = j -> new StringBuilder(j.hakukohdeOid).append(",").append(j.oid).toString();
    private static Map<String, Jono> groupBy(List<Jono> jonos) {
        return jonos.stream().collect(Collectors.toMap(toId, Function.identity(), (a,b) -> a));
    }

    public static List<JonoPair> pairJonos(List<Jono> fromLaskenta, List<Jono> fromValintaperusteet) {

        Map<String, Jono> laskenta = groupBy(fromLaskenta);
        Map<String, Jono> valintaperusteet = groupBy(fromValintaperusteet);


        Set<String> ids = Stream.concat(laskenta.keySet().stream(), valintaperusteet.keySet().stream()).collect(Collectors.toSet());
        return ids.stream().map(id -> new JonoPair(Optional.ofNullable(laskenta.get(id)), Optional.ofNullable(valintaperusteet.get(id)))).collect(Collectors.toList());
    }

    public static List<String> puutteellisetHakukohteet(List<JonoPair> jonoPairs) {
        return jonoPairs.stream().flatMap(j -> {
            if (j.isAinoastaanLaskennassa()) {
                if (!j.isLaskennassaValmisSijoiteltavaksiAndSiirretaanSijoitteluun()) {
                    return Stream.of(j.getHakukohdeOid());
                }
            } else if (j.isAinoastaanValintaperusteissa()) {
                if (j.isValintaperusteissaSiirretaanSijoitteluun()) {
                    return Stream.of(j.getHakukohdeOid());
                }
            } else {
                if (!j.isMolemmissaValmisSijoiteltavaksiJaSiirretaanSijoitteluun()) {
                    return Stream.of(j.getHakukohdeOid());
                }
            }
            return Stream.empty();
        }).collect(Collectors.toList());
    }
}
