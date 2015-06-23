package fi.vm.sade.valinta.kooste.viestintapalvelu.predicate;

import fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakutoiveenValintatapajonoDTO;

import java.util.Optional;
import java.util.function.Predicate;

public class SijoittelussaHyvaksyttyHakija implements Predicate<HakijaDTO> {
    private final String hakukohdeOid;

    public SijoittelussaHyvaksyttyHakija(String hakukohdeOid) {
        this.hakukohdeOid = hakukohdeOid;
    }

    @Override
    public boolean test(HakijaDTO input) {
        return Optional.ofNullable(input.getHakutoiveet())
                .map(hakutoiveet -> hakutoiveet.stream()
                        .filter(hakutoive -> this.hakukohdeOid.equals(hakutoive.getHakukohdeOid()))
                        .flatMap(hakutoive -> hakutoive.getHakutoiveenValintatapajonot().stream())
                        .map(HakutoiveenValintatapajonoDTO::getTila)
                        .anyMatch(HakemuksenTila::isHyvaksytty))
                .orElse(false);
    }
}
