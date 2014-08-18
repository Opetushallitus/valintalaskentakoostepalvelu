package fi.vm.sade.valinta.kooste.sijoittelu.komponentti;

import javax.annotation.Resource;

import fi.vm.sade.valinta.seuranta.resource.SijoittelunSeurantaResource;
import fi.vm.sade.valinta.seuranta.sijoittelu.dto.SijoitteluDto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 */
@Component("jatkuvaSijoittelu")
public class JatkuvaSijoittelu {

	private static final Logger LOG = LoggerFactory
			.getLogger(JatkuvaSijoittelu.class);

	@Autowired
	private SijoittelunSeurantaResource sijoittelunSeurantaResource;
	@Autowired
	private fi.vm.sade.valinta.kooste.sijoittelu.resource.SijoitteluResource sijoitteluResource;

	public void suorita() {
		LOG.debug("JATKUVA SIJOITTELU KÄYNNISTETTY");
		for (SijoitteluDto sijoittelu : sijoittelunSeurantaResource.hae()) {
			if (sijoittelu.isAjossa()) {
				LOG.debug("JATKUVA SIJOITTELU: {}", sijoittelu.getHakuOid());
				try {
					sijoitteluResource.sijoittele(sijoittelu.getHakuOid());
					sijoittelunSeurantaResource.merkkaaSijoittelunAjossaTila(
							sijoittelu.getHakuOid(), true);
					LOG.info("Viety sijoittelulle valinnan tulokset");
				} catch (Exception e) {
					LOG.error("JATKUVA SIJOITTELU", e);
					sijoittelunSeurantaResource.merkkaaSijoittelunAjossaVirhe(
							sijoittelu.getHakuOid(), e.getMessage());
				}
			}
		}
		LOG.debug("JATKUVA SIJOITTELU LOPETETTU");
	}
}
