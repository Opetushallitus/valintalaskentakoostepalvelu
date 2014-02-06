package fi.vm.sade.valinta.kooste.valintakokeet.komponentti.proxy;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import fi.vm.sade.service.valintaperusteet.schema.ValintaperusteetTyyppi;

/**
 * User: wuoti Date: 5.8.2013 Time: 12.58
 */
public interface HakukohteenValintaperusteetProxy {
	List<ValintaperusteetTyyppi> haeValintaperusteet(Set<String> hakukohdeOids)
			throws ExecutionException;

	List<ValintaperusteetTyyppi> haeValintaperusteet(String hakukohdeOid)
			throws ExecutionException;
}
