package fi.vm.sade.valinta.kooste.proxy.resource.jonotsijoittelussa.dto;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HakukohdePair {
    public final String hakukohdeOid;
    public final List<Jono> laskennanJonot;
    public final List<Jono> valintaperusteidenJonot;

    public HakukohdePair(String hakukohdeOid, List<Jono> laskennanJonot, List<Jono> valintaperusteidenJonot) {
        this.hakukohdeOid = hakukohdeOid;
        this.laskennanJonot = laskennanJonot;
        this.valintaperusteidenJonot = valintaperusteidenJonot;
    }

    public boolean hakukohteenKaikkiJonotVainLaskennassa() {
        return valintaperusteidenJonot.isEmpty();
    }

    public List<Jono> jonotVainValintaperusteissa() {
        Set<String> laskennanOids = laskennanJonot.stream().map(j -> j.oid).collect(Collectors.toSet());
        return valintaperusteidenJonot.stream().filter(j -> !laskennanOids.contains(j.oid)).collect(Collectors.toList());
    }

    public List<JonoPair> molemmistaLoytyvatJonot() {
        Set<String> jonoOids = Stream.concat(laskennanJonot.stream().map(j -> j.oid), valintaperusteidenJonot.stream().map((j -> j.oid))).collect(Collectors.toSet());
        return jonoOids.stream().flatMap(jonoOid -> {
            Optional<Jono> laskenta = laskennanJonot.stream().filter(j -> j.oid.equals(jonoOid)).findFirst();
            Optional<Jono> valintaperusteet = valintaperusteidenJonot.stream().filter(j -> j.oid.equals(jonoOid)).findFirst();
            return laskenta.map(laskennanJono ->
                    valintaperusteet.map(valintaperusteidenJono ->
                            Stream.of(new JonoPair(laskennanJono,valintaperusteidenJono))).orElse(Stream.empty())).orElse(Stream.empty());
        }).collect(Collectors.toList());
    }
}
