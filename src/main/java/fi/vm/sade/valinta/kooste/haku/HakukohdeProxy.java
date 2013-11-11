package fi.vm.sade.valinta.kooste.haku;

import fi.vm.sade.valinta.kooste.external.resource.haku.dto.SuppeaHakemus;

import java.util.List;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         10 retries!
 */
public interface HakukohdeProxy {

    List<SuppeaHakemus> haeHakukohteenHakemukset(String hakukohdeOid);

}
