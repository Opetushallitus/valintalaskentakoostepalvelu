package fi.vm.sade.valinta.kooste.valintalaskenta.tulos.predicate;

import java.util.Collection;
import java.util.Set;

import com.google.common.base.Predicate;
import com.google.common.collect.Sets;

import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.HakutoiveDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeOsallistuminenDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeValinnanvaiheDTO;

public class OsallistujatPredicate implements Predicate<ValintakoeOsallistuminenDTO> {
    private final String hakukohdeOid;
    private final Set<String> valintakoeOids;
    private final Set<String> tunnisteet;

    private OsallistujatPredicate() {
        this.hakukohdeOid = null;
        this.valintakoeOids = null;
        this.tunnisteet = null;
    }

    private OsallistujatPredicate(String hakukohdeOid, Collection<String> valintakoeOids) {
        this.hakukohdeOid = hakukohdeOid;
        this.valintakoeOids = Sets.newHashSet(valintakoeOids);
        this.tunnisteet = null;
    }

    private OsallistujatPredicate(Collection<String> tunnisteet, String hakukohdeOid) {
        this.hakukohdeOid = hakukohdeOid;
        this.tunnisteet = Sets.newHashSet(tunnisteet);
        this.valintakoeOids = null;
    }

    public static java.util.function.Predicate<ValintakoeOsallistuminenDTO> osallistujat(Collection<String> tunnisteet, String hakukohdeOid) {
        final Set<String> valintakoeTunnisteet = Sets.newHashSet(tunnisteet);
        return vk -> {
            for (HakutoiveDTO hakutoive : vk.getHakutoiveet()) {
                if (!hakukohdeOid.equals(hakutoive.getHakukohdeOid())) {
                    // vain tarkasteltavasta hakukohteesta ollaan kiinnostuneita
                    continue;
                }
                for (ValintakoeValinnanvaiheDTO valinnanvaihe : hakutoive.getValinnanVaiheet()) {
                    for (ValintakoeDTO valintakoe : valinnanvaihe.getValintakokeet()) {
                        if (!valintakoeTunnisteet.contains(valintakoe.getValintakoeTunniste())) {
                            // vain tarkasteltavista valintakokeista ollaan kiinnostuneita
                            continue;
                        }
                        if (fi.vm.sade.valintalaskenta.domain.valintakoe.Osallistuminen.OSALLISTUU.equals(valintakoe.getOsallistuminenTulos().getOsallistuminen())) {
                            return true;
                        }
                    }
                }
            }
            return false;
        };
    }

    public boolean apply(ValintakoeOsallistuminenDTO valintakoeOsallistuminen) {
        for (HakutoiveDTO hakutoive : valintakoeOsallistuminen.getHakutoiveet()) {
            if (!hakukohdeOid.equals(hakutoive.getHakukohdeOid())) {
                // vain tarkasteltavasta hakukohteesta ollaan kiinnostuneita
                continue;
            }
            for (ValintakoeValinnanvaiheDTO valinnanvaihe : hakutoive
                    .getValinnanVaiheet()) {
                for (ValintakoeDTO valintakoe : valinnanvaihe
                        .getValintakokeet()) {
                    if (tunnisteet == null) {
                        if (!valintakoeOids.contains(valintakoe.getValintakoeOid())) {
                            // vain tarkasteltavista valintakokeista ollaan kiinnostuneita
                            continue;
                        }
                    } else {
                        if (!tunnisteet.contains(valintakoe.getValintakoeTunniste())) {
                            // vain tarkasteltavista valintakokeista ollaan kiinnostuneita
                            continue;
                        }
                    }
                    if (fi.vm.sade.valintalaskenta.domain.valintakoe.Osallistuminen.OSALLISTUU.equals(valintakoe.getOsallistuminenTulos().getOsallistuminen())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static OsallistujatPredicate vainOsallistujat(String hakukohdeOid, Collection<String> valintakoeOids) {
        return new OsallistujatPredicate(hakukohdeOid, valintakoeOids);
    }

    public static OsallistujatPredicate vainOsallistujatTunnisteella(String hakukohdeOid, Collection<String> tunnisteet) {
        return new OsallistujatPredicate(tunnisteet, hakukohdeOid);
    }
}
