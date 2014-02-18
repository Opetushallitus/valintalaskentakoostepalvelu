package fi.vm.sade.valinta.kooste;

import org.springframework.stereotype.Component;

import fi.vm.sade.valinta.kooste.external.resource.haku.proxy.HakemusCacheInvalidator;

@Component
public class HakemusCacheInvalidatorMock implements HakemusCacheInvalidator {

	@Override
	public void invalidateAll() {
	}
}
