package fi.vm.sade.valinta.kooste;

import org.springframework.stereotype.Component;

import fi.vm.sade.valinta.kooste.external.resource.haku.proxy.HakemusCacheInvalidator;
import fi.vm.sade.valinta.kooste.valintakokeet.komponentti.proxy.HakukohteenValintaperusteetCacheInvalidator;

@Component
public class CacheInvalidatorMock implements HakemusCacheInvalidator,
		HakukohteenValintaperusteetCacheInvalidator {

	@Override
	public void invalidateAll() {
	}
}
