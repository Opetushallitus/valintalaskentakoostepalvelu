package fi.vm.sade.valinta.kooste.sijoittelu.komponentti;

import fi.vm.sade.valinta.seuranta.resource.SijoittelunSeurantaResource;
import fi.vm.sade.valinta.seuranta.sijoittelu.dto.SijoitteluDto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

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
                Instant now = Instant.now();
                Instant viimeksiAjettu = sijoittelu.getViimeksiAjettu().toInstant();

                long minutesBetween = ChronoUnit.MINUTES.between(viimeksiAjettu, now);
				if (sijoittelu.isAjossa() &&
                        sijoittelu.getAjotiheys() != null && sijoittelu.getAloitusajankohta() != null &&
                        minutesBetween >= sijoittelu.getAjotiheys() &&
                        now.isAfter(sijoittelu.getAloitusajankohta().toInstant())) {
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
