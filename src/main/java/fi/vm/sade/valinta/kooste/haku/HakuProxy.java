package fi.vm.sade.valinta.kooste.haku;

import java.util.List;

import fi.vm.sade.valinta.kooste.external.resource.haku.dto.SuppeaHakemus;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         10 retries!
 */
public interface HakuProxy {

    List<SuppeaHakemus> haeHaunHakemukset(String hakuOid);

}
