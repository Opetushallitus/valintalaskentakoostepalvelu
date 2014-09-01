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
		LOG.warn("Jatkuvasijoittelu kaynnistetty!");
		for (SijoitteluDto sijoittelu : sijoittelunSeurantaResource.hae()) {
			try {
				if (sijoittelu == null) {
					LOG.warn("Jatkuvassa sijoittelussa saatiin null olio seurantapalvelulta!");
					continue;
				}
				if (sijoittelu.isAjossa()) {
					LOG.warn(
							"Aloitetaan jatkuvasijoittelu ajossa olevalle haulle {}",
							sijoittelu.getHakuOid());

					sijoitteluResource.sijoittele(sijoittelu.getHakuOid());
					LOG.warn("Jatkuva sijoittelu saatiin tehtya haulle {}",
							sijoittelu.getHakuOid());
					sijoittelunSeurantaResource.merkkaaSijoittelunAjossaTila(
							sijoittelu.getHakuOid(), true);
					LOG.info("Sijoittelu haulle {} merkattu ajetuksi!",
							sijoittelu.getHakuOid());
				}
			} catch (Exception e) {
				LOG.error("Virhe jatkuvan sijoittelun aktivoinnissa {}",
						e.getMessage());
				try {
					sijoittelunSeurantaResource.merkkaaSijoittelunAjossaVirhe(
							sijoittelu.getHakuOid(), e.getMessage());
				} catch (Exception ee) {
				}
			}
		}
	}
}
