package fi.vm.sade.valinta.kooste.kela.komponentti;

import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         indirectness to enable use of cache if needed
 */
public interface HakemusSource {

	Hakemus getHakemusByOid(String oid);
}
