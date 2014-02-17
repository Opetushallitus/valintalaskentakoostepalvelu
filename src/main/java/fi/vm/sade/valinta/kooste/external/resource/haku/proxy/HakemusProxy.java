package fi.vm.sade.valinta.kooste.external.resource.haku.proxy;

import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;

import java.util.concurrent.ExecutionException;

public interface HakemusProxy {
	Hakemus haeHakemus(String hakemusOid)
			throws ExecutionException;
}
