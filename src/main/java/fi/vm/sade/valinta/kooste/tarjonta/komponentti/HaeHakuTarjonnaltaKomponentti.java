package fi.vm.sade.valinta.kooste.tarjonta.komponentti;

import org.apache.camel.Property;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;

@Component("hakuTarjonnaltaKomponentti")
public class HaeHakuTarjonnaltaKomponentti {

	@Autowired
	private fi.vm.sade.valinta.kooste.external.resource.haku.HakuV1Resource hakuResource;

	public HakuV1RDTO getHaku(@Property("hakuOid") String hakuOid) {
		HakuV1RDTO haku = hakuResource.findByOid(hakuOid).getResult();
		return haku;
	}
}
